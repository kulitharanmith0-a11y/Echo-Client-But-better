package com.blamejared.clumps.client.event.render;

import com.blamejared.clumps.client.event.Event;
import net.minecraft.block.Block;

public class BlockRenderTypeEvent extends Event {
    
    public final Block block;

    
    public BlockRenderTypeEvent(Block block) {
        this.block = block;
    }
}
