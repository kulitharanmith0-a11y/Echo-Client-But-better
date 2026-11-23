package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.render.FireOverlayEvent;
import com.blamejared.clumps.client.event.render.FloatingItemOverlayEvent;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlayHead(MatrixStack matrices, VertexConsumerProvider vertices, Sprite fireSprite, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && !ClumpsClient.externalRendererActive) {
            FireOverlayEvent event = new FireOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true)
    public void onRenderFloatingItemHead(MatrixStack matrices, float tickProgress, OrderedRenderCommandQueue queue, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && !ClumpsClient.externalRendererActive) {
            FloatingItemOverlayEvent event = new FloatingItemOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
