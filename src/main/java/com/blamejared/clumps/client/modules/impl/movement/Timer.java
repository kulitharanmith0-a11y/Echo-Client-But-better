package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.utils.strings.Concat;

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
