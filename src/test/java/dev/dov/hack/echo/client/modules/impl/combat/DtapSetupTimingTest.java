package dev.dov.hack.echo.client.modules.impl.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DtapSetupTimingTest {

    @Test
    void holdsBreakWhileHurtTimeIsActiveAbovePingLead() {
        assertTrue(DtapSetup.shouldHoldBreakForTickTiming(8, 2L, true, 0));
        assertTrue(DtapSetup.shouldHoldBreakForTickTiming(2, 8L, true, 50));
    }

    @Test
    void waitsForHurtTimePacketArrivalImmediatelyAfterCrystalBreak() {
        // Right after breaking Crystal #1, hurtTime on the client is still 0 due to ping;
        // Tick Timing must wait instead of wasting Crystal #2 during server i-frames.
        assertTrue(DtapSetup.shouldHoldBreakForTickTiming(0, 1L, false, 60));
        assertTrue(DtapSetup.shouldHoldBreakForTickTiming(0, 2L, false, 100));
    }

    @Test
    void releasesBreakOnceIframeWindowClearsOrNoDamageOccurred() {
        // HurtTime was observed and 10 ticks have elapsed -> break Crystal #2 now!
        assertFalse(DtapSetup.shouldHoldBreakForTickTiming(0, 10L, true, 0));
        assertFalse(DtapSetup.shouldHoldBreakForTickTiming(1, 9L, true, 80));
        // If Crystal #1 never damaged the enemy (no hurt observed after arrival window), do not hang.
        assertFalse(DtapSetup.shouldHoldBreakForTickTiming(0, 5L, false, 40));
    }
}
