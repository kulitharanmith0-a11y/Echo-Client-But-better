package com.blamejared.clumps.client.hud;

import java.util.Locale;

public enum HudElementType {
    ARRAY_LIST("Array List", 31, 28, 188, 272), INFO_BAR("Info Bar", 751, 28, 402, 41), POTION_EFFECTS("Potion Effects", 31, 451, 191, 178), KEYBINDS("Keybinds", 1638, 278, 190, 146), KEYSTROKES("Keystrokes", 31, 780, 140, 152), TARGET("Target", 870, 511, 168, 58), NOTIFICATIONS("Notifications", 858, 745, 226, 68), COORDINATES("Coordinates", 31, 1011, 157, 41), AUTO_CONFIG("Auto Config", 31, 960, 178, 41), OFFHAND("Offhand", 732, 1027, 44, 43), HOTBAR("Hotbar", 780, 1026, 361, 44), ARMOR_STATUS("Armor Status", 1145, 1026, 163, 44);

    private final String displayName;
    private final int defaultX;
    private final int defaultY;
    private final int defaultWidth;
    private final int defaultHeight;
    private final String id;

    HudElementType(String displayName, int defaultX, int defaultY, int defaultWidth, int defaultHeight) {
        this.displayName = displayName;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        this.id = name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    
    public String displayName() { return displayName; }

    
    public int defaultX() { return defaultX; }

    
    public int defaultY() { return defaultY; }

    
    public int defaultWidth() { return defaultWidth; }

    
    public int defaultHeight() { return defaultHeight; }

    
    public String id() { return id; }
}
