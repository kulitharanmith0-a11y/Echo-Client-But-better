package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import net.minecraft.entity.attribute.EntityAttributes;

public class Step extends Module {
    private static FloatSetting height;
    private static BooleanSetting disableOnSneak;

    public Step() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Step"), com.blamejared.clumps.client.init.CatalogText.literal("Changes the step height of the player"), com.blamejared.clumps.client.modules.ModuleCategory.MOVEMENT, false));
    }

    private void setStepHeight(float value) {
        client.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT).setBaseValue(value);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        client.player.getAttributeInstance(
                net.minecraft.entity.attribute.EntityAttributes.STEP_HEIGHT).setBaseValue(0.6f);
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.player.isSneaking() && disableOnSneak.getValue()) {
            return;
        }
        setStepHeight(height.getValue());
    }

    
    public static void appendBootstrapSettings(Step Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Step.height);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Step.disableOnSneak);
    }

    
    public static void assignBootstrapSettings(Step Module, com.blamejared.clumps.client.settings.BooleanSetting preparedDisableOnSneak, com.blamejared.clumps.client.settings.FloatSetting preparedHeight) {
        Step.disableOnSneak = preparedDisableOnSneak;
        Step.height = preparedHeight;
    }
}
