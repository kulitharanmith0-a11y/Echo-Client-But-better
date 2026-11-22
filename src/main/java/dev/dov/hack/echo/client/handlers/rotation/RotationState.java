package dev.dov.hack.echo.client.handlers.rotation;

import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.utils.Rotation;
import net.minecraft.entity.LivingEntity;

public record RotationState(Rotation playerRotation, Rotation currentRotation, Rotation previousRotation, boolean active, Module owner, LivingEntity target, AimState state) {
}
