package dev.dov.hack.echo.client.mixin;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.EventBus;
import dev.dov.hack.echo.client.event.window.WindowFocusEvent;
import dev.dov.hack.echo.client.event.window.WindowPositionEvent;
import dev.dov.hack.echo.client.event.window.WindowSizeEvent;
import net.minecraft.client.util.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.Platform;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin implements dev.dov.hack.echo.client.platform.Platform.DisplayControl {
    @Shadow @Final private long handle;
    @Shadow private boolean fullscreen;
    @Shadow private boolean currentFullscreen;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;
    @Unique private long clumps$borderlessMonitor;

    @Override
    public void invalidateFullscreenState() {
        if (fullscreen || clumps$borderlessMonitor != 0L || (handle != 0L && GLFW.glfwGetWindowMonitor(handle) != 0L)) {
            currentFullscreen = false;
        }
    }

    @Redirect(method = "updateWindowRegion", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwGetWindowMonitor(J)J", remap = false))
    private long getEffectiveMonitor(long window) {
        return clumps$borderlessMonitor != 0L ? clumps$borderlessMonitor : GLFW.glfwGetWindowMonitor(window);
    }

    @Redirect(method = "updateWindowRegion", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V", remap = false))
    private void applyBorderlessFullscreen(long window, long monitor, int requestedX, int requestedY, int requestedWidth, int requestedHeight, int refreshRate) {
        if (monitor != 0L && !EchoClient.eventsDisabled && EchoClient.externalRendererActive && Platform.get() == Platform.WINDOWS) {
            if (GLFW.glfwGetWindowMonitor(window) != 0L) {
                GLFW.glfwSetWindowMonitor(window, 0L, 0, 0, Math.max(1, requestedWidth), Math.max(1, requestedHeight), GLFW.GLFW_DONT_CARE);
            }
            GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);
            if (videoMode == null) {
                clumps$restoreDecorations(window);
                GLFW.glfwSetWindowMonitor(window, monitor, requestedX, requestedY, requestedWidth, requestedHeight, refreshRate);
                return;
            }
            int[] monitorX = new int[1];
            int[] monitorY = new int[1];
            GLFW.glfwGetMonitorPos(monitor, monitorX, monitorY);
            int baseWidth = videoMode.width();
            int baseHeight = videoMode.height();
            int fullscreenWidth = baseWidth;
            int fullscreenHeight = baseHeight + 1;
            if (clumps$hasAdjacentMonitor(monitor, monitorX[0], monitorY[0] + baseHeight, baseWidth, 1)
                    && !clumps$hasAdjacentMonitor(monitor, monitorX[0] + baseWidth, monitorY[0], 1, baseHeight)) {
                fullscreenWidth = baseWidth + 1;
                fullscreenHeight = baseHeight;
            }
            if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE) {
                GLFW.glfwRestoreWindow(window);
            }
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_FLOATING, GLFW.GLFW_FALSE);
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(window, 0L, monitorX[0], monitorY[0], fullscreenWidth, fullscreenHeight, GLFW.GLFW_DONT_CARE);
            clumps$borderlessMonitor = monitor;
            x = monitorX[0];
            y = monitorY[0];
            width = fullscreenWidth;
            height = fullscreenHeight;
        } else {
            clumps$restoreDecorations(window);
            GLFW.glfwSetWindowMonitor(window, monitor, requestedX, requestedY, requestedWidth, requestedHeight, refreshRate);
        }
    }

    @Unique
    private static boolean clumps$hasAdjacentMonitor(long currentMonitor, int probeX, int probeY, int probeWidth, int probeHeight) {
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors == null) {
            return false;
        }
        int[] otherX = new int[1];
        int[] otherY = new int[1];
        for (int index = 0; index < monitors.limit(); index++) {
            long candidate = monitors.get(index);
            if (candidate == currentMonitor) {
                continue;
            }
            GLFWVidMode mode = GLFW.glfwGetVideoMode(candidate);
            if (mode == null) {
                continue;
            }
            GLFW.glfwGetMonitorPos(candidate, otherX, otherY);
            if (probeX < otherX[0] + mode.width() && probeX + probeWidth > otherX[0]
                    && probeY < otherY[0] + mode.height() && probeY + probeHeight > otherY[0]) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void clumps$restoreDecorations(long window) {
        if (clumps$borderlessMonitor != 0L) {
            clumps$borderlessMonitor = 0L;
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
        }
    }

    @Inject(method = "onWindowPosChanged", at = @At("TAIL"))
    void postWindowPosition(long window, int newX, int newY, CallbackInfo callback) {
        if (EchoClient.eventsDisabled) {
            return;
        }
        EventBus.post(new WindowPositionEvent(newX, newY));
    }

    @Inject(method = "onWindowSizeChanged", at = @At("TAIL"))
    void postWindowSize(long window, int newWidth, int newHeight, CallbackInfo callback) {
        if (EchoClient.eventsDisabled) {
            return;
        }
        EventBus.post(new WindowSizeEvent(newWidth, newHeight));
    }

    @Inject(method = "onWindowFocusChanged", at = @At("TAIL"))
    void postWindowFocus(long window, boolean focused, CallbackInfo callback) {
        if (EchoClient.eventsDisabled) {
            return;
        }
        EventBus.post(new WindowFocusEvent(focused));
    }
}
