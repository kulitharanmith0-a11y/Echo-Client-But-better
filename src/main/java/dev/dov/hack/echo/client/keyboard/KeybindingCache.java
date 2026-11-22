package dev.dov.hack.echo.client.keyboard;

import dev.dov.hack.echo.client.mixin.accessor.KeyBindingAccessor;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class KeybindingCache {
    public static final int RELEASE = 0;
    public static final int PRESS = 1;

    private static final ConcurrentHashMap<Integer, Boolean> overrides = new ConcurrentHashMap<>();
    private static final boolean[] mousePressed = new boolean[8];

    private KeybindingCache() {
    }

    
    public static void updateOverride(int code, boolean pressed, boolean originalPressed) {
        if (pressed != originalPressed) {
            overrides.put(code, pressed);
        } else {
            overrides.remove(code);
        }
    }

    
    public static Boolean getOverride(int code) {
        return overrides.get(code);
    }

    public static void clearOverrides() {
        overrides.clear();
    }

    public static void setMousePressed(int button, boolean pressed) {
        if (button >= 0 && button < mousePressed.length) {
            mousePressed[button] = pressed;
        }
    }

    public static boolean isMousePressed(int button) {
        if (button < 0 || button >= mousePressed.length) {
            return false;
        }
        if (!MinecraftClient.getInstance().isWindowFocused()) {
            Arrays.fill(mousePressed, false);
            return false;
        }
        return mousePressed[button];
    }

    public static InputKey getKey(KeyBinding binding) {
        if (binding == null) {
            return InputKey.NONE;
        }
        InputUtil.Key boundKey = ((KeyBindingAccessor) binding).getBoundKey();
        if (boundKey == null) {
            return InputKey.NONE;
        }
        return boundKey.getCategory() == InputUtil.Type.MOUSE
                ? InputKey.fromMouseCode(boundKey.getCode())
                : InputKey.fromCode(boundKey.getCode());
    }

    public static boolean isPressed(KeyBinding binding) {
        ensureInitialized(KeyBinding.class);
        ensureInitialized(InputKey.class);
        return getKey(binding).isPressed();
    }

    
    private static void ensureInitialized(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Resolved class initialization failed", exception);
        }
    }
}
