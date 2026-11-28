package com.blamejared.clumps.natives;

import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.window.WindowFocusEvent;
import com.blamejared.clumps.client.event.window.WindowPositionEvent;
import com.blamejared.clumps.client.event.window.WindowSizeEvent;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
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
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.LibFFI;
import org.lwjgl.system.macosx.ObjCRuntime;
import org.lwjgl.system.windows.POINT;
import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.User32;

public final class WindowHandle implements WindowBackend {
    private static final int EXCLUDE_FROM_CAPTURE = 0x11;
    // Use 254 instead of 255 so Windows DWM always composites through its layered tree
    // and never promotes a fullscreen-sized overlay to DirectFlip/MPO.
    private static final int OVERLAY_VISIBLE_ALPHA = 254;
    private static final int SRCCOPY = 0x00CC0020;
    private static final int GW_HWNDPREV = 3;
    private static final long HWND_NOTOPMOST = -2L;
    private static final long WINDOW_ABOVE = 1L;
    private static final long SHARING_NONE = 0L;
    private static final long COCOA_COLLECTION_FULLSCREEN_AUXILIARY = 2L | 256L;
    private static final int BITMAPINFOHEADER_SIZE = 40;

    private static final SharedLibrary USER32 = Platform.get() == Platform.WINDOWS ? APIUtil.apiCreateLibrary("user32") : null;
    private static final SharedLibrary GDI32 = Platform.get() == Platform.WINDOWS ? APIUtil.apiCreateLibrary("gdi32") : null;
    private static final SharedLibrary DWMAPI = Platform.get() == Platform.WINDOWS ? createOptionalLibrary("dwmapi") : null;

    private static final long FN_SET_WINDOW_DISPLAY_AFFINITY = functionAddress(USER32, "SetWindowDisplayAffinity");
    private static final long FN_GET_WINDOW_DISPLAY_AFFINITY = functionAddress(USER32, "GetWindowDisplayAffinity");
    private static final long FN_GET_CLIENT_RECT = functionAddress(USER32, "GetClientRect");
    private static final long FN_GET_FOREGROUND_WINDOW = functionAddress(USER32, "GetForegroundWindow");
    private static final long FN_GET_WINDOW = functionAddress(USER32, "GetWindow");
    private static final long FN_SET_WINDOW_LONG_PTR = functionAddress(USER32, "SetWindowLongPtrW");
    private static final long FN_SET_LAYERED_WINDOW_ATTRIBUTES = functionAddress(USER32, "SetLayeredWindowAttributes");
    private static final long FN_SET_WINDOW_POS = functionAddress(USER32, "SetWindowPos");
    private static final long FN_GET_DC = functionAddress(USER32, "GetDC");
    private static final long FN_RELEASE_DC = functionAddress(USER32, "ReleaseDC");

    private static final long FN_CREATE_COMPATIBLE_DC = functionAddress(GDI32, "CreateCompatibleDC");
    private static final long FN_CREATE_DIB_SECTION = functionAddress(GDI32, "CreateDIBSection");
    private static final long FN_SELECT_OBJECT = functionAddress(GDI32, "SelectObject");
    private static final long FN_BIT_BLT = functionAddress(GDI32, "BitBlt");
    private static final long FN_DELETE_OBJECT = functionAddress(GDI32, "DeleteObject");
    private static final long FN_DELETE_DC = functionAddress(GDI32, "DeleteDC");

    private static final long FN_DWM_FLUSH = functionAddress(DWMAPI, "DwmFlush");

    private final boolean layeredWindows;
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
    private int currentAlpha = -1;
    private boolean lastMainFocused;
    private boolean lastMainTopmost;

    // Win32 GDI DIBSection + BitBlt backing surface (zero OpenGL context, zero SwapBuffers -> immune to OBS Game Capture)
    private long dibMemDc;
    private long dibBitmap;
    private long dibOldBitmap;
    private long dibBitsPtr;
    private int dibWidth;
    private int dibHeight;
    private FFICIF bitBltCif;
    private PointerBuffer bitBltArgTypes;

    public WindowHandle(boolean openGl) {
        mainWindow = MinecraftClient.getInstance().getWindow().getHandle();
        layeredWindows = Platform.get() == Platform.WINDOWS
                && FN_GET_DC != 0L
                && FN_RELEASE_DC != 0L
                && FN_CREATE_COMPATIBLE_DC != 0L
                && FN_CREATE_DIB_SECTION != 0L
                && FN_SELECT_OBJECT != 0L
                && FN_BIT_BLT != 0L
                && FN_DELETE_OBJECT != 0L
                && FN_DELETE_DC != 0L;
        window = createOverlayWindow(!layeredWindows && openGl);
        if (window == 0L) throw new IllegalStateException("Surface creation failed: " + lastGlfwError());
        if (layeredWindows) {
            initBitBltCif();
        }
        configureWindowStyles();
        if (Platform.get() == Platform.WINDOWS) {
            // Keep overlay strictly unowned on Windows so HWND_TOPMOST and WDA_EXCLUDEFROMCAPTURE never clash with owner Z-order.
            detachFromMainWindow();
            configureAndVerifyCaptureSharing();
            long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
            setWindowPos(overlay, 0L, 0, 0, 0, 0,
                    User32.SWP_NOACTIVATE | User32.SWP_NOMOVE | User32.SWP_NOSIZE | User32.SWP_NOZORDER | User32.SWP_FRAMECHANGED);
        } else {
            configureAndVerifyCaptureSharing();
            attachToMainWindow();
        }
        synchronizeWithMainWindow();
        EventBus.register(this);
    }

    public boolean isLayeredWindows() {
        return layeredWindows;
    }

    public boolean isOverlayRevealed() {
        return visible && revealedOpaque;
    }

    private long createOverlayWindow(boolean openGl) {
        GLFW.glfwGetWindowSize(mainWindow, horizontal, vertical);
        int initialWidth = Math.max(1, horizontal[0]);
        int initialHeight = Math.max(1, vertical[0]);
        if (!openGl) {
            return createOverlayWindowWithHints(initialWidth, initialHeight, false, 0);
        }
        long created = createOverlayWindowWithHints(initialWidth, initialHeight, true, 2);
        if (created == 0L) {
            created = createOverlayWindowWithHints(initialWidth, initialHeight, true, 1);
        }
        if (created == 0L) {
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
        // Set WS_EX_LAYERED | WS_EX_TRANSPARENT ourselves on Windows so GLFW doesn't override LWA_ALPHA behind our back.
        GLFW.glfwWindowHint(GLFW.GLFW_MOUSE_PASSTHROUGH, Platform.get() == Platform.WINDOWS ? GLFW.GLFW_FALSE : GLFW.GLFW_TRUE);
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
        if (Platform.get() == Platform.WINDOWS) {
            long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
            if (overlay == 0L) {
                captureFailed = true;
                return false;
            }
            if (visible && revealedOpaque) {
                if (geometryChangedSinceVerify) {
                    geometryChangedSinceVerify = false;
                    configureWindowStyles();
                    detachFromMainWindow();
                    if (!configureAndVerifyCaptureSharing()) {
                        captureFailed = true;
                        setVisible(false);
                        return false;
                    }
                    setOverlayAlpha(overlay, OVERLAY_VISIBLE_ALPHA);
                    reassertOverlayZOrder(overlay);
                    flushDwm();
                }
                if (!ensureCaptureProtected()) {
                    captureFailed = true;
                    setVisible(false);
                    return false;
                }
                captureFailed = false;
                return true;
            }
            if (shownForReveal && !revealedOpaque) {
                if (!isCaptureAffinityActive(overlay)) {
                    if (!configureAndVerifyCaptureSharing()) {
                        captureFailed = true;
                        setVisible(false);
                        return false;
                    }
                    flushDwm();
                }
                captureFailed = false;
                return true;
            }

            revealedOpaque = false;
            configureWindowStyles();
            setOverlayAlpha(overlay, 0);
            detachFromMainWindow();
            synchronizeWithMainWindow();
            if (width <= 0 || height <= 0 || isOwnerMinimizedOrHidden() || isOwnerExclusiveFullscreen()) {
                return false;
            }
            GLFW.glfwShowWindow(window);
            shownForReveal = true;
            long owner = GLFWNativeWin32.glfwGetWin32Window(mainWindow);
            long initialInsert = shouldBeTopmost(overlay, owner, x, y, width, height)
                    ? User32.HWND_TOPMOST : User32.HWND_TOP;
            setWindowPos(overlay, initialInsert, x, y, width, height,
                    User32.SWP_NOACTIVATE | User32.SWP_SHOWWINDOW | User32.SWP_FRAMECHANGED);
            if (!configureAndVerifyCaptureSharing()) {
                flushDwm();
                if (!configureAndVerifyCaptureSharing()) {
                    captureFailed = true;
                    setVisible(false);
                    return false;
                }
            }
            reassertOverlayZOrder(overlay);
            flushDwm();
            captureFailed = false;
            return true;
        }

        if (Platform.get() == Platform.MACOSX) {
            if (visible && shownForReveal) {
                return true;
            }
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

    public boolean presentLayeredBgra(long srcPixelsPtr, int srcWidth, int srcHeight) {
        if (!layeredWindows || srcPixelsPtr == 0L || srcWidth <= 0 || srcHeight <= 0) {
            return false;
        }
        long overlay = GLFWNativeWin32.glfwGetWin32Window(window);
        if (overlay == 0L || (!visible && !shownForReveal)) {
            return false;
        }
        if (!isCaptureAffinityActive(overlay)) {
            setOverlayAlpha(overlay, 0);
            revealedOpaque = false;
            if (!configureAndVerifyCaptureSharing()) {
                captureFailed = true;
                setVisible(false);
                return false;
            }
            flushDwm();
        }
        if (!ensureDibSection(overlay, srcWidth, srcHeight)) {
            return false;
        }
        long byteCount = (long) srcWidth * (long) srcHeight * 4L;
        MemoryUtil.memCopy(srcPixelsPtr, dibBitsPtr, byteCount);

        long winDc = JNI.invokePP(overlay, FN_GET_DC);
        if (winDc == 0L) {
            return false;
        }
        boolean blitted;
        try {
            blitted = bitBlt(winDc, 0, 0, srcWidth, srcHeight, dibMemDc, 0, 0, SRCCOPY);
        } finally {
            JNI.invokePPI(overlay, winDc, FN_RELEASE_DC);
        }
        if (blitted) {
            visible = true;
            if (!revealedOpaque) {
                setOverlayAlpha(overlay, OVERLAY_VISIBLE_ALPHA);
                flushDwm();
                revealedOpaque = true;
            }
        }
        return blitted;
    }

    public void clearLayeredTransparent() {
        setVisible(false);
    }

    private boolean ensureDibSection(long overlayHwnd, int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return false;
        }
        if (dibMemDc != 0L && dibBitmap != 0L && dibBitsPtr != 0L
                && dibWidth == targetWidth && dibHeight == targetHeight) {
            return true;
        }
        destroyDibResources();
        long refDc = JNI.invokePP(overlayHwnd, FN_GET_DC);
        if (refDc == 0L) {
            return false;
        }
        try {
            long memDc = JNI.invokePP(refDc, FN_CREATE_COMPATIBLE_DC);
            if (memDc == 0L) {
                return false;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer bmi = stack.calloc(BITMAPINFOHEADER_SIZE + 4);
                bmi.putInt(0, BITMAPINFOHEADER_SIZE); // biSize
                bmi.putInt(4, targetWidth);           // biWidth
                bmi.putInt(8, -targetHeight);         // biHeight (negative = top-down DIB matching OpenGL FBO row order)
                bmi.putShort(12, (short) 1);          // biPlanes
                bmi.putShort(14, (short) 32);         // biBitCount
                bmi.putInt(16, 0);                    // biCompression = BI_RGB
                bmi.putInt(20, targetWidth * targetHeight * 4);
                PointerBuffer bitsOut = stack.callocPointer(1);
                long hBitmap = JNI.invokePJPPP(memDc, MemoryUtil.memAddress(bmi), 0,
                        MemoryUtil.memAddress(bitsOut), 0L, 0, FN_CREATE_DIB_SECTION);
                long bitsPtr = bitsOut.get(0);
                if (hBitmap == 0L || bitsPtr == 0L) {
                    if (hBitmap != 0L) JNI.invokePI(hBitmap, FN_DELETE_OBJECT);
                    JNI.invokePI(memDc, FN_DELETE_DC);
                    return false;
                }
                long oldObj = JNI.invokePPP(memDc, hBitmap, FN_SELECT_OBJECT);
                dibMemDc = memDc;
                dibBitmap = hBitmap;
                dibOldBitmap = oldObj;
                dibBitsPtr = bitsPtr;
                dibWidth = targetWidth;
                dibHeight = targetHeight;
                MemoryUtil.memSet(dibBitsPtr, 0, (long) targetWidth * (long) targetHeight * 4L);
                return true;
            }
        } finally {
            JNI.invokePPI(overlayHwnd, refDc, FN_RELEASE_DC);
        }
    }

    private void initBitBltCif() {
        FFICIF cif = FFICIF.calloc();
        PointerBuffer argTypes = MemoryUtil.memAllocPointer(9);
        argTypes.put(0, LibFFI.ffi_type_pointer);
        argTypes.put(1, LibFFI.ffi_type_sint32);
        argTypes.put(2, LibFFI.ffi_type_sint32);
        argTypes.put(3, LibFFI.ffi_type_sint32);
        argTypes.put(4, LibFFI.ffi_type_sint32);
        argTypes.put(5, LibFFI.ffi_type_pointer);
        argTypes.put(6, LibFFI.ffi_type_sint32);
        argTypes.put(7, LibFFI.ffi_type_sint32);
        argTypes.put(8, LibFFI.ffi_type_uint32);
        int status = LibFFI.ffi_prep_cif(cif, LibFFI.FFI_DEFAULT_ABI, LibFFI.ffi_type_sint32, argTypes);
        if (status != LibFFI.FFI_OK) {
            MemoryUtil.memFree(argTypes);
            cif.free();
            return;
        }
        bitBltCif = cif;
        bitBltArgTypes = argTypes;
    }

    private void destroyBitBltCif() {
        if (bitBltCif != null) {
            bitBltCif.free();
            bitBltCif = null;
        }
        if (bitBltArgTypes != null) {
            MemoryUtil.memFree(bitBltArgTypes);
            bitBltArgTypes = null;
        }
    }

    private boolean bitBlt(long dstDc, int dx, int dy, int w, int h, long srcDc, int sx, int sy, int rop) {
        if (FN_BIT_BLT == 0L || dstDc == 0L || srcDc == 0L || bitBltCif == null) {
            return false;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer a0 = stack.longs(dstDc);
            IntBuffer a1 = stack.ints(dx);
            IntBuffer a2 = stack.ints(dy);
            IntBuffer a3 = stack.ints(w);
            IntBuffer a4 = stack.ints(h);
            LongBuffer a5 = stack.longs(srcDc);
            IntBuffer a6 = stack.ints(sx);
            IntBuffer a7 = stack.ints(sy);
            IntBuffer a8 = stack.ints(rop);
            PointerBuffer vals = stack.pointers(
                    MemoryUtil.memAddress(a0), MemoryUtil.memAddress(a1), MemoryUtil.memAddress(a2),
                    MemoryUtil.memAddress(a3), MemoryUtil.memAddress(a4), MemoryUtil.memAddress(a5),
                    MemoryUtil.memAddress(a6), MemoryUtil.memAddress(a7), MemoryUtil.memAddress(a8));
            ByteBuffer ret = stack.calloc(8);
            LibFFI.ffi_call(bitBltCif, FN_BIT_BLT, ret, vals);
            return ret.getInt(0) != 0;
        }
    }

    private void destroyDibResources() {
        if (dibMemDc != 0L) {
            if (dibOldBitmap != 0L) {
                JNI.invokePPP(dibMemDc, dibOldBitmap, FN_SELECT_OBJECT);
                dibOldBitmap = 0L;
            }
            if (dibBitmap != 0L) {
                JNI.invokePI(dibBitmap, FN_DELETE_OBJECT);
                dibBitmap = 0L;
            }
            JNI.invokePI(dibMemDc, FN_DELETE_DC);
            dibMemDc = 0L;
        } else if (dibBitmap != 0L) {
            JNI.invokePI(dibBitmap, FN_DELETE_OBJECT);
            dibBitmap = 0L;
        }
        dibBitsPtr = 0L;
        dibWidth = 0;
        dibHeight = 0;
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
            boolean borderlessFull = isOwnerBorderlessFullscreen(targetX, targetY, targetWidth, targetHeight);
            boolean overlayTopmost = (User32.GetWindowLongPtr(overlay, User32.GWL_EXSTYLE) & User32.WS_EX_TOPMOST) != 0L;
            boolean wantTopmost = mainFocused && (mainTopmost || borderlessFull);

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
                    if (!overlayTopmost || !directlyAboveOwner || changed || !lastMainFocused) {
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
            framebufferWidth = width;
            framebufferHeight = height;
            return;
        }

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
        GLFW.glfwGetFramebufferSize(window, horizontal, vertical);
        framebufferWidth = horizontal[0] > 0 ? horizontal[0] : width;
        framebufferHeight = vertical[0] > 0 ? vertical[0] : height;
    }

    private boolean isOwnerBorderlessFullscreen(int winX, int winY, int winW, int winH) {
        if (mainWindow == 0L || winW <= 0 || winH <= 0) {
            return false;
        }
        if (GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_DECORATED) == GLFW.GLFW_TRUE) {
            return false;
        }
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors == null) {
            return false;
        }
        int[] monX = new int[1];
        int[] monY = new int[1];
        for (int i = 0; i < monitors.limit(); i++) {
            long mon = monitors.get(i);
            var mode = GLFW.glfwGetVideoMode(mon);
            if (mode == null) continue;
            GLFW.glfwGetMonitorPos(mon, monX, monY);
            if (Math.abs(winX - monX[0]) <= 2 && Math.abs(winY - monY[0]) <= 2
                    && winW >= mode.width() && winH >= mode.height()) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldBeTopmost(long overlay, long owner, int winX, int winY, int winW, int winH) {
        if (owner == 0L) return false;
        long foreground = FN_GET_FOREGROUND_WINDOW != 0L ? JNI.invokeP(FN_GET_FOREGROUND_WINDOW) : 0L;
        boolean mainFocused = foreground == owner || foreground == overlay
                || GLFW.glfwGetWindowAttrib(mainWindow, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
        if (!mainFocused) return false;
        boolean mainTopmost = (User32.GetWindowLongPtr(owner, User32.GWL_EXSTYLE) & User32.WS_EX_TOPMOST) != 0L;
        return mainTopmost || isOwnerBorderlessFullscreen(winX, winY, winW, winH);
    }

    private void reassertOverlayZOrder(long overlay) {
        if (Platform.get() != Platform.WINDOWS || overlay == 0L || mainWindow == 0L) return;
        long owner = GLFWNativeWin32.glfwGetWin32Window(mainWindow);
        boolean wantTopmost = shouldBeTopmost(overlay, owner, x, y, width, height);
        long insertAfter = wantTopmost ? User32.HWND_TOPMOST : User32.HWND_TOP;
        setWindowPos(overlay, insertAfter, x, y, width, height,
                User32.SWP_NOACTIVATE | User32.SWP_NOOWNERZORDER | User32.SWP_FRAMECHANGED);
        lastMainTopmost = wantTopmost;
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
        destroyDibResources();
        destroyBitBltCif();
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
        if (currentAlpha != alpha && FN_SET_LAYERED_WINDOW_ATTRIBUTES != 0L && overlay != 0L) {
            JNI.invokePI(overlay, 0, alpha & 0xFF, User32.LWA_ALPHA, FN_SET_LAYERED_WINDOW_ATTRIBUTES);
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
        if (Platform.get() == Platform.MACOSX) {
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
                detachFromMainWindow();
                return trySetCaptureAffinity(overlay, EXCLUDE_FROM_CAPTURE)
                        && isCaptureAffinityActive(overlay);
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
            return affinity.get(0) == EXCLUDE_FROM_CAPTURE;
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
