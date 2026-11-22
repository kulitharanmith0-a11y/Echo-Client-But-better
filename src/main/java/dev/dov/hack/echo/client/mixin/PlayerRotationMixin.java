package dev.dov.hack.echo.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.MinecraftAccess;
import dev.dov.hack.echo.client.event.movement.RotationContext;
import dev.dov.hack.echo.client.event.movement.RotationEvent;
import dev.dov.hack.echo.client.modules.impl.movement.KeepSprint;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public abstract class PlayerRotationMixin {
    @ModifyExpressionValue(method = "tickMovement", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F", ordinal = 0))
    private float modifyTickMovementAtGetYawAtOrdinal0(float yaw) {
        if ((Object) this != MinecraftAccess.CLIENT.player) {
            return yaw;
        }
        return RotationEvent.dispatch(RotationContext.HEAD, yaw, MinecraftAccess.CLIENT.player.getPitch()).getYaw();
    }

    @Redirect(method = {"knockbackTarget", "doSweepingAttack"}, at = @At(
        value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float redirectKnockbackTargetAtGetYaw(PlayerEntity player) {
        return player != MinecraftAccess.CLIENT.player ? player.getYaw()
            : RotationEvent.dispatch(RotationContext.ATTACK, player.getYaw(), player.getPitch()).getYaw();
    }

    @Redirect(method = "knockbackTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;multiply(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d redirectKnockbackTargetAtMultiply(Vec3d velocity, double x, double y, double z) {
        if (!EchoClient.eventsDisabled && (Object) this == MinecraftAccess.CLIENT.player
                && EchoClient.ModuleManager != null) {
            KeepSprint keepSprint = (KeepSprint) EchoClient.ModuleManager.findFeature(KeepSprint.class);
            if (keepSprint != null && keepSprint.isEnabled()) {
                float multiplier = KeepSprint.speedKeptVelocity.getValue();
                return velocity.multiply(multiplier, y, multiplier);
            }
        }
        return velocity.multiply(x, y, z);
    }

    @Redirect(method = "knockbackTarget", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"))
    private void redirectKnockbackTargetAtSetSprinting(PlayerEntity player, boolean sprinting) {
        if (!EchoClient.eventsDisabled && EchoClient.ModuleManager != null) {
            KeepSprint keepSprint = (KeepSprint) EchoClient.ModuleManager.findFeature(KeepSprint.class);
            if (keepSprint != null && keepSprint.isEnabled() && keepSprint.shouldKeepSprinting()) {
                return;
            }
        }
        player.setSprinting(sprinting);
    }
}
