package dev.dov.hack.echo.client.render.ui;

import net.minecraft.client.MinecraftClient;

public final class GuiDimensions {
    public static final double SCALE = 2.0;

    private GuiDimensions() {}

    public static int width() {
        return (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferWidth() / SCALE);
    }

    public static int height() {
        return (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferHeight() / SCALE);
    }
}
