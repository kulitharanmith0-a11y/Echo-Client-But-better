package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.impl.render.SkyShaders;
import dev.dov.hack.echo.client.gfx.FullscreenEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyRenderingStarsMixin {
    @Unique
    private static boolean echo$hasCustomSky() {
        if (EchoClient.eventsDisabled || EchoClient.externalRendererActive) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || EchoClient.ModuleManager == null) {
            return false;
        }
        SkyShaders sky = (SkyShaders) EchoClient.ModuleManager.findFeature(SkyShaders.class);
        return sky != null && sky.isEnabled();
    }

    @Inject(method = "renderTopSky", at = @At("HEAD"), cancellable = true)
    private void replaceTopSky(int color, CallbackInfo callback) {
        if (echo$hasCustomSky() && FullscreenEffects.renderCustomSky()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderEndSky", at = @At("HEAD"), cancellable = true)
    private void replaceEndSky(CallbackInfo callback) {
        if (echo$hasCustomSky() && SkyShaders.shouldRenderInEnd() && FullscreenEffects.renderCustomSky()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderGlowingSky", at = @At("HEAD"), cancellable = true)
    private void suppressGlowingSky(CallbackInfo callback) {
        if (echo$hasCustomSky()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true)
    private void controlCelestialBodies(CallbackInfo callback) {
        if (echo$hasCustomSky() && !SkyShaders.shouldRenderSunAndMoon()) {
            callback.cancel();
        }
    }

    @Inject(method = "drawEndLightFlash", at = @At("HEAD"), cancellable = true)
    private void suppressEndLightFlash(CallbackInfo callback) {
        if (echo$hasCustomSky() && SkyShaders.shouldRenderInEnd()) {
            callback.cancel();
        }
    }
}
