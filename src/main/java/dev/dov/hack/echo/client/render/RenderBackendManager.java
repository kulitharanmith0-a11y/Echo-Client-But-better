package dev.dov.hack.echo.client.render;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.gui.ClientScreen;
import dev.dov.hack.echo.client.render.ui.GuiDimensions;
import dev.jadyen.hack.echo.natives.WindowHandle;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.opengl.OpenGlDevice;
import dev.dov.hack.echo.client.gfx.queue.CommandQueue;
import dev.dov.hack.echo.client.gfx.queue.QueueInfo;
import dev.dov.hack.echo.client.gfx.surface.SurfaceInfo.PresentMode;
import dev.dov.hack.echo.client.gfx.surface.Surface;
import dev.dov.hack.echo.client.gfx.surface.SurfaceInfo;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

public final class RenderBackendManager implements AutoCloseable {
    private static final int GRAPHICS_AND_PRESENT_QUEUE = 9;
    private static final int DEPTH_ATTACHMENT_USAGE = 8;
    private static final int DEPTH_MIP_LEVELS = 1;

    private final WindowHandle window;
    private final Surface surface;
    private final CommandQueue queue;
    private Texture depthTexture;
    private PresentMode presentMode;
    private int framebufferWidth;
    private int framebufferHeight;
    private final boolean openGl;
    private final FramebufferBlur framebufferBlur;
    private final long invisibleCursor;
    private boolean osCursorHidden;
    private final double[] cursorXBuf = new double[1];
    private final double[] cursorYBuf = new double[1];

    public RenderBackendManager() {
        FramebufferBlur createdBlur = new FramebufferBlur();
        WindowHandle createdWindow = null;
        Surface createdSurface = null;
        CommandQueue createdQueue = null;
        try {
            openGl = EchoClient.renderDevice instanceof OpenGlDevice;
            createdWindow = new WindowHandle(openGl);
            createdSurface = EchoClient.renderDevice.createSurface(
                    SurfaceInfo.builder().windowHandle(createdWindow.windowHandle()).build());
            createdQueue = EchoClient.renderDevice.createQueue(QueueInfo.builder()
                    .NativeCapabilities(GRAPHICS_AND_PRESENT_QUEUE).surface(createdSurface).build());
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
        window.setVisible(false);
    }

    public void endFrame() {
        try {
            renderFrame();
        } catch (Throwable failure) {
            closeQuietly(() -> window.setVisible(false));
        } finally {
            framebufferBlur.resetFrame();
            if (EchoClient.worldRenderer != null) {
                EchoClient.worldRenderer.clearExternalOverlays();
            }
            if (EchoClient.uiRenderer != null) {
                EchoClient.uiRenderer.clearFrameQueues();
            }
        }
    }

    public void renderFrame() {
        MinecraftClient client = MinecraftClient.getInstance();
        Window mcWindow = client == null ? null : client.getWindow();
        if (EchoClient.externalRendererActive && !EchoClient.eventsDisabled && mcWindow != null && window.isOwnerExclusiveFullscreen()) {
            window.setVisible(false);
            try {
                ((dev.dov.hack.echo.client.platform.Platform.DisplayControl) (Object) mcWindow).invalidateFullscreenState();
            } catch (Throwable ignored) {
            }
            return;
        }
        boolean visible = EchoClient.externalRendererActive
                && !EchoClient.eventsDisabled
                && client != null
                && client.world != null
                && mcWindow != null
                && !mcWindow.isMinimized()
                && !mcWindow.hasZeroWidthOrHeight()
                && !window.isOwnerMinimizedOrHidden()
                && !window.isOwnerExclusiveFullscreen();
        if (!visible) {
            restoreOsCursor();
            window.setVisible(false);
            return;
        }

        window.synchronizeWithMainWindow();
        if (window.isOwnerMinimizedOrHidden() || window.isOwnerExclusiveFullscreen()) {
            restoreOsCursor();
            window.setVisible(false);
            return;
        }
        if (!window.prepareCaptureProtectedShow()) {
            restoreOsCursor();
            window.setVisible(false);
            if (window.isCaptureFailed()) {
                EchoClient.failStreamProof();
            }
            return;
        }
        int width = window.framebufferWidth();
        int height = window.framebufferHeight();
        if (width <= 0 || height <= 0) {
            restoreOsCursor();
            window.setVisible(false);
            return;
        }

        boolean clientScreenOpen = client.currentScreen instanceof ClientScreen;
        boolean vanillaScreenOpen = client.currentScreen != null && !clientScreenOpen;
        syncOsCursorVisibility(client, mcWindow, clientScreenOpen);

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
            EchoClient.worldRenderer.compositeToTarget(commands, target, EchoClient.worldRenderer.mainColorTexture(), false);
        } else {
            if (framebufferBlur.isActive()) {
                EchoClient.worldRenderer.compositeToTarget(commands, target, framebufferBlur.colorTexture(), false);
            } else {
                EchoClient.worldRenderer.compositeToTarget(commands, target, EchoClient.worldRenderer.mainColorTexture(), true);
            }
            if (clientScreenOpen) {
                queueSoftwareCursor(mcWindow);
            }
            EchoClient.uiRenderer.beginFrame(commands, target, depthTexture.views[0], OptionalInt.empty());
            EchoClient.uiRenderer.endFrame();
        }
        surface.present(queue, commands);
        window.setVisible(true);
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
        if (EchoClient.uiRenderer == null || mcWindow == null || mcWindow.getHandle() == 0L) {
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
        EchoClient.uiRenderer.drawPointerCursor(uiX, uiY);
    }

    @Override
    public void close() {
        closeQuietly(this::restoreOsCursor);
        if (invisibleCursor != 0L) {
            closeQuietly(() -> GLFW.glfwDestroyCursor(invisibleCursor));
        }
        closeQuietly(() -> window.setVisible(false));
        closeQuietly(queue::waitIdle);
        closeQuietly(framebufferBlur::close);
        closeQuietly(surface::destroy);
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
        depthTexture = EchoClient.renderDevice.createTexture(TextureInfo.builder()
                .width(width).height(height).format(EchoClient.renderDevice.depthFormat())
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
