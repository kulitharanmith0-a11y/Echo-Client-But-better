package com.blamejared.clumps.client.modules.impl.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

public record PiercingTarget(LivingEntity entity, Vec3d hitPosition) {
    
    public EntityHitResult toHitResult() {
        return new EntityHitResult(entity, hitPosition);
    }
}
