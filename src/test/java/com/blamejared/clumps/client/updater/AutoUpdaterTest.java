package com.blamejared.clumps.client.updater;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoUpdaterTest {

    @Test
    void versionComparisonHandlesSemverAndPrefixes() {
        assertTrue(AutoUpdater.compareVersions("3.2", "3.1") > 0);
        assertTrue(AutoUpdater.compareVersions("3.10", "3.2") > 0);
        assertTrue(AutoUpdater.compareVersions("v3.2.1", "3.2.0") > 0);
        assertEquals(0, AutoUpdater.compareVersions("v3.2", "3.2.0"));
        assertTrue(AutoUpdater.compareVersions("3.1", "3.2") < 0);
    }

    @Test
    void asciiSanitizerRemovesUnsupportedGlyphs() {
        String cleaned = AutoUpdater.sanitizeAscii("✨ New Module • Plate Web — “fast”");
        assertEquals("New Module | Plate Web - \"fast\"", cleaned);
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            assertTrue(c >= 32 && c <= 126, "Non-ASCII char found: " + (int) c);
        }
    }
}
