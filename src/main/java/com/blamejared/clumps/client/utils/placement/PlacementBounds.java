package com.blamejared.clumps.client.utils.placement;

import java.util.function.Predicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public record PlacementBounds(int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
    public static PlacementBounds around(LivingEntity entity, Vec3d offset, Predicate<BlockPos> replaceable) {
        Box box = entity.getBoundingBox().offset(offset).contract(1.0E-4);
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);
        int minY = supportLevel(minX, maxX, minZ, maxZ, MathHelper.floor(box.minY), replaceable);
        return new PlacementBounds(minX, maxX, minZ, maxZ, minY, MathHelper.floor(minY + box.getLengthY()));
    }

    public static PlacementBounds atCenter(LivingEntity entity, Vec3d offset, Predicate<BlockPos> replaceable) {
        Box box = entity.getBoundingBox().offset(offset).contract(1.0E-4);
        BlockPos center = BlockPos.ofFloored(entity.getEntityPos().add(offset));
        int minY = supportLevel(center.getX(), center.getX(), center.getZ(), center.getZ(), center.getY(), replaceable);
        return new PlacementBounds(center.getX(), center.getX(), center.getZ(), center.getZ(), minY, MathHelper.floor(minY + box.getLengthY()));
    }

    private static int supportLevel(int minX, int maxX, int minZ, int maxZ, int startingY, Predicate<BlockPos> replaceable) {
        for (int y = startingY; y > startingY - 4; y--) {
            for (BlockPos below : BlockPos.iterate(minX, y - 1, minZ, maxX, y - 1, maxZ)) {
                if (!replaceable.test(below)) {
                    return y;
                }
            }
        }
        return startingY;
    }

    public int aboveTop() {
        return maxY + 1;
    }
}
