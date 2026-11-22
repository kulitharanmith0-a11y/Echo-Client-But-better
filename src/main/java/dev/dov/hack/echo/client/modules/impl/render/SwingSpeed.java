package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.IntSetting;

public class SwingSpeed extends Module {
    public static IntSetting swingSpeed;

    public SwingSpeed() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Swing Speed"), dev.dov.hack.echo.client.init.CatalogText.literal("Increases the speed of your swing animation"), dev.dov.hack.echo.client.modules.ModuleCategory.RENDER, false));
    }

    @Override
    public void postInitialize() {
        swingSpeed.tooltip("Timing");
    }

}
