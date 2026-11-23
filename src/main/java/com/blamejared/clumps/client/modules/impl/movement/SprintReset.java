package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.SprintOverrideHandler;
import com.blamejared.clumps.client.keyboard.KeyStateOverrides;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.mixin.accessor.InputAccessor;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

public class SprintReset extends Module {
    private static final ChoiceSetting sprintResetMode = new ChoiceSetting(CatalogText.literal("Mode"), Mode.W_TAP.label, Mode.W_TAP.label, Mode.S_TAP.label, Mode.NO_STOP.label, Mode.SNEAK.label);
    private static IntSetting reactionTime;
    private static IntSetting duration;
    private static IntSetting durationBetweenReset;
    private static BooleanSetting groundOnly;
    private int lastResetTick;
    private boolean sprintSuppressionActive;

    public SprintReset() {
        super(new ModuleInfo(CatalogText.literal("Sprint Reset"), CatalogText.literal("Assists with combo attacks"), ModuleCategory.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        lastResetTick = -1;
        reactionTime.setAllowOutOfRange(true);
        duration.setAllowOutOfRange(true);
        durationBetweenReset.setAllowOutOfRange(true);
        reactionTime.description(CatalogText.literal(
            "Ticks after the hit before the reset starts. NoStop and Sneak wait at least 1 tick."));
        groundOnly.setVisibilityPredicate(ignored -> selectedMode().stopsMovement());
        groundOnly.description(CatalogText.literal("Do not reset while airborne"));
        reactionTime.tooltip("Timing");
        duration.tooltip("Timing");
        durationBetweenReset.tooltip("Timing");
        sprintResetMode.onChange(this::resetSprintResetState);
    }

    private Mode selectedMode() {
        for (Mode mode : Mode.values()) {
            if (sprintResetMode.isSelected(mode.label)) {
                return mode;
            }
        }
        return Mode.W_TAP;
    }

    @Override
    public CharSequence getSuffix() {
        return selectedMode().label;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetSprintResetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetSprintResetState();
    }

    @Subscribe
    public void onWorldTickContext(WorldTickContextEvent event) {
        resetSprintResetState();
    }

    private void resetSprintResetState() {
        lastResetTick = -1;
        releaseSprintSuppression();
    }

    private void releaseSprintSuppression() {
        if (!sprintSuppressionActive) {
            return;
        }
        sprintSuppressionActive = false;
        SprintOverrideHandler.setSuppressSprint(false);
    }

    public boolean isValidTarget(LivingEntity entity) {
        return TargetSelection.isValidTarget(entity);
    }

    @Subscribe(priority = EventPriority.LOWEST)
    public void onEntityAttack(EntityAttackEvent.Pre event) {
        if (isOutsideWorld() || event.cancelled || event.getPlayer() != client.player) {
            return;
        }
        if (lastResetTick > client.player.age) {
            resetSprintResetState();
        }
        if (MinecraftRenderState.getScreen() != null) {
            return;
        }
        Mode mode = selectedMode();
        boolean moving = mode == Mode.NO_STOP ? client.player.isSprinting() : !(client.player.forwardSpeed <= 0.0);
        if (!moving) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!isValidTarget(target)) {
            return;
        }
        if (lastResetTick != -1 && client.player.age - lastResetTick < durationBetweenReset.getValue()) {
            return;
        }
        lastResetTick = client.player.age;
        applyToCurrentInput(mode);
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (lastResetTick > client.player.age) {
            resetSprintResetState();
        }
        if (lastResetTick == -1) {
            return;
        }
        if (MinecraftRenderState.getScreen() != null || client.player.isDead()) {
            resetSprintResetState();
            return;
        }
        applyReset(selectedMode(), event);
    }

    private void applyReset(Mode mode, PlayerInputEvent input) {
        int elapsed = client.player.age - lastResetTick;
        int delay = mode.requiresOneTickDelay() ? Math.max(1, reactionTime.getValue()) : reactionTime.getValue();
        int end = delay + duration.getValue();
        if (elapsed >= end) {
            restoreInput(mode, input);
            lastResetTick = -1;
            releaseSprintSuppression();
            return;
        }
        if (elapsed < delay) {
            return;
        }
        boolean skipAirborne = mode.stopsMovement() && groundOnly.getValue() && !client.player.isOnGround();
        switch (mode) {
            case W_TAP -> {
                input.forward = skipAirborne && isForwardPressed();
                if (!skipAirborne) {
                    KeyStateOverrides.setPressed(client.options.forwardKey, false);
                }
            }
            case S_TAP -> {
                if (skipAirborne) {
                    input.forward = isForwardPressed();
                } else {
                    input.forward = false;
                    input.backward = true;
                    KeyStateOverrides.setPressed(client.options.forwardKey, false);
                    KeyStateOverrides.setPressed(client.options.backKey, true);
                }
            }
            case NO_STOP -> {
                sprintSuppressionActive = true;
                SprintOverrideHandler.setSuppressSprint(true);
            }
            case SNEAK -> {
                input.sneak = true;
                KeyStateOverrides.setPressed(client.options.sneakKey, true);
            }
        }
    }

    private void restoreInput(Mode mode, PlayerInputEvent input) {
        switch (mode) {
            case W_TAP -> input.forward = isForwardPressed();
            case S_TAP -> {
                input.forward = isForwardPressed();
                input.backward = isBackwardPressed();
            }
            case SNEAK -> input.sneak = isSneakPressed();
        }
    }

    private void applyToCurrentInput(Mode mode) {
        if (client.player.input == null) {
            return;
        }
        InputAccessor accessor = (InputAccessor) client.player.input;
        PlayerInput keys = accessor.getPlayerInput();
        PlayerInputEvent input = new PlayerInputEvent(keys.forward(), keys.backward(), keys.left(), keys.right(), keys.jump(), keys.sneak(), keys.sprint());
        applyReset(mode, input);
        accessor.setPlayerInput(new PlayerInput(input.forward, input.backward, input.left, input.right, input.jump, input.sneak, input.sprint));
        accessor.setMovementVector(new Vec2f(PlayerInputEvent.movementMultiplier(input.left, input.right), PlayerInputEvent.movementMultiplier(input.forward, input.backward)));
    }

    private boolean isForwardPressed() {
        return KeybindingCache.isPressed(client.options.forwardKey);
    }

    private boolean isBackwardPressed() {
        return KeybindingCache.isPressed(client.options.backKey);
    }

    private boolean isSneakPressed() {
        return KeybindingCache.isPressed(client.options.sneakKey);
    }

    public static void appendBootstrapSettings(SprintReset Module) {
        CatalogSettings.append(Module, sprintResetMode);
        CatalogSettings.append(Module, reactionTime);
        CatalogSettings.append(Module, duration);
        CatalogSettings.append(Module, durationBetweenReset);
        CatalogSettings.append(Module, groundOnly);
    }

    public static void assignBootstrapSettings(SprintReset Module, IntSetting preparedDurationBetweenReset, IntSetting preparedDuration, BooleanSetting preparedGroundOnly, IntSetting preparedReactionTime) {
        durationBetweenReset = preparedDurationBetweenReset;
        duration = preparedDuration;
        groundOnly = preparedGroundOnly;
        reactionTime = preparedReactionTime;
    }

    
    enum Mode {
        W_TAP(CatalogText.literal("W-Tap")), S_TAP(CatalogText.literal("S-Tap")), NO_STOP(CatalogText.literal("NoStop")), SNEAK(CatalogText.literal("Sneak"));

        final CharSequence label;

        Mode(CharSequence label) {
            this.label = label;
        }

        boolean requiresOneTickDelay() {
            return this == NO_STOP || this == SNEAK;
        }

        boolean stopsMovement() {
            return this == W_TAP || this == S_TAP;
        }
    }
}
