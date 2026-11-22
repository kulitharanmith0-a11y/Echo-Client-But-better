package dev.dov.hack.echo.client.gui.dashboard;

import dev.dov.hack.echo.client.settings.Setting;
import java.util.List;

public record SettingSection(CharSequence title, List<Setting> settings) {
}
