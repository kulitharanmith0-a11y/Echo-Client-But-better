package dev.dov.hack.echo.client.gfx;

import com.mojang.blaze3d.systems.CommandEncoder;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

public final class FramebufferOperations {
    private FramebufferOperations() {
    }

    
    public static Framebuffer create(String name, int width, int height, boolean useDepth) {
        return new SimpleFramebuffer(name, width, height, useDepth);
    }

    
    public static void clear(CommandEncoder encoder, Framebuffer framebuffer) {
        encoder.clearColorAndDepthTextures(
                framebuffer.getColorAttachment(), 0, framebuffer.getDepthAttachment(), 1.0);
    }

    
    public static void blitColor(Framebuffer source, Framebuffer destination) {
        source.drawBlit(destination.getColorAttachmentView());
    }
}
