package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.keyboard.KeyStateOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = LunarKeyStateCompatibilityMixin.EXTERNAL_LUNAR_KEY_STATE_CLASS, remap = false)
public class LunarKeyStateCompatibilityMixin {
    
    public static final String EXTERNAL_LUNAR_KEY_STATE_CLASS =
            "com/moonsworth/lunar/IHIRRHORRRRHICCOHOHORCHIHHHIOO/"
            + "CICOHIIRRHHIIHRROHOICOCRHHHHOO/CICOHIIRRHHIIHRROHOICOCRHHHHOO";
    public static final String EXTERNAL_LUNAR_KEY_STATE_METHOD = "IHICROHHIOORRHIICCRCHICRROICIR(I)Z";

    @Inject(method = EXTERNAL_LUNAR_KEY_STATE_METHOD, at = @At("HEAD"), cancellable = true, require = 0)
    private void onExternalKeyStateQuery(int keyCode, CallbackInfoReturnable<Boolean> callback) {
        if (!EchoClient.eventsDisabled) {
            Boolean override = KeyStateOverrides.pressedActionForMouseButton(keyCode);
            if (override != null) {
                callback.setReturnValue(override);
            }
        }
    }
}
