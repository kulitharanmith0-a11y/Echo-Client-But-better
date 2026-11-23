package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.render.FluidRenderEvent;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidRendering.class, remap = false)
public class FluidRenderingMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void onRenderHead(CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled && EventBus.hasSubscribers(FluidRenderEvent.class)) {
            if (new FluidRenderEvent().post()) {
                callback.cancel();
            }
        }
    }
}
