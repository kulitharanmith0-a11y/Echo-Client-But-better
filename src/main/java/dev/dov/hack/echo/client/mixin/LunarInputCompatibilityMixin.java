package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.keyboard.KeyStateOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com/moonsworth/lunar/IHIRRHORRRRHICCOHOHORCHIHHHIOO/CICOHIIRRHHIIHRROHOICOCRHHHHOO/CRRHRIRIHOHRHRCCHRCOICHOOIHOIC", remap = false)
public abstract class LunarInputCompatibilityMixin {
    @Inject(
        method = "CRRHRIRIHOHRHRCCHRCOICHOOIHOIC(Lcom/moonsworth/lunar/IHHHICHHRICRCIOHCIOHHOHIOIORRH/RCHOIOIIRCHIRRHIIHHRHOORCCIHCH/CICOHIIRRHHIIHRROHOICOCRHHHHOO;)Z", at = @At("HEAD"), cancellable = true, require = 0
    )
    private void overrideLunarKeyState(@Coerce Object lunarKey, CallbackInfoReturnable<Boolean> callback) {
        if (!EchoClient.eventsDisabled) {
            Boolean overriddenState = KeyStateOverrides.forBoundKey(lunarKey);
            if (overriddenState != null) {
                callback.setReturnValue(overriddenState);
            }
        }
    }
}
