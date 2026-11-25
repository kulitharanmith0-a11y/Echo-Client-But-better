package com.blamejared.clumps.client.handlers;

import com.blamejared.clumps.client.diagnostics.DevDiagnostics;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.KeyPressedQueryEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.StickyKeyBinding;

public class SprintOverrideHandler extends ClientHandler {
    private static boolean forceSprint;
    private static boolean suppressSprint;

    public static void setForceSprint(boolean force) {
        if (DevDiagnostics.ENABLED && forceSprint != force) {
            DevDiagnostics.logRateLimited("sprint-force:" + force, 200L, "SprintOverride", "FORCE_SPRINT", "value=" + force);
        }
        forceSprint = force;
        if (!force || CLIENT.player == null || CLIENT.player.isSprinting()) {
            return;
        }
        CLIENT.player.setSprinting(true);
        setSprintKeyPressed(true);
    }

    public static void setSuppressSprint(boolean suppress) {
        if (DevDiagnostics.ENABLED && suppressSprint != suppress) {
            DevDiagnostics.logRateLimited("sprint-suppress:" + suppress, 200L, "SprintOverride", "SUPPRESS_SPRINT", "value=" + suppress);
        }
        suppressSprint = suppress;
        if (!suppress || CLIENT.player == null || !CLIENT.player.isSprinting()) {
            return;
        }
        CLIENT.player.setSprinting(false);
        setSprintKeyPressed(false);
    }

    private static void setSprintKeyPressed(boolean pressed) {
        GameOptions options = CLIENT.options;
        if (options == null) {
            return;
        }
        if (options.sprintKey instanceof StickyKeyBinding && options.getSprintToggled().getValue()) {
            if (options.sprintKey.isPressed() != pressed) {
                options.sprintKey.setPressed(true);
            }
            return;
        }
        options.sprintKey.setPressed(pressed);
    }

    public static void resetOverrides() {
        forceSprint = false;
        suppressSprint = false;
    }

    @Subscribe
    public void onWorldTickContext(WorldTickContextEvent event) {
        resetOverrides();
    }

    @Subscribe
    public void onSprintKeyQuery(KeyPressedQueryEvent event) {
        if (suppressSprint) {
            event.setPressed(false);
            return;
        }
        if (forceSprint) {
            event.setPressed(true);
        }
    }

    public static boolean isForceSprint() {
        return forceSprint;
    }

    public static boolean isSuppressSprint() {
        return suppressSprint;
    }
}
