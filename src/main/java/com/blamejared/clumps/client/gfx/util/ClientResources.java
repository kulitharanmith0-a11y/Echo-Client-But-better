package com.blamejared.clumps.client.gfx.util;

import com.blamejared.clumps.client.ClumpsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ClientResources {
    private ClientResources() {}

    public static byte[] readBytes(String path) {
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

    // Bare paths are mod assets; Identifier.of(path) alone would namespace them to minecraft.
    private static Identifier resolve(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Resource path is empty");
        }
        return path.indexOf(':') >= 0 ? Identifier.of(path) : Identifier.of("clumps", path);
    }
}
