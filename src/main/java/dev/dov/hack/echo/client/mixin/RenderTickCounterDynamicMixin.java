package dev.dov.hack.echo.client.mixin;
import dev.dov.hack.echo.client.platform.Platform;

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
    @Unique private float echo$tickSpeedMultiplier = 1.0F;

    @Inject(method = "beginRenderTick(J)I", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;lastTimeMillis:J", ordinal = 1))
    private void scaleFrameDuration(long timeMillis, CallbackInfoReturnable<Integer> callback) {
        dynamicDeltaTicks *= getTickSpeedMultiplier();
    }

    @Override
    public float getTickSpeedMultiplier() {
        return echo$tickSpeedMultiplier;
    }

    @Override
    public void setTickSpeedMultiplier(float multiplier) {
        echo$tickSpeedMultiplier = multiplier;
    }
}
