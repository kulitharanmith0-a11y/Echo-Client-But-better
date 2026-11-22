package dev.dov.hack.echo.client.event.world;

import dev.dov.hack.echo.client.event.Event;
import java.lang.invoke.MethodHandles;
import net.minecraft.util.math.ChunkPos;

public class ChunkUnloadEvent extends Event {
    private final ChunkPos chunkPos;

    public ChunkUnloadEvent(ChunkPos chunkPos) {
        super();
        this.chunkPos = chunkPos;
    }

    public ChunkPos getChunkPos() {
        initializePayloadClass();
        return chunkPos;
    }

    
    private static void initializePayloadClass() {
        try {
            MethodHandles.lookup().ensureInitialized(ChunkPos.class);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Minecraft ChunkPos class is not accessible", failure);
        }
    }
}
