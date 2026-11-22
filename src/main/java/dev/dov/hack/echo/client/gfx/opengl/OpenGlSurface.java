package dev.dov.hack.echo.client.gfx.opengl;

import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.queue.CommandQueue;
import dev.dov.hack.echo.client.gfx.surface.SurfaceInfo.PresentMode;
import dev.dov.hack.echo.client.gfx.surface.Surface;
import dev.dov.hack.echo.client.gfx.surface.SurfaceInfo;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureFormat;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import dev.dov.hack.echo.client.gfx.window.WindowSystem;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL32C;

public class OpenGlSurface extends Surface {
    private final long windowHandle;
    private final WindowSystem windowSystem;
    private final TextureFormat format;
    private final int[] fbWidth = new int[1];
    private final int[] fbHeight = new int[1];
    private OpenGlTexture texture;
    private int readFramebuffer;
    private int width;
    private int height;

    public OpenGlSurface(SurfaceInfo info) {
        windowHandle = info.windowHandle;
        windowSystem = info.windowSystem;
        format = info.format;
    }

    @Override
    public void resize(int width, int height, PresentMode presentMode) {
        destroyAttachments();
        this.width = width;
        this.height = height;
        texture = new OpenGlTexture(width, height, 1, format, 9, 1);
        // Ensure shared texture storage is materialized across contexts (required on Intel GL 3.3).
        GL11C.glFinish();
        long previousContext = windowSystem.currentContextWindow();
        windowSystem.makeContextCurrent(windowHandle);
        try {
            int fbo = GL32C.glGenFramebuffers();
            GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, fbo);
            GL32C.glFramebufferTexture2D(GL32C.GL_READ_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0, GL32C.GL_TEXTURE_2D, texture.textureId, 0);
            int status = GL32C.glCheckFramebufferStatus(GL32C.GL_READ_FRAMEBUFFER);
            GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, 0);
            if (status == GL32C.GL_FRAMEBUFFER_COMPLETE) {
                readFramebuffer = fbo;
            } else {
                GL32C.glDeleteFramebuffers(fbo);
                readFramebuffer = 0;
            }
            windowSystem.setSwapInterval(0);
        } finally {
            windowSystem.makeContextCurrent(previousContext);
        }
    }

    @Override
    public boolean needsResize() {
        return readFramebuffer == 0 || texture == null;
    }

    @Override
    public TextureView acquireTexture() {
        return texture == null ? null : texture.views[0];
    }

    @Override
    public void present(CommandQueue queue, CommandEncoder commands) {
        queue.submit(commands);
        if (readFramebuffer == 0 || texture == null || width <= 0 || height <= 0) {
            return;
        }
        long fence = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        if (fence != 0L) {
            GL11C.glFlush();
        } else {
            GL11C.glFinish();
        }
        long previousContext = windowSystem.currentContextWindow();
        windowSystem.makeContextCurrent(windowHandle);
        try {
            if (fence != 0L) {
                GL32C.glWaitSync(fence, 0, GL32C.GL_TIMEOUT_IGNORED);
            }
            GLFW.glfwGetFramebufferSize(windowHandle, fbWidth, fbHeight);
            int dstWidth = fbWidth[0] > 0 ? fbWidth[0] : width;
            int dstHeight = fbHeight[0] > 0 ? fbHeight[0] : height;
            int filter = (dstWidth == width && dstHeight == height) ? GL11C.GL_NEAREST : GL11C.GL_LINEAR;
            GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
            GL11C.glColorMask(true, true, true, true);
            GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL11C.glReadBuffer(GL32C.GL_COLOR_ATTACHMENT0);
            GL32C.glBindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, 0);
            GL11C.glDrawBuffer(GL11C.GL_BACK);
            GL32C.glBlitFramebuffer(0, 0, width, height, 0, dstHeight, dstWidth, 0, GL32C.GL_COLOR_BUFFER_BIT, filter);
            GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, 0);
            GL11C.glFlush();
            windowSystem.swapBuffers(windowHandle);
        } finally {
            windowSystem.makeContextCurrent(previousContext);
            if (fence != 0L) {
                GL32C.glDeleteSync(fence);
            }
        }
    }

    @Override
    public void destroy() { destroyAttachments(); }

    private void destroyAttachments() {
        if (readFramebuffer != 0) {
            long previousContext = windowSystem.currentContextWindow();
            windowSystem.makeContextCurrent(windowHandle);
            try {
                GL32C.glDeleteFramebuffers(readFramebuffer);
            } finally {
                windowSystem.makeContextCurrent(previousContext);
            }
            readFramebuffer = 0;
        }
        if (texture != null) {
            texture.destroy();
            texture = null;
        }
    }
}
