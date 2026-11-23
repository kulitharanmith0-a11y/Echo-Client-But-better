package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.handlers.input.InputSimulator;
import com.blamejared.clumps.client.modules.impl.render.Freecam;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.mixin.accessor.KeyBindingAccessor;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Shadow @Final private GameOptions settings;

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTickHead(CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            clearInputOverrides();
            return;
        }
        PlayerInputEvent.inputProcessed = false;
        PlayerInputEvent event = new PlayerInputEvent(
            settings.forwardKey.isPressed(), settings.backKey.isPressed(), settings.leftKey.isPressed(), settings.rightKey.isPressed(), settings.jumpKey.isPressed(), settings.sneakKey.isPressed(), settings.sprintKey.isPressed()
        );
        event.post();
        PlayerInputEvent.lastInput = event;
        PlayerInputEvent.inputProcessed = true;
        updateKeyOverride(settings.forwardKey, event.forward, event.originalInput.forward());
        updateKeyOverride(settings.backKey, event.backward, event.originalInput.backward());
        updateKeyOverride(settings.leftKey, event.left, event.originalInput.left());
        updateKeyOverride(settings.rightKey, event.right, event.originalInput.right());
        updateKeyOverride(settings.jumpKey, event.jump, event.originalInput.jump());
        updateKeyOverride(settings.sneakKey, event.sneak, event.originalInput.sneak());
        updateKeyOverride(settings.sprintKey, event.sprint, event.originalInput.sprint());
        InputSimulator simulator = InputSimulator.get();
        if (simulator != null) {
            simulator.syncMovementInput(event);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void onTickReturn(CallbackInfo callback) {
        PlayerInputEvent event = PlayerInputEvent.lastInput;
        if (!ClumpsClient.eventsDisabled && event != null) {
            Freecam freecam = ClumpsClient.ModuleManager == null
                ? null : (Freecam) ClumpsClient.ModuleManager.findFeature(Freecam.class);
            if (freecam != null && freecam.isEnabled() && !event.allowMovementInFreecam) {
                playerInput = new PlayerInput(false, false, false, false, false, false, false);
                movementVector = Vec2f.ZERO;
            }
        }
    }

    @Unique
    private void updateKeyOverride(KeyBinding binding, boolean overridden, boolean original) {
        InputUtil.Key key = ((KeyBindingAccessor) binding).getBoundKey();
        if (key != null && key.getCategory() == InputUtil.Type.KEYSYM) {
            KeybindingCache.updateOverride(key.getCode(), overridden, original);
        }
    }

    @Unique
    private void clearInputOverrides() {
        PlayerInputEvent.lastInput = null;
        PlayerInputEvent.inputProcessed = false;
        KeybindingCache.clearOverrides();
        InputSimulator simulator = InputSimulator.get();
        if (simulator != null) {
            simulator.clearMovementOverrides();
        }
    }
}
