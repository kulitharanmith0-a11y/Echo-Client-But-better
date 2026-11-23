package com.blamejared.clumps.client.handlers;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.KeyboardEvent;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.internals.ClickGui;
import com.blamejared.clumps.client.keyboard.InputKey;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public class FeatureBindHandler extends ClientHandler {
    private final Map<Integer, Boolean> previousMouseStates = new HashMap<>();

    @Subscribe
    public void onKeyboard(KeyboardEvent event) {
        if (ClumpsClient.ModuleManager == null) {
            return;
        }
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        if (!pressed && event.getAction() != GLFW.GLFW_RELEASE) {
            return;
        }
        if (pressed && CLIENT.currentScreen != null) {
            boolean dashboardOpen = ClumpsClient.screenManager != null && ClumpsClient.screenManager.isOpen();
            if (!dashboardOpen) {
                return;
            }
        }
        boolean screenOpen = CLIENT.currentScreen != null;
        for (Module Module : ClumpsClient.ModuleManager.getFeatures()) {
            if (!Module.isKeybindable()) {
                continue;
            }
            if (pressed && screenOpen && !(Module instanceof ClickGui)) {
                continue;
            }
            InputKey key = Module.getKey();
            if (key == InputKey.NONE || key != InputKey.fromCode(event.getKey())) {
                continue;
            }
            if (Module.isHoldMode()) {
                Module.setEnabled(pressed);
            } else if (pressed) {
                Module.toggle();
            }
        }
    }

    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (CLIENT.currentScreen != null || ClumpsClient.ModuleManager == null) {
            return;
        }
        Map<Integer, Boolean> currentMouseStates = new HashMap<>();
        for (Module Module : ClumpsClient.ModuleManager.getFeatures()) {
            if (!Module.isKeybindable()) {
                continue;
            }
            InputKey key = Module.getKey();
            if (key == InputKey.NONE || !key.isMouse()) {
                continue;
            }
            int inputCode = key.code();
            boolean pressed = currentMouseStates.computeIfAbsent(inputCode, ignored -> key.isPressed());
            if (Module.isHoldMode()) {
                Module.setEnabled(pressed);
            } else if (pressed && !previousMouseStates.getOrDefault(inputCode, false)) {
                Module.toggle();
            }
        }
        previousMouseStates.putAll(currentMouseStates);
    }
}
