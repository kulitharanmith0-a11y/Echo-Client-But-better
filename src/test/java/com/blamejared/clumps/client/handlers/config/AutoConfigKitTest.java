package com.blamejared.clumps.client.handlers.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoConfigKitTest {

    @Test
    void diaSmpExistsWithExpectedDisplayName() {
        assertEquals("Dia SMP", GamemodeKit.DIA_SMP.displayName());
        assertFalse(GamemodeKit.DIA_SMP.hint().isBlank());
    }

    @Test
    void depletionSubsetKeepsFightLockedWhenConsumablesRunOut() {
        assertTrue(GamemodeKit.SWORD.isDepletionSubsetOf(GamemodeKit.DIA_SMP));
        assertTrue(GamemodeKit.SWORD.isDepletionSubsetOf(GamemodeKit.SMP));
        assertTrue(GamemodeKit.SWORD.isDepletionSubsetOf(GamemodeKit.CRYSTAL));
        assertTrue(GamemodeKit.AXE.isDepletionSubsetOf(GamemodeKit.DIA_SMP));
        assertTrue(GamemodeKit.AXE.isDepletionSubsetOf(GamemodeKit.SMP));
        assertTrue(GamemodeKit.DIA_SMP.isDepletionSubsetOf(GamemodeKit.CRYSTAL));
        assertTrue(GamemodeKit.SMP.isDepletionSubsetOf(GamemodeKit.MACE));
    }

    @Test
    void depletionSubsetDoesNotBlockSwitchingToDistinctHigherKit() {
        assertFalse(GamemodeKit.CRYSTAL.isDepletionSubsetOf(GamemodeKit.DIA_SMP));
        assertFalse(GamemodeKit.MACE.isDepletionSubsetOf(GamemodeKit.SMP));
        assertFalse(GamemodeKit.DIA_SMP.isDepletionSubsetOf(GamemodeKit.SMP));
        assertFalse(GamemodeKit.SMP.isDepletionSubsetOf(GamemodeKit.DIA_SMP));
        assertFalse(GamemodeKit.DIA_SMP.isDepletionSubsetOf(GamemodeKit.DIA_SMP));
        assertFalse(GamemodeKit.SWORD.isDepletionSubsetOf(null));
    }

    @Test
    void normalizeServerKeyCleansWhitespaceCaseAndTrailingDots() {
        assertEquals("play.pvplegacy.net", AutoConfigManager.normalizeServerKey("  Play.PvPLegacy.NET. "));
        assertEquals("mc.server.gg:25565", AutoConfigManager.normalizeServerKey("MC.Server.GG:25565"));
        assertEquals("", AutoConfigManager.normalizeServerKey(null));
        assertEquals("", AutoConfigManager.normalizeServerKey("   "));
    }
}
