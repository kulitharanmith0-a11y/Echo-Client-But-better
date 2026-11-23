package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.utils.placement.PlacementTarget;
import java.util.function.Predicate;
import net.minecraft.item.ItemStack;

public record TrapPlacementPlan(PlacementTarget target, Predicate<ItemStack> itemFilter, int hotbarSlot, boolean ignoreInterveningBlocks) {
}
