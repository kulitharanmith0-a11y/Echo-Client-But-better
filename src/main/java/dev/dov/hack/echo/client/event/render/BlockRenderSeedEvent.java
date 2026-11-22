package dev.dov.hack.echo.client.event.render;

import dev.dov.hack.echo.client.event.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BlockRenderSeedEvent extends Event {
    
    public final BlockState state;
    
    public final BlockPos position;

    
    public BlockRenderSeedEvent(BlockState state, BlockPos position) {
        this.state = state;
        this.position = position;
    }
}
