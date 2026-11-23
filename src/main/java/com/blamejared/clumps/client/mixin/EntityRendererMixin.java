package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.modules.impl.render.Freecam;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderStateTail(T entity, S state, float tickProgress, CallbackInfo callback) {
        if (MinecraftAccess.CLIENT.player != null && MinecraftAccess.CLIENT.world != null
                && !ClumpsClient.eventsDisabled && ClumpsClient.ModuleManager != null) {
            Freecam freecam = (Freecam) ClumpsClient.ModuleManager.findFeature(Freecam.class);
            if (freecam != null && freecam.isEnabled()) {
                ClientPlayerEntity player = MinecraftAccess.CLIENT.player;
                if (entity == player) {
                    state.displayName = player.getDisplayName();
                    state.nameLabelPos = player.getAttachments().getPointNullable(
                        EntityAttachmentType.NAME_TAG, 0, player.getLerpedYaw(tickProgress));
                }
            }
        }
    }
}
