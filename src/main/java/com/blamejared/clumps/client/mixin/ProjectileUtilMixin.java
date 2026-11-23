package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.world.EntityHitboxEvent;
import com.blamejared.clumps.client.modules.impl.utility.AntiTrap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {
    @Redirect(method = "raycast", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;"))
    private static Box redirectRaycastAtGetBoundingBox(Entity entity) {
        Box targetingBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
        if (ClumpsClient.eventsDisabled) {
            return targetingBox;
        }
        if (AntiTrap.shouldIgnoreEntity(entity)) {
            double excludedY = entity.getY() - 1024.0;
            return new Box(entity.getX(), excludedY, entity.getZ(), entity.getX(), excludedY, entity.getZ());
        }
        EntityHitboxEvent event = new EntityHitboxEvent(entity, targetingBox);
        event.post();
        return event.getBoundingBox();
    }
}
