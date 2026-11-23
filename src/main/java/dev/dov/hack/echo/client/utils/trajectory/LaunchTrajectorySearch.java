package dev.dov.hack.echo.client.utils.trajectory;

import dev.dov.hack.echo.client.MinecraftAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class LaunchTrajectorySearch implements MinecraftAccess {
    private static final int SPREAD_SAMPLES = 32;
    private static final double[][] LAUNCH_SPREAD = TrajectoryNumerics.launchSpread(SPREAD_SAMPLES, 0x5eed1L);
    private static final double[][] PROJECTILE_SPREAD = TrajectoryNumerics.launchSpread(SPREAD_SAMPLES, 0x5eed2L);
    private static final float[] COARSE_PITCH = {-3.0F, -2.0F, -1.0F, 0.0F, 1.0F, 2.0F, 3.0F};
    private static final float[] COARSE_YAW = {-1.0F, 0.0F, 1.0F};
    private static final float[] FINE_PITCH = {-0.6F, -0.3F, 0.0F, 0.3F, 0.6F};
    private static final float[] FINE_YAW = {-0.6F, -0.3F, 0.0F, 0.3F, 0.6F};
    private static final float[] ULTRA_FINE_PITCH = {-0.15F, -0.075F, 0.0F, 0.075F, 0.15F};
    private static final float[] ULTRA_FINE_YAW = {-0.15F, -0.075F, 0.0F, 0.075F, 0.15F};

    private LaunchTrajectorySearch() {
    }

    public static int flightTicks(ProjectileState state) {
        double x = state.position().x;
        double y = state.position().y;
        double z = state.position().z;
        double velocityX = state.velocity().x;
        double velocityY = state.velocity().y;
        double velocityZ = state.velocity().z;
        for (int tick = 1; tick <= 80; tick++) {
            velocityY -= 0.03;
            velocityX *= 0.99;
            velocityY *= 0.99;
            velocityZ *= 0.99;
            Vec3d start = new Vec3d(x, y, z);
            Vec3d end = new Vec3d(x + velocityX, y + velocityY, z + velocityZ);
            if (CLIENT.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, CLIENT.player)).getType() != HitResult.Type.MISS) {
                return tick;
            }
            x = end.x;
            y = end.y;
            z = end.z;
        }
        return 81;
    }

    public static LaunchCandidate findCatch(ProjectileState state, Vec3d origin, Vec3d velocity, int elapsedTicks, int flightTicks, boolean lowTrajectory) {
        int finalTick = Math.min(80, flightTicks - 1);
        double[][] path = TrajectoryNumerics.sampleProjectile(state, finalTick);
        List<AngleSeed> seeds = new ArrayList<>();
        for (int delay = Math.max(0, elapsedTicks - 1); delay <= elapsedTicks + 1; delay++) {
            collectAngles(path, origin, velocity, delay, finalTick, seeds);
        }
        List<LaunchCandidate> candidates = new ArrayList<>();
        for (AngleSeed seed : seeds) {
            LaunchCandidate candidate = refine(state, seed, origin, velocity, elapsedTicks, finalTick);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        LaunchCandidate best = null;
        for (LaunchCandidate candidate : candidates) {
            if (best == null || candidate.robustnessScore() > best.robustnessScore() + 1.0E-9) {
                best = candidate;
            }
        }
        if (best == null) {
            return null;
        }
        if (lowTrajectory) {
            for (LaunchCandidate candidate : candidates) {
                if (candidate.robustnessScore() >= best.robustnessScore() - 0.05
                        && candidate.meanHitTick() < best.meanHitTick()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    public static int interceptTick(ProjectileState state, Vec3d position, Vec3d velocity, int launchDelay, int finalTick) {
        return sampledInterceptTick(state, 0, position, velocity, launchDelay, Math.min(80, finalTick));
    }

    public static double hitFraction(ProjectileState state, Vec3d origin, Vec3d velocity, int elapsedTicks, float yaw, float pitch, int finalTick) {
        Vec3d direction = LaunchTrajectoryMath.direction(yaw, pitch);
        int hits = 0;
        for (int sample = 0; sample < SPREAD_SAMPLES; sample++) {
            if (sampledInterceptTick(state, sample, origin, launchVelocity(direction, velocity, sample), elapsedTicks, finalTick) > 0) {
                hits++;
            }
        }
        return (double) hits / (double) SPREAD_SAMPLES;
    }

    private static void collectAngles(double[][] path, Vec3d origin, Vec3d velocity, int launchDelay, int finalTick, List<AngleSeed> seeds) {
        for (int tick = launchDelay + 2; tick <= finalTick; tick++) {
            int launchAge = tick - 1 - launchDelay;
            double[] start = path[tick - 1];
            double[] end = path[tick];
            double previousResidual = Double.NaN;
            for (int division = 0; division <= 10; division++) {
                double fraction = division / 10.0;
                double residual = LaunchTrajectoryMath.reachResidual(start, end, fraction, origin, velocity, launchAge);
                if (division > 0 && (Math.signum(residual) != Math.signum(previousResidual) || Math.abs(residual) < 0.02)) {
                    double crossing = TrajectoryNumerics.bisectReach(start, end, (division - 1) / 10.0, fraction, origin, velocity, launchAge);
                    Vec3d point = new Vec3d(start[0] + (end[0] - start[0]) * crossing, start[1] + (end[1] - start[1]) * crossing, start[2] + (end[2] - start[2]) * crossing);
                    Vec3d relative = point.subtract(origin).subtract(velocity.multiply(launchAge));
                    if (!(relative.lengthSquared() > 1.0E-8)) {
                        break;
                    }
                    Vec3d direction = relative.normalize();
                    float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
                    float pitch = (float) Math.toDegrees(-Math.asin(MathHelper.clamp(direction.y, -1.0, 1.0)));
                    boolean duplicate = false;
                    for (AngleSeed existing : seeds) {
                        if (Math.abs(MathHelper.wrapDegrees(existing.yaw() - yaw)) < 1.0F
                                && Math.abs(existing.pitch() - pitch) < 1.0F) {
                            duplicate = true;
                        }
                    }
                    if (!duplicate) {
                        seeds.add(new AngleSeed(yaw, pitch, (float) tick));
                    }
                    break;
                }
                previousResidual = residual;
            }
        }
    }

    private static LaunchCandidate refine(ProjectileState state, AngleSeed seed, Vec3d origin, Vec3d velocity, int launchDelay, int finalTick) {
        int horizon = Math.min(finalTick, (int) seed.hitTick() + 10);
        LaunchCandidate coarse = null;
        for (float yawOffset : COARSE_YAW) {
            for (float pitchOffset : COARSE_PITCH) {
                coarse = better(coarse, evaluate(state, seed.yaw() + yawOffset, seed.pitch() + pitchOffset, origin, velocity, launchDelay, horizon));
            }
        }
        if (coarse == null) {
            return null;
        }
        LaunchCandidate fine = coarse;
        for (float yawOffset : FINE_YAW) {
            for (float pitchOffset : FINE_PITCH) {
                if (yawOffset == 0.0F && pitchOffset == 0.0F) {
                    continue;
                }
                fine = better(fine, evaluate(state, coarse.yaw() + yawOffset, coarse.pitch() + pitchOffset, origin, velocity, launchDelay, horizon));
            }
        }
        LaunchCandidate best = fine;
        for (float yawOffset : ULTRA_FINE_YAW) {
            for (float pitchOffset : ULTRA_FINE_PITCH) {
                if (yawOffset == 0.0F && pitchOffset == 0.0F) {
                    continue;
                }
                best = better(best, evaluate(state, fine.yaw() + yawOffset, fine.pitch() + pitchOffset, origin, velocity, launchDelay, horizon));
            }
        }
        return isPathClear(origin, velocity, best, launchDelay) ? best : null;
    }

    private static LaunchCandidate better(LaunchCandidate current, LaunchCandidate candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        if (candidate.robustnessScore() > current.robustnessScore() + 1.0E-9) {
            return candidate;
        }
        if (Math.abs(candidate.robustnessScore() - current.robustnessScore()) <= 1.0E-9
                && candidate.hitFraction() > current.hitFraction() + 1.0E-9) {
            return candidate;
        }
        return current;
    }

    private static LaunchCandidate evaluate(ProjectileState state, float yaw, float pitch, Vec3d origin, Vec3d velocity, int launchDelay, int finalTick) {
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        Vec3d direction = LaunchTrajectoryMath.direction(yaw, pitch);
        int hits = 0;
        int earlierHits = 0;
        int laterHits = 0;
        int hitTickSum = 0;
        for (int sample = 0; sample < SPREAD_SAMPLES; sample++) {
            Vec3d initialVelocity = launchVelocity(direction, velocity, sample);
            int hit = sampledInterceptTick(state, sample, origin, initialVelocity, launchDelay, finalTick);
            int earlier = sampledInterceptTick(state, sample, origin, initialVelocity, launchDelay - 1, finalTick);
            int later = sampledInterceptTick(state, sample, origin, initialVelocity, launchDelay + 1, finalTick);
            if (hit > 0) {
                hits++;
                hitTickSum += hit;
            }
            if (earlier > 0) {
                earlierHits++;
            }
            if (later > 0) {
                laterHits++;
            }
        }
        if (hits == 0) {
            return null;
        }
        double denom = SPREAD_SAMPLES;
        double fraction = hits / denom;
        double score = fraction * 1.15 + (0.45 * earlierHits + 0.65 * laterHits) / denom;
        return new LaunchCandidate(yaw, pitch, fraction, score, Math.round((float) hitTickSum / (float) hits));
    }

    private static boolean isPathClear(Vec3d origin, Vec3d inheritedVelocity, LaunchCandidate candidate, int launchDelay) {
        Vec3d velocity = LaunchTrajectoryMath.direction(candidate.yaw(), candidate.pitch()).multiply(1.5).add(inheritedVelocity);
        Vec3d end = origin.add(velocity.multiply(Math.max(1, candidate.meanHitTick() - launchDelay)));
        return CLIENT.world.raycast(new RaycastContext(origin, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, CLIENT.player)).getType() == HitResult.Type.MISS;
    }

    private static int sampledInterceptTick(ProjectileState state, int sample, Vec3d origin, Vec3d velocity, int launchDelay, int finalTick) {
        if (launchDelay < 0) {
            return -1;
        }
        double x = state.position().x;
        double y = state.position().y;
        double z = state.position().z;
        double velocityX = state.velocity().x;
        double velocityY = state.velocity().y;
        double velocityZ = state.velocity().z;
        if (!state.exactVelocity()) {
            velocityX += PROJECTILE_SPREAD[sample][0] * 1.5;
            velocityY += PROJECTILE_SPREAD[sample][1] * 1.5;
            velocityZ += PROJECTILE_SPREAD[sample][2] * 1.5;
        }
        for (int tick = 1; tick <= finalTick; tick++) {
            velocityY -= 0.03;
            velocityX *= 0.99;
            velocityY *= 0.99;
            velocityZ *= 0.99;
            double nextX = x + velocityX;
            double nextY = y + velocityY;
            double nextZ = z + velocityZ;
            if (tick > launchDelay) {
                int launchAge = tick - 1 - launchDelay;
                double centerX = origin.x + velocity.x * launchAge;
                double centerY = origin.y + velocity.y * launchAge;
                double centerZ = origin.z + velocity.z * launchAge;
                double expansion = TrajectoryTolerance.collisionExpansion(tick);
                // Phase 1: Pearl ticks first and raycasts against Wind Charge at launchAge
                if (SegmentBoxIntersection.intersects(
                        x, y, z, velocityX, velocityY, velocityZ,
                        centerX - 0.15625 - expansion, centerY - 0.15 - expansion, centerZ - 0.15625 - expansion,
                        centerX + 0.15625 + expansion, centerY - 0.15 + 0.3125 + expansion, centerZ + 0.15625 + expansion)) {
                    return tick;
                }
                // Phase 2: Wind Charge ticks next and raycasts against moved Ender Pearl (0.25x0.25x0.25 box)
                if (SegmentBoxIntersection.intersects(
                        centerX, centerY, centerZ, velocity.x, velocity.y, velocity.z,
                        nextX - 0.125 - expansion, nextY - expansion, nextZ - 0.125 - expansion,
                        nextX + 0.125 + expansion, nextY + 0.25 + expansion, nextZ + 0.125 + expansion)) {
                    return tick;
                }
            }
            x = nextX;
            y = nextY;
            z = nextZ;
        }
        return -1;
    }

    private static Vec3d launchVelocity(Vec3d direction, Vec3d inheritedVelocity, int sample) {
        return new Vec3d((direction.x + LAUNCH_SPREAD[sample][0]) * 1.5 + inheritedVelocity.x, (direction.y + LAUNCH_SPREAD[sample][1]) * 1.5 + inheritedVelocity.y, (direction.z + LAUNCH_SPREAD[sample][2]) * 1.5 + inheritedVelocity.z);
    }

    private record AngleSeed(float yaw, float pitch, float hitTick) {
    }
}
