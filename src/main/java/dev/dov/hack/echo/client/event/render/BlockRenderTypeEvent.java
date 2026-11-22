package dev.dov.hack.echo.client.event.render;

import dev.dov.hack.echo.client.event.Event;
import net.minecraft.block.Block;

public class BlockRenderTypeEvent extends Event {
    
    public final Block block;

    
    public BlockRenderTypeEvent(Block block) {
        this.block = block;
    }
}
