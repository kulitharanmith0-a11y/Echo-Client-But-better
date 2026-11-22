package dev.dov.hack.echo.client.utils.placement;

import java.util.function.Predicate;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public record PlacementFilter(BlockPos position, Predicate<ItemStack> acceptsStack) {
}
