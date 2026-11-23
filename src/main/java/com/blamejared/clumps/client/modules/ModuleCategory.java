package com.blamejared.clumps.client.modules;

import com.blamejared.clumps.client.utils.strings.SegmentedText;

public enum ModuleCategory {
    COMBAT(SegmentedText.literal("Combat"), "\ue2b4"), MOVEMENT(SegmentedText.literal("Movement"), "\ue1bf"), RENDER(SegmentedText.literal("Visuals"), "\ue53a"), WORLD(SegmentedText.literal("World"), "\ue1f3"), UTILITY(SegmentedText.literal("Utility"), "\ue19f"), INTERNALS(SegmentedText.literal("Internals"), "\ue30b");

    public final CharSequence name;
    public final CharSequence icon;

    ModuleCategory(CharSequence name, CharSequence icon) {
        this.name = name;
        this.icon = icon;
    }
}
