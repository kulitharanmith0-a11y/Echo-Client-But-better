package com.blamejared.clumps.client.handlers.rotation;

import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.utils.Rotation;
import net.minecraft.entity.LivingEntity;

public record RotationState(Rotation playerRotation, Rotation currentRotation, Rotation previousRotation, boolean active, Module owner, LivingEntity target, AimState state) {
}
