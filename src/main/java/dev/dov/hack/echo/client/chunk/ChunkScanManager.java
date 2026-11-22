package dev.dov.hack.echo.client.chunk;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;

public final class ChunkScanManager {
    public ChunkScanManager() {}

    public void register(Object owner, Object2IntOpenHashMap<Block> colors, boolean outline, boolean tracers) {}
    public void unregister(Object owner) {}
    public void setOutline(Object owner, boolean outline) {}
    public void setTracers(Object owner, boolean tracers) {}
    public void shutdown() {}
}
