package com.blamejared.clumps.client.utils.placement;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public record PlacementTarget(BlockPos position, BlockHitResult hitResult) {
}
