package dev.dov.hack.echo.client.handlers;

import dev.dov.hack.echo.client.modules.Module;

final class DeferredFeatureAction {
    final Module owner;
    final int baselineSlot;
    int selectedSlot;
    boolean restoreAll;
    long expiresAtTick;
    ActionPhase phase;
    Runnable callback;

    DeferredFeatureAction(Module owner, int baselineSlot, int selectedSlot, boolean restoreAll, long expiresAtTick, ActionPhase phase, Runnable callback) {
        this.owner = owner;
        this.baselineSlot = baselineSlot;
        this.selectedSlot = selectedSlot;
        this.restoreAll = restoreAll;
        this.expiresAtTick = expiresAtTick;
        this.phase = phase;
        this.callback = callback;
    }

    boolean isDue(long tick, ActionPhase currentPhase) {
        return expiresAtTick != Long.MAX_VALUE && tick >= expiresAtTick && currentPhase.ordinal() >= phase.ordinal();
    }
}
