package dev.dov.hack.echo.client.ui;

import dev.dov.hack.echo.client.hud.HudElementType;

public record UiSize(float width, float height) {
    
    public UiSize {
        width = Math.max(0.0f, width);
        height = Math.max(0.0f, height);
    }

    
    public static UiSize fromDefaults(HudElementType type) {
        return new UiSize(type.defaultWidth(), type.defaultHeight());
    }
}
