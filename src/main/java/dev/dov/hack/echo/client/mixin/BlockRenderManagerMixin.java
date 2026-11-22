package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.EventBus;
import dev.dov.hack.echo.client.event.render.FluidRenderEvent;
import dev.jadyen.hack.echo.natives.SodiumBridge;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderManager.class)
public class BlockRenderManagerMixin {
    @Inject(method = "renderFluid", at = @At("HEAD"), cancellable = true)
    public void onRenderFluidHead(BlockPos position, BlockRenderView world, VertexConsumer vertices, BlockState blockState, FluidState fluidState, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled && !SodiumBridge.isLoaded()
                && EventBus.hasSubscribers(FluidRenderEvent.class)) {
            if (new FluidRenderEvent().post()) {
                callback.cancel();
            }
        }
    }
}
