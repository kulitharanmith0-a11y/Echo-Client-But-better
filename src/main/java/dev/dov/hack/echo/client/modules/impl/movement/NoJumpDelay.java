package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.mixin.accessor.LivingEntityAccessor;
import static dev.dov.hack.echo.client.modules.ModuleLiterals.literal;

public class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("No Jump Delay"), dev.dov.hack.echo.client.init.CatalogText.literal("Removes jump cooldown"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT));
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (client.player == null || client.world == null) return;
        ((LivingEntityAccessor) client.player).setJumpingCooldown(0);
    }

}
