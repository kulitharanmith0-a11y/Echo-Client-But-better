package dev.dov.hack.echo.client.settings.ui;

import dev.dov.hack.echo.client.hud.ItemHudLayout;

@FunctionalInterface
public interface SettingWidgetRenderer {
    void render(ItemHudLayout context);
}
