package com.blamejared.clumps.client.utils.targeting;

import java.util.Collection;
import java.util.List;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.RaycastContext;

public final class KineticCollisionQueries {
    private KineticCollisionQueries() {
    }

    public static Collection<EntityHitResult> collectHits(LivingEntity attacker, AttackRangeComponent range) {
        return ProjectileUtil.collectPiercingCollisions(attacker, range, target -> PiercingWeaponComponent.canHit(attacker, target), RaycastContext.ShapeType.COLLIDER).map(blockHit -> List.of(), entityHits -> entityHits);
    }
}
