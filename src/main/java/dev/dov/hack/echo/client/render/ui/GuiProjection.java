package dev.dov.hack.echo.client.render.ui;

import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import org.joml.Matrix4f;

public final class GuiProjection {
    private GuiProjection() {}

    public static Matrix4f create(RenderDevice device) {
        return new Matrix4f().ortho(0.0f, GuiDimensions.width(), GuiDimensions.height(), 0.0f, -1000.0f, 1000.0f, device.isVulkan());
    }
}
