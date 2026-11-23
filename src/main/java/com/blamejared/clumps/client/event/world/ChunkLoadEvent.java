package com.blamejared.clumps.client.event.world;

import com.blamejared.clumps.client.event.Event;
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
