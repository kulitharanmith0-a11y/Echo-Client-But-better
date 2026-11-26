package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.window.WindowFocusEvent;
import com.blamejared.clumps.client.event.window.WindowPositionEvent;
import com.blamejared.clumps.client.event.window.WindowSizeEvent;
import java.util.Optional;
import net.minecraft.client.gui.cursor.Cursor;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.windows.DEVMODE;
import org.lwjgl.system.windows.GDI32;
import org.lwjgl.system.windows.User32;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin implements com.blamejared.clumps.client.platform.Platform.DisplayControl {
    @Shadow @Final private long handle;
    @Shadow private Optional<VideoMode> fullscreenVideoMode;
    @Shadow private boolean fullscreen;
    @Shadow private boolean currentFullscreen;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;
    @Unique private long clumps$borderlessMonitor;
    @Unique private int clumps$customModeWidth;
    @Unique private int clumps$customModeHeight;
    @Unique private int clumps$customModeRefresh;
    @Unique private boolean clumps$customModeSuspended;
    @Unique private static volatile String clumps$customModeDevice;
    @Unique private static volatile boolean clumps$shutdownHookRegistered;

    @Override
    public void invalidateFullscreenState() {
        boolean exclusiveMonitor = handle != 0L && GLFW.glfwGetWindowMonitor(handle) != 0L;
        if (exclusiveMonitor && ClumpsClient.externalRendererActive && !ClumpsClient.eventsDisabled) {
            fullscreen = true;
            currentFullscreen = false;
            return;
        }
        if (fullscreen || clumps$borderlessMonitor != 0L || exclusiveMonitor) {
            currentFullscreen = false;
        }
    }

    @Redirect(method = "updateWindowRegion", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwGetWindowMonitor(J)J", remap = false))
    private long getEffectiveMonitor(long window) {
        return clumps$borderlessMonitor != 0L ? clumps$borderlessMonitor : GLFW.glfwGetWindowMonitor(window);
    }

    @Redirect(method = "updateWindowRegion", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V", remap = false))
    private void applyBorderlessFullscreen(long window, long monitor, int requestedX, int requestedY, int requestedWidth, int requestedHeight, int refreshRate) {
        if (monitor != 0L && !ClumpsClient.eventsDisabled && ClumpsClient.externalRendererActive && Platform.get() == Platform.WINDOWS) {
            if (GLFW.glfwGetWindowMonitor(window) != 0L) {
                GLFW.glfwSetWindowMonitor(window, 0L, 0, 0, Math.max(1, requestedWidth), Math.max(1, requestedHeight), GLFW.GLFW_DONT_CARE);
            }
            clumps$syncBorderlessDisplayMode(monitor, requestedWidth, requestedHeight, refreshRate);
            GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);
            if (videoMode == null) {
                clumps$restoreDisplayMode();
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
            clumps$restoreDisplayMode();
            clumps$restoreDecorations(window);
            GLFW.glfwSetWindowMonitor(window, monitor, requestedX, requestedY, requestedWidth, requestedHeight, refreshRate);
        }
    }

    @Unique
    private void clumps$syncBorderlessDisplayMode(long monitor, int requestedWidth, int requestedHeight, int requestedRefreshRate) {
        if (Platform.get() != Platform.WINDOWS || monitor == 0L) {
            return;
        }
        String deviceName = GLFWNativeWin32.glfwGetWin32Adapter(monitor);
        if (deviceName == null || deviceName.isEmpty()) {
            return;
        }
        if (fullscreenVideoMode != null && fullscreenVideoMode.isEmpty()) {
            clumps$restoreDisplayMode();
            return;
        }
        int targetWidth = requestedWidth;
        int targetHeight = requestedHeight;
        int targetRefresh = requestedRefreshRate;
        if (fullscreenVideoMode != null && fullscreenVideoMode.isPresent()) {
            VideoMode configured = fullscreenVideoMode.get();
            targetWidth = configured.getWidth();
            targetHeight = configured.getHeight();
            targetRefresh = configured.getRefreshRate();
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        int regWidth = 0;
        int regHeight = 0;
        int regRefresh = 0;
        int curWidth = 0;
        int curHeight = 0;
        int curRefresh = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DEVMODE regMode = DEVMODE.calloc(stack);
            regMode.dmSize((short) DEVMODE.SIZEOF);
            if (User32.EnumDisplaySettingsEx(deviceName, User32.ENUM_REGISTRY_SETTINGS, regMode, 0)) {
                regWidth = regMode.dmPelsWidth();
                regHeight = regMode.dmPelsHeight();
                regRefresh = regMode.dmDisplayFrequency();
            }
            DEVMODE curMode = DEVMODE.calloc(stack);
            curMode.dmSize((short) DEVMODE.SIZEOF);
            if (User32.EnumDisplaySettingsEx(deviceName, User32.ENUM_CURRENT_SETTINGS, curMode, 0)) {
                curWidth = curMode.dmPelsWidth();
                curHeight = curMode.dmPelsHeight();
                curRefresh = curMode.dmDisplayFrequency();
            }
        } catch (Throwable ignored) {
            return;
        }
        boolean matchesRegistry = regWidth > 0 && regHeight > 0
                && targetWidth == regWidth
                && targetHeight == regHeight
                && (targetRefresh <= 0 || regRefresh <= 0 || targetRefresh == regRefresh);
        if (matchesRegistry) {
            clumps$restoreDisplayMode();
            return;
        }
        String previousDevice = clumps$customModeDevice;
        if (previousDevice != null && !previousDevice.equals(deviceName)) {
            clumps$restoreDisplayMode();
        }
        boolean alreadyApplied = curWidth == targetWidth
                && curHeight == targetHeight
                && (targetRefresh <= 0 || curRefresh == targetRefresh);
        if (alreadyApplied || clumps$applyDisplayMode(deviceName, targetWidth, targetHeight, targetRefresh)) {
            clumps$customModeDevice = deviceName;
            clumps$customModeWidth = targetWidth;
            clumps$customModeHeight = targetHeight;
            clumps$customModeRefresh = targetRefresh;
            clumps$customModeSuspended = false;
            clumps$ensureShutdownHook();
        }
    }

    @Unique
    private static boolean clumps$applyDisplayMode(String deviceName, int targetWidth, int targetHeight, int targetRefresh) {
        if (Platform.get() != Platform.WINDOWS || deviceName == null || deviceName.isEmpty()) {
            return false;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DEVMODE devMode = DEVMODE.calloc(stack);
            devMode.dmSize((short) DEVMODE.SIZEOF);
            int fields = GDI32.DM_PELSWIDTH | GDI32.DM_PELSHEIGHT;
            MemoryUtil.memPutInt(devMode.address() + DEVMODE.DMPELSWIDTH, targetWidth);
            MemoryUtil.memPutInt(devMode.address() + DEVMODE.DMPELSHEIGHT, targetHeight);
            if (targetRefresh > 0) {
                fields |= GDI32.DM_DISPLAYFREQUENCY;
                MemoryUtil.memPutInt(devMode.address() + DEVMODE.DMDISPLAYFREQUENCY, targetRefresh);
            }
            MemoryUtil.memPutInt(devMode.address() + DEVMODE.DMFIELDS, fields);
            int status = User32.ChangeDisplaySettingsEx(deviceName, devMode, 0L, User32.CDS_FULLSCREEN, 0L);
            if (status != User32.DISP_CHANGE_SUCCESSFUL && targetRefresh > 0) {
                MemoryUtil.memPutInt(devMode.address() + DEVMODE.DMFIELDS, GDI32.DM_PELSWIDTH | GDI32.DM_PELSHEIGHT);
                status = User32.ChangeDisplaySettingsEx(deviceName, devMode, 0L, User32.CDS_FULLSCREEN, 0L);
            }
            return status == User32.DISP_CHANGE_SUCCESSFUL;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Unique
    private void clumps$restoreDisplayMode() {
        clumps$customModeWidth = 0;
        clumps$customModeHeight = 0;
        clumps$customModeRefresh = 0;
        clumps$customModeSuspended = false;
        clumps$restoreDisplayModeStatic();
    }

    @Unique
    private static void clumps$restoreDisplayModeStatic() {
        String deviceName = clumps$customModeDevice;
        if (deviceName == null || Platform.get() != Platform.WINDOWS) {
            return;
        }
        clumps$customModeDevice = null;
        try {
            User32.ChangeDisplaySettingsEx(deviceName, (DEVMODE) null, 0L, 0, 0L);
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static void clumps$ensureShutdownHook() {
        if (clumps$shutdownHookRegistered) {
            return;
        }
        clumps$shutdownHookRegistered = true;
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(WindowMixin::clumps$restoreDisplayModeStatic, "clumps-display-restore"));
        } catch (Throwable ignored) {
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

    @Inject(method = "setCursor", at = @At("HEAD"), cancellable = true)
    private void suppressOsCursorUpdatesOnStreamProofGui(Cursor cursor, CallbackInfo callback) {
        if (ClumpsClient.shouldHideOsCursorForStreamProof()) {
            callback.cancel();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void clumps$onClose(CallbackInfo callback) {
        clumps$restoreDisplayMode();
    }

    @Inject(method = "onWindowPosChanged", at = @At("TAIL"))
    void postWindowPosition(long window, int newX, int newY, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        EventBus.post(new WindowPositionEvent(newX, newY));
    }

    @Inject(method = "onWindowSizeChanged", at = @At("TAIL"))
    void postWindowSize(long window, int newWidth, int newHeight, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        EventBus.post(new WindowSizeEvent(newWidth, newHeight));
    }

    @Inject(method = "onWindowFocusChanged", at = @At("TAIL"))
    void postWindowFocus(long window, boolean focused, CallbackInfo callback) {
        if (Platform.get() == Platform.WINDOWS && clumps$borderlessMonitor != 0L && clumps$customModeDevice != null) {
            if (!focused && !clumps$customModeSuspended && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE) {
                clumps$customModeSuspended = true;
                try {
                    User32.ChangeDisplaySettingsEx(clumps$customModeDevice, (DEVMODE) null, 0L, 0, 0L);
                } catch (Throwable ignored) {
                }
            } else if (focused && clumps$customModeSuspended && clumps$customModeWidth > 0 && clumps$customModeHeight > 0) {
                if (clumps$applyDisplayMode(clumps$customModeDevice, clumps$customModeWidth, clumps$customModeHeight, clumps$customModeRefresh)) {
                    clumps$customModeSuspended = false;
                }
            }
        }
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        EventBus.post(new WindowFocusEvent(focused));
    }
}
