package dev.dov.hack.echo.client.utils.targeting;

import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public final class AttackQueries {
    private static final EntityQueryCache contactCache = new EntityQueryCache();
    private static int previousElapsedUseTicks = Integer.MAX_VALUE;

    private AttackQueries() {
    }

    
    public static List<AttackEvaluation> findKineticWeaponTargets(LivingEntity user, ItemStack stack, int remainingUseTicks) {
        int elapsedUseTicks = stack.getMaxUseTime(user) - remainingUseTicks;
        if (elapsedUseTicks <= previousElapsedUseTicks) {
            contactCache.clear();
        }
        previousElapsedUseTicks = elapsedUseTicks;
        return KineticAttackEvaluation.predict(user, stack, remainingUseTicks, contactCache);
    }
}
