package dev.dov.hack.echo.client.utils.trajectory;

import dev.dov.hack.echo.client.utils.movement.PlayerEyePositions;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class FluidAwareProjectileTrajectories {
    private static final double LAUNCH_SPEED = 1.5;

    private FluidAwareProjectileTrajectories() {}

    public static List<Vec3d> fromPlayer(PlayerEntity player, float tickDelta) {
        if (player == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        Vec3d position = PlayerEyePositions.loweredEyePosition(player);
        Vec3d velocity = PlayerProjectileLaunch.direction(player, 0.0F).multiply(LAUNCH_SPEED)
                .add(PlayerProjectileLaunch.inheritedMotion(player));
        FluidAwareProjectileSimulation simulation = new FluidAwareProjectileSimulation(position, velocity, player);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3d> fromVectors(Vec3d position, Vec3d velocity) {
        if (position == null || velocity == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        FluidAwareProjectileSimulation simulation = new FluidAwareProjectileSimulation(position, velocity, null);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3d> fromEntity(Entity entity) {
        if (entity == null || MinecraftClient.getInstance().world == null) {
            return Collections.emptyList();
        }
        FluidAwareProjectileSimulation simulation = new FluidAwareProjectileSimulation(
                entity.getEntityPos(), entity.getVelocity(), entity);
        return TrajectorySteps.collect(80, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
