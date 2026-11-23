package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.render.InGameHudEvent;
import com.blamejared.clumps.client.event.render.NauseaOverlayEvent;
import com.blamejared.clumps.client.event.render.PortalOverlayEvent;
import com.blamejared.clumps.client.event.render.SpyglassOverlayEvent;
import com.blamejared.clumps.client.event.render.VignetteOverlayEvent;
import com.blamejared.clumps.client.modules.impl.render.Hotbar;
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
        if (!ClumpsClient.eventsDisabled) {
            InGameHudEvent event = new InGameHudEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void onRenderHotbarHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && !ClumpsClient.externalRendererActive && Hotbar.replacesVanillaHotbar()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderVignetteOverlayHead(DrawContext context, Entity entity, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && !ClumpsClient.externalRendererActive) {
            VignetteOverlayEvent event = new VignetteOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderNauseaOverlayHead(DrawContext context, float strength, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && !ClumpsClient.externalRendererActive) {
            NauseaOverlayEvent event = new NauseaOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderSpyglassOverlayHead(DrawContext context, float scale, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && !ClumpsClient.externalRendererActive) {
            SpyglassOverlayEvent event = new SpyglassOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    public void onRenderPortalOverlayHead(DrawContext context, float strength, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && !ClumpsClient.externalRendererActive) {
            PortalOverlayEvent event = new PortalOverlayEvent();
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
