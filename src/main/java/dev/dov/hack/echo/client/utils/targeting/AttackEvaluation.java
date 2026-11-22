package dev.dov.hack.echo.client.utils.targeting;

import net.minecraft.entity.Entity;

public record AttackEvaluation(Entity target, boolean damage, boolean knockback, boolean dismount, float damageAmount, double attackerSpeed, double approachSpeed, boolean onCooldown) {
    public boolean isAvailable() {
        return !onCooldown;
    }
}
