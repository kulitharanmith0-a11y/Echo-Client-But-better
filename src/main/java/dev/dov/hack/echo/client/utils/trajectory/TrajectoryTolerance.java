package dev.dov.hack.echo.client.utils.trajectory;

import net.minecraft.util.math.MathHelper;

public final class TrajectoryTolerance {
    private TrajectoryTolerance() {
    }

    public static double collisionExpansion(int ticks) {
        return MathHelper.clamp((ticks - 2) / 20.0, 0.0, 0.3);
    }
}
