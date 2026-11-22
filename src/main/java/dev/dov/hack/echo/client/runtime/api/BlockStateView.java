package dev.dov.hack.echo.client.runtime.api;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;

public class BlockStateView {
    private final BlockState state;

    public BlockStateView(BlockState state) {
        this.state = state;
    }

    public boolean isAir() {
        return state.isAir();
    }

    public String getId() {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }
}
