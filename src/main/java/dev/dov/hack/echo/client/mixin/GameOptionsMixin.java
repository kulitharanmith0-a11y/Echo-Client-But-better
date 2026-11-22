package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.render.PerspectiveEvent;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Inject(method = "setPerspective", at = @At("HEAD"), cancellable = true)
    private void onSetPerspectiveHead(Perspective perspective, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled) {
            PerspectiveEvent event = new PerspectiveEvent(perspective);
            event.post();
            if (event.cancelled) {
                callback.cancel();
            }
        }
    }
}
