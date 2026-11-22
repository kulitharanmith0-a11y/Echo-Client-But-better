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
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

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
    private boolean streamProofCursorHidden;

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
        } catch (Throwable failure) {
            closeQuietly(createdQueue == null ? null : createdQueue::destroy);
            closeQuietly(createdSurface == null ? null : createdSurface::destroy);
            closeQuietly(createdWindow == null ? null : createdWindow::close);
            closeQuietly(createdBlur::close);
            throw failure;
        }
    }

    public long windowHandle() {
        return window.windowHandle();
    }

    public void hideOverlayImmediately() {
        restoreCursorIfHidden();
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
            restoreCursorIfHidden();
            window.setVisible(false);
            return;
        }

        window.synchronizeWithMainWindow();
        if (window.isOwnerMinimizedOrHidden() || window.isOwnerExclusiveFullscreen()) {
            restoreCursorIfHidden();
            window.setVisible(false);
            return;
        }
        if (!window.prepareCaptureProtectedShow()) {
            restoreCursorIfHidden();
            window.setVisible(false);
            if (window.isCaptureFailed()) {
                EchoClient.failStreamProof();
            }
            return;
        }
        int width = window.framebufferWidth();
        int height = window.framebufferHeight();
        if (width <= 0 || height <= 0) {
            restoreCursorIfHidden();
            window.setVisible(false);
            return;
        }

        boolean clientScreenOpen = client.currentScreen instanceof ClientScreen;
        boolean vanillaScreenOpen = client.currentScreen != null && !clientScreenOpen;
        updateStreamProofCursor(client, mcWindow, clientScreenOpen);

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
                drawStreamProofCursor(client, mcWindow);
            }
            EchoClient.uiRenderer.beginFrame(commands, target, depthTexture.views[0], OptionalInt.empty());
            EchoClient.uiRenderer.endFrame();
        }
        surface.present(queue, commands);
        window.setVisible(true);
    }

    private void updateStreamProofCursor(MinecraftClient client, Window mcWindow, boolean hideOsCursor) {
        if (mcWindow == null) {
            return;
        }
        long handle = mcWindow.getHandle();
        if (handle == 0L) {
            return;
        }
        if (hideOsCursor) {
            if (GLFW.glfwGetInputMode(handle, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_HIDDEN) {
                GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
            }
            streamProofCursorHidden = true;
        } else if (streamProofCursorHidden) {
            restoreCursorIfHidden();
        }
    }

    private void restoreCursorIfHidden() {
        if (!streamProofCursorHidden) {
            return;
        }
        streamProofCursorHidden = false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().getHandle();
        if (handle == 0L) {
            return;
        }
        int desired = (client.currentScreen == null && client.mouse != null && client.mouse.isCursorLocked())
                ? GLFW.GLFW_CURSOR_DISABLED
                : GLFW.GLFW_CURSOR_NORMAL;
        if (GLFW.glfwGetInputMode(handle, GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_HIDDEN) {
            GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, desired);
        }
    }

    private static void drawStreamProofCursor(MinecraftClient client, Window mcWindow) {
        if (EchoClient.uiRenderer == null || client.mouse == null || mcWindow == null) {
            return;
        }
        int scaledW = mcWindow.getScaledWidth();
        int scaledH = mcWindow.getScaledHeight();
        if (scaledW <= 0 || scaledH <= 0) {
            return;
        }
        float x = (float) (client.mouse.getScaledX(mcWindow) * GuiDimensions.width() / scaledW);
        float y = (float) (client.mouse.getScaledY(mcWindow) * GuiDimensions.height() / scaledH);
        // Clean software pointer drawn exclusively on the capture-excluded overlay window.
        EchoClient.uiRenderer.drawQuadraticStroke(x, y, x + 5.0f, y + 6.0f, x + 10.5f, y + 11.5f, 3.2f, 0xFF000000);
        EchoClient.uiRenderer.drawQuadraticStroke(x, y, x, y + 7.5f, x, y + 14.5f, 3.2f, 0xFF000000);
        EchoClient.uiRenderer.drawQuadraticStroke(x, y + 14.0f, x + 4.5f, y + 11.5f, x + 10.0f, y + 11.5f, 3.0f, 0xFF000000);
        EchoClient.uiRenderer.drawQuadraticStroke(x + 0.6f, y + 0.8f, x + 4.8f, y + 5.8f, x + 9.4f, y + 10.6f, 1.8f, 0xFFFFFFFF);
        EchoClient.uiRenderer.drawQuadraticStroke(x + 0.6f, y + 0.8f, x + 0.6f, y + 7.2f, x + 0.6f, y + 13.2f, 1.8f, 0xFFFFFFFF);
        EchoClient.uiRenderer.drawQuadraticStroke(x + 0.8f, y + 12.8f, x + 4.4f, y + 10.8f, x + 9.0f, y + 10.8f, 1.6f, 0xFFFFFFFF);
        EchoClient.uiRenderer.drawRoundedRect(x - 1.0f, y - 1.0f, 3.2f, 3.2f, 1.6f, 0xFFFFFFFF, 0.8f, 0xFF000000);
    }

    @Override
    public void close() {
        closeQuietly(this::restoreCursorIfHidden);
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
