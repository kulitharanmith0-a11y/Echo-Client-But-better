package dev.dov.hack.echo.client.render.bridge;

import com.mojang.blaze3d.opengl.GlConst;
import dev.dov.hack.echo.client.gfx.opengl.GlFramebufferAccess;
import net.minecraft.client.MinecraftClient;

public final class GlFramebufferBridge implements GlFramebufferAccess {
    @Override
    public int depthInternalFormat() {
        return GlConst.toGlInternalId(MinecraftClient.getInstance().getFramebuffer().getDepthAttachment().getFormat());
    }

    @Override
    public void bindFramebuffer(int target, int framebuffer) {
        GlFramebufferOperations.bindFramebuffer(target, framebuffer);
    }

    @Override
    public void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int mipLevel) {
        GlFramebufferOperations.framebufferTexture2D(target, attachment, textureTarget, texture, mipLevel);
    }

    @Override
    public void deleteFramebuffer(int framebuffer) {
        GlFramebufferOperations.deleteFramebuffer(framebuffer);
    }
}
