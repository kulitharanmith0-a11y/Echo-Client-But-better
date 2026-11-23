package com.blamejared.clumps.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.event.interaction.CrosshairTargetEvent;
import com.blamejared.clumps.client.event.interaction.HandSwingEvent;
import com.blamejared.clumps.client.event.lifecycle.PlayerTickEvent;
import com.blamejared.clumps.client.event.movement.MovementPacketEvent;
import com.blamejared.clumps.client.event.movement.MovementSpeedEvent;
import com.blamejared.clumps.client.handlers.rotation.PitchLimits;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    @Unique private MovementPacketEvent clumps$movementPacket;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void beforeMovementPackets(CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        float pitch = PitchLimits.clampDegrees(getPitch());
        MovementPacketEvent.Pre event = new MovementPacketEvent.Pre(
                getX(), getY(), getZ(), isOnGround(), getYaw(), pitch);
        event.post();
        if (event.cancelled) {
            clumps$movementPacket = null;
            callback.cancel();
        } else {
            clumps$movementPacket = event;
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void afterMovementPackets(CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        new MovementPacketEvent.Post(getX(), getY(), getZ(), isOnGround(), getYaw(), getPitch()).post();
        clumps$movementPacket = null;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getX()D"))
    private double usePacketX(double original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        return clumps$movementPacket != null ? clumps$movementPacket.getX() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getY()D"))
    private double usePacketY(double original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        return clumps$movementPacket != null ? clumps$movementPacket.getY() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getZ()D"))
    private double usePacketZ(double original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        return clumps$movementPacket != null ? clumps$movementPacket.getZ() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float usePacketYaw(float original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        return clumps$movementPacket != null ? (float) clumps$movementPacket.getYaw() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float usePacketPitch(float original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        float pitch = clumps$movementPacket != null ? (float) clumps$movementPacket.getPitch() : original;
        return PitchLimits.clampDegrees(pitch);
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"))
    private boolean usePacketOnGround(boolean original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        return clumps$movementPacket != null ? clumps$movementPacket.isOnGround() : original;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEntityPos()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d usePacketPosition(Vec3d original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        return clumps$movementPacket != null
                ? new Vec3d(clumps$movementPacket.getX(), clumps$movementPacket.getY(), clumps$movementPacket.getZ())
                : original;
    }

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void beforeHandSwing(Hand hand, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        HandSwingEvent event = new HandSwingEvent((ClientPlayerEntity) (Object) this, hand);
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void beforePlayerTick(CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        PlayerTickEvent event = new PlayerTickEvent();
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isBlockedFromSprinting()Z"))
    private boolean modifySprintBlocking(boolean original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        boolean serverSyncSprintBlocked = false;
        if (ClumpsClient.handlerManager != null) {
            var rotationManager = ClumpsClient.handlerManager.getRotationManager();
            if (rotationManager != null) {
                if (rotationManager.isMovementSprintBlocked()) {
                    serverSyncSprintBlocked = true;
                    original = true;
                } else if (!original && rotationManager.isSilentRotationActive()) {
                    PlayerInputEvent last = PlayerInputEvent.lastInput;
                    if (last != null && PlayerInputEvent.inputProcessed && !last.forward) {
                        original = true;
                    }
                }
            }
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, original, false, false);
        event.post();
        if (serverSyncSprintBlocked) {
            return true;
        }
        return !event.cancelled && event.isBlockedFromSprinting();
    }

    @ModifyExpressionValue(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getActiveItemSpeedMultiplier()F"))
    private float modifyActiveItemSpeed(float original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(original, false, false, false);
        event.post();
        return event.cancelled ? 1.0F : event.getActiveItemSpeedMultiplier();
    }

    @ModifyExpressionValue(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;shouldSlowDown()Z"))
    private boolean modifySneakingSlowdown(boolean original) {
        if (ClumpsClient.eventsDisabled) {
            return original;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, false, original, false);
        event.post();
        return !event.cancelled && event.shouldSlowDown();
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasVehicle()Z", ordinal = 1))
    private boolean modifyMovementVehicleCheck(ClientPlayerEntity player) {
        if (ClumpsClient.eventsDisabled) {
            return player.hasVehicle();
        }
        if (player.hasVehicle()) {
            return true;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, false, false, false);
        event.post();
        return event.hasVehicle();
    }

    @Redirect(method = "applyMovementSpeedFactors", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasVehicle()Z"))
    private boolean modifySpeedVehicleCheck(ClientPlayerEntity player) {
        if (ClumpsClient.eventsDisabled) {
            return player.hasVehicle();
        }
        if (player.hasVehicle()) {
            return true;
        }
        MovementSpeedEvent event = new MovementSpeedEvent(1.0F, false, false, false);
        event.post();
        return event.hasVehicle();
    }

    @Inject(method = "getCrosshairTarget", at = @At("RETURN"), cancellable = true)
    private void updateCrosshairTarget(float tickProgress, Entity cameraEntity, CallbackInfoReturnable<HitResult> callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        CrosshairTargetEvent event = new CrosshairTargetEvent(callback.getReturnValue());
        event.post();
        if (event.target != callback.getReturnValue()) {
            callback.setReturnValue(event.target);
        }
    }
}
