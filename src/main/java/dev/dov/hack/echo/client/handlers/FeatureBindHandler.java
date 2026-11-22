package dev.dov.hack.echo.client.handlers;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.KeyboardEvent;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.internals.ClickGui;
import dev.dov.hack.echo.client.keyboard.InputKey;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public class FeatureBindHandler extends ClientHandler {
    private final Map<Integer, Boolean> previousMouseStates = new HashMap<>();

    @Subscribe
    public void onKeyboard(KeyboardEvent event) {
        if (EchoClient.ModuleManager == null) {
            return;
        }
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        if (!pressed && event.getAction() != GLFW.GLFW_RELEASE) {
            return;
        }
        if (pressed && CLIENT.currentScreen != null) {
            boolean dashboardOpen = EchoClient.screenManager != null && EchoClient.screenManager.isOpen();
            if (!dashboardOpen) {
                return;
            }
        }
        boolean screenOpen = CLIENT.currentScreen != null;
        for (Module Module : EchoClient.ModuleManager.getFeatures()) {
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
        if (CLIENT.currentScreen != null || EchoClient.ModuleManager == null) {
            return;
        }
        Map<Integer, Boolean> currentMouseStates = new HashMap<>();
        for (Module Module : EchoClient.ModuleManager.getFeatures()) {
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
