package com.blamejared.clumps.client.ui;

import com.blamejared.clumps.client.keyboard.InputKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class InputState {
    
    public static final int INPUT_COUNT = InputKey.MOUSE_8.code() + 1;

    private final boolean[] down = new boolean[INPUT_COUNT];
    private final boolean[] pressed = new boolean[INPUT_COUNT];
    private final boolean[] released = new boolean[INPUT_COUNT];
    private final List<KeyStroke> keyEvents = new ArrayList<>();
    private final List<Integer> characters = new ArrayList<>();
    private float mouseX = -Float.MAX_VALUE;
    private float mouseY = -Float.MAX_VALUE;
    private double horizontalScroll;
    private double verticalScroll;

    
    public void setPointer(float x, float y) {
        mouseX = x;
        mouseY = y;
    }

    
    public void setDown(int inputCode, boolean isDown) {
        if (inputCode < 0 || inputCode >= INPUT_COUNT) {
            return;
        }
        if (isDown && !down[inputCode]) {
            pressed[inputCode] = true;
        }
        if (!isDown && down[inputCode]) {
            released[inputCode] = true;
        }
        down[inputCode] = isDown;
    }

    
    public void addScroll(double horizontalAmount, double verticalAmount) {
        horizontalScroll += horizontalAmount;
        verticalScroll += verticalAmount;
    }

    
    public void addKeyEvent(int keyCode, int scanCode, int modifiers) {
        keyEvents.add(new KeyStroke(keyCode, scanCode, modifiers));
    }

    
    public void addCharacter(int character) {
        characters.add(character);
    }

    
    public float mouseX() { return mouseX; }

    
    public float mouseY() { return mouseY; }

    
    public boolean isDown(int inputCode) {
        return validInputCode(inputCode) && down[inputCode];
    }

    
    public boolean wasPressed(int inputCode) {
        return validInputCode(inputCode) && pressed[inputCode];
    }

    
    public boolean wasReleased(int inputCode) {
        return validInputCode(inputCode) && released[inputCode];
    }

    
    public double horizontalScroll() { return horizontalScroll; }

    
    public double verticalScroll() { return verticalScroll; }

    
    public List<KeyStroke> keyEvents() {
        return Collections.unmodifiableList(keyEvents);
    }

    
    public List<Integer> characters() {
        return Collections.unmodifiableList(characters);
    }

    
    public void endFrame() {
        Arrays.fill(pressed, false);
        Arrays.fill(released, false);
        horizontalScroll = 0.0;
        verticalScroll = 0.0;
        keyEvents.clear();
        characters.clear();
    }

    private static boolean validInputCode(int inputCode) {
        return inputCode >= 0 && inputCode < INPUT_COUNT;
    }
}
