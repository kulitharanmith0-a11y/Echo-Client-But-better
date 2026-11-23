package com.blamejared.clumps.client.gui.dashboard;

import com.blamejared.clumps.client.settings.Setting;
import java.util.List;

public record SettingSection(CharSequence title, List<Setting> settings) {
}
