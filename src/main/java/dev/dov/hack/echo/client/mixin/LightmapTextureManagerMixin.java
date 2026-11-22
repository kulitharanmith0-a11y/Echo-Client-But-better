package dev.dov.hack.echo.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.impl.render.FullBright;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Shadow @Final private GpuTexture glTexture;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void replaceLightmapWithWhite(float tickProgress, CallbackInfo callback) {
        if (EchoClient.eventsDisabled || EchoClient.externalRendererActive) {
            return;
        }
        if (FullBright.isFullBrightActive()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(glTexture, -1);
            callback.cancel();
        }
    }
}
