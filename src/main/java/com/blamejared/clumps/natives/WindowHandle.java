package com.blamejared.clumps.natives;

import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.window.WindowFocusEvent;
import com.blamejared.clumps.client.event.window.WindowPositionEvent;
import com.blamejared.clumps.client.event.window.WindowSizeEvent;
import java.nio.IntBuffer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.macosx.ObjCRuntime;
import org.lwjgl.system.windows.POINT;
import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.User32;

public final class WindowHandle implements WindowBackend {
    private static final int EXCLUDE_FROM_CAPTURE = 0x11;
    private static final int MONITOR_CAPTURE = 0x01;
    // Use 254 instead of 255 so Windows DWM always composites the overlay through its layered tree
    // and never promotes a fullscreen-sized OpenGL overlay to DirectFlip/MPO (which bypasses display affinity).
    private static final int OVERLAY_VISIBLE_ALPHA = 254;
    private static final int GW_HWNDPREV = 3;
    private static final long HWND_NOTOPMOST = -2L;
    private static final long WINDOW_ABOVE = 1L;
    private static final long SHARING_NONE = 0L;
    private static final long COCOA_COLLECTION_FULLSCREEN_AUXILIARY = 2L | 256L;
    private static final SharedLibrary USER32 = Platform.get() == Platform.WINDOWS ? APIUtil.apiCreateLibrary("user32") : null;
    private static final SharedLibrary DWMAPI = Platform.get() == Platform.WINDOWS ? createOptionalLibrary("dwmapi") : null;
    private static final long FN_SET_WINDOW_DISPLAY_AFFINITY = functionAddress(USER32, "SetWindowDisplayAffinity");
    private static final long FN_GET_WINDOW_DISPLAY_AFFINITY = functionAddress(USER32, "GetWindowDisplayAffinity");
    private static final long FN_GET_CLIENT_RECT = functionAddress(USER32, "GetClientRect");
    private static final long FN_GET_FOREGROUND_WINDOW = functionAddress(USER32, "GetForegroundWindow");
    private static final long FN_GET_WINDOW = functionAddress(USER32, "GetWindow");
    private static final long FN_SET_WINDOW_LONG_PTR = functionAddress(USER32, "SetWindowLongPtrW");
    private static final long FN_SET_LAYERED_WINDOW_ATTRIBUTES = functionAddress(USER32, "SetLayeredWindowAttributes");
    private static final long FN_SET_WINDOW_POS = functionAddress(USER32, "SetWindowPos");
    private static final long FN_DWM_FLUSH = functionAddress(DWMAPI, "DwmFlush");

    private final long window;
    private final long mainWindow;
    private final int[] horizontal = new int[1];
    private final int[] vertical = new int[1];
    private int x;
    private int y;
    private int width;
    private int height;
    private int framebufferWidth;
    private int framebufferHeight;
    private boolean visible;
    private boolean shownForReveal;
    private boolean revealedOpaque;
    private boolean captureFailed;
    private boolean geometryChangedSinceVerify;
    private int appliedAffinity = EXCLUDE_FROM_CAPTURE;
    private int currentAlpha = -1;
    private boolean lastMainFocused;
    private boolean lastMainTopmost;

    public WindowHandle(boolean openGl) {
        mainWindow = MinecraftClient.getInstance().getWindow().getHandle();
        window = createOverlayWindow(openGl);
        if (window == 0L) throw new IllegalStateException("Surface creation failed: " + lastGlfwError());
        configureWindowStyles();
        configureAndVerifyCaptureSharing();
        attachToMainWindow();
        if (Platform.get() == Platform.WINDOWS) {
            long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
            setWindowPos(overlay, 0L, 0, 0, 0, 0,
                    User32.SWP_NOACTIVATE | User32.SWP_NOMOVE | User32.SWP_NOSIZE | User32.SWP_NOZORDER | User32.SWP_FRAMECHANGED);
        }
        synchronizeWithMainWindow();
        EventBus.register(this);
    }

    private long createOverlayWindow(boolean openGl) {
        GLFW.glfwGetWindowSize(mainWindow, horizontal, vertical);
        int initialWidth = Math.max(1, horizontal[0]);
        int initialHeight = Math.max(1, vertical[0]);
        long created = createOverlayWindowWithHints(initialWidth, initialHeight, openGl, 2);
        if (created == 0L && openGl) {
            created = createOverlayWindowWithHints(initialWidth, initialHeight, true, 1);
        }
        if (created == 0L && openGl) {
            created = createOverlayWindowWithHints(initialWidth, initialHeight, true, 0);
        }
        return created;
    }

    private long createOverlayWindowWithHints(int initialWidth, int initialHeight, boolean openGl, int contextHintLevel) {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, openGl ? GLFW.GLFW_OPENGL_API : GLFW.GLFW_NO_API);
        if (openGl) {
            if (contextHintLevel >= 1) {
                int creationApi = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_CONTEXT_CREATION_API);
                int major = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_CONTEXT_VERSION_MAJOR);
                int minor = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_CONTEXT_VERSION_MINOR);
                int profile = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_OPENGL_PROFILE);
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, creationApi != 0 ? creationApi : GLFW.GLFW_NATIVE_CONTEXT_API);
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, major > 0 ? major : 3);
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, major > 0 ? minor : 3);
                if (profile != 0) {
                    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, profile);
                }
            }
            if (contextHintLevel >= 2) {
                int forwardCompat = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_OPENGL_FORWARD_COMPAT);
                int debugContext = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_OPENGL_DEBUG_CONTEXT);
                int noError = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_CONTEXT_NO_ERROR);
                int robustness = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_CONTEXT_ROBUSTNESS);
                int releaseBehavior = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_CONTEXT_RELEASE_BEHAVIOR);
                GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, forwardCompat == GLFW.GLFW_TRUE ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
                GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, debugContext == GLFW.GLFW_TRUE ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
                if (noError == GLFW.GLFW_TRUE) {
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
                }
                if (robustness != 0) {
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_ROBUSTNESS, robustness);
                }
                if (releaseBehavior != 0) {
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_RELEASE_BEHAVIOR, releaseBehavior);
                }
            } else if (contextHintLevel == 0) {
                int noError = GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_CONTEXT_NO_ERROR);
                if (noError == GLFW.GLFW_TRUE) {
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
                }
            }
        }
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_MOUSE_PASSTHROUGH, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FLOATING, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_SCALE_TO_MONITOR, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_AUTO_ICONIFY, GLFW.GLFW_FALSE);
        long previousContext = openGl ? GLFW.glfwGetCurrentContext() : 0L;
        if (previousContext != 0L) GLFW.glfwMakeContextCurrent(0L);
        try {
            return GLFW.glfwCreateWindow(initialWidth, initialHeight, "", 0L, openGl ? mainWindow : 0L);
        } finally {
            if (previousContext != 0L) GLFW.glfwMakeContextCurrent(previousContext);
            GLFW.glfwDefaultWindowHints();
        }
    }

    public boolean isOwnerMinimizedOrHidden() {
        if (mainWindow == 0L) {
            return true;
        }
        if (Platform.get() == Platform.WINDOWS) {
            long owner = GLFWNativeWin32.glfwGetWin32Window(mainWindow);
            return owner == 0L || User32.IsIconic(owner) || !User32.IsWindowVisible(owner);
        }
        return GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE
                || GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE;
    }

    public boolean isOwnerExclusiveFullscreen() {
        return Platform.get() == Platform.WINDOWS && mainWindow != 0L && GLFW.glfwGetWindowMonitor(mainWindow) != 0L;
    }

    public boolean isCaptureFailed() {
        return captureFailed;
    }

    public boolean prepareCaptureProtectedShow() {
        if (isOwnerMinimizedOrHidden() || isOwnerExclusiveFullscreen()) {
            return false;
        }
        if (visible && revealedOpaque) {
            if (geometryChangedSinceVerify && Platform.get() == Platform.WINDOWS) {
                geometryChangedSinceVerify = false;
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                if (overlay != 0L) {
                    configureWindowStyles();
                    attachToMainWindow();
                    if (!configureAndVerifyCaptureSharing()) {
                        captureFailed = true;
                        setVisible(false);
                        return false;
                    }
                    setOverlayAlpha(overlay, OVERLAY_VISIBLE_ALPHA);
                    flushDwm();
                }
            }
            if (!ensureCaptureProtected()) {
                captureFailed = true;
                setVisible(false);
                return false;
            }
            return true;
        }
        if (shownForReveal && !revealedOpaque) {
            if (Platform.get() == Platform.WINDOWS) {
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                if (overlay == 0L) {
                    captureFailed = true;
                    setVisible(false);
                    return false;
                }
                if (!isCaptureAffinityActive(overlay)) {
                    if (!configureAndVerifyCaptureSharing()) {
                        captureFailed = true;
                        setVisible(false);
                        return false;
                    }
                    flushDwm();
                }
            }
            captureFailed = false;
            return true;
        }
        if (Platform.get() == Platform.WINDOWS) {
            long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
            if (overlay == 0L) {
                captureFailed = true;
                return false;
            }
            revealedOpaque = false;
            configureWindowStyles();
            setOverlayAlpha(overlay, 0);
            attachToMainWindow();
            synchronizeWithMainWindow();
            if (width <= 0 || height <= 0 || isOwnerMinimizedOrHidden() || isOwnerExclusiveFullscreen()) {
                return false;
            }
            GLFW.glfwShowWindow(window);
            shownForReveal = true;
            setWindowPos(overlay, User32.HWND_TOP, x, y, width, height,
                    User32.SWP_NOACTIVATE | User32.SWP_SHOWWINDOW | User32.SWP_FRAMECHANGED);
            synchronizeWithMainWindow();
            if (!configureAndVerifyCaptureSharing()) {
                flushDwm();
                if (!configureAndVerifyCaptureSharing()) {
                    captureFailed = true;
                    setVisible(false);
                    return false;
                }
            }
            flushDwm();
            captureFailed = false;
            return true;
        }
        if (Platform.get() == Platform.MACOSX) {
            attachToMainWindow();
            configureAndVerifyCaptureSharing();
            synchronizeWithMainWindow();
            if (width <= 0 || height <= 0 || isOwnerMinimizedOrHidden()) {
                return false;
            }
            GLFW.glfwShowWindow(window);
            shownForReveal = true;
            attachToMainWindow();
            configureAndVerifyCaptureSharing();
            captureFailed = false;
            return true;
        }
        return false;
    }

    @Override
    public void synchronizeWithMainWindow() {
        if (isOwnerMinimizedOrHidden()) {
            setVisible(false);
            return;
        }
        int targetX;
        int targetY;
        int targetWidth;
        int targetHeight;
        if (Platform.get() == Platform.WINDOWS) {
            long owner = GLFWNativeWin32.glfwGetWin32Window(mainWindow);
            long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                RECT rect = RECT.calloc(stack);
                POINT origin = POINT.calloc(stack);
                if (FN_GET_CLIENT_RECT != 0L
                        && JNI.invokePPI(owner, rect.address(), FN_GET_CLIENT_RECT) != 0
                        && User32.ClientToScreen(owner, origin)) {
                    targetX = origin.x() + rect.left();
                    targetY = origin.y() + rect.top();
                    targetWidth = rect.right() - rect.left();
                    targetHeight = rect.bottom() - rect.top();
                } else {
                    GLFW.glfwGetWindowPos(mainWindow, horizontal, vertical);
                    targetX = horizontal[0];
                    targetY = vertical[0];
                    GLFW.glfwGetWindowSize(mainWindow, horizontal, vertical);
                    targetWidth = horizontal[0];
                    targetHeight = vertical[0];
                }
            }
            if (targetWidth <= 0 || targetHeight <= 0) {
                setVisible(false);
                return;
            }
            boolean changed = targetX != x || targetY != y || targetWidth != width || targetHeight != height;
            if (changed) {
                geometryChangedSinceVerify = true;
            }
            x = targetX;
            y = targetY;
            width = targetWidth;
            height = targetHeight;

            long foreground = FN_GET_FOREGROUND_WINDOW != 0L ? JNI.invokeP(FN_GET_FOREGROUND_WINDOW) : 0L;
            boolean mainFocused = foreground == owner || foreground == overlay
                    || GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
            boolean mainTopmost = (User32.GetWindowLongPtr(owner, User32.GWL_EXSTYLE) & User32.WS_EX_TOPMOST) != 0L;
            boolean overlayTopmost = (User32.GetWindowLongPtr(overlay, User32.GWL_EXSTYLE) & User32.WS_EX_TOPMOST) != 0L;
            boolean wantTopmost = mainTopmost && mainFocused;

            if (!visible && !shownForReveal) {
                if (changed) {
                    setWindowPos(overlay, 0L, x, y, width, height,
                            User32.SWP_NOACTIVATE | User32.SWP_NOZORDER | User32.SWP_NOOWNERZORDER);
                }
            } else {
                if (overlayTopmost && !wantTopmost) {
                    setWindowPos(overlay, HWND_NOTOPMOST, 0, 0, 0, 0,
                            User32.SWP_NOACTIVATE | User32.SWP_NOMOVE | User32.SWP_NOSIZE | User32.SWP_NOOWNERZORDER);
                }
                long insertAfter = 0L;
                int zFlag = User32.SWP_NOZORDER;
                long visibleAboveOwner = findVisibleWindowAbove(owner);
                boolean directlyAboveOwner = visibleAboveOwner == overlay;
                if (wantTopmost) {
                    if (!overlayTopmost || !directlyAboveOwner) {
                        insertAfter = User32.HWND_TOPMOST;
                        zFlag = 0;
                    }
                } else if (mainFocused) {
                    if (!directlyAboveOwner || !lastMainFocused) {
                        insertAfter = User32.HWND_TOP;
                        zFlag = 0;
                    }
                } else if (!directlyAboveOwner) {
                    insertAfter = visibleAboveOwner != 0L ? visibleAboveOwner : User32.HWND_TOP;
                    zFlag = 0;
                }
                if (changed || wantTopmost != lastMainTopmost || mainFocused != lastMainFocused || zFlag == 0) {
                    int posFlags = User32.SWP_NOACTIVATE | User32.SWP_NOOWNERZORDER | zFlag;
                    if (!changed) {
                        posFlags |= User32.SWP_NOMOVE | User32.SWP_NOSIZE;
                    }
                    setWindowPos(overlay, insertAfter, x, y, width, height, posFlags);
                }
            }
            lastMainFocused = mainFocused;
            lastMainTopmost = wantTopmost;
        } else {
            GLFW.glfwGetWindowPos(mainWindow, horizontal, vertical);
            targetX = horizontal[0];
            targetY = vertical[0];
            GLFW.glfwGetWindowSize(mainWindow, horizontal, vertical);
            targetWidth = horizontal[0];
            targetHeight = vertical[0];
            if (targetWidth <= 0 || targetHeight <= 0) {
                setVisible(false);
                return;
            }
            boolean changed = targetX != x || targetY != y || targetWidth != width || targetHeight != height;
            if (changed) {
                geometryChangedSinceVerify = true;
            }
            x = targetX;
            y = targetY;
            width = targetWidth;
            height = targetHeight;
            if (changed) {
                GLFW.glfwSetWindowSize(window, width, height);
                GLFW.glfwSetWindowPos(window, x, y);
            }
        }
        GLFW.glfwGetFramebufferSize(window, horizontal, vertical);
        framebufferWidth = horizontal[0];
        framebufferHeight = vertical[0];
    }

    @Override
    public void setVisible(boolean requested) {
        if (requested) {
            if (visible && revealedOpaque) {
                return;
            }
            if (!prepareCaptureProtectedShow()) {
                return;
            }
            visible = true;
            if (Platform.get() == Platform.WINDOWS && !revealedOpaque) {
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                if (overlay != 0L) {
                    if (!isCaptureAffinityActive(overlay) && !configureAndVerifyCaptureSharing()) {
                        captureFailed = true;
                        setVisible(false);
                        return;
                    }
                    setOverlayAlpha(overlay, OVERLAY_VISIBLE_ALPHA);
                    flushDwm();
                }
                revealedOpaque = true;
            } else {
                revealedOpaque = true;
            }
        } else {
            if (!visible && !shownForReveal && !revealedOpaque) {
                return;
            }
            visible = false;
            shownForReveal = false;
            revealedOpaque = false;
            if (Platform.get() == Platform.WINDOWS) {
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                if (overlay != 0L) {
                    setOverlayAlpha(overlay, 0);
                }
            }
            GLFW.glfwHideWindow(window);
        }
    }

    @Override
    public void close() {
        EventBus.unregister(this);
        setVisible(false);
        detachFromMainWindow();
        GLFW.glfwDestroyWindow(window);
    }

    @Subscribe
    public void onWindowPosition(WindowPositionEvent event) {
        synchronizeWithMainWindow();
    }

    @Subscribe
    public void onWindowSize(WindowSizeEvent event) {
        synchronizeWithMainWindow();
    }

    @Subscribe
    public void onWindowFocus(WindowFocusEvent event) {
        if (!visible) {
            return;
        }
        synchronizeWithMainWindow();
        if (event.focused && Platform.get() == Platform.MACOSX) {
            attachToMainWindow();
            configureAndVerifyCaptureSharing();
        }
    }

    private void configureWindowStyles() {
        if (Platform.get() != Platform.WINDOWS) {
            return;
        }
        long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
        if (overlay == 0L) {
            return;
        }
        long exStyle = User32.GetWindowLongPtr(overlay, User32.GWL_EXSTYLE);
        long desired = (exStyle & ~User32.WS_EX_APPWINDOW)
                | User32.WS_EX_LAYERED
                | User32.WS_EX_TRANSPARENT
                | User32.WS_EX_TOOLWINDOW
                | User32.WS_EX_NOACTIVATE;
        if (desired != exStyle) {
            setWindowLongPtr(overlay, User32.GWL_EXSTYLE, desired);
            currentAlpha = -1;
        }
        setOverlayAlpha(overlay, revealedOpaque ? OVERLAY_VISIBLE_ALPHA : 0);
    }

    private void setOverlayAlpha(long overlay, int alpha) {
        if (currentAlpha != alpha) {
            setLayeredWindowAttributes(overlay, 0, (byte) alpha, User32.LWA_ALPHA);
            currentAlpha = alpha;
        }
    }

    private String lastGlfwError() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer description = stack.mallocPointer(1);
            int code = GLFW.glfwGetError(description);
            String text = description.get(0) == 0L ? "" : MemoryUtil.memUTF8(description.get(0));
            return code + " " + text;
        }
    }

    private void attachToMainWindow() {
        switch (Platform.get()) {
            case WINDOWS -> {
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                long owner = GLFWNativeWin32.glfwGetWin32Window(mainWindow);
                if (overlay != 0L && owner != 0L) {
                    setWindowLongPtr(overlay, User32.GWL_HWNDPARENT, owner);
                }
            }
            case MACOSX -> {
                long overlay = GLFWNativeCocoa.glfwGetCocoaWindow(window);
                long owner = GLFWNativeCocoa.glfwGetCocoaWindow(mainWindow);
                if (overlay == 0L || owner == 0L) {
                    return;
                }
                long sendMessage = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
                long setIgnoresMouse = ObjCRuntime.sel_getUid("setIgnoresMouseEvents:");
                JNI.invokePPV(overlay, setIgnoresMouse, true, sendMessage);
                long setCollectionBehavior = ObjCRuntime.sel_getUid("setCollectionBehavior:");
                JNI.invokePPPV(overlay, setCollectionBehavior, COCOA_COLLECTION_FULLSCREEN_AUXILIARY, sendMessage);
                long addChild = ObjCRuntime.sel_getUid("addChildWindow:ordered:");
                JNI.invokePPPPV(owner, addChild, overlay, WINDOW_ABOVE, sendMessage);
            }
            default -> {
            }
        }
    }

    private void detachFromMainWindow() {
        switch (Platform.get()) {
            case WINDOWS -> {
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                if (overlay != 0L) {
                    setWindowLongPtr(overlay, User32.GWL_HWNDPARENT, 0L);
                }
            }
            case MACOSX -> {
                long overlay = GLFWNativeCocoa.glfwGetCocoaWindow(window);
                long owner = GLFWNativeCocoa.glfwGetCocoaWindow(mainWindow);
                if (overlay != 0L && owner != 0L) {
                    long sendMessage = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
                    long removeChild = ObjCRuntime.sel_getUid("removeChildWindow:");
                    JNI.invokePPPV(owner, removeChild, overlay, sendMessage);
                }
            }
            default -> {
            }
        }
    }

    private boolean configureAndVerifyCaptureSharing() {
        switch (Platform.get()) {
            case WINDOWS -> {
                if (FN_SET_WINDOW_DISPLAY_AFFINITY == 0L) {
                    return false;
                }
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                if (overlay == 0L) {
                    return false;
                }
                if (trySetCaptureAffinity(overlay, EXCLUDE_FROM_CAPTURE)) {
                    appliedAffinity = EXCLUDE_FROM_CAPTURE;
                    return isCaptureAffinityActive(overlay);
                }
                // Retry unowned in case Windows DWM rejects affinity while GWL_HWNDPARENT is attached.
                detachFromMainWindow();
                boolean detachedSucceeded = trySetCaptureAffinity(overlay, EXCLUDE_FROM_CAPTURE);
                attachToMainWindow();
                if (detachedSucceeded) {
                    appliedAffinity = EXCLUDE_FROM_CAPTURE;
                    return isCaptureAffinityActive(overlay);
                }
                if (trySetCaptureAffinity(overlay, MONITOR_CAPTURE)) {
                    appliedAffinity = MONITOR_CAPTURE;
                    return isCaptureAffinityActive(overlay);
                }
                return false;
            }
            case MACOSX -> {
                long overlay = GLFWNativeCocoa.glfwGetCocoaWindow(window);
                if (overlay == 0L) {
                    return false;
                }
                long sendMessage = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
                long setSharing = ObjCRuntime.sel_getUid("setSharingType:");
                JNI.invokePPPV(overlay, setSharing, SHARING_NONE, sendMessage);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean ensureCaptureProtected() {
        switch (Platform.get()) {
            case WINDOWS -> {
                long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
                if (overlay == 0L) {
                    return false;
                }
                if (isCaptureAffinityActive(overlay)) {
                    return true;
                }
                setOverlayAlpha(overlay, 0);
                revealedOpaque = false;
                if (!configureAndVerifyCaptureSharing()) {
                    return false;
                }
                flushDwm();
                setOverlayAlpha(overlay, OVERLAY_VISIBLE_ALPHA);
                revealedOpaque = true;
                return true;
            }
            case MACOSX -> {
                return configureAndVerifyCaptureSharing();
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean trySetCaptureAffinity(long overlay, int affinity) {
        return FN_SET_WINDOW_DISPLAY_AFFINITY != 0L
                && JNI.invokePI(overlay, affinity, FN_SET_WINDOW_DISPLAY_AFFINITY) != 0;
    }

    private boolean isCaptureAffinityActive(long overlay) {
        if (FN_GET_WINDOW_DISPLAY_AFFINITY == 0L) {
            return true;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer affinity = stack.mallocInt(1);
            if (JNI.invokePPI(overlay, MemoryUtil.memAddress(affinity), FN_GET_WINDOW_DISPLAY_AFFINITY) == 0) {
                return false;
            }
            int current = affinity.get(0);
            return current == appliedAffinity || current == EXCLUDE_FROM_CAPTURE || current == MONITOR_CAPTURE;
        }
    }

    private static long findVisibleWindowAbove(long hwnd) {
        if (FN_GET_WINDOW == 0L || hwnd == 0L) {
            return 0L;
        }
        long current = JNI.invokePP(hwnd, GW_HWNDPREV, FN_GET_WINDOW);
        for (int steps = 0; current != 0L && steps < 64; steps++) {
            if (User32.IsWindowVisible(current)) {
                return current;
            }
            current = JNI.invokePP(current, GW_HWNDPREV, FN_GET_WINDOW);
        }
        return 0L;
    }

    private static void setWindowPos(long hwnd, long insertAfter, int x, int y, int w, int h, int flags) {
        if (FN_SET_WINDOW_POS != 0L && hwnd != 0L) {
            JNI.invokePPV(hwnd, insertAfter, x, y, w, h, flags, FN_SET_WINDOW_POS);
        }
    }

    private static void setWindowLongPtr(long hwnd, int index, long value) {
        if (FN_SET_WINDOW_LONG_PTR != 0L && hwnd != 0L) {
            JNI.invokePPP(hwnd, index, value, FN_SET_WINDOW_LONG_PTR);
        }
    }

    private static void setLayeredWindowAttributes(long hwnd, int colorKey, byte alpha, int flags) {
        if (FN_SET_LAYERED_WINDOW_ATTRIBUTES != 0L && hwnd != 0L) {
            JNI.invokePI(hwnd, colorKey, alpha & 0xFF, flags, FN_SET_LAYERED_WINDOW_ATTRIBUTES);
        }
    }

    private static void flushDwm() {
        if (FN_DWM_FLUSH != 0L) {
            JNI.invokeI(FN_DWM_FLUSH);
        }
    }

    private static SharedLibrary createOptionalLibrary(String name) {
        try {
            return APIUtil.apiCreateLibrary(name);
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static long functionAddress(SharedLibrary library, String name) {
        return library != null ? library.getFunctionAddress(name) : 0L;
    }

    @Override public long windowHandle() { return window; }
    @Override public int framebufferWidth() { return framebufferWidth; }
    @Override public int framebufferHeight() { return framebufferHeight; }
}
