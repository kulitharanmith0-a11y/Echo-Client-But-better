package com.blamejared.clumps.client.hud;

public final class HudLayout {
    private HudLayout() {
    }

    
    public static float targetContentWidth(float nameWidth, float healthWidth) {
        float width = Math.max(0.0f, nameWidth) + 14.0f + Math.max(0.0f, healthWidth);
        return (float) Math.ceil(Math.max(96.0f, width));
    }

    
    public static float targetPanelWidth(float contentWidth) {
        return 62.0f + Math.max(96.0f, contentWidth) + 10.0f;
    }

    
    public static float effectsPanelHeight(int rows) {
        return 54.0f + Math.max(0, rows) * 23.0f;
    }

    
    public static float centeredLeft(float center, float width) {
        return center - Math.max(0.0f, width) * 0.5f;
    }

    
    public static float rightAlignedLeft(float right, float width) {
        return right - Math.max(0.0f, width);
    }

    
    public static float roundWidth(float width) {
        return (float) Math.ceil(Math.max(0.0f, width) / 5.0f) * 5.0f;
    }

    
    public static float hotbarItemY(float top) {
        return top + 14.0f;
    }

    
    public static float armorItemY(float top) {
        return top + 14.0f;
    }

    
    public static float equipmentItemY(float top) {
        return top + 14.0f;
    }

    
    public static float equipmentLabelY(float top) {
        return top + 13.5f;
    }

    
    public static int slotIndex(int index, int slotCount) {
        return Math.max(0, Math.min(Math.max(0, slotCount - 1), index));
    }

    
    public static float clampFraction(float fraction) {
        return Math.max(0.0f, Math.min(1.0f, fraction));
    }
}
