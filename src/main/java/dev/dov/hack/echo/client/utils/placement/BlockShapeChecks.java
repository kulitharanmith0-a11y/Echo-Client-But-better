package dev.dov.hack.echo.client.utils.placement;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

public final class BlockShapeChecks {
    private BlockShapeChecks() {
    }

    public static boolean isDefaultStateFullCube(Block block) {
        return block.getDefaultState().isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
    }
}
