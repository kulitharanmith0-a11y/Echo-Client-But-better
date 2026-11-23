package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;

public class AutoWalk extends Module {
    public AutoWalk() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Auto Walk"), com.blamejared.clumps.client.init.CatalogText.literal("Automatically walks forward"), com.blamejared.clumps.client.modules.ModuleCategory.MOVEMENT, false));
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
