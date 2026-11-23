package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.modules.impl.render.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(Vec3d position);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("RETURN"), cancellable = true)
    private void onUpdateReturn(World world, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && world != null && focusedEntity != null
                && MinecraftAccess.CLIENT.player != null && ClumpsClient.ModuleManager != null) {
            Freecam freecam = (Freecam) ClumpsClient.ModuleManager.findFeature(Freecam.class);
            if (freecam != null && freecam.isEnabled()) {
                Vec3d position = freecam.getCameraPosition();
                if (position == null) {
                    return;
                }
                setRotation(freecam.getCameraYaw(), freecam.getCameraPitch());
                setPos(position);
            }
        }
    }
}
