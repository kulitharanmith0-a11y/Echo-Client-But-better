package com.blamejared.clumps.client.modules.impl.combat;

import net.minecraft.entity.LivingEntity;

public record MaceImpactPrediction(LivingEntity target, int impactTick) {
}
