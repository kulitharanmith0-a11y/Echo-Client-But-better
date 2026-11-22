package dev.dov.hack.echo.client.gui.dashboard;

import dev.dov.hack.echo.client.ui.UiRect;

public final class ConfigCardLayout {
    private ConfigCardLayout() {
    }

    
    public static UiRect primaryButton(float cardX, float cardY, ConfigPrimaryAction action) {
        float width = action == null ? 461.0f : 461.0f - action.secondaryWidth - 8.0f;
        return new UiRect(cardX + 16.0f, cardY + 97.0f, width, 31.0f);
    }

    
    public static UiRect secondaryButton(float cardX, float cardY, ConfigPrimaryAction action) {
        return new UiRect(cardX + 477.0f - action.secondaryWidth, cardY + 97.0f, action.secondaryWidth, 31.0f);
    }
}
