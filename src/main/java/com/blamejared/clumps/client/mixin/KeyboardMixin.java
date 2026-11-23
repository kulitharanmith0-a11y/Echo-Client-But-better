package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.input.KeyboardEvent;
import com.blamejared.clumps.client.handlers.input.InputSimulator;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.render.RenderBackendManager;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Keyboard.class, priority = 2000)
public class KeyboardMixin {
    @ModifyVariable(method = "onKey", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeKeyWindow(long window) {
        return clumps$resolveMainWindow(window);
    }

    @ModifyVariable(method = "onChar", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeCharWindow(long window) {
        return clumps$resolveMainWindow(window);
    }

    @Unique
    private static long clumps$resolveMainWindow(long window) {
        RenderBackendManager backend = ClumpsClient.externalRenderer;
        return backend != null && window == backend.windowHandle()
                ? MinecraftClient.getInstance().getWindow().getHandle()
                : window;
    }

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    public void postKeyEvent(long window, int action, KeyInput input, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        if (InputSimulator.isSimulating()) {
            callback.cancel();
            return;
        }
        InputSimulator simulator = InputSimulator.get();
        if (simulator != null && simulator.consumeOsReplayKey(input.key(), action)) {
            callback.cancel();
            return;
        }
        if (!InputKey.ESCAPE.matches(input.key())) {
            new KeyboardEvent(input.key(), action).post();
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void filterSimulatedChar(long window, CharInput input, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        if (InputSimulator.isSimulating()) {
            callback.cancel();
            return;
        }
        InputSimulator simulator = InputSimulator.get();
        if (simulator != null && simulator.shouldSuppressOsChar()) {
            callback.cancel();
        }
    }
}
