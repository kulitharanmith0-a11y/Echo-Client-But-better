package com.blamejared.clumps.client.mixin.accessor;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.type.PiercingWeaponComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker("syncSelectedSlot")
    void invokeSyncSelectedSlot();

    @Invoker("attackWithPiercingWeapon")
    void invokeAttackWithPiercingWeapon(PiercingWeaponComponent weapon);
}
