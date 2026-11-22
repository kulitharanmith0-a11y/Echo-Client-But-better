package dev.dov.hack.echo.client.gui.dashboard;

import dev.dov.hack.echo.client.ui.UiRect;

public final class DashboardLayout {
    private DashboardLayout() {
    }

    
    public static float contentHeight() {
        return 842.0f;
    }

    
    public static float availableContentHeight() {
        return contentHeight() - 20.0f - 140.0f;
    }

    
    public static UiRect moduleCard(int index) {
        return new UiRect(346.0f + index % 3 * 509.0f, 102.0f + index / 3 * 74.0f, 493.0f, 63.0f);
    }

    
    public static float scriptCardX(int index) {
        return 354 + index % 3 * 509;
    }

    
    public static UiRect configMenuButton(float cardX, float cardY) {
        return new UiRect(cardX + 455.0f, cardY + 17.0f, 22.0f, 22.0f);
    }

    
    public static float optionGridBottom(float top, int count) {
        return top + 48.0f + Math.max(1, (count + 1) / 2) * 44;
    }

    
    public static float clampFraction(float value) {
        return Math.clamp(value, 0.0f, 1.0f);
    }


    public static float sliderThumbCenter(float left, float width, float thumbWidth, float value) {
        float halfThumb = thumbWidth * 0.5f;
        float fraction = Math.clamp(value, 0.0f, 1.0f);
        return left + halfThumb + Math.max(0.0f, width - thumbWidth) * fraction;
    }


    public static float sliderValue(float mouseX, float left, float width, float thumbWidth) {
        float travel = Math.max(1.0e-4f, width - thumbWidth);
        return Math.clamp((mouseX - left - thumbWidth * 0.5f) / travel, 0.0f, 1.0f);
    }

    
    public static int visibleEditorLines() {
        return 35;
    }
}
