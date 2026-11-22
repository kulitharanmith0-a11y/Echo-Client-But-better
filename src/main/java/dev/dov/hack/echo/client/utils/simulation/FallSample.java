package dev.dov.hack.echo.client.utils.simulation;

import net.minecraft.util.math.Vec3d;

public record FallSample(int tick, Vec3d eyePosition, boolean onGround, boolean gliding, double fallDistance) {
    public boolean isFalling() {
        return !onGround && !gliding;
    }
}
