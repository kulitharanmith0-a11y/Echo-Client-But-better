package dev.dov.hack.echo.client.event.world;

import dev.dov.hack.echo.client.event.Event;
import java.lang.invoke.MethodHandles;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkLoadEvent extends Event {
    private final WorldChunk chunk;

    
    public ChunkLoadEvent(WorldChunk chunk) {
        super();
        this.chunk = chunk;
    }

    
    public WorldChunk getChunk() {
        try {
            MethodHandles.lookup().ensureInitialized(WorldChunk.class);
        } catch (IllegalAccessException inaccessibleType) {
            throw new IllegalAccessError(inaccessibleType.getMessage());
        }
        return chunk;
    }
}
