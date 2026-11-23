package com.blamejared.clumps.client.render.bridge;

import com.mojang.blaze3d.opengl.GlStateManager;

public final class GlFramebufferOperations {
    private GlFramebufferOperations() {}

    public static void bindFramebuffer(int target, int framebuffer) {
        GlStateManager._glBindFramebuffer(target, framebuffer);
    }

    public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int mipLevel) {
        GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, mipLevel);
    }

    public static void deleteFramebuffer(int framebuffer) {
        GlStateManager._glDeleteFramebuffers(framebuffer);
    }
}
