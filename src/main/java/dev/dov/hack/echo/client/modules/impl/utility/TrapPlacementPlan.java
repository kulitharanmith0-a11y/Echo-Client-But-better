package dev.dov.hack.echo.client.modules.impl.utility;

import dev.dov.hack.echo.client.utils.placement.PlacementTarget;
import java.util.function.Predicate;
import net.minecraft.item.ItemStack;

public record TrapPlacementPlan(PlacementTarget target, Predicate<ItemStack> itemFilter, int hotbarSlot, boolean ignoreInterveningBlocks) {
}
