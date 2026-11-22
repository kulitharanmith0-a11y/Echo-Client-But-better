package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.modules.hud.HudElement;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.hud.HudElementLayout;
import dev.dov.hack.echo.client.hud.HudElementType;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.ui.UiSize;

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
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Module.duration);
    }

    
    public static void assignBootstrapSettings(Notifications Module, dev.dov.hack.echo.client.settings.FloatSetting preparedDuration) {
        Module.duration = preparedDuration;
    }
}
