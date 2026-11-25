package com.blamejared.clumps.client.gfx.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public final class ClientResources {
    private static final String VAULT_RESOURCE = "/assets/clumps/data.bin";
    private static final int VAULT_SEED = 0x6C554D50;
    private static volatile Map<Long, byte[]> vaultEntries;
    private static volatile boolean vaultLoaded;

    private ClientResources() {}

    public static byte[] readBytes(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Resource path is empty");
        }
        Map<Long, byte[]> vault = getVault();
        if (vault != null) {
            byte[] found = vault.get(fnv64(normalizeVaultKey(path)));
            if (found != null) {
                return found.clone();
            }
        }
        Identifier loc = resolve(path);
        try (InputStream in = MinecraftClient.getInstance().getResourceManager().open(loc)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + loc, e);
        }
    }

    public static String readText(String path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }

    private static Map<Long, byte[]> getVault() {
        if (vaultLoaded) {
            return vaultEntries;
        }
        synchronized (ClientResources.class) {
            if (vaultLoaded) {
                return vaultEntries;
            }
            vaultEntries = loadVault();
            vaultLoaded = true;
            return vaultEntries;
        }
    }

    private static Map<Long, byte[]> loadVault() {
        try (InputStream rawIn = ClientResources.class.getResourceAsStream(VAULT_RESOURCE)) {
            if (rawIn == null) {
                return null;
            }
            byte[] masked = rawIn.readAllBytes();
            int k = mix32(VAULT_SEED);
            for (int i = 0; i < masked.length; i++) {
                k = Integer.rotateLeft(k, 7) ^ (k * 31 + i + 13);
                masked[i] = (byte) (masked[i] ^ (k & 0xFF));
            }
            Map<Long, byte[]> out = new HashMap<>();
            try (DataInputStream dis = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(masked)))) {
                int count = dis.readInt();
                for (int i = 0; i < count; i++) {
                    long hash = dis.readLong();
                    int len = dis.readInt();
                    byte[] data = dis.readNBytes(len);
                    out.put(hash, data);
                }
            }
            return out;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String normalizeVaultKey(String path) {
        String p = path.replace('\\', '/').trim();
        if (p.startsWith("clumps:")) {
            p = p.substring("clumps:".length());
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (p.startsWith("assets/clumps/")) {
            p = p.substring("assets/clumps/".length());
        }
        return p.toLowerCase(Locale.ROOT);
    }

    private static long fnv64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= (s.charAt(i) & 0xFF);
            h *= 0x100000001b3L;
        }
        return h;
    }

    private static int mix32(int x) {
        x ^= (x >>> 16);
        x *= 0x85EBCA6B;
        x ^= (x >>> 13);
        x *= 0xC2B2AE35;
        x ^= (x >>> 16);
        return x;
    }

    // Bare paths are mod assets; Identifier.of(path) alone would namespace them to minecraft.
    private static Identifier resolve(String path) {
        return path.indexOf(':') >= 0 ? Identifier.of(path) : Identifier.of("clumps", path);
    }
}
