package dev.dov.hack.echo.client.utils.targeting;

import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public final class AttackRangeProperties {
    private AttackRangeProperties() {
    }

    public static AttackRangeComponent getAttackRange(LivingEntity entity, ItemStack stack) {
        return entity.getAttackRange();
    }

    public static double getEffectiveMaxRange(PlayerEntity player) {
        return player.getAttackRange().getEffectiveMaxRange(player);
    }

    public static boolean isWithinRange(PlayerEntity player, Vec3d point) {
        if (player == null || point == null) {
            return false;
        }
        return player.getAttackRange().isWithinRange(player, point);
    }
}
