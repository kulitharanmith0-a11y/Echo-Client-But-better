package dev.dov.hack.echo.client.gui.dashboard;

import dev.dov.hack.echo.client.settings.Setting;

public record SettingRow(Setting setting, float x, float y, float height, int column) {
}
