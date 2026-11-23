package com.blamejared.clumps.client;

import com.blamejared.clumps.client.chunk.ChunkScanManager;
import com.blamejared.clumps.client.chunk.ChunkController;
import com.blamejared.clumps.client.config.ModuleConfig;
import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.FrameStartEvent;
import com.blamejared.clumps.client.modules.ModuleManager;
import com.blamejared.clumps.client.friends.FriendManager;
import com.blamejared.clumps.client.handlers.HandlerManager;
import com.blamejared.clumps.client.hud.HudRenderer;
import com.blamejared.clumps.client.particle.ParticleManager;
import com.blamejared.clumps.client.render.bridge.GlFramebufferBridge;
import com.blamejared.clumps.client.render.RenderBackendManager;
import com.blamejared.clumps.client.render.bridge.RendererPlatformBridge;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.render.world.WorldRenderContext;
import com.blamejared.clumps.client.gui.ScreenManager;
import com.blamejared.clumps.client.gui.dashboard.DashboardController;
import com.blamejared.clumps.client.runtime.ScriptEngine;
import com.blamejared.clumps.client.runtime.ScriptManager;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.opengl.OpenGlDevice;
import com.blamejared.clumps.client.platform.Platform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

public class ClumpsClient implements ModInitializer, SimpleSynchronousResourceReloadListener {
    public static final String ID;
    public static ClumpsClient instance;
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
        try {
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
            }
            if (worldRenderer == null) {
                try {
                    worldRenderer = new WorldRenderContext(renderDevice);
                } catch (Throwable ignored) {
                }
            }
            try {
                DashboardController.INSTANCE.initialize(renderDevice);
            } catch (Throwable ignored) {
            }
            try {
                HudRenderer.INSTANCE.initialize(renderDevice);
                EventBus.register(HudRenderer.INSTANCE);
            } catch (Throwable ignored) {
            }
            return uiRenderer != null && commandEncoder != null;
        } catch (Throwable ignored) {
            return false;
        }
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

    public static boolean shouldHideOsCursorForStreamProof() {
        if (eventsDisabled || !externalRendererActive || externalRenderer == null) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.currentScreen instanceof com.blamejared.clumps.client.gui.ClientScreen;
    }

    public static void failStreamProof() {
        shutdownExternalRenderer();
        com.blamejared.clumps.client.modules.internals.ClickGui.syncStreamProofFailure();
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
                    com.blamejared.clumps.client.modules.internals.ClickGui.syncStreamProofFailure();
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
