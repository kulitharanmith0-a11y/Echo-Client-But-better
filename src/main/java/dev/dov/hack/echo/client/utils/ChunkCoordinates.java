package dev.dov.hack.echo.client.utils;

import net.minecraft.util.math.ChunkPos;

public final class ChunkCoordinates {
    private ChunkCoordinates() {
    }

    public static int getX(ChunkPos chunk) {
        return chunk.x;
    }

    public static int getZ(ChunkPos chunk) {
        return chunk.z;
    }
}
