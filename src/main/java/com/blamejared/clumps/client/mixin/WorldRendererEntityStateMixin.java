package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.modules.impl.render.Freecam;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererEntityStateMixin {
    @Shadow
    protected abstract EntityRenderState getAndUpdateRenderState(Entity entity, float tickProgress);

    @Inject(method = "fillEntityRenderStates", at = @At("RETURN"))
    private void onFillEntityRenderStatesReturn(Camera camera, Frustum frustum, RenderTickCounter ticks, WorldRenderState state, CallbackInfo callback) {
        if (MinecraftAccess.CLIENT.player != null && MinecraftAccess.CLIENT.world != null
                && !ClumpsClient.eventsDisabled && ClumpsClient.ModuleManager != null) {
            Freecam freecam = (Freecam) ClumpsClient.ModuleManager.findFeature(Freecam.class);
            if (freecam.isEnabled()) {
                ClientPlayerEntity player = MinecraftAccess.CLIENT.player;
                TickManager tickManager = MinecraftAccess.CLIENT.world.getTickManager();
                float tickProgress = ticks.getTickProgress(!tickManager.shouldSkipTick(player));
                EntityRenderState playerState = getAndUpdateRenderState(player, tickProgress);
                state.entityRenderStates.add(playerState);
            }
        }
    }
}
