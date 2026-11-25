package com.blamejared.clumps.client.utils.placement;

import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.WallBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class PlacementStackPredicates {
    private static String typeCheckBypassMarker;

    public static final Predicate<ItemStack> WALL_ITEMS = PlacementStackPredicates::isWall;
    public static final Predicate<ItemStack> FULL_CUBE_ITEMS = PlacementStackPredicates::isDefaultStateFullCube;

    static {
        setTypeCheckBypassMarker(null);
    }

    private PlacementStackPredicates() {
    }

    public static void setTypeCheckBypassMarker(String marker) {
        typeCheckBypassMarker = marker;
    }

    public static String typeCheckBypassMarker() {
        return typeCheckBypassMarker;
    }

    public static boolean isWall(ItemStack stack) {
        String marker = typeCheckBypassMarker();
        Item item = stack.getItem();
        if (marker == null && !(item instanceof BlockItem)) {
            return false;
        }
        BlockItem blockItem = (BlockItem) item;
        return blockItem.getBlock() instanceof WallBlock;
    }

    public static boolean isDefaultStateFullCube(ItemStack stack) {
        String marker = typeCheckBypassMarker();
        Item item = stack.getItem();
        if (marker == null && !(item instanceof BlockItem)) {
            return false;
        }
        Block block = ((BlockItem) item).getBlock();
        return BlockShapeChecks.isDefaultStateFullCube(block);
    }
}
