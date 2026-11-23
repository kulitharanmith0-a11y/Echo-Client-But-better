package com.blamejared.clumps.client.utils.trajectory;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

public final class ProjectileOwnerBounds {
    private ProjectileOwnerBounds() {
    }

    public static boolean intersects(Box sweptBounds, Entity entity) {
        return sweptBounds.intersects(entity.getBoundingBox());
    }
}
