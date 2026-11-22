package dev.dov.hack.echo.client.event.world;

import dev.dov.hack.echo.client.event.Event;
import java.lang.invoke.MethodHandles;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BlockUpdateEvent extends Event {
    private final BlockPos position;
    private final BlockState state;

    public BlockUpdateEvent(BlockPos position, BlockState state) {
        super();
        this.position = position;
        this.state = state;
    }

    public BlockPos getPosition() {
        initializePayloadClass(BlockPos.class);
        return position;
    }

    public BlockState getState() {
        initializePayloadClass(BlockState.class);
        return state;
    }

    
    private static void initializePayloadClass(Class<?> type) {
        try {
            MethodHandles.lookup().ensureInitialized(type);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Minecraft payload class is not accessible", failure);
        }
    }
}
