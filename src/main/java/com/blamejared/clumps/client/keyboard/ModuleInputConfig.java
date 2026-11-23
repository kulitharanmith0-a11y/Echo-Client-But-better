package com.blamejared.clumps.client.keyboard;

import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.settings.Setting;
import java.util.ArrayList;
import java.util.List;

public final class ModuleInputConfig {
    private static final String SECTION = "Input Simulation";

    private final BooleanSetting enabled;
    private final BooleanSetting customKeybinds;
    private final KeybindSetting attackKey;
    private final KeybindSetting useKey;
    private final KeybindSetting offhandKey;
    private final KeybindSetting[] slotKeys = new KeybindSetting[9];
    private final List<Setting> allSettings = new ArrayList<>(14);

    public ModuleInputConfig() {
        enabled = new BooleanSetting(CatalogText.literal("Input Simulation"), true);
        customKeybinds = new BooleanSetting(CatalogText.literal("Custom Keybinds"), false);
        attackKey = new KeybindSetting(CatalogText.literal("Sim Attack Key"), InputKey.NONE);
        useKey = new KeybindSetting(CatalogText.literal("Sim Use Key"), InputKey.NONE);
        offhandKey = new KeybindSetting(CatalogText.literal("Sim Offhand Key"), InputKey.NONE);

        enabled.description(CatalogText.literal("Simulate your in-game keybinds and clicks whenever this module acts"));
        customKeybinds.description(CatalogText.literal("Override this module's simulated keys (NONE automatically uses your live Minecraft controls)"));
        attackKey.description(CatalogText.literal("Custom attack key for this module (NONE = Minecraft Attack key)"));
        useKey.description(CatalogText.literal("Custom use/place key for this module (NONE = Minecraft Use key)"));
        offhandKey.description(CatalogText.literal("Custom swap-offhand key for this module (NONE = Minecraft Swap Hands key)"));

        customKeybinds.setVisibilityPredicate(ignored -> enabled.getValue());
        attackKey.setVisibilityPredicate(ignored -> enabled.getValue() && customKeybinds.getValue());
        useKey.setVisibilityPredicate(ignored -> enabled.getValue() && customKeybinds.getValue());
        offhandKey.setVisibilityPredicate(ignored -> enabled.getValue() && customKeybinds.getValue());

        enabled.tooltip(SECTION);
        customKeybinds.tooltip(SECTION);
        attackKey.tooltip(SECTION);
        useKey.tooltip(SECTION);
        offhandKey.tooltip(SECTION);

        allSettings.add(enabled);
        allSettings.add(customKeybinds);
        allSettings.add(attackKey);
        allSettings.add(useKey);
        allSettings.add(offhandKey);

        for (int i = 0; i < 9; i++) {
            int slotNumber = i + 1;
            KeybindSetting setting = new KeybindSetting(CatalogText.literal("Sim Slot " + slotNumber), InputKey.NONE);
            setting.description(CatalogText.literal("Custom keybind for Hotbar Slot " + slotNumber + " (NONE = Minecraft Slot " + slotNumber + " key)"));
            setting.setVisibilityPredicate(ignored -> enabled.getValue() && customKeybinds.getValue());
            setting.tooltip(SECTION);
            slotKeys[i] = setting;
            allSettings.add(setting);
        }
    }

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public boolean usesCustomKeybinds() {
        return customKeybinds.getValue();
    }

    public InputKey customAttackKey() {
        return customKeybinds.getValue() ? attackKey.getKey() : InputKey.NONE;
    }

    public InputKey customUseKey() {
        return customKeybinds.getValue() ? useKey.getKey() : InputKey.NONE;
    }

    public InputKey customOffhandKey() {
        return customKeybinds.getValue() ? offhandKey.getKey() : InputKey.NONE;
    }

    public InputKey customSlotKey(int slot) {
        if (!customKeybinds.getValue() || slot < 0 || slot >= slotKeys.length) {
            return InputKey.NONE;
        }
        return slotKeys[slot].getKey();
    }

    public boolean isListeningForKey() {
        if (attackKey.isListeningForKey() || useKey.isListeningForKey() || offhandKey.isListeningForKey()) {
            return true;
        }
        for (KeybindSetting setting : slotKeys) {
            if (setting.isListeningForKey()) {
                return true;
            }
        }
        return false;
    }

    public List<Setting> getSettings() {
        return allSettings;
    }
}
