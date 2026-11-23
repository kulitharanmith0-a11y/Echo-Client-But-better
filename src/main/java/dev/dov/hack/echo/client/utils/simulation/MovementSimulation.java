package dev.dov.hack.echo.client.utils.simulation;

import dev.dov.hack.echo.client.MinecraftAccess;
import dev.dov.hack.echo.client.event.world.EntityHitboxEvent;
import dev.dov.hack.echo.client.mixin.accessor.EntityAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class MovementSimulation implements MinecraftAccess {
    private static ClientPlayerEntity sampledPlayer;
    private static long sampledAge = Long.MIN_VALUE;
    private static final Map<Integer, List<FallSample>> samplesByGlideTicks = new HashMap<>();

    private MovementSimulation() {
    }

    public static double attackRange() {
        return CLIENT.player == null ? 3.0 : CLIENT.player.getEntityInteractionRange();
    }

    public static List<FallSample> fallSamples(int glideTicks) {
        ClientPlayerEntity player = CLIENT.player;
        if (player == null || CLIENT.world == null) {
            return List.of();
        }
        if (player != sampledPlayer || player.age != sampledAge) {
            samplesByGlideTicks.clear();
            sampledPlayer = player;
            sampledAge = player.age;
        }
        return samplesByGlideTicks.computeIfAbsent(Math.max(0, glideTicks), ticks -> sampleFall(player, ticks));
    }

    private static List<FallSample> sampleFall(ClientPlayerEntity player, int glideTicks) {
        List<FallSample> samples = new ArrayList<>(21);
        samples.add(new FallSample(0, player.getEyePos(), player.isOnGround(), player.isGliding(), player.fallDistance));
        if (player.isOnGround() || CLIENT.world == null) {
            return samples;
        }
        SimulatedPlayer simulation = PlayerPrediction.forWorld(CLIENT.world);
        try {
            simulation.copyFrom(player, player.getVelocity(), player.getYaw(), player.getPitch(), player.forwardSpeed, player.sidewaysSpeed, player.input.playerInput.jump());
            double fallDistance = player.fallDistance;
            double previousY = player.getY();
            for (int tick = 1; tick <= 20; tick++) {
                if (simulation.isGliding()) {
                    if (simulation.getVelocity().y > -0.5 && fallDistance > 1.0) {
                        fallDistance = 1.0;
                    }
                    if (tick > glideTicks) {
                        ((EntityAccessor) simulation).invokeSetFlag(7, false);
                        simulation.setPose(EntityPose.STANDING);
                        simulation.calculateDimensions();
                    }
                }
                simulation.tickPrediction();
                double currentY = simulation.getY();
                if (!simulation.isTouchingWater() && currentY < previousY) {
                    fallDistance += previousY - currentY;
                }
                previousY = currentY;
                boolean onGround = simulation.isOnGround();
                samples.add(new FallSample(tick, simulation.getEyePos(), onGround, simulation.isGliding(), onGround ? 0.0 : fallDistance));
                if (onGround) {
                    break;
                }
            }
        } catch (Throwable failure) {
            return List.of(samples.get(0));
        }
        return samples;
    }

    public static Box sampleBounds(List<FallSample> samples) {
        Box bounds = new Box(samples.get(0).eyePosition(), samples.get(0).eyePosition());
        for (FallSample sample : samples) {
            bounds = bounds.union(new Box(sample.eyePosition(), sample.eyePosition()));
        }
        return bounds;
    }

    public static boolean isHorizontallyReachable(List<FallSample> samples, LivingEntity target, double distance) {
        CollisionPrediction prediction = collisionPrediction(target);
        double distanceSquared = distance * distance;
        for (FallSample sample : samples) {
            Box bounds = prediction.predictClamped(sample.tick());
            double nearestX = Math.max(bounds.minX, Math.min(sample.eyePosition().x, bounds.maxX));
            double nearestZ = Math.max(bounds.minZ, Math.min(sample.eyePosition().z, bounds.maxZ));
            double x = sample.eyePosition().x - nearestX;
            double z = sample.eyePosition().z - nearestZ;
            if (x * x + z * z <= distanceSquared) {
                return true;
            }
        }
        return false;
    }

    public static int firstImpactTick(List<FallSample> samples, LivingEntity target, double minimumFallDistance) {
        CollisionPrediction prediction = collisionPrediction(target);
        FallSample previous = null;
        for (FallSample sample : samples) {
            if (sample.isFalling() && sample.fallDistance() > minimumFallDistance
                    && withinAttackRange(sample, previous, prediction)) {
                return sample.tick();
            }
            // High fall speed tunneling guard: if this tick landed on the ground after falling fast,
            // check if the swept path from the previous airborne tick passed through attack range.
            if (sample.onGround() && previous != null && previous.isFalling()) {
                double stepDrop = Math.max(0.0, previous.eyePosition().y - sample.eyePosition().y);
                if (stepDrop > 0.85 && previous.fallDistance() + stepDrop * 0.5 > minimumFallDistance
                        && sweptWithinAttackRange(previous, sample, prediction, attackRange() * 0.96)) {
                    return previous.tick();
                }
            }
            previous = sample;
        }
        return -1;
    }

    public static int ticksUntilGround(List<FallSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return -1;
        }
        for (FallSample sample : samples) {
            if (sample.onGround()) {
                return sample.tick();
            }
        }
        return -1;
    }

    private static boolean withinAttackRange(FallSample sample, FallSample previous, CollisionPrediction prediction) {
        double range = attackRange() * (sample.tick() == 0 ? 1.0 : 0.96);
        if (prediction.predictClamped(sample.tick()).squaredMagnitude(sample.eyePosition()) <= range * range) {
            return true;
        }
        if (previous != null && previous.eyePosition().squaredDistanceTo(sample.eyePosition()) > 1.0) {
            return sweptWithinAttackRange(previous, sample, prediction, range);
        }
        return false;
    }

    private static boolean sweptWithinAttackRange(FallSample from, FallSample to, CollisionPrediction prediction, double range) {
        double rangeSq = range * range;
        Box fromBox = prediction.predictClamped(from.tick());
        Box toBox = prediction.predictClamped(to.tick());
        for (int step = 1; step <= 3; step++) {
            double t = step * 0.25;
            Vec3d eye = from.eyePosition().lerp(to.eyePosition(), t);
            Box box = new Box(
                    fromBox.minX + (toBox.minX - fromBox.minX) * t,
                    fromBox.minY + (toBox.minY - fromBox.minY) * t,
                    fromBox.minZ + (toBox.minZ - fromBox.minZ) * t,
                    fromBox.maxX + (toBox.maxX - fromBox.maxX) * t,
                    fromBox.maxY + (toBox.maxY - fromBox.maxY) * t,
                    fromBox.maxZ + (toBox.maxZ - fromBox.maxZ) * t);
            if (box.squaredMagnitude(eye) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    public static boolean canHitWithinWindow(List<FallSample> samples, LivingEntity target, double minimumFallDistance, double horizontalDistance, TickWindow window) {
        CollisionPrediction prediction = collisionPrediction(target);
        return canHitWithinWindow(samples, prediction, minimumFallDistance, horizontalDistance, attackRange(), window);
    }

    private static boolean canHitWithinWindow(List<FallSample> samples, CollisionPrediction prediction, double minimumFallDistance, double horizontalDistance, double attackRange, TickWindow window) {
        for (int i = 0; i < samples.size(); i++) {
            FallSample first = samples.get(i);
            FallSample prevFirst = i > 0 ? samples.get(i - 1) : null;
            if (first.tick() < window.firstTick() || first.tick() + window.pairedTickOffset() > window.lastTick()) {
                continue;
            }
            if (!canHit(first, prevFirst, prediction, minimumFallDistance, horizontalDistance, attackRange)) {
                continue;
            }
            if (window.pairedTickOffset() == 0) {
                return true;
            }
            for (int j = i; j < samples.size(); j++) {
                FallSample paired = samples.get(j);
                FallSample prevPaired = j > 0 ? samples.get(j - 1) : null;
                if (paired.tick() == first.tick() + window.pairedTickOffset()
                        && canHit(paired, prevPaired, prediction, minimumFallDistance, horizontalDistance, attackRange)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean canHit(FallSample sample, FallSample previous, CollisionPrediction prediction, double minimumFallDistance, double horizontalDistance, double attackRange) {
        if (!sample.isFalling() || sample.fallDistance() <= minimumFallDistance) {
            return false;
        }
        Box bounds = prediction.predict(sample.tick());
        double range = attackRange * 0.96;
        boolean inReach = bounds.squaredMagnitude(sample.eyePosition()) <= range * range
                || (previous != null && previous.eyePosition().squaredDistanceTo(sample.eyePosition()) > 1.0
                && sweptWithinAttackRange(previous, sample, prediction, range));
        if (!inReach) {
            return false;
        }
        double x = sample.eyePosition().x - Math.max(bounds.minX, Math.min(sample.eyePosition().x, bounds.maxX));
        double z = sample.eyePosition().z - Math.max(bounds.minZ, Math.min(sample.eyePosition().z, bounds.maxZ));
        return x * x + z * z <= horizontalDistance * horizontalDistance;
    }

    public static Vec3d predictedAimPoint(LivingEntity target, int ticks) {
        Box bounds = collisionPrediction(target).predictClamped(Math.max(0, ticks));
        return new Vec3d((bounds.minX + bounds.maxX) * 0.5, bounds.minY + (bounds.maxY - bounds.minY) * 0.75, (bounds.minZ + bounds.maxZ) * 0.5);
    }

    private static CollisionPrediction collisionPrediction(LivingEntity target) {
        EntityHitboxEvent event = new EntityHitboxEvent(target, target.getBoundingBox().expand(target.getTargetingMargin()));
        event.post();
        Vec3d measuredVelocity = target.getEntityPos().subtract(target.lastX, target.lastY, target.lastZ);
        PositionInterpolator interpolator = target.getInterpolator();
        Vec3d interpolationDisplacement = Vec3d.ZERO;
        if (interpolator != null && interpolator.isInterpolating()) {
            interpolationDisplacement = interpolator.getLerpedPos().subtract(target.getEntityPos());
        }
        return CollisionPrediction.create(event.getBoundingBox(), measuredVelocity, target.getVelocity(), interpolationDisplacement, target.isOnGround());
    }
}
