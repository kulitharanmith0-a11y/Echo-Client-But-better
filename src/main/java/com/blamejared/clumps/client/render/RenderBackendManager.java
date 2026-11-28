package com.blamejared.clumps.client.render;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.gui.ClientScreen;
import com.blamejared.clumps.client.modules.internals.ClickGui;
import com.blamejared.clumps.client.render.ui.GuiDimensions;
import com.blamejared.clumps.natives.WindowHandle;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.opengl.OpenGlDevice;
import com.blamejared.clumps.client.gfx.opengl.OpenGlTexture;
import com.blamejared.clumps.client.gfx.queue.CommandQueue;
import com.blamejared.clumps.client.gfx.queue.QueueInfo;
import com.blamejared.clumps.client.gfx.surface.SurfaceInfo.PresentMode;
import com.blamejared.clumps.client.gfx.surface.Surface;
import com.blamejared.clumps.client.gfx.surface.SurfaceInfo;
import com.blamejared.clumps.client.gfx.texture.Texture;
import com.blamejared.clumps.client.gfx.texture.TextureInfo;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureFormat;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class RenderBackendManager implements AutoCloseable {
    private static final int GRAPHICS_AND_PRESENT_QUEUE = 9;
    private static final int DEPTH_ATTACHMENT_USAGE = 8;
    private static final int COLOR_ATTACHMENT_USAGE = 9;
    private static final int DEPTH_MIP_LEVELS = 1;

    private final WindowHandle window;
    private final boolean openGl;
    private final boolean layeredWindows;
    private final Surface surface;
    private final CommandQueue queue;
    private Texture depthTexture;
    private PresentMode presentMode;
    private int framebufferWidth;
    private int framebufferHeight;
    private final FramebufferBlur framebufferBlur;
    private final long invisibleCursor;
    private boolean osCursorHidden;
    private final double[] cursorXBuf = new double[1];
    private final double[] cursorYBuf = new double[1];

    // Offscreen FBO + double-buffered async PBOs for Windows zero-SwapBuffers layered presentation
    private OpenGlTexture layeredColorTexture;
    private int layeredReadFbo;
    private final int[] layeredPbos = new int[2];
    private int layeredPboIndex;
    private boolean layeredPboPrimed;
    private int layeredWidth;
    private int layeredHeight;
    private long lastLayeredPresentNanos;

    public RenderBackendManager() {
        FramebufferBlur createdBlur = new FramebufferBlur();
        WindowHandle createdWindow = null;
        Surface createdSurface = null;
        CommandQueue createdQueue = null;
        try {
            openGl = ClumpsClient.renderDevice instanceof OpenGlDevice;
            createdWindow = new WindowHandle(openGl);
            layeredWindows = openGl && createdWindow.isLayeredWindows();
            if (layeredWindows) {
                createdSurface = null;
                createdQueue = ClumpsClient.renderDevice.createQueue(QueueInfo.builder()
                        .NativeCapabilities(GRAPHICS_AND_PRESENT_QUEUE).build());
            } else {
                createdSurface = ClumpsClient.renderDevice.createSurface(
                        SurfaceInfo.builder().windowHandle(createdWindow.windowHandle()).build());
                createdQueue = ClumpsClient.renderDevice.createQueue(QueueInfo.builder()
                        .NativeCapabilities(GRAPHICS_AND_PRESENT_QUEUE).surface(createdSurface).build());
            }
            framebufferBlur = createdBlur;
            window = createdWindow;
            surface = createdSurface;
            queue = createdQueue;
            invisibleCursor = createInvisibleCursor();
        } catch (Throwable failure) {
            closeQuietly(createdQueue == null ? null : createdQueue::destroy);
            closeQuietly(createdSurface == null ? null : createdSurface::destroy);
            closeQuietly(createdWindow == null ? null : createdWindow::close);
            closeQuietly(createdBlur::close);
            throw failure;
        }
    }

    private static long createInvisibleCursor() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pixels = stack.calloc(4);
            GLFWImage image = GLFWImage.calloc(stack);
            image.width(1);
            image.height(1);
            image.pixels(pixels);
            return GLFW.glfwCreateCursor(image, 0, 0);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    public long windowHandle() {
        return window.windowHandle();
    }

    public void hideOverlayImmediately() {
        restoreOsCursor();
        layeredPboPrimed = false;
        lastLayeredPresentNanos = 0L;
        window.setVisible(false);
    }

    public void endFrame() {
        try {
            renderFrame();
        } catch (Throwable failure) {
            layeredPboPrimed = false;
            lastLayeredPresentNanos = 0L;
            closeQuietly(() -> window.setVisible(false));
        } finally {
            framebufferBlur.resetFrame();
            if (ClumpsClient.worldRenderer != null) {
                ClumpsClient.worldRenderer.clearExternalOverlays();
            }
            if (ClumpsClient.uiRenderer != null) {
                ClumpsClient.uiRenderer.clearFrameQueues();
            }
        }
    }

    public void renderFrame() {
        MinecraftClient client = MinecraftClient.getInstance();
        Window mcWindow = client == null ? null : client.getWindow();
        if (ClumpsClient.externalRendererActive && !ClumpsClient.eventsDisabled && mcWindow != null && window.isOwnerExclusiveFullscreen()) {
            window.setVisible(false);
            layeredPboPrimed = false;
            try {
                ((com.blamejared.clumps.client.platform.Platform.DisplayControl) (Object) mcWindow).invalidateFullscreenState();
            } catch (Throwable ignored) {
            }
            return;
        }
        boolean visible = ClumpsClient.externalRendererActive
                && !ClumpsClient.eventsDisabled
                && client != null
                && client.world != null
                && mcWindow != null
                && !mcWindow.isMinimized()
                && !mcWindow.hasZeroWidthOrHeight()
                && !window.isOwnerMinimizedOrHidden()
                && !window.isOwnerExclusiveFullscreen();
        if (!visible) {
            restoreOsCursor();
            layeredPboPrimed = false;
            lastLayeredPresentNanos = 0L;
            window.setVisible(false);
            return;
        }

        boolean clientScreenOpen = client.currentScreen instanceof ClientScreen;
        boolean vanillaScreenOpen = client.currentScreen != null && !clientScreenOpen;
        syncOsCursorVisibility(client, mcWindow, clientScreenOpen);

        if (layeredWindows) {
            boolean hasUiCalls = ClumpsClient.uiRenderer != null && ClumpsClient.uiRenderer.hasPendingCalls();
            boolean hasWorldOverlays = ClumpsClient.worldRenderer != null && ClumpsClient.worldRenderer.hasWorldOverlayContent();
            boolean hasOverlayContent = clientScreenOpen || framebufferBlur.isActive() || hasUiCalls || hasWorldOverlays;
            if (vanillaScreenOpen || !hasOverlayContent) {
                layeredPboPrimed = false;
                lastLayeredPresentNanos = 0L;
                window.setVisible(false);
                return;
            }
        }

        window.synchronizeWithMainWindow();
        if (window.isOwnerMinimizedOrHidden() || window.isOwnerExclusiveFullscreen()) {
            restoreOsCursor();
            layeredPboPrimed = false;
            lastLayeredPresentNanos = 0L;
            window.setVisible(false);
            return;
        }
        // Fail-closed: never turn StreamProof off or fall back to Minecraft's main framebuffer during transient transitions.
        if (!window.prepareCaptureProtectedShow()) {
            restoreOsCursor();
            layeredPboPrimed = false;
            lastLayeredPresentNanos = 0L;
            window.setVisible(false);
            return;
        }

        int width = window.framebufferWidth();
        int height = window.framebufferHeight();
        if (width <= 0 || height <= 0) {
            restoreOsCursor();
            layeredPboPrimed = false;
            lastLayeredPresentNanos = 0L;
            window.setVisible(false);
            return;
        }

        if (layeredWindows) {
            renderLayeredWindowsFrame(mcWindow, width, height, clientScreenOpen);
            return;
        }

        PresentMode requestedMode = client.options.getEnableVsync().getValue()
                ? PresentMode.FIFO : PresentMode.IMMEDIATE;
        if (width != framebufferWidth || height != framebufferHeight
                || requestedMode != presentMode || surface.needsResize()) {
            surface.resize(width, height, requestedMode);
            framebufferWidth = width;
            framebufferHeight = height;
            presentMode = requestedMode;
        }

        TextureView target = surface.acquireTexture();
        if (target == null) {
            window.setVisible(false);
            return;
        }
        ensureDepthTexture(target.width(0), target.height(0));
        CommandEncoder commands = queue.beginCommands();
        if (vanillaScreenOpen) {
            ClumpsClient.worldRenderer.compositeToTarget(commands, target, ClumpsClient.worldRenderer.mainColorTexture(), false);
        } else {
            if (framebufferBlur.isActive()) {
                ClumpsClient.worldRenderer.compositeToTarget(commands, target, framebufferBlur.colorTexture(), false);
            } else {
                ClumpsClient.worldRenderer.compositeToTarget(commands, target, ClumpsClient.worldRenderer.mainColorTexture(), true);
            }
            if (clientScreenOpen) {
                queueSoftwareCursor(mcWindow);
            }
            ClumpsClient.uiRenderer.beginFrame(commands, target, depthTexture.views[0], OptionalInt.empty());
            ClumpsClient.uiRenderer.endFrame();
        }
        surface.present(queue, commands);
        window.setVisible(true);
    }

    private void renderLayeredWindowsFrame(Window mcWindow, int width, int height, boolean clientScreenOpen) {
        if (!ensureLayeredTargets(width, height)) {
            layeredPboPrimed = false;
            lastLayeredPresentNanos = 0L;
            window.setVisible(false);
            return;
        }
        long now = System.nanoTime();
        long minInterval = ClickGui.streamProofMinFrameNanos();
        if (minInterval > 0L && layeredPboPrimed && window.isOverlayRevealed()
                && (now - lastLayeredPresentNanos) < minInterval) {
            return;
        }
        lastLayeredPresentNanos = now;

        TextureView target = layeredColorTexture.views[0];
        ensureDepthTexture(width, height);
        CommandEncoder commands = queue.beginCommands();
        if (framebufferBlur.isActive()) {
            ClumpsClient.worldRenderer.compositeToTarget(commands, target, framebufferBlur.colorTexture(), false);
        } else {
            ClumpsClient.worldRenderer.compositeToTarget(commands, target, ClumpsClient.worldRenderer.mainColorTexture(), true);
        }
        if (clientScreenOpen) {
            queueSoftwareCursor(mcWindow);
        }
        ClumpsClient.uiRenderer.beginFrame(commands, target, depthTexture.views[0], OptionalInt.empty());
        ClumpsClient.uiRenderer.endFrame();
        queue.submit(commands);

        readbackAndPresentLayered(width, height);
    }

    private boolean ensureLayeredTargets(int width, int height) {
        if (layeredColorTexture != null && layeredReadFbo != 0
                && layeredPbos[0] != 0 && layeredPbos[1] != 0
                && layeredWidth == width && layeredHeight == height) {
            return true;
        }
        destroyLayeredTargets();
        int prevReadFbo = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int prevPackBuf = GL11C.glGetInteger(GL21C.GL_PIXEL_PACK_BUFFER_BINDING);
        try {
            OpenGlTexture color = new OpenGlTexture(width, height, 1, TextureFormat.BGRA8Unorm, COLOR_ATTACHMENT_USAGE, 1);
            int fbo = GL30C.glGenFramebuffers();
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, fbo);
            GL30C.glFramebufferTexture2D(GL30C.GL_READ_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                    GL11C.GL_TEXTURE_2D, color.textureId, 0);
            int status = GL30C.glCheckFramebufferStatus(GL30C.GL_READ_FRAMEBUFFER);
            if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
                GL30C.glDeleteFramebuffers(fbo);
                color.destroy();
                return false;
            }
            long byteSize = (long) width * (long) height * 4L;
            int pbo0 = GL15C.glGenBuffers();
            int pbo1 = GL15C.glGenBuffers();
            GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, pbo0);
            GL15C.glBufferData(GL21C.GL_PIXEL_PACK_BUFFER, byteSize, GL15C.GL_STREAM_READ);
            GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, pbo1);
            GL15C.glBufferData(GL21C.GL_PIXEL_PACK_BUFFER, byteSize, GL15C.GL_STREAM_READ);

            layeredColorTexture = color;
            layeredReadFbo = fbo;
            layeredPbos[0] = pbo0;
            layeredPbos[1] = pbo1;
            layeredPboIndex = 0;
            layeredPboPrimed = false;
            layeredWidth = width;
            layeredHeight = height;
            framebufferWidth = width;
            framebufferHeight = height;
            return true;
        } finally {
            GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, prevPackBuf);
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, prevReadFbo);
        }
    }

    private void readbackAndPresentLayered(int width, int height) {
        int prevReadFbo = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int prevPackBuf = GL11C.glGetInteger(GL21C.GL_PIXEL_PACK_BUFFER_BINDING);
        int prevPackAlign = GL11C.glGetInteger(GL11C.GL_PACK_ALIGNMENT);
        int prevPackRowLen = GL11C.glGetInteger(GL11C.GL_PACK_ROW_LENGTH);
        int prevPackSkipRows = GL11C.glGetInteger(GL11C.GL_PACK_SKIP_ROWS);
        int prevPackSkipPixels = GL11C.glGetInteger(GL11C.GL_PACK_SKIP_PIXELS);
        try {
            GL11C.glPixelStorei(GL11C.GL_PACK_ALIGNMENT, 4);
            GL11C.glPixelStorei(GL11C.GL_PACK_ROW_LENGTH, 0);
            GL11C.glPixelStorei(GL11C.GL_PACK_SKIP_ROWS, 0);
            GL11C.glPixelStorei(GL11C.GL_PACK_SKIP_PIXELS, 0);

            int curr = layeredPboIndex;
            int prev = curr ^ 1;
            long byteSize = (long) width * (long) height * 4L;

            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, layeredReadFbo);
            GL11C.glReadBuffer(GL30C.GL_COLOR_ATTACHMENT0);
            GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, layeredPbos[curr]);
            GL11C.glReadPixels(0, 0, width, height, GL12C.GL_BGRA, GL11C.GL_UNSIGNED_BYTE, 0L);

            if (layeredPboPrimed) {
                GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, layeredPbos[prev]);
                ByteBuffer mapped = GL30C.glMapBufferRange(GL21C.GL_PIXEL_PACK_BUFFER, 0L, byteSize, GL30C.GL_MAP_READ_BIT);
                if (mapped != null) {
                    try {
                        window.presentLayeredBgra(MemoryUtil.memAddress(mapped), width, height);
                    } finally {
                        GL15C.glUnmapBuffer(GL21C.GL_PIXEL_PACK_BUFFER);
                    }
                }
            }
            layeredPboPrimed = true;
            layeredPboIndex = prev;
        } finally {
            GL11C.glPixelStorei(GL11C.GL_PACK_ALIGNMENT, prevPackAlign);
            GL11C.glPixelStorei(GL11C.GL_PACK_ROW_LENGTH, prevPackRowLen);
            GL11C.glPixelStorei(GL11C.GL_PACK_SKIP_ROWS, prevPackSkipRows);
            GL11C.glPixelStorei(GL11C.GL_PACK_SKIP_PIXELS, prevPackSkipPixels);
            GL15C.glBindBuffer(GL21C.GL_PIXEL_PACK_BUFFER, prevPackBuf);
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, prevReadFbo);
        }
    }

    private void destroyLayeredTargets() {
        if (layeredPbos[0] != 0) {
            GL15C.glDeleteBuffers(layeredPbos[0]);
            layeredPbos[0] = 0;
        }
        if (layeredPbos[1] != 0) {
            GL15C.glDeleteBuffers(layeredPbos[1]);
            layeredPbos[1] = 0;
        }
        if (layeredReadFbo != 0) {
            GL30C.glDeleteFramebuffers(layeredReadFbo);
            layeredReadFbo = 0;
        }
        if (layeredColorTexture != null) {
            layeredColorTexture.destroy();
            layeredColorTexture = null;
        }
        layeredPboIndex = 0;
        layeredPboPrimed = false;
        layeredWidth = 0;
        layeredHeight = 0;
    }

    private void syncOsCursorVisibility(MinecraftClient client, Window mcWindow, boolean hideCursor) {
        if (mcWindow == null || mcWindow.getHandle() == 0L) {
            return;
        }
        long mainHandle = mcWindow.getHandle();
        if (hideCursor) {
            if (GLFW.glfwGetInputMode(mainHandle, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_HIDDEN) {
                GLFW.glfwSetInputMode(mainHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
            }
            if (invisibleCursor != 0L) {
                GLFW.glfwSetCursor(mainHandle, invisibleCursor);
            }
            osCursorHidden = true;
        } else if (osCursorHidden) {
            restoreOsCursor();
        }
    }

    private void restoreOsCursor() {
        if (!osCursorHidden) {
            return;
        }
        osCursorHidden = false;
        MinecraftClient client = MinecraftClient.getInstance();
        Window mcWindow = client == null ? null : client.getWindow();
        if (mcWindow == null || mcWindow.getHandle() == 0L) {
            return;
        }
        long mainHandle = mcWindow.getHandle();
        try {
            GLFW.glfwSetCursor(mainHandle, 0L);
            if (GLFW.glfwGetInputMode(mainHandle, GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN) {
                boolean locked = client.mouse != null && client.mouse.isCursorLocked();
                GLFW.glfwSetInputMode(mainHandle, GLFW.GLFW_CURSOR,
                        locked ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
            }
        } catch (Throwable ignored) {
        }
    }

    private void queueSoftwareCursor(Window mcWindow) {
        if (ClumpsClient.uiRenderer == null || mcWindow == null || mcWindow.getHandle() == 0L) {
            return;
        }
        long mainHandle = mcWindow.getHandle();
        boolean focused = GLFW.glfwGetWindowAttrib(mainHandle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
        boolean hovered = GLFW.glfwGetWindowAttrib(mainHandle, GLFW.GLFW_HOVERED) == GLFW.GLFW_TRUE;
        if (!focused && !hovered) {
            return;
        }
        int windowWidth = mcWindow.getWidth();
        int windowHeight = mcWindow.getHeight();
        if (windowWidth <= 0 || windowHeight <= 0) {
            return;
        }
        GLFW.glfwGetCursorPos(mainHandle, cursorXBuf, cursorYBuf);
        double cx = cursorXBuf[0];
        double cy = cursorYBuf[0];
        if (cx < 0.0 || cy < 0.0 || cx > windowWidth || cy > windowHeight) {
            return;
        }
        float uiX = (float) (cx * GuiDimensions.width() / windowWidth);
        float uiY = (float) (cy * GuiDimensions.height() / windowHeight);
        ClumpsClient.uiRenderer.drawPointerCursor(uiX, uiY);
    }

    @Override
    public void close() {
        closeQuietly(this::restoreOsCursor);
        if (invisibleCursor != 0L) {
            closeQuietly(() -> GLFW.glfwDestroyCursor(invisibleCursor));
        }
        closeQuietly(() -> window.setVisible(false));
        closeQuietly(queue::waitIdle);
        closeQuietly(this::destroyLayeredTargets);
        closeQuietly(framebufferBlur::close);
        if (surface != null) {
            closeQuietly(surface::destroy);
        }
        if (depthTexture != null) {
            closeQuietly(depthTexture::destroy);
            depthTexture = null;
        }
        closeQuietly(queue::destroy);
        closeQuietly(window::close);
    }

    private void ensureDepthTexture(int width, int height) {
        if (depthTexture != null && depthTexture.width == width && depthTexture.height == height) {
            return;
        }
        queue.waitIdle();
        if (depthTexture != null) {
            depthTexture.destroy();
        }
        depthTexture = ClumpsClient.renderDevice.createTexture(TextureInfo.builder()
                .width(width).height(height).format(ClumpsClient.renderDevice.depthFormat())
                .usage(DEPTH_ATTACHMENT_USAGE).mipCount(DEPTH_MIP_LEVELS).build());
    }

    public FramebufferBlur framebufferBlur() {
        return framebufferBlur;
    }

    private static void closeQuietly(Runnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (Throwable ignored) {
        }
    }
}
