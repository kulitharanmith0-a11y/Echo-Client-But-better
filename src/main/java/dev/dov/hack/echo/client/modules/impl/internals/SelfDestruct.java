package dev.dov.hack.echo.client.modules.impl.internals;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.config.ModuleConfig;
import dev.dov.hack.echo.client.config.NameRegistry;
import dev.dov.hack.echo.client.event.EventBus;
import dev.dov.hack.echo.client.event.input.KeybindingEvent;
import dev.dov.hack.echo.client.event.interaction.EntityAttackEvent;
import dev.dov.hack.echo.client.gfx.FullscreenEffects;
import dev.dov.hack.echo.client.gfx.HandChamsRenderer;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import dev.dov.hack.echo.client.gui.ScreenManager;
import dev.dov.hack.echo.client.gui.dashboard.DashboardController;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.init.ClientBootstrap;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.keyboard.KeyStateOverrides;
import dev.dov.hack.echo.client.keyboard.KeybindingCache;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.player.EntityPreviewRenderer;
import dev.dov.hack.echo.client.render.player.PlayerPreviewRenderer;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.render.world.WorldRenderContext;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.TextSetting;
import dev.dov.hack.echo.client.ui.HintRenderer;
import dev.dov.hack.echo.client.utils.WeakRefSet;
import dev.dov.hack.echo.client.utils.audio.AudioUtils;
import dev.dov.hack.echo.client.utils.targeting.CachedPlayerTargets;
import dev.dov.hack.echo.client.utils.targeting.TargetSelection;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;

public class SelfDestruct extends Module {
    private boolean destructionStarted;
    private static BooleanSetting replaceMods;
    private static TextSetting url;

    public SelfDestruct() {
        super(new ModuleInfo(CatalogText.literal("Self Destruct"), CatalogText.literal("Disables and unloads module state"), ModuleCategory.INTERNALS));
    }

    @Override
    public void postInitialize() {
        url.setVisibilityPredicate(ignored -> replaceMods.getValue());
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public void onEnable() {
        if (destructionStarted || EchoClient.eventsDisabled) {
            return;
        }
        initializeClass(ModuleConfig.class);
        if (EchoClient.ModuleConfig != null) {
            try {
                EchoClient.ModuleConfig.save("_autosave");
            } catch (RuntimeException ignored) {
            }
        }
        destructionStarted = true;
        EchoClient.eventsDisabled = true;
        EchoClient.hideExternalRendererOverlay();
        initializeClass(ScreenManager.class);
        if (EchoClient.screenManager != null) {
            EchoClient.screenManager.close();
        }
        initializeClass(MinecraftClient.class);
        if (client != null) {
            client.send(this::finishSelfDestruct);
        } else {
            finishSelfDestruct();
        }
    }

    private void finishSelfDestruct() {
        String protectedUsername = resolveSessionUsername();

        Set<String> preCollectedStrings = Collections.newSetFromMap(new IdentityHashMap<>());
        NameRegistry.collectAssignedNames(preCollectedStrings);
        HudRenderer.INSTANCE.collectKeybindIds(preCollectedStrings);

        List<Class<?>> seedClasses = List.of(
                EchoClient.class,
                ClientBootstrap.class,
                NameRegistry.class,
                DashboardController.class,
                HudRenderer.class,
                ScreenManager.class,
                SelfDestruct.class,
                KeybindingCache.class,
                KeyStateOverrides.class,
                KeybindingEvent.class,
                EntityAttackEvent.class,
                DeferredActionQueue.class,
                TargetSelection.class,
                CachedPlayerTargets.class,
                HandChamsRenderer.class,
                FullscreenEffects.class,
                EntityPreviewRenderer.class,
                PlayerPreviewRenderer.class,
                HintRenderer.class,
                WeakRefSet.class);
        List<Object> seedObjects = collectSeedObjects();

        ClientStringCleanup cleanup = new ClientStringCleanup(EchoClient.class);
        ClientStringCleanup.CleanupPlan cleanupPlan = cleanup.collectPlan(seedClasses, seedObjects, preCollectedStrings);

        if (replaceMods != null && replaceMods.getValue()) {
            replaceModFiles();
        }

        closeUiAndScreens();
        shutdownManagersAndModules();
        releaseGpuAndAudioResources();
        clearEventAndInputCaches();

        if (EchoClient.ModuleConfig != null) {
            try {
                EchoClient.ModuleConfig.close();
            } catch (RuntimeException ignored) {
            }
            EchoClient.ModuleConfig = null;
        }
        HudRenderer.INSTANCE.resetTransientState();
        NameRegistry.clearAssignments();

        for (ClientCleanupAction action : cleanupPlan.actions()) {
            try {
                action.clear();
            } catch (RuntimeException ignored) {
            }
        }
        cleanupPlan.actions().clear();

        clearSystemProperties();
        ClientStringCleanup.clearCollectedStrings(
                cleanupPlan.heapStrings(), cleanupPlan.literalStrings(), protectedUsername);
        cleanupPlan.heapStrings().clear();
        cleanupPlan.literalStrings().clear();
        preCollectedStrings.clear();

        replaceMods = null;
        url = null;
        EchoClient.instance = null;
        EchoClient.frameNumber = 0L;
        System.gc();
    }

    private List<Object> collectSeedObjects() {
        List<Object> roots = new ArrayList<>();
        roots.add(this);
        roots.add(DashboardController.INSTANCE);
        roots.add(HudRenderer.INSTANCE);
        addIfNonNull(roots, EchoClient.instance);
        addIfNonNull(roots, EchoClient.ModuleManager);
        addIfNonNull(roots, EchoClient.ModuleConfig);
        addIfNonNull(roots, EchoClient.friendManager);
        addIfNonNull(roots, EchoClient.screenManager);
        addIfNonNull(roots, EchoClient.handlerManager);
        addIfNonNull(roots, EchoClient.ChunkController);
        addIfNonNull(roots, EchoClient.ChunkScanManager);
        addIfNonNull(roots, EchoClient.particleManager);
        addIfNonNull(roots, EchoClient.worldRenderer);
        addIfNonNull(roots, EchoClient.uiRenderer);
        addIfNonNull(roots, EchoClient.scriptEngine);
        addIfNonNull(roots, EchoClient.scriptManager);
        addIfNonNull(roots, replaceMods);
        addIfNonNull(roots, url);
        return roots;
    }

    private void closeUiAndScreens() {
        ScreenManager.focusedSetting = null;
        if (EchoClient.screenManager != null) {
            try {
                EchoClient.screenManager.close();
                EchoClient.screenManager.destroy();
            } catch (RuntimeException ignored) {
            }
            EchoClient.screenManager = null;
        }
    }

    private void shutdownManagersAndModules() {
        if (EchoClient.handlerManager != null) {
            try {
                EchoClient.handlerManager.shutdown();
            } catch (RuntimeException ignored) {
            }
            EchoClient.handlerManager = null;
        }
        if (EchoClient.ModuleManager != null) {
            try {
                EchoClient.ModuleManager.shutdownExcept(this);
            } catch (RuntimeException ignored) {
            }
            EchoClient.ModuleManager = null;
        }
        EventBus.unregister(this);
        setEnabled(false);
        setKey(InputKey.NONE);
        setVisible(false);
        setHoldMode(false);
        settings.clear();
        if (url != null) {
            url.setText("");
            url.setPlaceholder("");
            url.clearEntries();
            url.description("");
            url.tooltip("");
        }
        if (EchoClient.friendManager != null) {
            EchoClient.friendManager.clear();
            EchoClient.friendManager = null;
        }
        if (EchoClient.ChunkController != null) {
            EchoClient.ChunkController.shutdown();
            EchoClient.ChunkController.setListener(null);
            EchoClient.ChunkController = null;
        }
        if (EchoClient.ChunkScanManager != null) {
            EchoClient.ChunkScanManager.shutdown();
            EchoClient.ChunkScanManager = null;
        }
        if (EchoClient.particleManager != null) {
            EchoClient.particleManager.clear();
            EchoClient.particleManager.setTexture(null);
            EchoClient.particleManager = null;
        }
        if (EchoClient.scriptManager != null) {
            EchoClient.scriptManager.loadedScripts().clear();
            EchoClient.scriptManager = null;
        }
        EchoClient.scriptEngine = null;
    }

    private void releaseGpuAndAudioResources() {
        EchoClient.shutdownExternalRenderer();
        AudioUtils.shutdown();
        FullscreenEffects.releaseResources();
        HandChamsRenderer.reset();
        HintRenderer.reset();
        HudRenderer.INSTANCE.releaseRendererResources();

        StringMemoryAccess memory = StringMemoryAccess.create();
        releaseDashboardResources(memory);
        releaseEntityPreviewRenderer(memory);

        if (EchoClient.uiRenderer != null) {
            releaseUiRendererExtras(EchoClient.uiRenderer, memory);
            EchoClient.uiRenderer.clearFrameQueues();
            EchoClient.uiRenderer.destroy();
            EchoClient.uiRenderer = null;
        }
        if (EchoClient.worldRenderer != null) {
            EchoClient.worldRenderer.clearExternalOverlays();
            EchoClient.worldRenderer.destroy();
            releaseWorldRendererExtras(EchoClient.worldRenderer, EchoClient.renderDevice, memory);
            EchoClient.worldRenderer = null;
        }
        EchoClient.commandEncoder = null;
        EchoClient.renderDevice = null;
    }

    private static void releaseDashboardResources(StringMemoryAccess memory) {
        if (memory == null) {
            return;
        }
        try {
            Field svgField = DashboardController.class.getDeclaredField("svgTextures");
            Object svgCache = memory.readObject(svgField, DashboardController.INSTANCE);
            if (svgCache instanceof AutoCloseable closeable) {
                closeable.close();
            }
            memory.clearReference(svgField, DashboardController.INSTANCE);
        } catch (Throwable ignored) {
        }
        try {
            Field iconsField = DashboardController.class.getDeclaredField("serverIcons");
            Object rawIcons = memory.readObject(iconsField, DashboardController.INSTANCE);
            if (rawIcons instanceof Map<?, ?> icons) {
                RenderDevice device = EchoClient.renderDevice;
                if (device != null) {
                    for (Object value : icons.values()) {
                        if (value instanceof TextureView view && view.texture != null) {
                            device.release(view.texture);
                        }
                    }
                }
                icons.clear();
            }
        } catch (Throwable ignored) {
        }
    }

    private static void releaseEntityPreviewRenderer(StringMemoryAccess memory) {
        if (memory == null) {
            return;
        }
        try {
            Field rendererField = EntityPreviewRenderer.class.getDeclaredField("renderer");
            Object renderer = memory.readObject(rendererField, null);
            if (renderer instanceof AutoCloseable closeable) {
                closeable.close();
            }
            memory.clearReference(rendererField, null);
        } catch (Throwable ignored) {
        }
    }

    private static void releaseUiRendererExtras(UiRenderer uiRenderer, StringMemoryAccess memory) {
        if (memory == null) {
            return;
        }
        try {
            Field itemsField = UiRenderer.class.getDeclaredField("items");
            Object items = memory.readObject(itemsField, uiRenderer);
            if (items != null) {
                items.getClass().getMethod("clear").invoke(items);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void releaseWorldRendererExtras(WorldRenderContext worldRenderer, RenderDevice device, StringMemoryAccess memory) {
        if (memory == null || device == null) {
            return;
        }
        releaseDeviceResourceField(worldRenderer, "worldColorTexture", device, memory);
        releaseDeviceResourceField(worldRenderer, "cameraUniforms", device, memory);
        releaseDeviceResourceField(worldRenderer, "compositionUniforms", device, memory);
    }

    private static void releaseDeviceResourceField(Object owner, String fieldName, RenderDevice device, StringMemoryAccess memory) {
        try {
            Field field = owner.getClass().getDeclaredField(fieldName);
            Object resource = memory.readObject(field, owner);
            if (resource instanceof Texture texture) {
                device.release(texture);
                memory.clearReference(field, owner);
            } else if (resource instanceof dev.dov.hack.echo.client.gfx.buffer.Buffer buffer) {
                device.release(buffer);
                memory.clearReference(field, owner);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void clearEventAndInputCaches() {
        EventBus.clear();
        DeferredActionQueue.clear();
        KeybindingCache.clearOverrides();
        TargetSelection.clearTargets();
        KeybindingEvent.setInputSentinel(null);
        EntityAttackEvent.setAttackSentinel(null);
    }

    private static void clearSystemProperties() {
        try {
            System.clearProperty("renderbase.echo.motionMultiplier");
        } catch (SecurityException ignored) {
        }
    }

    private static String resolveSessionUsername() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            return mc != null && mc.getSession() != null ? mc.getSession().getUsername() : null;
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private static void addIfNonNull(List<Object> list, Object value) {
        if (value != null) {
            list.add(value);
        }
    }

    private void replaceModFiles() {
        return;
    }

    private static void initializeClass(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    public static void appendBootstrapSettings(SelfDestruct Module) {
        CatalogSettings.append(Module, replaceMods);
        CatalogSettings.append(Module, url);
    }

    public static void assignBootstrapSettings(SelfDestruct Module, BooleanSetting preparedReplaceMods, TextSetting preparedUrl) {
        replaceMods = preparedReplaceMods;
        url = preparedUrl;
    }
}
