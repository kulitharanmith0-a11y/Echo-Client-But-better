package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.MultiChoiceSetting;
import com.blamejared.clumps.client.init.CatalogText;

public class KeepSprint extends Module {
    private static final CharSequence GROUND = CatalogText.literal("Ground");
    private static final CharSequence AIR = CatalogText.literal("Air");
    public static FloatSetting speedKeptVelocity;
    private static BooleanSetting shouldSprint;
    private static MultiChoiceSetting sprintCondition;

    public KeepSprint() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Keep Sprint"), com.blamejared.clumps.client.init.CatalogText.literal("Keeps you sprinting"), com.blamejared.clumps.client.modules.ModuleCategory.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        speedKeptVelocity.tooltip("Timing");
        sprintCondition.setSelected(CatalogText.literal("Air"), true);
        sprintCondition.setSelected(CatalogText.literal("Ground"), true);
    }

    public boolean shouldKeepSprinting() {
        if (!shouldSprint.getValue()) {
            return false;
        }
        return sprintCondition.isSelected(client.player.isOnGround() ? GROUND : AIR);
    }

    
    public static void appendBootstrapSettings(KeepSprint Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, KeepSprint.speedKeptVelocity);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, KeepSprint.shouldSprint);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, KeepSprint.sprintCondition);
    }

    
    public static void assignBootstrapSettings(KeepSprint Module, com.blamejared.clumps.client.settings.MultiChoiceSetting preparedSprintCondition, com.blamejared.clumps.client.settings.FloatSetting preparedSpeedKeptVelocity, com.blamejared.clumps.client.settings.BooleanSetting preparedShouldSprint) {
        KeepSprint.sprintCondition = preparedSprintCondition;
        KeepSprint.speedKeptVelocity = preparedSpeedKeptVelocity;
        KeepSprint.shouldSprint = preparedShouldSprint;
    }
}
