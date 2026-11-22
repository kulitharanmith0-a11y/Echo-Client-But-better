package dev.dov.hack.echo.client.utils.trajectory;

import dev.dov.hack.echo.client.MinecraftAccess;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class BlockImpactProjectileTrajectories implements MinecraftAccess {
    private static final int MAX_STEPS = 80;

    private BlockImpactProjectileTrajectories() {}

    public static List<Vec3d> fromState(Vec3d position, Vec3d velocity) {
        if (position == null || velocity == null || CLIENT.world == null) {
            return Collections.emptyList();
        }
        BlockImpactProjectileSimulation simulation = new BlockImpactProjectileSimulation(position, velocity, null);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }

    public static List<Vec3d> fromEntity(Entity entity) {
        if (entity == null || CLIENT.world == null) {
            return Collections.emptyList();
        }
        BlockImpactProjectileSimulation simulation = new BlockImpactProjectileSimulation(
                entity.getEntityPos(), entity.getVelocity(), entity);
        return TrajectorySteps.collect(MAX_STEPS, simulation::getPosition, simulation::isStopped, simulation::step);
    }
}
