package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.handlers.input.InputSimulator;
import dev.dov.hack.echo.client.keyboard.KeybindingCache;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputUtil.class)
public class InputUtilMixin {
    @Inject(method = "isKeyPressed", at = @At("HEAD"), cancellable = true)
    private static void useCachedKeyState(Window window, int keyCode, CallbackInfoReturnable<Boolean> callback) {
        Boolean pressed = KeybindingCache.getOverride(keyCode);
        if (pressed != null) {
            callback.setReturnValue(pressed);
            return;
        }
        InputSimulator simulator = InputSimulator.get();
        if (simulator != null && simulator.isOsSimulatedKey(keyCode)) {
            callback.setReturnValue(false);
        }
    }
}
