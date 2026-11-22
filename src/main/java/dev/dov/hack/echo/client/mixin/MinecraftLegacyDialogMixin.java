package dev.dov.hack.echo.client.mixin;
import dev.dov.hack.echo.client.platform.Platform;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public abstract class MinecraftLegacyDialogMixin {
    @Redirect(method = "<init>(Lnet/minecraft/client/RunArgs;)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/tinyfd/TinyFileDialogs;tinyfd_messageBox(Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Z)Z", remap = false), require = 2, expect = 2, allow = 2)
    private static boolean echo$legacyMessageBox(CharSequence title, CharSequence message, CharSequence dialogType, CharSequence iconType, boolean defaultButton) {
        return Platform.DialogBridge.messageBox(title, message, dialogType, iconType, defaultButton);
    }
}
