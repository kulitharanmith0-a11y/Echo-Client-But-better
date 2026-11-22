package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.render.FireOverlayEvent;
import dev.dov.hack.echo.client.event.render.FloatingItemOverlayEvent;
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
        if (!EchoClient.eventsDisabled && !EchoClient.externalRendererActive) {
            FireOverlayEvent event = new FireOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true)
    public void onRenderFloatingItemHead(MatrixStack matrices, float tickProgress, OrderedRenderCommandQueue queue, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled && !EchoClient.externalRendererActive) {
            FloatingItemOverlayEvent event = new FloatingItemOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
