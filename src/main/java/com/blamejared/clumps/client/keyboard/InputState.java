package com.blamejared.clumps.client.keyboard;

import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.DropKeyEvent;
import com.blamejared.clumps.client.event.input.KeybindingEvent;
import com.blamejared.clumps.client.event.input.SwapHandsKeyEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.handlers.input.InputSimulator;
import com.blamejared.clumps.client.mixin.accessor.KeyBindingAccessor;
import com.blamejared.clumps.client.mixin.accessor.MouseAccessor;
import com.blamejared.clumps.client.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class InputState {
    private InputState() {
    }

    public static void applyExtraClicks(KeybindingEvent event) {
        if (event.cancelled || event.getClickCountDelta() <= 0
                || MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        InputSimulator simulator = InputSimulator.get();
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (simulator != null) {
            if (event instanceof AttackKeyEvent) {
                Module owner = coordinator != null ? coordinator.syntheticAttackOwner() : null;
                simulator.simulateAttackTap(owner);
                return;
            }
            if (event instanceof UseKeyEvent) {
                Module owner = coordinator != null ? coordinator.syntheticUseOwner() : null;
                simulator.simulateUseTap(owner);
                return;
            }
            if (event instanceof SwapHandsKeyEvent) {
                simulator.simulateOffhandSwap(null);
                return;
            }
            if (event instanceof DropKeyEvent) {
                simulator.simulateDropTap(null);
                return;
            }
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (event instanceof AttackKeyEvent) {
            replayMouseClicks(client, client.options.attackKey, event.getClickCountDelta());
        } else if (event instanceof UseKeyEvent) {
            replayMouseClicks(client, client.options.useKey, event.getClickCountDelta());
        }
    }

    private static void replayMouseClicks(MinecraftClient client, KeyBinding binding, int count) {
        if (KeybindingCache.isPressed(binding)) {
            return;
        }
        KeyStateOverrides.setPressed(binding, true);
        InputUtil.Key key = ((KeyBindingAccessor) binding).getBoundKey();
        if (key.getCategory() != InputUtil.Type.MOUSE) {
            return;
        }
        boolean wasPressed = binding.isPressed();
        long handle = client.getWindow().getHandle();
        MouseInput input = new MouseInput(key.getCode(), 0);
        for (int i = 0; i < count; i++) {
            ((MouseAccessor) client.mouse).invokeOnMouseButton(handle, input, GLFW.GLFW_PRESS);
            ((MouseAccessor) client.mouse).invokeOnMouseButton(handle, input, GLFW.GLFW_RELEASE);
        }
        binding.setPressed(wasPressed);
    }
}
