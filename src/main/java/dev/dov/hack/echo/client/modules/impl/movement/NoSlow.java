package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.event.movement.MovementSpeedEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.utils.strings.Concat;

public class NoSlow extends Module {
    private static final CharSequence GRIM_MODE = Concat.strings("Grim");
    private static ChoiceSetting mode;
    private static BooleanSetting enableSneak;
    private static IntSetting delayTicks;
    private int itemUseDelayTicks;
    private boolean slowdownCancelledThisTick;

    public NoSlow() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("No Slow"), dev.dov.hack.echo.client.init.CatalogText.literal("Removes slow effect from items"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        itemUseDelayTicks = 0;
        slowdownCancelledThisTick = false;
        delayTicks.setVisibilityPredicate(ignored -> mode.isSelected(GRIM_MODE));
        delayTicks.tooltip("Timing");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        itemUseDelayTicks = 0;
        slowdownCancelledThisTick = false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        itemUseDelayTicks = 0;
        slowdownCancelledThisTick = false;
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        slowdownCancelledThisTick = false;
        if (!client.player.isUsingItem()) {
            itemUseDelayTicks = 0;
        } else {
            itemUseDelayTicks++;
        }
    }

    @Subscribe
    public void onMovementSpeed(MovementSpeedEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (mode.isSelected(GRIM_MODE)) {
            if (slowdownCancelledThisTick) {
                event.setHasVehicle(true);
                return;
            }
            if (itemUseDelayTicks >= delayTicks.getValue()) {
                slowdownCancelledThisTick = true;
                itemUseDelayTicks = 0;
                event.setHasVehicle(true);
            }
            return;
        }
        event.setActiveItemSpeedMultiplier(1.0f);
        event.setBlockedFromSprinting(false);
        if (enableSneak.getValue()) {
            event.setSlowDown(false);
        }
    }

    
    public static void appendBootstrapSettings(NoSlow Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, NoSlow.mode);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, NoSlow.enableSneak);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, NoSlow.delayTicks);
    }

    
    public static void assignBootstrapSettings(NoSlow Module, dev.dov.hack.echo.client.settings.IntSetting preparedDelayTicks, dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode, dev.dov.hack.echo.client.settings.BooleanSetting preparedEnableSneak) {
        NoSlow.delayTicks = preparedDelayTicks;
        NoSlow.mode = preparedMode;
        NoSlow.enableSneak = preparedEnableSneak;
    }
}
