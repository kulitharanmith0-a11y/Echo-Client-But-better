package com.blamejared.clumps.client.config;

import com.blamejared.clumps.client.friends.FriendManager;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleManager;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.network.ServerInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class CloudConfigCatalogTest {

    @AfterEach
    void resetCache() {
        CloudConfigCatalog.seedCacheForTests(List.of());
    }

    @Test
    void shareCodeRoundTripStripsFriendsAndRejectsInvalidInput() {
        String rawSnapshot = "{\"settings\":[{\"name\":\"abcd1234\",\"enabled\":true}],\"friends\":[\"SecretFriend\"]}";
        String code = CloudConfigCatalog.encodeShareCode("Sword PvP", "Legit duels", "GHOST", "Tester", rawSnapshot);
        assertNotNull(code);
        assertTrue(code.startsWith(CloudConfigCatalog.SHARE_PREFIX));

        CloudConfigCatalog.DecodedShare decoded = CloudConfigCatalog.decodeShareCode(code);
        assertNotNull(decoded);
        assertEquals("Sword PvP", decoded.name());
        assertEquals("Legit duels", decoded.description());
        assertEquals("GHOST", decoded.type());
        assertEquals("Tester", decoded.author());
        assertFalse(decoded.dataJson().contains("SecretFriend"), "Friends must be stripped from share codes");
        assertTrue(decoded.dataJson().contains("abcd1234"));

        assertNull(CloudConfigCatalog.decodeShareCode(""));
        assertNull(CloudConfigCatalog.decodeShareCode("CFG1:not-valid-base64!!!"));
        assertNull(CloudConfigCatalog.decodeShareCode("{\"unrelated\":true}"));
    }

    @Test
    void rawSnapshotFallbackDecodesCleanly() {
        CloudConfigCatalog.DecodedShare decoded = CloudConfigCatalog.decodeShareCode("{\"settings\":[]}");
        assertNotNull(decoded);
        assertEquals("Imported Config", decoded.name());
        assertTrue(decoded.dataJson().contains("\"settings\":[]"));
    }

    @Test
    void parseCatalogJsonHandlesObjectAndStringDataEntries() {
        String json = """
                {
                  "configs": [
                    {
                      "id": "legit-sword",
                      "name": "Legit Sword",
                      "description": "Clean PvP setup",
                      "author": "Dev",
                      "serverName": "Minemen",
                      "serverIp": "minemen.club",
                      "type": "GHOST",
                      "updatedAt": 1700000000000,
                      "data": { "settings": [] }
                    },
                    {
                      "name": "Crystal Starter",
                      "type": "RAGE",
                      "updatedAt": 1700000005000,
                      "data": "{\\"settings\\":[]}"
                    }
                  ]
                }
                """;
        List<CloudConfigCatalog.CloudEntry> entries = CloudConfigCatalog.parseCatalogJson(json);
        assertEquals(2, entries.size());

        StoreMeta first = entries.get(0).meta();
        assertEquals("cloud:legit-sword", first.id());
        assertTrue(first.publicVisibility());
        assertFalse(first.owned());
        assertEquals("Dev", first.author());
        assertEquals("Minemen", first.serverName());

        StoreMeta second = entries.get(1).meta();
        assertEquals("cloud:crystal-starter", second.id());
        assertEquals("Community", second.author());
        assertEquals("RAGE", second.type());
    }

    @Test
    void cloudInstallUpdateAndShareCodeImportWorkEndToEnd(@TempDir Path tempDir) {
        System.setProperty("clumps.noCloudConfigs", "true");
        String initialCatalog = """
                {
                  "configs": [
                    {
                      "id": "starter-pvp",
                      "name": "Starter PvP",
                      "description": "Balanced starter",
                      "author": "Community",
                      "type": "GHOST",
                      "updatedAt": 1000,
                      "data": { "settings": [] }
                    }
                  ]
                }
                """;
        CloudConfigCatalog.seedCacheForTests(CloudConfigCatalog.parseCatalogJson(initialCatalog));

        ModuleConfig.ClientState stubState = new ModuleConfig.ClientState() {
            @Override public ModuleManager features() { return null; }
            @Override public FriendManager friends() { return null; }
            @Override public ServerInfo server() { return null; }
            @Override public boolean writesDisabled() { return false; }
            @Override public boolean isClickGui(Module feature) { return false; }
        };

        try (ModuleConfig config = new ModuleConfig(new ProfileService(tempDir), stubState)) {
            List<StoreMeta> listed = config.list();
            assertTrue(listed.stream().anyMatch(m -> "cloud:starter-pvp".equals(m.id()) && m.publicVisibility()));

            StoreMeta installed = config.selectById("cloud:starter-pvp");
            assertNotNull(installed);
            assertFalse(installed.publicVisibility());
            assertEquals("cloud:starter-pvp", installed.sourceId());

            // Simulate upstream cloud update with a future timestamp
            long futureTs = System.currentTimeMillis() + 60_000L;
            String updatedCatalog = """
                    {
                      "configs": [
                        {
                          "id": "starter-pvp",
                          "name": "Starter PvP",
                          "description": "Updated",
                          "author": "Community",
                          "type": "GHOST",
                          "updatedAt": %d,
                          "data": { "settings": [] }
                        }
                      ]
                    }
                    """.formatted(futureTs);
            CloudConfigCatalog.seedCacheForTests(CloudConfigCatalog.parseCatalogJson(updatedCatalog));

            List<StoreMeta> afterBump = config.list();
            StoreMeta localMeta = afterBump.stream()
                    .filter(m -> installed.id().equals(m.id()))
                    .findFirst()
                    .orElseThrow();
            assertTrue(localMeta.updateAvailable(), "Local copy should flag updateAvailable when cloud updatedAt is newer");

            StoreMeta updated = config.selectAndApplyPreferred(installed.id(), installed.sourceId());
            assertNotNull(updated);
            assertEquals(installed.id(), updated.id());

            String exported = config.exportShareCode(installed.id());
            assertNotNull(exported);
            StoreMeta imported = config.importShareCode(exported);
            assertNotNull(imported);
            assertEquals("Starter PvP (2)", imported.name());
        }
    }
}
