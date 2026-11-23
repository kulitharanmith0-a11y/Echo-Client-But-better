package com.blamejared.clumps.client.handlers.input;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.SharedLibrary;

public final class NativeOsInput {
    private static final int INPUT_MOUSE = 0;
    private static final int INPUT_KEYBOARD = 1;
    private static final int INPUT_SIZE_X64 = 40;

    private static final int MOUSEEVENTF_LEFTDOWN = 0x0002;
    private static final int MOUSEEVENTF_LEFTUP = 0x0004;
    private static final int MOUSEEVENTF_RIGHTDOWN = 0x0008;
    private static final int MOUSEEVENTF_RIGHTUP = 0x0010;
    private static final int MOUSEEVENTF_MIDDLEDOWN = 0x0020;
    private static final int MOUSEEVENTF_MIDDLEUP = 0x0040;
    private static final int MOUSEEVENTF_XDOWN = 0x0080;
    private static final int MOUSEEVENTF_XUP = 0x0100;
    private static final int MOUSEEVENTF_WHEEL = 0x0800;

    private static final int XBUTTON1 = 0x0001;
    private static final int XBUTTON2 = 0x0002;
    private static final int WHEEL_DELTA = 120;

    private static final int KEYEVENTF_EXTENDEDKEY = 0x0001;
    private static final int KEYEVENTF_KEYUP = 0x0002;
    private static final int MAPVK_VK_TO_VSC = 0;

    private static final long EXTRA_INFO_TAG = 0L;

    private static final SharedLibrary USER32 = Platform.get() == Platform.WINDOWS && Pointer.POINTER_SIZE == 8
            ? openUser32() : null;
    private static final long FN_SEND_INPUT = lookup("SendInput");
    private static final long FN_MAP_VIRTUAL_KEY = lookup("MapVirtualKeyW");
    private static final long FN_GET_ASYNC_KEY_STATE = lookup("GetAsyncKeyState");

    private final Set<Integer> heldKeys = new HashSet<>();
    private final Set<Integer> heldButtons = new HashSet<>();

    private static SharedLibrary openUser32() {
        try {
            return APIUtil.apiCreateLibrary("user32");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static long lookup(String name) {
        if (USER32 == null) {
            return 0L;
        }
        try {
            return USER32.getFunctionAddress(name);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    public boolean isAvailable() {
        return FN_SEND_INPUT != 0L;
    }

    public boolean isWindowFocused() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.isWindowFocused();
    }

    public boolean isPhysicallyHeldKey(int glfwKey) {
        if (FN_GET_ASYNC_KEY_STATE == 0L) {
            return false;
        }
        int vk = toVirtualKey(glfwKey);
        if (vk <= 0) {
            return false;
        }
        if (heldKeys.contains(glfwKey)) {
            return false;
        }
        return (JNI.callI(vk, FN_GET_ASYNC_KEY_STATE) & 0x8000) != 0;
    }

    public boolean isPhysicallyHeldMouse(int glfwButton) {
        if (FN_GET_ASYNC_KEY_STATE == 0L) {
            return false;
        }
        int vk = mouseButtonToVirtualKey(glfwButton);
        if (vk <= 0) {
            return false;
        }
        if (heldButtons.contains(glfwButton)) {
            return false;
        }
        return (JNI.callI(vk, FN_GET_ASYNC_KEY_STATE) & 0x8000) != 0;
    }

    public boolean sendKey(int glfwKey, boolean pressed) {
        if (!isAvailable() || (pressed && !isWindowFocused())) {
            return false;
        }
        int vk = toVirtualKey(glfwKey);
        if (vk <= 0) {
            return false;
        }
        int scan = FN_MAP_VIRTUAL_KEY != 0L ? JNI.callI(vk, MAPVK_VK_TO_VSC, FN_MAP_VIRTUAL_KEY) & 0xFFFF : 0;
        int flags = isExtendedKey(glfwKey) ? KEYEVENTF_EXTENDEDKEY : 0;
        if (!pressed) {
            flags |= KEYEVENTF_KEYUP;
        }
        boolean sent = dispatchKeyboardInput(vk, scan, flags);
        if (sent) {
            if (pressed) {
                heldKeys.add(glfwKey);
            } else {
                heldKeys.remove(glfwKey);
            }
        }
        return sent;
    }

    public boolean sendMouseButton(int glfwButton, boolean pressed) {
        if (!isAvailable() || (pressed && !isWindowFocused())) {
            return false;
        }
        int flags;
        int mouseData = 0;
        switch (glfwButton) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> flags = pressed ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_LEFTUP;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> flags = pressed ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_RIGHTUP;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> flags = pressed ? MOUSEEVENTF_MIDDLEDOWN : MOUSEEVENTF_MIDDLEUP;
            case GLFW.GLFW_MOUSE_BUTTON_4 -> {
                flags = pressed ? MOUSEEVENTF_XDOWN : MOUSEEVENTF_XUP;
                mouseData = XBUTTON1;
            }
            case GLFW.GLFW_MOUSE_BUTTON_5 -> {
                flags = pressed ? MOUSEEVENTF_XDOWN : MOUSEEVENTF_XUP;
                mouseData = XBUTTON2;
            }
            default -> {
                return false;
            }
        }
        boolean sent = dispatchMouseInput(flags, mouseData);
        if (sent) {
            if (pressed) {
                heldButtons.add(glfwButton);
            } else {
                heldButtons.remove(glfwButton);
            }
        }
        return sent;
    }

    public boolean sendMouseScroll(int notches) {
        if (!isAvailable() || !isWindowFocused() || notches == 0) {
            return false;
        }
        return dispatchMouseInput(MOUSEEVENTF_WHEEL, notches * WHEEL_DELTA);
    }

    public void releaseAll() {
        if (!isAvailable()) {
            heldKeys.clear();
            heldButtons.clear();
            return;
        }
        if (!heldKeys.isEmpty()) {
            Integer[] keys = heldKeys.toArray(new Integer[0]);
            heldKeys.clear();
            for (int key : keys) {
                int vk = toVirtualKey(key);
                if (vk > 0) {
                    int scan = FN_MAP_VIRTUAL_KEY != 0L ? JNI.callI(vk, MAPVK_VK_TO_VSC, FN_MAP_VIRTUAL_KEY) & 0xFFFF : 0;
                    int flags = KEYEVENTF_KEYUP | (isExtendedKey(key) ? KEYEVENTF_EXTENDEDKEY : 0);
                    dispatchKeyboardInput(vk, scan, flags);
                }
            }
        }
        if (!heldButtons.isEmpty()) {
            Integer[] buttons = heldButtons.toArray(new Integer[0]);
            heldButtons.clear();
            for (int button : buttons) {
                sendMouseButton(button, false);
            }
        }
    }

    private static boolean dispatchKeyboardInput(int vk, int scan, int flags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.calloc(INPUT_SIZE_X64);
            buffer.putInt(0, INPUT_KEYBOARD);
            buffer.putShort(8, (short) vk);
            buffer.putShort(10, (short) scan);
            buffer.putInt(12, flags);
            buffer.putInt(16, 0);
            buffer.putLong(24, EXTRA_INFO_TAG);
            long address = MemoryUtil.memAddress(buffer);
            return JNI.callPI(1, address, INPUT_SIZE_X64, FN_SEND_INPUT) > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean dispatchMouseInput(int flags, int mouseData) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.calloc(INPUT_SIZE_X64);
            buffer.putInt(0, INPUT_MOUSE);
            buffer.putInt(8, 0);
            buffer.putInt(12, 0);
            buffer.putInt(16, mouseData);
            buffer.putInt(20, flags);
            buffer.putInt(24, 0);
            buffer.putLong(32, EXTRA_INFO_TAG);
            long address = MemoryUtil.memAddress(buffer);
            return JNI.callPI(1, address, INPUT_SIZE_X64, FN_SEND_INPUT) > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int mouseButtonToVirtualKey(int glfwButton) {
        return switch (glfwButton) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> 0x01;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> 0x02;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> 0x04;
            case GLFW.GLFW_MOUSE_BUTTON_4 -> 0x05;
            case GLFW.GLFW_MOUSE_BUTTON_5 -> 0x06;
            default -> 0;
        };
    }

    private static boolean isExtendedKey(int glfwKey) {
        return switch (glfwKey) {
            case GLFW.GLFW_KEY_INSERT, GLFW.GLFW_KEY_DELETE, GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_END,
                    GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
                    GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_RIGHT_CONTROL, GLFW.GLFW_KEY_RIGHT_ALT,
                    GLFW.GLFW_KEY_KP_DIVIDE, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_NUM_LOCK,
                    GLFW.GLFW_KEY_PRINT_SCREEN, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> true;
            default -> false;
        };
    }

    public static int toVirtualKey(int glfwKey) {
        if (glfwKey >= GLFW.GLFW_KEY_A && glfwKey <= GLFW.GLFW_KEY_Z) {
            return glfwKey;
        }
        if (glfwKey >= GLFW.GLFW_KEY_0 && glfwKey <= GLFW.GLFW_KEY_9) {
            return glfwKey;
        }
        if (glfwKey >= GLFW.GLFW_KEY_F1 && glfwKey <= GLFW.GLFW_KEY_F24) {
            return 0x70 + (glfwKey - GLFW.GLFW_KEY_F1);
        }
        if (glfwKey >= GLFW.GLFW_KEY_KP_0 && glfwKey <= GLFW.GLFW_KEY_KP_9) {
            return 0x60 + (glfwKey - GLFW.GLFW_KEY_KP_0);
        }
        return switch (glfwKey) {
            case GLFW.GLFW_KEY_SPACE -> 0x20;
            case GLFW.GLFW_KEY_APOSTROPHE -> 0xDE;
            case GLFW.GLFW_KEY_COMMA -> 0xBC;
            case GLFW.GLFW_KEY_MINUS -> 0xBD;
            case GLFW.GLFW_KEY_PERIOD -> 0xBE;
            case GLFW.GLFW_KEY_SLASH -> 0xBF;
            case GLFW.GLFW_KEY_SEMICOLON -> 0xBA;
            case GLFW.GLFW_KEY_EQUAL -> 0xBB;
            case GLFW.GLFW_KEY_LEFT_BRACKET -> 0xDB;
            case GLFW.GLFW_KEY_BACKSLASH -> 0xDC;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> 0xDD;
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> 0xC0;
            case GLFW.GLFW_KEY_ESCAPE -> 0x1B;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> 0x0D;
            case GLFW.GLFW_KEY_TAB -> 0x09;
            case GLFW.GLFW_KEY_BACKSPACE -> 0x08;
            case GLFW.GLFW_KEY_INSERT -> 0x2D;
            case GLFW.GLFW_KEY_DELETE -> 0x2E;
            case GLFW.GLFW_KEY_RIGHT -> 0x27;
            case GLFW.GLFW_KEY_LEFT -> 0x25;
            case GLFW.GLFW_KEY_DOWN -> 0x28;
            case GLFW.GLFW_KEY_UP -> 0x26;
            case GLFW.GLFW_KEY_PAGE_UP -> 0x21;
            case GLFW.GLFW_KEY_PAGE_DOWN -> 0x22;
            case GLFW.GLFW_KEY_HOME -> 0x24;
            case GLFW.GLFW_KEY_END -> 0x23;
            case GLFW.GLFW_KEY_CAPS_LOCK -> 0x14;
            case GLFW.GLFW_KEY_SCROLL_LOCK -> 0x91;
            case GLFW.GLFW_KEY_NUM_LOCK -> 0x90;
            case GLFW.GLFW_KEY_PRINT_SCREEN -> 0x2C;
            case GLFW.GLFW_KEY_PAUSE -> 0x13;
            case GLFW.GLFW_KEY_KP_DECIMAL -> 0x6E;
            case GLFW.GLFW_KEY_KP_DIVIDE -> 0x6F;
            case GLFW.GLFW_KEY_KP_MULTIPLY -> 0x6A;
            case GLFW.GLFW_KEY_KP_SUBTRACT -> 0x6D;
            case GLFW.GLFW_KEY_KP_ADD -> 0x6B;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> 0xA0;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> 0xA1;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> 0xA2;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> 0xA3;
            case GLFW.GLFW_KEY_LEFT_ALT -> 0xA4;
            case GLFW.GLFW_KEY_RIGHT_ALT -> 0xA5;
            case GLFW.GLFW_KEY_LEFT_SUPER -> 0x5B;
            case GLFW.GLFW_KEY_RIGHT_SUPER -> 0x5C;
            default -> 0;
        };
    }
}
