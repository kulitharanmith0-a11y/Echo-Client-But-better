package com.blamejared.clumps.client.settings.ui;

import com.blamejared.clumps.client.hud.ItemHudLayout;

@FunctionalInterface
public interface SettingWidgetRenderer {
    void render(ItemHudLayout context);
}
