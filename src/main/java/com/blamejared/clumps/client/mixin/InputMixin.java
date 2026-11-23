package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.modules.impl.movement.Sprint;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Input.class)
public class InputMixin {
    @Shadow protected Vec2f movementVector;

    @Inject(method = "hasForwardMovement", at = @At("HEAD"), cancellable = true)
    private void allowOmnidirectionalSprint(CallbackInfoReturnable<Boolean> callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        if (ClumpsClient.ModuleManager != null) {
            Sprint sprint = (Sprint) ClumpsClient.ModuleManager.findFeature(Sprint.class);
            if (sprint != null && sprint.isOmniEnabled()) {
                callback.setReturnValue(movementVector.lengthSquared() > 1.0E-5F);
            }
        }
    }
}
