package dev.dov.hack.echo.client.utils.trajectory;

import net.minecraft.util.math.Vec3d;

public record ProjectileState(Vec3d position, Vec3d velocity, boolean exactVelocity) {
}
