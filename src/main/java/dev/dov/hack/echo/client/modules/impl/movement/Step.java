package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import net.minecraft.entity.attribute.EntityAttributes;

public class Step extends Module {
    private static FloatSetting height;
    private static BooleanSetting disableOnSneak;

    public Step() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Step"), dev.dov.hack.echo.client.init.CatalogText.literal("Changes the step height of the player"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT, false));
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
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Step.height);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Step.disableOnSneak);
    }

    
    public static void assignBootstrapSettings(Step Module, dev.dov.hack.echo.client.settings.BooleanSetting preparedDisableOnSneak, dev.dov.hack.echo.client.settings.FloatSetting preparedHeight) {
        Step.disableOnSneak = preparedDisableOnSneak;
        Step.height = preparedHeight;
    }
}
