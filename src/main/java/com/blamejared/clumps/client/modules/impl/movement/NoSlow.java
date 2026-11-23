package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.movement.MovementSpeedEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.utils.strings.Concat;

public class NoSlow extends Module {
    private static final CharSequence GRIM_MODE = Concat.strings("Grim");
    private static ChoiceSetting mode;
    private static BooleanSetting enableSneak;
    private static IntSetting delayTicks;
    private int itemUseDelayTicks;
    private boolean slowdownCancelledThisTick;

    public NoSlow() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("No Slow"), com.blamejared.clumps.client.init.CatalogText.literal("Removes slow effect from items"), com.blamejared.clumps.client.modules.ModuleCategory.MOVEMENT));
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
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, NoSlow.mode);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, NoSlow.enableSneak);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, NoSlow.delayTicks);
    }

    
    public static void assignBootstrapSettings(NoSlow Module, com.blamejared.clumps.client.settings.IntSetting preparedDelayTicks, com.blamejared.clumps.client.settings.ChoiceSetting preparedMode, com.blamejared.clumps.client.settings.BooleanSetting preparedEnableSneak) {
        NoSlow.delayTicks = preparedDelayTicks;
        NoSlow.mode = preparedMode;
        NoSlow.enableSneak = preparedEnableSneak;
    }
}
