package com.blamejared.clumps.client.keyboard;

import com.blamejared.clumps.client.handlers.input.InputSimulator;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public enum InputKey {
    SPACE("SPACE", 32, 44), APOSTROPHE("APOSTROPHE", 39, 52), COMMA("COMMA", 44, 54), MINUS("MINUS", 45, 45), PERIOD("PERIOD", 46, 55), SLASH("SLASH", 47, 56), NUM_0("NUM_0", 48, 39), NUM_1("NUM_1", 49, 30), NUM_2("NUM_2", 50, 31), NUM_3("NUM_3", 51, 32), NUM_4("NUM_4", 52, 33), NUM_5("NUM_5", 53, 34), NUM_6("NUM_6", 54, 35), NUM_7("NUM_7", 55, 36), NUM_8("NUM_8", 56, 37), NUM_9("NUM_9", 57, 38), SEMICOLON("SEMICOLON", 59, 51), EQUAL("EQUAL", 61, 46), A("A", 65, 4), B("B", 66, 5), C("C", 67, 6), D("D", 68, 7), E("E", 69, 8), F("F", 70, 9), G("G", 71, 10), H("H", 72, 11), I("I", 73, 12), J("J", 74, 13), K("K", 75, 14), L("L", 76, 15), M("M", 77, 16), N("N", 78, 17), O("O", 79, 18), P("P", 80, 19), Q("Q", 81, 20), R("R", 82, 21), S("S", 83, 22), T("T", 84, 23), U("U", 85, 24), V("V", 86, 25), W("W", 87, 26), X("X", 88, 27), Y("Y", 89, 28), Z("Z", 90, 29), LEFT_BRACKET("LEFT_BRACKET", 91, 47), BACKSLASH("BACKSLASH", 92, 49), RIGHT_BRACKET("RIGHT_BRACKET", 93, 48), GRAVE_ACCENT("GRAVE_ACCENT", 96, 53), ESCAPE("ESCAPE", 256, 41), ENTER("ENTER", 257, 40), TAB("TAB", 258, 43), BACKSPACE("BACKSPACE", 259, 42), INSERT("INSERT", 260, 73), DELETE("DELETE", 261, 76), RIGHT("RIGHT", 262, 79), LEFT("LEFT", 263, 80), DOWN("DOWN", 264, 81), UP("UP", 265, 82), PAGE_UP("PAGE_UP", 266, 75), PAGE_DOWN("PAGE_DOWN", 267, 78), HOME("HOME", 268, 74), END("END", 269, 77), CAPS_LOCK("CAPS_LOCK", 280, 57), SCROLL_LOCK("SCROLL_LOCK", 281, 71), NUM_LOCK("NUM_LOCK", 282, 83), PRINT_SCREEN("PRINT_SCREEN", 283, 70), PAUSE("PAUSE", 284, 72), F1("F1", 290, 58), F2("F2", 291, 59), F3("F3", 292, 60), F4("F4", 293, 61), F5("F5", 294, 62), F6("F6", 295, 63), F7("F7", 296, 64), F8("F8", 297, 65), F9("F9", 298, 66), F10("F10", 299, 67), F11("F11", 300, 68), F12("F12", 301, 69), F13("F13", 302, 104), F14("F14", 303, 105), F15("F15", 304, 106), F16("F16", 305, 107), F17("F17", 306, 108), F18("F18", 307, 109), F19("F19", 308, 110), F20("F20", 309, 111), F21("F21", 310, 112), F22("F22", 311, 113), F23("F23", 312, 114), F24("F24", 313, 115), KEYPAD_0("KEYPAD_0", 320, 98), KEYPAD_1("KEYPAD_1", 321, 89), KEYPAD_2("KEYPAD_2", 322, 90), KEYPAD_3("KEYPAD_3", 323, 91), KEYPAD_4("KEYPAD_4", 324, 92), KEYPAD_5("KEYPAD_5", 325, 93), KEYPAD_6("KEYPAD_6", 326, 94), KEYPAD_7("KEYPAD_7", 327, 95), KEYPAD_8("KEYPAD_8", 328, 96), KEYPAD_9("KEYPAD_9", 329, 97), KEYPAD_DECIMAL("KEYPAD_DECIMAL", 330, 99), KEYPAD_DIVIDE("KEYPAD_DIVIDE", 331, 84), KEYPAD_MULTIPLY("KEYPAD_MULTIPLY", 332, 85), KEYPAD_SUBTRACT("KEYPAD_SUBTRACT", 333, 86), KEYPAD_ADD("KEYPAD_ADD", 334, 87), KEYPAD_ENTER("KEYPAD_ENTER", 335, 88), KEYPAD_EQUAL("KEYPAD_EQUAL", 336, 103), LEFT_SHIFT("LEFT_SHIFT", 340, 225), LEFT_CONTROL("LEFT_CONTROL", 341, 224), LEFT_ALT("LEFT_ALT", 342, 226), LEFT_SUPER("LEFT_SUPER", 343, 227), RIGHT_SHIFT("RIGHT_SHIFT", 344, 229), RIGHT_CONTROL("RIGHT_CONTROL", 345, 228), RIGHT_ALT("RIGHT_ALT", 346, 230), RIGHT_SUPER("RIGHT_SUPER", 347, 231), MOUSE_LEFT("MOUSE_LEFT", 0, 1, true), MOUSE_RIGHT("MOUSE_RIGHT", 1, 3, true), MOUSE_MIDDLE("MOUSE_MIDDLE", 2, 2, true), MOUSE_4("MOUSE_4", 3, 4, true), MOUSE_5("MOUSE_5", 4, 5, true), MOUSE_6("MOUSE_6", 5, 6, true), MOUSE_7("MOUSE_7", 6, 7, true), MOUSE_8("MOUSE_8", 7, 8, true), NONE("NONE", -1, -1);

    private static final Int2ObjectArrayMap<InputKey> keyboardByCode = new Int2ObjectArrayMap<>();
    private static final Int2ObjectArrayMap<InputKey> mouseByCode = new Int2ObjectArrayMap<>();

    private final String label;
    private final int glfwCode;
    private final int legacyScanCode;
    private final boolean mouse;
    private int code;
    private static final String DISPLAY_NONE = NONE.label;
    private static final String DISPLAY_LEFT_MOUSE = "LMB";
    private static final String DISPLAY_RIGHT_MOUSE = "RMB";
    private static final String DISPLAY_MIDDLE_MOUSE = "MMB";
    private static final String DISPLAY_MOUSE_PREFIX = "MB";
    private static final String DISPLAY_LEFT = "LEFT ";
    private static final String DISPLAY_LEFT_SHORT = "L ";
    private static final String DISPLAY_RIGHT = "RIGHT ";
    private static final String DISPLAY_RIGHT_SHORT = "R ";
    private static final String DISPLAY_CONTROL = "CONTROL";
    private static final String DISPLAY_CONTROL_SHORT = "CTRL";
    private static final String DISPLAY_BACKSPACE = BACKSPACE.label;
    private static final String DISPLAY_BACKSPACE_SHORT = "BKSP";
    private static final String DISPLAY_DELETE = DELETE.label;
    private static final String DISPLAY_DELETE_SHORT = "DEL";
    private static final String DISPLAY_INSERT = INSERT.label;
    private static final String DISPLAY_INSERT_SHORT = "INS";
    private static final String DISPLAY_ESCAPE = ESCAPE.label;
    private static final String DISPLAY_ESCAPE_SHORT = "ESC";
    private static final String DISPLAY_PAGE_UP = "PAGE UP";
    private static final String DISPLAY_PAGE_UP_SHORT = "PG UP";
    private static final String DISPLAY_PAGE_DOWN = "PAGE DOWN";
    private static final String DISPLAY_PAGE_DOWN_SHORT = "PG DN";

    InputKey(String label, int glfwCode, int legacyScanCode) {
        this(label, glfwCode, legacyScanCode, false);
    }

    InputKey(String label, int glfwCode, int legacyScanCode, boolean mouse) {
        this.label = label;
        this.glfwCode = glfwCode;
        this.legacyScanCode = legacyScanCode;
        this.mouse = mouse;
    }

    static {
        for (InputKey key : values()) {
            key.code = key.glfwCode;
            if (key != NONE) {
                (key.mouse ? mouseByCode : keyboardByCode).put(key.code, key);
            }
        }
    }

    @Override
    public String toString() {
        return label;
    }

    
    public String displayLabel() {
        if (this == NONE) return DISPLAY_NONE;
        if (mouse) {
            return switch (this) {
                case MOUSE_LEFT -> DISPLAY_LEFT_MOUSE;
                case MOUSE_RIGHT -> DISPLAY_RIGHT_MOUSE;
                case MOUSE_MIDDLE -> DISPLAY_MIDDLE_MOUSE;
                default -> DISPLAY_MOUSE_PREFIX + (ordinal() - MOUSE_LEFT.ordinal() + 1);
            };
        }
        return normalizeDisplayLabel(InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString());
    }

    private static String normalizeDisplayLabel(String text) {
        return text.toUpperCase()
                .replace(DISPLAY_LEFT, DISPLAY_LEFT_SHORT)
                .replace(DISPLAY_RIGHT, DISPLAY_RIGHT_SHORT)
                .replace(DISPLAY_CONTROL, DISPLAY_CONTROL_SHORT)
                .replace(DISPLAY_BACKSPACE, DISPLAY_BACKSPACE_SHORT)
                .replace(DISPLAY_DELETE, DISPLAY_DELETE_SHORT)
                .replace(DISPLAY_INSERT, DISPLAY_INSERT_SHORT)
                .replace(DISPLAY_ESCAPE, DISPLAY_ESCAPE_SHORT)
                .replace(DISPLAY_PAGE_UP, DISPLAY_PAGE_UP_SHORT)
                .replace(DISPLAY_PAGE_DOWN, DISPLAY_PAGE_DOWN_SHORT);
    }

    public int code() {
        return code;
    }

    public boolean isMouse() {
        return mouse;
    }

    public boolean matches(int inputCode) {
        return code == inputCode;
    }

    public static InputKey fromCode(int code) {
        return keyboardByCode.getOrDefault(code, NONE);
    }

    public static InputKey fromMouseCode(int code) {
        return mouseByCode.getOrDefault(code, NONE);
    }

    
    public static InputKey parse(String text) {
        if (text == null || text.isBlank()) {
            return NONE;
        }
        for (InputKey key : values()) {
            if (key.label.equalsIgnoreCase(text)) {
                return key;
            }
        }
        return NONE;
    }

    public boolean isPressed() {
        if (this == NONE) {
            return false;
        }
        InputSimulator simulator = InputSimulator.get();
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        if (mouse) {
            if (simulator != null && simulator.isOsSimulatedMouse(code)) {
                return KeybindingCache.isMousePressed(code);
            }
            return GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS
                    || KeybindingCache.isMousePressed(code);
        }
        if (simulator != null && simulator.isOsSimulatedKey(code)) {
            return false;
        }
        return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }
}
