package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.MinecraftAccess;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.utils.strings.Concat;

public class Timer extends Module {
    public static FloatSetting timer;

    public Timer() {
        super(new ModuleInfo(CatalogText.literal("Timer"), CatalogText.literal("Modifies game tick speed"), ModuleCategory.MOVEMENT, false));
    }

    @Override
    public void postInitialize() {
        timer.setAllowOutOfRange(true);
    }

    @Override
    public CharSequence getSuffix() {
        return Concat.formattedFloat(timer.getValue(), 2);
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || !isEnabled()) {
            return;
        }
        MinecraftAccess.setTickSpeed(timer.getValue());
    }

    
    @Override
    public void onDisable() {
        MinecraftAccess.setTickSpeed(1.0f);
    }

    public static void appendBootstrapSettings(Timer Module) {
        CatalogSettings.append(Module, timer);
    }

    public static void assignBootstrapSettings(Timer Module, FloatSetting preparedTimer) {
        timer = preparedTimer;
    }
}
