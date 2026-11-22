package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.PlayerInputEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;

public class AutoWalk extends Module {
    public AutoWalk() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Auto Walk"), dev.dov.hack.echo.client.init.CatalogText.literal("Automatically walks forward"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT, false));
    }

    @Subscribe
    public void onMovementInput(PlayerInputEvent event) {
        if (client.player == null || client.world == null || client.player.isDead()) {
            return;
        }
        event.forward = true;
        event.allowMovementInFreecam = true;
    }

}
