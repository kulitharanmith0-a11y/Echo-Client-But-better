package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.impl.movement.Sprint;
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
        if (EchoClient.eventsDisabled) {
            return;
        }
        if (EchoClient.ModuleManager != null) {
            Sprint sprint = (Sprint) EchoClient.ModuleManager.findFeature(Sprint.class);
            if (sprint != null && sprint.isOmniEnabled()) {
                callback.setReturnValue(movementVector.lengthSquared() > 1.0E-5F);
            }
        }
    }
}
