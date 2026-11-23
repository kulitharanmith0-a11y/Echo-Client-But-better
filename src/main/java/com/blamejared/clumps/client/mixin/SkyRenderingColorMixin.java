package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.modules.impl.render.Ambience;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyRenderingColorMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void onUpdateRenderStateTail(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && !ClumpsClient.externalRendererActive && MinecraftAccess.CLIENT.player != null && MinecraftAccess.CLIENT.world != null
                && ClumpsClient.ModuleManager != null) {
            Ambience ambience = (Ambience) ClumpsClient.ModuleManager.findFeature(Ambience.class);
            if (ambience != null && ambience.isEnabled() && Ambience.isFogColorEnabled()) {
                state.skyColor = ColorHelper.getArgb(Ambience.getFogRed(), Ambience.getFogGreen(), Ambience.getFogBlue());
                if (Ambience.isGradientEnabled()) {
                    state.sunriseAndSunsetColor = ColorHelper.getArgb(255, Ambience.getGradientRed(), Ambience.getGradientGreen(), Ambience.getGradientBlue());
                } else {
                    state.sunriseAndSunsetColor = 0;
                }
                state.shouldRenderSkyDark = false;
            }
        }
    }
}
