package com.blamejared.clumps.client.gfx.opengl;

public interface GlFramebufferAccess {
    
    int depthInternalFormat();

    void bindFramebuffer(int target, int framebuffer);

    void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int mipLevel);

    void deleteFramebuffer(int framebuffer);
}
