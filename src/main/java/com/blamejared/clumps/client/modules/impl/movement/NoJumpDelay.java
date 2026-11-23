package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.mixin.accessor.LivingEntityAccessor;
import static com.blamejared.clumps.client.modules.ModuleLiterals.literal;

public class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("No Jump Delay"), com.blamejared.clumps.client.init.CatalogText.literal("Removes jump cooldown"), com.blamejared.clumps.client.modules.ModuleCategory.MOVEMENT));
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (client.player == null || client.world == null) return;
        ((LivingEntityAccessor) client.player).setJumpingCooldown(0);
    }

}
