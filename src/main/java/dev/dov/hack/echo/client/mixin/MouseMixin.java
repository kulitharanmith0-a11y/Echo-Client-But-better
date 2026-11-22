package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.input.MouseMovementEvent;
import dev.dov.hack.echo.client.handlers.input.InputSimulator;
import dev.dov.hack.echo.client.keyboard.KeybindingCache;
import dev.dov.hack.echo.client.render.RenderBackendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mouse.class, priority = 2000)
public class MouseMixin {
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @ModifyVariable(method = "onMouseButton", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeButtonWindow(long window) {
        return echo$normalizeWindow(window);
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void cacheMouseButton(long window, MouseInput input, int action, CallbackInfo callback) {
        if (EchoClient.eventsDisabled) {
            return;
        }
        if (InputSimulator.isSimulating()) {
            callback.cancel();
            return;
        }
        InputSimulator simulator = InputSimulator.get();
        if (simulator != null && simulator.consumeOsReplayMouse(input.button(), action)) {
            callback.cancel();
            return;
        }
        KeybindingCache.setMousePressed(input.button(), action == GLFW.GLFW_PRESS);
    }

    @ModifyVariable(method = "onCursorPos", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeCursorWindow(long window) {
        return echo$normalizeWindow(window);
    }

    @ModifyVariable(method = "onMouseScroll", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long normalizeScrollWindow(long window) {
        return echo$normalizeWindow(window);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void filterSimulatedScroll(long window, double horizontal, double vertical, CallbackInfo callback) {
        if (EchoClient.eventsDisabled) {
            return;
        }
        if (InputSimulator.isSimulating()) {
            callback.cancel();
            return;
        }
        InputSimulator simulator = InputSimulator.get();
        if (simulator != null && simulator.consumeOsReplayScroll(vertical)) {
            callback.cancel();
        }
    }

    @Unique
    private static long echo$normalizeWindow(long window) {
        RenderBackendManager backend = EchoClient.externalRenderer;
        return backend != null && window == backend.windowHandle()
                ? MinecraftClient.getInstance().getWindow().getHandle() : window;
    }

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void updateMouseDeltas(CallbackInfo callback) {
        if (EchoClient.eventsDisabled) {
            return;
        }
        MouseMovementEvent.Pre event = new MouseMovementEvent.Pre(cursorDeltaX, cursorDeltaY);
        event.post();
        cursorDeltaX = event.deltaX;
        cursorDeltaY = event.deltaY;
    }

    @Inject(method = "updateMouse", at = @At("TAIL"), cancellable = true)
    private void afterMouseUpdate(CallbackInfo callback) {
        if (EchoClient.eventsDisabled) {
            return;
        }
        MouseMovementEvent.Post event = new MouseMovementEvent.Post(cursorDeltaX, cursorDeltaY);
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }
}
