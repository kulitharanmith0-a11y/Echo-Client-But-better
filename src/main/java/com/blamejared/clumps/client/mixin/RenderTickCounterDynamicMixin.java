package com.blamejared.clumps.client.mixin;
import com.blamejared.clumps.client.platform.Platform;

import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public class RenderTickCounterDynamicMixin implements Platform.TickControl {
    @Shadow private float dynamicDeltaTicks;
    @Unique private float clumps$tickSpeedMultiplier;
    @Unique private boolean clumps$tickSpeedSet;

    @Inject(method = "beginRenderTick(J)I", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;lastTimeMillis:J", ordinal = 1))
    private void scaleFrameDuration(long timeMillis, CallbackInfoReturnable<Integer> callback) {
        dynamicDeltaTicks *= getTickSpeedMultiplier();
    }

    @Override
    public float getTickSpeedMultiplier() {
        return clumps$tickSpeedSet ? clumps$tickSpeedMultiplier : 1.0F;
    }

    @Override
    public void setTickSpeedMultiplier(float multiplier) {
        clumps$tickSpeedMultiplier = multiplier;
        clumps$tickSpeedSet = true;
    }
}
