package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.event.movement.RotationContext;
import com.blamejared.clumps.client.event.movement.RotationEvent;
import com.blamejared.clumps.client.modules.impl.render.Freecam;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityTargetingMixin {
    @Inject(method = "getRotationVec", at = @At("HEAD"), cancellable = true)
    private void onGetRotationVecHead(float tickProgress, CallbackInfoReturnable<Vec3d> callback) {
        if ((Object) this == MinecraftAccess.CLIENT.player) {
            Entity entity = (Entity) (Object) this;
            RotationEvent event = RotationEvent.dispatch(RotationContext.LOOK, entity.getYaw(tickProgress), entity.getPitch(tickProgress));
            callback.setReturnValue(entity.getRotationVector(event.getPitch(), event.getYaw()));
        }
    }

    @Inject(method = "getHeadRotationVector", at = @At("HEAD"), cancellable = true)
    private void onGetHeadRotationVectorHead(CallbackInfoReturnable<Vec3d> callback) {
        if ((Object) this == MinecraftAccess.CLIENT.player) {
            Entity entity = (Entity) (Object) this;
            RotationEvent event = RotationEvent.dispatch(RotationContext.LOOK, entity.getHeadYaw(), entity.getPitch());
            callback.setReturnValue(entity.getRotationVector(event.getPitch(), event.getYaw()));
        }
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirectionHead(double cursorDeltaX, double cursorDeltaY, CallbackInfo callback) {
        if ((Object) this == MinecraftAccess.CLIENT.player && !ClumpsClient.eventsDisabled && ClumpsClient.ModuleManager != null) {
            Freecam freecam = (Freecam) ClumpsClient.ModuleManager.findFeature(Freecam.class);
            if (freecam != null && freecam.isEnabled() && freecam.getCameraPosition() != null) {
                freecam.rotateCamera((float) cursorDeltaX, (float) cursorDeltaY);
                callback.cancel();
            }
        }
    }
}
