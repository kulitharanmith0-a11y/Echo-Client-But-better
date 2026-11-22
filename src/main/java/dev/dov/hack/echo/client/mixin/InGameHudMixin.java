package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.render.InGameHudEvent;
import dev.dov.hack.echo.client.event.render.NauseaOverlayEvent;
import dev.dov.hack.echo.client.event.render.PortalOverlayEvent;
import dev.dov.hack.echo.client.event.render.SpyglassOverlayEvent;
import dev.dov.hack.echo.client.event.render.VignetteOverlayEvent;
import dev.dov.hack.echo.client.modules.impl.render.Hotbar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled) {
            InGameHudEvent event = new InGameHudEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void onRenderHotbarHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled && !EchoClient.externalRendererActive && Hotbar.replacesVanillaHotbar()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderVignetteOverlayHead(DrawContext context, Entity entity, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled && !EchoClient.externalRendererActive) {
            VignetteOverlayEvent event = new VignetteOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderNauseaOverlayHead(DrawContext context, float strength, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled && !EchoClient.externalRendererActive) {
            NauseaOverlayEvent event = new NauseaOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderSpyglassOverlayHead(DrawContext context, float scale, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled && !EchoClient.externalRendererActive) {
            SpyglassOverlayEvent event = new SpyglassOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderPortalOverlayHead(DrawContext context, float strength, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled && !EchoClient.externalRendererActive) {
            PortalOverlayEvent event = new PortalOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
