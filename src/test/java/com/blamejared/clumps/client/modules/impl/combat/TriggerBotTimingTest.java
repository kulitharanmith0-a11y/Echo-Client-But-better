package com.blamejared.clumps.client.modules.impl.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blamejared.clumps.client.modules.impl.combat.TriggerBotTiming.CritPolicy;
import org.junit.jupiter.api.Test;

class TriggerBotTimingTest {

    @Test
    void critCooldownEnforcesVanillaThresholdAndAxeBreakBypassesCooldown() {
        assertFalse(TriggerBotTiming.passesCooldown(0.89f, true, 0.85, 0.88, 100.0f, false));
        assertTrue(TriggerBotTiming.passesCooldown(0.91f, true, 0.85, 0.88, 100.0f, false));
        assertTrue(TriggerBotTiming.passesCooldown(0.92f, false, 0.90, 0.95, 100.0f, false));
        assertTrue(TriggerBotTiming.passesCooldown(0.10f, true, 0.95, 0.95, 100.0f, true));
    }

    @Test
    void normalAndPreciseCritRequireFallingAndNotSprinting() {
        assertTrue(TriggerBotTiming.allowsCriticalTiming(
                CritPolicy.PRECISE, true, false, false, false, true, false, false));
        assertFalse(TriggerBotTiming.allowsCriticalTiming(
                CritPolicy.PRECISE, true, false, false, false, true, true, false));
        assertTrue(TriggerBotTiming.allowsCriticalTiming(
                CritPolicy.NORMAL, true, false, false, false, true, false, false));
        assertFalse(TriggerBotTiming.allowsCriticalTiming(
                CritPolicy.NORMAL, true, false, false, false, true, true, false));
        assertTrue(TriggerBotTiming.allowsCriticalTiming(
                CritPolicy.NORMAL, true, false, false, true, false, false, true));
        assertFalse(TriggerBotTiming.allowsCriticalTiming(
                CritPolicy.NORMAL, true, false, true, true, false, false, true));
        assertTrue(TriggerBotTiming.allowsCriticalTiming(
                CritPolicy.NONE, true, true, true, true, false, true, false));
    }

    @Test
    void critOnlyOnSpaceHoldsGroundSwingUntilAirborne() {
        assertFalse(TriggerBotTiming.allowsCriticalTiming(
                CritPolicy.PRECISE, true, true, true, true, false, false, true));
        assertTrue(TriggerBotTiming.allowsCriticalTiming(
                CritPolicy.PRECISE, true, true, false, false, false, false, false));
    }

    @Test
    void pingHurtLeadIsCappedAndRespectsToggle() {
        assertEquals(0, TriggerBotTiming.effectiveHurtLeadTicks(120, false));
        assertEquals(0, TriggerBotTiming.effectiveHurtLeadTicks(0, true));
        assertEquals(1, TriggerBotTiming.effectiveHurtLeadTicks(80, true));
        assertEquals(2, TriggerBotTiming.effectiveHurtLeadTicks(220, true));
        assertEquals(TriggerBotTiming.MAX_PING_HURT_LEAD_TICKS, TriggerBotTiming.effectiveHurtLeadTicks(600, true));
    }

    @Test
    void hurtTimeAllowsPingLeadAndPostShieldBreakFollowup() {
        assertTrue(TriggerBotTiming.passesHurtTime(true, 1, 1, false, false, false, 1));
        assertFalse(TriggerBotTiming.passesHurtTime(true, 2, 2, false, false, false, 1));
        assertTrue(TriggerBotTiming.passesHurtTime(true, 0, 8, true, false, false, 0));
        assertTrue(TriggerBotTiming.passesHurtTime(true, 5, 5, false, true, false, 0));
        assertFalse(TriggerBotTiming.passesHurtTime(true, 5, 5, false, true, true, 0));
        assertTrue(TriggerBotTiming.passesHurtTime(false, 10, 10, false, false, true, 0));
    }

    @Test
    void targetAcquisitionGracePreventsOneTickFlickerReset() {
        assertTrue(TriggerBotTiming.isTargetAcquisitionStillValid(42, 42, 1000L, 1120L, 150L));
        assertFalse(TriggerBotTiming.isTargetAcquisitionStillValid(42, 42, 1000L, 1200L, 150L));
        assertFalse(TriggerBotTiming.isTargetAcquisitionStillValid(42, 99, 1000L, 1050L, 150L));
    }

    @Test
    void quickSwapToSwordOrAxeClampsStaleVanillaCooldownProgress() {
        // Same-tick swap from fast item to sword (attack speed 1.6 -> 12.5 ticks): vanilla progress is stale 1.0f
        float sameTickSword = TriggerBotTiming.effectiveSwapAwareProgress(1.0f, 100L, 100L, 1.6);
        assertTrue(sameTickSword < 0.10f);
        // Same-tick swap to axe (attack speed 1.0 -> 20 ticks): stale 1.0f must be clamped
        float sameTickAxe = TriggerBotTiming.effectiveSwapAwareProgress(1.0f, 100L, 100L, 1.0);
        assertTrue(sameTickAxe < 0.05f);
        // After 12 ticks on sword (12.5/12.5 = 1.0f), full cooldown is allowed
        float chargedSword = TriggerBotTiming.effectiveSwapAwareProgress(1.0f, 112L, 100L, 1.6);
        assertEquals(1.0f, chargedSword, 0.001f);
        // After 10 ticks on axe (10.5/20 = 0.525f), still waiting
        float partialAxe = TriggerBotTiming.effectiveSwapAwareProgress(1.0f, 110L, 100L, 1.0);
        assertEquals(0.525f, partialAxe, 0.01f);
        // No swap recorded -> trusts vanilla progress directly
        assertEquals(0.85f, TriggerBotTiming.effectiveSwapAwareProgress(0.85f, 100L, -1L, 1.6), 0.001f);
    }

    @Test
    void missChanceRespectsPercentBounds() {
        assertFalse(TriggerBotTiming.shouldMissAttack(0.0f, 0.0f));
        assertTrue(TriggerBotTiming.shouldMissAttack(25.0f, 0.24f));
        assertFalse(TriggerBotTiming.shouldMissAttack(25.0f, 0.25f));
        assertTrue(TriggerBotTiming.shouldMissAttack(100.0f, 0.99f));
    }
}
