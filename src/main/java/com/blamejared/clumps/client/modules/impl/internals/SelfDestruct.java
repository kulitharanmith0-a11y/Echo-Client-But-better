package com.blamejared.clumps.client.modules.impl.internals;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.config.ModuleConfig;
import com.blamejared.clumps.client.config.NameRegistry;
import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.input.KeybindingEvent;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.gfx.FullscreenEffects;
import com.blamejared.clumps.client.gfx.HandChamsRenderer;
import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.texture.Texture;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import com.blamejared.clumps.client.gui.ScreenManager;
import com.blamejared.clumps.client.gui.dashboard.DashboardController;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.hud.HudRenderer;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.init.ClientBootstrap;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.keyboard.KeyStateOverrides;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.player.EntityPreviewRenderer;
import com.blamejared.clumps.client.render.player.PlayerPreviewRenderer;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.render.world.WorldRenderContext;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.TextSetting;
import com.blamejared.clumps.client.ui.HintRenderer;
import com.blamejared.clumps.client.utils.WeakRefSet;
import com.blamejared.clumps.client.utils.audio.AudioUtils;
import com.blamejared.clumps.client.utils.targeting.CachedPlayerTargets;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
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
        if (destructionStarted || ClumpsClient.eventsDisabled) {
            return;
        }
        destructionStarted = true;
        try {
            initializeClass(ModuleConfig.class);
            if (ClumpsClient.ModuleConfig != null) {
                ClumpsClient.ModuleConfig.save("_autosave");
            }
        } catch (Throwable ignored) {
        }
        ClumpsClient.eventsDisabled = true;
        try {
            ClumpsClient.hideExternalRendererOverlay();
        } catch (Throwable ignored) {
        }
        ScreenManager.focusedSetting = null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.send(this::finishSelfDestruct);
        } else {
            finishSelfDestruct();
        }
    }

    private void finishSelfDestruct() {
        try {
            com.blamejared.clumps.client.updater.AutoUpdater.abortAndCleanup();
        } catch (Throwable ignored) {
        }
        String protectedUsername = resolveSessionUsername();

        Set<String> preCollectedStrings = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            NameRegistry.collectAssignedNames(preCollectedStrings);
        } catch (Throwable ignored) {
        }
        try {
            HudRenderer.INSTANCE.collectKeybindIds(preCollectedStrings);
        } catch (Throwable ignored) {
        }

        List<Class<?>> seedClasses = List.of(
                ClumpsClient.class,
                ClientBootstrap.class,
                NameRegistry.class,
                DashboardController.class,
                HudRenderer.class,
                ScreenManager.class,
                SelfDestruct.class,
                com.blamejared.clumps.client.updater.AutoUpdater.class,
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

        ClientStringCleanup.CleanupPlan cleanupPlan = null;
        try {
            ClientStringCleanup cleanup = new ClientStringCleanup(ClumpsClient.class);
            cleanupPlan = cleanup.collectPlan(seedClasses, seedObjects, preCollectedStrings);
        } catch (Throwable ignored) {
        }

        if (replaceMods != null && replaceMods.getValue()) {
            try {
                replaceModFiles();
            } catch (Throwable ignored) {
            }
        }

        closeUiAndScreens();
        shutdownManagersAndModules();
        releaseGpuAndAudioResources();
        clearEventAndInputCaches();

        if (ClumpsClient.ModuleConfig != null) {
            try {
                ClumpsClient.ModuleConfig.close();
            } catch (Throwable ignored) {
            }
            ClumpsClient.ModuleConfig = null;
        }
        try {
            HudRenderer.INSTANCE.resetTransientState();
        } catch (Throwable ignored) {
        }
        try {
            NameRegistry.clearAssignments();
        } catch (Throwable ignored) {
        }

        if (cleanupPlan != null) {
            for (ClientCleanupAction action : cleanupPlan.actions()) {
                try {
                    action.clear();
                } catch (Throwable ignored) {
                }
            }
            cleanupPlan.actions().clear();
            try {
                ClientStringCleanup.clearCollectedStrings(
                        cleanupPlan.heapStrings(), cleanupPlan.literalStrings(), protectedUsername);
            } catch (Throwable ignored) {
            }
            cleanupPlan.heapStrings().clear();
            cleanupPlan.literalStrings().clear();
        }
        clearSystemProperties();
        preCollectedStrings.clear();

        replaceMods = null;
        url = null;
        ClumpsClient.instance = null;
        ClumpsClient.frameNumber = 0L;
    }

    private List<Object> collectSeedObjects() {
        List<Object> roots = new ArrayList<>();
        roots.add(this);
        roots.add(DashboardController.INSTANCE);
        roots.add(HudRenderer.INSTANCE);
        addIfNonNull(roots, ClumpsClient.instance);
        addIfNonNull(roots, ClumpsClient.ModuleManager);
        addIfNonNull(roots, ClumpsClient.ModuleConfig);
        addIfNonNull(roots, ClumpsClient.friendManager);
        addIfNonNull(roots, ClumpsClient.screenManager);
        addIfNonNull(roots, ClumpsClient.handlerManager);
        addIfNonNull(roots, ClumpsClient.ChunkController);
        addIfNonNull(roots, ClumpsClient.ChunkScanManager);
        addIfNonNull(roots, ClumpsClient.particleManager);
        addIfNonNull(roots, ClumpsClient.worldRenderer);
        addIfNonNull(roots, ClumpsClient.uiRenderer);
        addIfNonNull(roots, ClumpsClient.scriptEngine);
        addIfNonNull(roots, ClumpsClient.scriptManager);
        addIfNonNull(roots, replaceMods);
        addIfNonNull(roots, url);
        return roots;
    }

    private void closeUiAndScreens() {
        ScreenManager.focusedSetting = null;
        if (ClumpsClient.screenManager != null) {
            try {
                ClumpsClient.screenManager.close();
                ClumpsClient.screenManager.destroy();
            } catch (Throwable ignored) {
            }
            ClumpsClient.screenManager = null;
        }
    }

    private void shutdownManagersAndModules() {
        if (ClumpsClient.ModuleManager != null) {
            try {
                ClumpsClient.ModuleManager.shutdownExcept(this);
            } catch (Throwable ignored) {
            }
            ClumpsClient.ModuleManager = null;
        }
        if (ClumpsClient.handlerManager != null) {
            try {
                ClumpsClient.handlerManager.shutdown();
            } catch (Throwable ignored) {
            }
            ClumpsClient.handlerManager = null;
        }
        try {
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
        } catch (Throwable ignored) {
        }
        if (ClumpsClient.friendManager != null) {
            try {
                ClumpsClient.friendManager.clear();
            } catch (Throwable ignored) {
            }
            ClumpsClient.friendManager = null;
        }
        if (ClumpsClient.ChunkController != null) {
            try {
                ClumpsClient.ChunkController.shutdown();
                ClumpsClient.ChunkController.setListener(null);
            } catch (Throwable ignored) {
            }
            ClumpsClient.ChunkController = null;
        }
        if (ClumpsClient.ChunkScanManager != null) {
            try {
                ClumpsClient.ChunkScanManager.shutdown();
            } catch (Throwable ignored) {
            }
            ClumpsClient.ChunkScanManager = null;
        }
        if (ClumpsClient.particleManager != null) {
            try {
                ClumpsClient.particleManager.clear();
                ClumpsClient.particleManager.setTexture(null);
            } catch (Throwable ignored) {
            }
            ClumpsClient.particleManager = null;
        }
        if (ClumpsClient.scriptManager != null) {
            try {
                ClumpsClient.scriptManager.loadedScripts().clear();
            } catch (Throwable ignored) {
            }
            ClumpsClient.scriptManager = null;
        }
        ClumpsClient.scriptEngine = null;
    }

    private void releaseGpuAndAudioResources() {
        try { ClumpsClient.shutdownExternalRenderer(); } catch (Throwable ignored) {}
        try { AudioUtils.shutdown(); } catch (Throwable ignored) {}
        try { FullscreenEffects.releaseResources(); } catch (Throwable ignored) {}
        try { HandChamsRenderer.reset(); } catch (Throwable ignored) {}
        try { HintRenderer.reset(); } catch (Throwable ignored) {}
        try { HudRenderer.INSTANCE.releaseRendererResources(); } catch (Throwable ignored) {}

        StringMemoryAccess memory = StringMemoryAccess.create();
        try { releaseDashboardResources(memory); } catch (Throwable ignored) {}
        try { releaseEntityPreviewRenderer(memory); } catch (Throwable ignored) {}

        if (ClumpsClient.uiRenderer != null) {
            UiRenderer ui = ClumpsClient.uiRenderer;
            ClumpsClient.uiRenderer = null;
            try { releaseUiRendererExtras(ui, memory); } catch (Throwable ignored) {}
            try { ui.clearFrameQueues(); } catch (Throwable ignored) {}
            try { ui.destroy(); } catch (Throwable ignored) {}
        }
        if (ClumpsClient.worldRenderer != null) {
            WorldRenderContext world = ClumpsClient.worldRenderer;
            ClumpsClient.worldRenderer = null;
            try { world.clearExternalOverlays(); } catch (Throwable ignored) {}
            try { world.destroy(); } catch (Throwable ignored) {}
            try { releaseWorldRendererExtras(world, ClumpsClient.renderDevice, memory); } catch (Throwable ignored) {}
        }
        ClumpsClient.commandEncoder = null;
        ClumpsClient.renderDevice = null;
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
                RenderDevice device = ClumpsClient.renderDevice;
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
            } else if (resource instanceof com.blamejared.clumps.client.gfx.buffer.Buffer buffer) {
                device.release(buffer);
                memory.clearReference(field, owner);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void clearEventAndInputCaches() {
        try { EventBus.clear(); } catch (Throwable ignored) {}
        try { DeferredActionQueue.clear(); } catch (Throwable ignored) {}
        try { KeybindingCache.clearOverrides(); } catch (Throwable ignored) {}
        try { KeyStateOverrides.clearAll(); } catch (Throwable ignored) {}
        try { TargetSelection.clearTargets(); } catch (Throwable ignored) {}
        try { KeybindingEvent.setInputSentinel(null); } catch (Throwable ignored) {}
        try { EntityAttackEvent.setAttackSentinel(null); } catch (Throwable ignored) {}
    }

    private static void clearSystemProperties() {
        try {
            System.clearProperty("ui.motionMultiplier");
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
