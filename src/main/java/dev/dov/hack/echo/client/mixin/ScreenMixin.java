package dev.dov.hack.echo.client.mixin;
import dev.dov.hack.echo.client.platform.Platform;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void onRenderWithTooltipHead(DrawContext context, int mouseX, int mouseY, float tickProgress, CallbackInfo callback) {
        ((Platform.GuiState) context.state).markScreenLayerStart();
    }
}
