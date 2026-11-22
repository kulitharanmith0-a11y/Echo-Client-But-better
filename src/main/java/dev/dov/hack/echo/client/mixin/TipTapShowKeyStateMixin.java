package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.keyboard.KeyStateOverrides;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = TipTapShowKeyStateMixin.EXTERNAL_TIPTAP_RENDERABLE_BUTTON, remap = false)
public class TipTapShowKeyStateMixin {
    
    public static final String EXTERNAL_TIPTAP_RENDERABLE_BUTTON =
            "com/spyxar/tiptapshow/components/RenderableButton";

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z", remap = true), remap = false, require = 0)
    private boolean redirectDisplayedKeyState(KeyBinding binding) {
        return EchoClient.eventsDisabled ? binding.isPressed() : KeyStateOverrides.isPressed(binding);
    }
}
