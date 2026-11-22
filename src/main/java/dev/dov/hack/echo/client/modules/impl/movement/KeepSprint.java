package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.MultiChoiceSetting;
import dev.dov.hack.echo.client.init.CatalogText;

public class KeepSprint extends Module {
    private static final CharSequence GROUND = CatalogText.literal("Ground");
    private static final CharSequence AIR = CatalogText.literal("Air");
    public static FloatSetting speedKeptVelocity;
    private static BooleanSetting shouldSprint;
    private static MultiChoiceSetting sprintCondition;

    public KeepSprint() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Keep Sprint"), dev.dov.hack.echo.client.init.CatalogText.literal("Keeps you sprinting"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT));
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
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, KeepSprint.speedKeptVelocity);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, KeepSprint.shouldSprint);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, KeepSprint.sprintCondition);
    }

    
    public static void assignBootstrapSettings(KeepSprint Module, dev.dov.hack.echo.client.settings.MultiChoiceSetting preparedSprintCondition, dev.dov.hack.echo.client.settings.FloatSetting preparedSpeedKeptVelocity, dev.dov.hack.echo.client.settings.BooleanSetting preparedShouldSprint) {
        KeepSprint.sprintCondition = preparedSprintCondition;
        KeepSprint.speedKeptVelocity = preparedSpeedKeptVelocity;
        KeepSprint.shouldSprint = preparedShouldSprint;
    }
}
