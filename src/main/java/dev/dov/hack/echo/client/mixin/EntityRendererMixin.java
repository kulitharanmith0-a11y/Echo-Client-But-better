package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.MinecraftAccess;
import dev.dov.hack.echo.client.modules.impl.render.Freecam;
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
                && !EchoClient.eventsDisabled && EchoClient.ModuleManager != null) {
            Freecam freecam = (Freecam) EchoClient.ModuleManager.findFeature(Freecam.class);
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
