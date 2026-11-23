package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.gui.HudEditorScreen;
import com.blamejared.clumps.client.handlers.config.AutoConfigManager;
import com.blamejared.clumps.client.hud.HudElementLayout;
import com.blamejared.clumps.client.hud.HudElementType;
import com.blamejared.clumps.client.hud.HudIcon;
import com.blamejared.clumps.client.hud.HudLayout;
import com.blamejared.clumps.client.hud.HudRenderer;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.hud.HudElement;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.ui.UiSize;

public class AutoConfigHud extends HudElement {
    private static final String SEPARATOR = " · ";

    public static KeybindSetting unlockKey;
    public static BooleanSetting hideWhenIdle;

    private boolean unlockWasPressed;

    public AutoConfigHud() {
        super(new ModuleInfo(
                CatalogText.literal("Auto Config HUD"),
                CatalogText.literal("Shows active Auto Config kit and adds an unlock/re-scan keybind"),
                ModuleCategory.RENDER), HudElementType.AUTO_CONFIG);
    }

    @Override
    public void postInitialize() {
        unlockWasPressed = false;
        unlockKey.description(CatalogText.literal("Press in-game to unlock the current Auto Config kit and immediately re-scan your hotbar"));
        hideWhenIdle.description(CatalogText.literal("Hides the HUD pill when Auto Config is off or no kit is locked"));
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            unlockWasPressed = false;
            return;
        }
        InputKey key = unlockKey == null ? InputKey.NONE : unlockKey.getKey();
        boolean pressed = key != null && key != InputKey.NONE && key.isPressed();
        if (pressed && !unlockWasPressed) {
            AutoConfigManager manager = AutoConfigManager.get();
            if (manager != null) {
                manager.triggerUnlockRescan();
            }
        }
        unlockWasPressed = pressed;
    }

    private static boolean isPreviewActive() {
        return MinecraftRenderState.getScreen() instanceof HudEditorScreen;
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        AutoConfigManager manager = AutoConfigManager.get();
        boolean preview = isPreviewActive();
        if (!preview && hideWhenIdle != null && hideWhenIdle.getValue()
                && (manager == null || !manager.isEnabled() || (!manager.isKitLocked() && !manager.isServerOverrideActive()))) {
            return;
        }
        String primary = resolvePrimaryLabel(manager, preview);
        String secondary = resolveSecondaryLabel(manager, preview);

        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.AUTO_CONFIG.defaultX();
        float y = HudElementType.AUTO_CONFIG.defaultY();
        hud.drawPanel(layout, x, y, layout.getWidth(), 41.0f, 10.0f, 1.0f, 14.0f);
        layout.drawIcon(HudIcon.SHIELD_TICK, x + 12.0f, y + 13.5f, 14.0f, 14.0f, -1, 1.0f);

        float textX = x + 36.0f;
        layout.drawText(FontAsset.INTER_SEMI_BOLD, primary, textX, y + 13.0f, 13.0f, -1, 1.0f);
        if (!secondary.isEmpty()) {
            textX += layout.textWidth(FontAsset.INTER_SEMI_BOLD, primary, 13.0f);
            layout.drawText(FontAsset.INTER, SEPARATOR, textX, y + 13.0f, 12.0f, -1929379841, 1.0f);
            textX += layout.textWidth(FontAsset.INTER, SEPARATOR, 12.0f);
            layout.drawText(FontAsset.INTER, secondary, textX, y + 13.5f, 12.0f, -1090519041, 1.0f);
        }
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        AutoConfigManager manager = AutoConfigManager.get();
        boolean preview = isPreviewActive();
        String primary = resolvePrimaryLabel(manager, preview);
        String secondary = resolveSecondaryLabel(manager, preview);
        float width = 36.0f + layout.textWidth(FontAsset.INTER_SEMI_BOLD, primary, 13.0f) + 14.0f;
        if (!secondary.isEmpty()) {
            width += layout.textWidth(FontAsset.INTER, SEPARATOR, 12.0f)
                    + layout.textWidth(FontAsset.INTER, secondary, 12.0f);
        }
        return new UiSize(HudLayout.roundWidth(Math.max(HudElementType.AUTO_CONFIG.defaultWidth(), width)), 41.0f);
    }

    private static String resolvePrimaryLabel(AutoConfigManager manager, boolean preview) {
        if (preview) {
            return "Dia SMP";
        }
        if (manager == null || !manager.isEnabled()) {
            return "Auto Config";
        }
        if (manager.isServerOverrideActive()) {
            return "Server";
        }
        if (manager.isKitLocked()) {
            String kit = manager.getLockedKitLabel();
            return kit.isEmpty() ? "Locked" : kit;
        }
        return "Auto Config";
    }

    private static String resolveSecondaryLabel(AutoConfigManager manager, boolean preview) {
        if (preview) {
            return "diasmp";
        }
        if (manager == null || !manager.isEnabled()) {
            return "Off";
        }
        String activeCfg = manager.getActiveConfigName();
        if (!activeCfg.isEmpty()) {
            return activeCfg;
        }
        return "Scanning";
    }

    public static void appendBootstrapSettings(AutoConfigHud module) {
        CatalogSettings.append(module, unlockKey);
        CatalogSettings.append(module, hideWhenIdle);
    }

    public static void assignBootstrapSettings(
            AutoConfigHud module,
            KeybindSetting preparedUnlockKey,
            BooleanSetting preparedHideWhenIdle) {
        unlockKey = preparedUnlockKey;
        hideWhenIdle = preparedHideWhenIdle;
    }
}
