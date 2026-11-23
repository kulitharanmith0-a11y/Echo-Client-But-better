package com.blamejared.clumps.client.gui.dashboard;

import com.blamejared.clumps.client.settings.Setting;

public record SettingRow(Setting setting, float x, float y, float height, int column) {
}
