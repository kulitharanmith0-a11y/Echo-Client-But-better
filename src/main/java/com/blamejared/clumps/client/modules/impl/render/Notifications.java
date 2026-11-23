package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.hud.HudElement;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.hud.HudElementLayout;
import com.blamejared.clumps.client.hud.HudElementType;
import com.blamejared.clumps.client.hud.HudRenderer;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.ui.UiSize;

public class Notifications extends HudElement {
    private FloatSetting duration;

    public Notifications() {
        super(new ModuleInfo(CatalogText.literal("Notifications"), CatalogText.literal("Timed client event messages"), ModuleCategory.RENDER), HudElementType.NOTIFICATIONS);
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        return HudRenderer.INSTANCE.measureElement(layout, HudElementType.NOTIFICATIONS);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer.INSTANCE.drawNotifications(layout);
    }

    @Override
    public void postInitialize() {
        super.postInitialize();
        duration.onChange(this::onDurationChanged);
        HudRenderer.INSTANCE.setNotificationDuration(duration.getValue());
        duration.tooltip("Timing");
    }

    private void onDurationChanged() {
        HudRenderer.INSTANCE.setNotificationDuration(duration.getValue());
    }

    
    public static void appendBootstrapSettings(Notifications Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.duration);
    }

    
    public static void assignBootstrapSettings(Notifications Module, com.blamejared.clumps.client.settings.FloatSetting preparedDuration) {
        Module.duration = preparedDuration;
    }
}
