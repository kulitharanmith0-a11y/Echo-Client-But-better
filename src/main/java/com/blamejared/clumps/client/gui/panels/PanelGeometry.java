package com.blamejared.clumps.client.gui.panels;

public final class PanelGeometry {
    private static final float SCRIPT_ROW_HEIGHT = 44.0F;
    private static final float ROW_GAP = 6.0F;
    private static final float CONFIG_CARD_HEIGHT = 72.0F;
    private static final float CONFIG_COLUMNS = 3.0F;

    private PanelGeometry() {
    }

    
    public static float clampPosition(float position, float panelExtent, int viewportExtent) {
        return Math.clamp(position, 0.0F, Math.max(0.0F, viewportExtent - panelExtent));
    }

    
    public static float scriptListHeight(int rowCount) {
        return rowCount * SCRIPT_ROW_HEIGHT + Math.max(0, rowCount - 1) * ROW_GAP;
    }

    
    public static float configGridHeight(int configCount) {
        int rowCount = (int) Math.ceil(configCount / CONFIG_COLUMNS);
        float cardsHeight = rowCount * CONFIG_CARD_HEIGHT;
        float gapsHeight = Math.max(0, rowCount - 1) * ROW_GAP;
        return cardsHeight + gapsHeight;
    }

    
    public static float squaredDistance(float x, float y, float otherX, float otherY) {
        float deltaX = x - otherX;
        float deltaY = y - otherY;
        return deltaX * deltaX + deltaY * deltaY;
    }
}
