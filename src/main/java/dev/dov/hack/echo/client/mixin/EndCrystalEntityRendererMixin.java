package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.impl.render.NoRender;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class EndCrystalEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
    public void onRenderHead(EndCrystalEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue commands, CameraRenderState camera, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled && !EchoClient.externalRendererActive && EchoClient.ModuleManager != null) {
            NoRender noRender = (NoRender) EchoClient.ModuleManager.findFeature(NoRender.class);
            if (noRender != null && noRender.isEnabled() && NoRender.noCrystalBounce.getValue()) {
                state.age = 0.0F;
            }
        }
    }
}
