package dev.dov.hack.echo.client;

import dev.dov.hack.echo.client.chunk.ChunkScanManager;
import dev.dov.hack.echo.client.chunk.ChunkController;
import dev.dov.hack.echo.client.config.ModuleConfig;
import dev.dov.hack.echo.client.event.EventBus;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.lifecycle.FrameStartEvent;
import dev.dov.hack.echo.client.modules.ModuleManager;
import dev.dov.hack.echo.client.friends.FriendManager;
import dev.dov.hack.echo.client.handlers.HandlerManager;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.dov.hack.echo.client.particle.ParticleManager;
import dev.dov.hack.echo.client.render.bridge.GlFramebufferBridge;
import dev.dov.hack.echo.client.render.RenderBackendManager;
import dev.dov.hack.echo.client.render.bridge.RendererPlatformBridge;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.render.world.WorldRenderContext;
import dev.dov.hack.echo.client.gui.ScreenManager;
import dev.dov.hack.echo.client.gui.dashboard.DashboardController;
import dev.dov.hack.echo.client.runtime.ScriptEngine;
import dev.dov.hack.echo.client.runtime.ScriptManager;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.opengl.OpenGlDevice;
import dev.dov.hack.echo.client.platform.Platform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

public class EchoClient implements ModInitializer, SimpleSynchronousResourceReloadListener {
    public static final String ID;
    public static EchoClient instance;
    public static ModuleManager ModuleManager;
    public static FriendManager friendManager;
    public static ScreenManager screenManager;
    public static ModuleConfig ModuleConfig;
    public static HandlerManager handlerManager;
    public static ChunkScanManager ChunkScanManager;

    
    public static volatile boolean eventsDisabled;

    public static long frameNumber;
    public static WorldRenderContext worldRenderer;
    public static ChunkController ChunkController;
    public static ParticleManager particleManager;
    public static RenderDevice renderDevice;
    public static CommandEncoder commandEncoder;
    public static UiRenderer uiRenderer;
    public static RenderBackendManager externalRenderer;
    public static boolean externalRendererActive;
    public static boolean externalRendererRequested;
    private static boolean inRenderFrame;
    public static ScriptEngine scriptEngine;
    public static ScriptManager scriptManager;
    public static final Logger LOGGER;

    static {
        ID = "clumps";
        LOGGER = NOPLogger.NOP_LOGGER;
    }

    
    @Override
    public void onInitialize() {
        instance = this;
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(this);
        EventBus.register(this);
        EventBus.register(DashboardController.INSTANCE);
    }

    
    @Subscribe
    public void onFrameStart(FrameStartEvent event) {
        if (eventsDisabled) {
            return;
        }
        initializeReferencedClass(RenderDevice.class);
        if (renderDevice != null) {
            frameNumber++;
            renderDevice.beginFrame();
            commandEncoder = renderDevice.commandEncoder();
        }
    }

    
    public static boolean ensureRenderResources() {
        if (eventsDisabled) {
            return false;
        }
        initializeReferencedClass(UiRenderer.class);
        initializeReferencedClass(RenderDevice.class);
        if (renderDevice == null) {
            renderDevice = createDefaultRenderDevice();
        }
        if (renderDevice == null) {
            return false;
        }
        if (commandEncoder == null) {
            commandEncoder = renderDevice.commandEncoder();
        }
        if (uiRenderer == null) {
            uiRenderer = new UiRenderer(renderDevice);
            worldRenderer = new WorldRenderContext(renderDevice);
            DashboardController.INSTANCE.initialize(renderDevice);
            HudRenderer.INSTANCE.initialize(renderDevice);
            EventBus.register(HudRenderer.INSTANCE);
        }
        return uiRenderer != null;
    }

    
    private static RenderDevice createDefaultRenderDevice() {
        RendererPlatformBridge platform = new RendererPlatformBridge();
        return new OpenGlDevice(new GlFramebufferBridge(), platform, OpenGlDevice.queryCapabilities(false));
    }

    
    @Override
    public void reload(ResourceManager resources) {
        if (eventsDisabled) {
            return;
        }
        boolean firstInitialization = uiRenderer == null;
        ensureRenderResources();
        if (!firstInitialization) {
            return;
        }
        initializeReferencedClass(ParticleManager.class);
        if (particleManager == null) {
            return;
        }
        initializeReferencedClass(ModuleManager.class);
        if (ModuleManager == null) {
            return;
        }
        initializeReferencedClass(ModuleConfig.class);
        if (ModuleConfig == null || !ModuleConfig.loading) {
            return;
        }
        particleManager.initialize();
        ModuleConfig.load("_autosave");
        ModuleConfig.loading = false;
    }

    
    @Override
    public Identifier getFabricId() {
        return Identifier.of("clumps", "clumps");
    }

    public static void setExternalRendererEnabled(boolean enabled) {
        if (eventsDisabled) {
            externalRendererRequested = false;
            if (!inRenderFrame) {
                shutdownExternalRenderer();
            }
            return;
        }
        externalRendererRequested = enabled;
        if (!inRenderFrame) {
            syncExternalRenderer();
        }
    }

    public static void beginRenderFrame() {
        if (eventsDisabled) {
            shutdownExternalRenderer();
            return;
        }
        syncExternalRenderer();
        inRenderFrame = true;
    }

    public static void endRenderFrame() {
        inRenderFrame = false;
        if (eventsDisabled) {
            shutdownExternalRenderer();
            return;
        }
        if (externalRenderer != null) {
            externalRenderer.endFrame();
        }
        syncExternalRenderer();
    }

    public static void hideExternalRendererOverlay() {
        if (externalRenderer != null) {
            externalRenderer.hideOverlayImmediately();
        }
    }

    public static void failStreamProof() {
        shutdownExternalRenderer();
        dev.dov.hack.echo.client.modules.internals.ClickGui.syncStreamProofFailure();
    }

    public static void shutdownExternalRenderer() {
        boolean wasActive = externalRendererActive || externalRenderer != null;
        externalRendererRequested = false;
        externalRendererActive = false;
        inRenderFrame = false;
        if (externalRenderer != null) {
            try {
                externalRenderer.close();
            } catch (Throwable ignored) {
            } finally {
                externalRenderer = null;
            }
        }
        if (uiRenderer != null) {
            uiRenderer.clearFrameQueues();
        }
        if (worldRenderer != null) {
            worldRenderer.clearExternalOverlays();
        }
        if (wasActive) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null && client.getWindow().getHandle() != 0L
                    && client.getWindow().isFullscreen()) {
                try {
                    ((Platform.DisplayControl) (Object) client.getWindow()).invalidateFullscreenState();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public static void syncExternalRenderer() {
        if (eventsDisabled) {
            shutdownExternalRenderer();
            return;
        }
        initializeReferencedClass(RenderBackendManager.class);
        boolean wasActive = externalRendererActive;
        MinecraftClient client = MinecraftClient.getInstance();
        if (externalRendererRequested) {
            if (client == null || client.getWindow() == null || !ensureRenderResources()) {
                externalRendererActive = false;
                return;
            }
            if (externalRenderer == null) {
                try {
                    externalRenderer = new RenderBackendManager();
                } catch (Throwable failure) {
                    externalRendererRequested = false;
                    externalRendererActive = false;
                    dev.dov.hack.echo.client.modules.internals.ClickGui.syncStreamProofFailure();
                }
            }
            externalRendererActive = externalRenderer != null;
        } else {
            externalRendererActive = false;
            if (externalRenderer != null) {
                try {
                    externalRenderer.close();
                } catch (Throwable ignored) {
                } finally {
                    externalRenderer = null;
                }
                initializeReferencedClass(UiRenderer.class);
                if (uiRenderer != null) {
                    uiRenderer.clearFrameQueues();
                }
                if (worldRenderer != null) {
                    worldRenderer.clearExternalOverlays();
                }
            }
        }
        if (wasActive != externalRendererActive && client != null && client.getWindow() != null
                && (client.getWindow().isFullscreen()
                || (client.getWindow().getHandle() != 0L && org.lwjgl.glfw.GLFW.glfwGetWindowMonitor(client.getWindow().getHandle()) != 0L))) {
            ((Platform.DisplayControl) (Object) client.getWindow()).invalidateFullscreenState();
        }
    }

    
    private static void initializeReferencedClass(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }
}
