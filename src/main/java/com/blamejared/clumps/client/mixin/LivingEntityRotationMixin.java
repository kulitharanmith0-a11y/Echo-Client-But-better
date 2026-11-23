package com.blamejared.clumps.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.movement.RotationContext;
import com.blamejared.clumps.client.event.movement.RotationEvent;
import com.blamejared.clumps.client.modules.impl.render.SwingSpeed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRotationMixin extends Entity {
    @Unique private boolean clumps$rotationOverridden;
    @Unique private float clumps$savedYaw;
    @Unique private float clumps$savedPitch;

    public LivingEntityRotationMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;tickMovementInput()V", shift = At.Shift.AFTER))
    private void applyMovementRotation(CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && (Object) this == MinecraftClient.getInstance().player) {
            RotationEvent rotation = RotationEvent.dispatch(RotationContext.MOVEMENT, getYaw(), getPitch());
            clumps$rotationOverridden = rotation.getYaw() != getYaw() || rotation.getPitch() != getPitch();
            if (clumps$rotationOverridden) {
                clumps$savedYaw = getYaw();
                clumps$savedPitch = getPitch();
                setYaw(rotation.getYaw());
                setPitch(rotation.getPitch());
            }
        }
    }

    @Inject(method = "tickMovement", at = @At("RETURN"))
    private void restoreMovementRotation(CallbackInfo callback) {
        if (clumps$rotationOverridden) {
            clumps$rotationOverridden = false;
            setYaw(clumps$savedYaw);
            setPitch(clumps$savedPitch);
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F", ordinal = 0))
    private float modifyFirstTickYaw(float original) {
        return clumps$resolveBodyYaw(original);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F", ordinal = 1))
    private float modifySecondTickYaw(float original) {
        return clumps$resolveBodyYaw(original);
    }

    @ModifyExpressionValue(method = "turnHead", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float modifyHeadTurnYaw(float original) {
        return clumps$resolveBodyYaw(original);
    }

    @Unique
    private float clumps$resolveBodyYaw(float original) {
        return (Object) this != MinecraftClient.getInstance().player ? original
                : RotationEvent.dispatch(RotationContext.BODY, original, MinecraftClient.getInstance().player.getPitch()).getYaw();
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void modifyHandSwingDuration(CallbackInfoReturnable<Integer> callback) {
        if (!ClumpsClient.eventsDisabled && !ClumpsClient.externalRendererActive && ClumpsClient.ModuleManager != null && (Object) this == MinecraftClient.getInstance().player) {
            SwingSpeed swing = (SwingSpeed) ClumpsClient.ModuleManager.findFeature(SwingSpeed.class);
            if (swing != null && swing.isEnabled()) {
                callback.setReturnValue(SwingSpeed.swingSpeed.getValue());
            }
        }
    }
}
