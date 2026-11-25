package com.blamejared.clumps.client.handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeferredFeatureActionTest {

    @Test
    void postPhaseActionIsDueWhenOverdueAtEarlyOfNextTick() {
        DeferredFeatureAction action = new DeferredFeatureAction(null, 0, 1, true, 100L, ActionPhase.POST, null);
        assertFalse(action.isDue(99L, ActionPhase.POST));
        assertFalse(action.isDue(100L, ActionPhase.EARLY));
        assertTrue(action.isDue(100L, ActionPhase.POST));
        assertTrue(action.isDue(101L, ActionPhase.EARLY));
        assertTrue(action.isDue(101L, ActionPhase.POST));
    }

    @Test
    void earlyPhaseActionFiresAtEarlyAndPostOfExpiryTick() {
        DeferredFeatureAction action = new DeferredFeatureAction(null, 0, 1, false, 50L, ActionPhase.EARLY, null);
        assertFalse(action.isDue(49L, ActionPhase.POST));
        assertTrue(action.isDue(50L, ActionPhase.EARLY));
        assertTrue(action.isDue(50L, ActionPhase.POST));
        assertTrue(action.isDue(51L, ActionPhase.EARLY));
    }

    @Test
    void manualHoldNeverBecomesDueAutomatically() {
        DeferredFeatureAction action = new DeferredFeatureAction(null, 0, 1, true, Long.MAX_VALUE, ActionPhase.EARLY, null);
        assertFalse(action.isDue(0L, ActionPhase.EARLY));
        assertFalse(action.isDue(Long.MAX_VALUE, ActionPhase.POST));
    }
}
