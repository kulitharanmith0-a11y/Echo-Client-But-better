package dev.dov.hack.echo.client.handlers;

import net.minecraft.util.math.Vec3d;

public record PositionSample(int tick, Vec3d position, boolean onGround) {
}
