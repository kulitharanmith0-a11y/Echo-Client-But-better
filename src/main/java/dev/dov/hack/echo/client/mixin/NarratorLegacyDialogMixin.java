package dev.dov.hack.echo.client.mixin;
import dev.dov.hack.echo.client.platform.Platform;

import net.minecraft.client.util.NarratorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NarratorManager.class)
public abstract class NarratorLegacyDialogMixin {
    @Redirect(method = "checkNarratorLibrary(Z)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/util/tinyfd/TinyFileDialogs;tinyfd_messageBox(Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Z)Z", remap = false), require = 1, expect = 1, allow = 1)
    private static boolean echo$legacyMessageBox(CharSequence title, CharSequence message, CharSequence dialogType, CharSequence iconType, boolean defaultButton) {
        return Platform.DialogBridge.messageBox(title, message, dialogType, iconType, defaultButton);
    }
}
