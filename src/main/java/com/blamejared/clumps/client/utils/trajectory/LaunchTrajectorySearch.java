package com.blamejared.clumps.client.utils.trajectory;

import com.blamejared.clumps.client.MinecraftAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class LaunchTrajectorySearch implements MinecraftAccess {
    private static final int SPREAD_SAMPLES = 32;
    private static final int COARSE_SAMPLES = 8;
    private static final int FINE_SAMPLES = 16;
    private static final int MAX_REFINED_SEEDS = 3;
    private static final double[][] LAUNCH_SPREAD = TrajectoryNumerics.launchSpread(SPREAD_SAMPLES, 0x5eed1L);
    private static final double[][] PROJECTILE_SPREAD = TrajectoryNumerics.launchSpread(SPREAD_SAMPLES, 0x5eed2L);
    private static final float[] COARSE_PITCH = {-2.0F, -1.0F, 0.0F, 1.0F, 2.0F};
    private static final float[] COARSE_YAW = {-2.0F, -1.0F, 0.0F, 1.0F, 2.0F};
    private static final float[] FINE_PITCH = {-0.45F, 0.0F, 0.45F};
    private static final float[] FINE_YAW = {-0.45F, 0.0F, 0.45F};
    private static final float[] ULTRA_FINE_PITCH = {-0.15F, 0.0F, 0.15F};
    private static final float[] ULTRA_FINE_YAW = {-0.15F, 0.0F, 0.15F};

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
            if (CLIENT.world != null && CLIENT.player != null
                    && CLIENT.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, CLIENT.player)).getType() != HitResult.Type.MISS) {
                return tick;
            }
            x = end.x;
            y = end.y;
            z = end.z;
        }
        return 81;
    }

    public static LaunchCandidate findCatch(ProjectileState state, Vec3d origin, Vec3d velocity, int elapsedTicks, int flightTicks, boolean lowTrajectory) {
        return findCatch(state, origin, velocity, elapsedTicks, flightTicks, lowTrajectory, true);
    }

    public static LaunchCandidate findCatch(ProjectileState state, Vec3d origin, Vec3d velocity, int elapsedTicks, int flightTicks, boolean lowTrajectory, boolean checkWorldPath) {
        int finalTick = Math.min(80, Math.max(2, flightTicks - 1));
        double[][][] pearlPaths = precomputePearlPaths(state, finalTick);
        double[][] nominalPath = pearlPaths[0];
        List<AngleSeed> seeds = new ArrayList<>();
        collectAngles(nominalPath, origin, velocity, Math.max(0, elapsedTicks), finalTick, seeds);
        if (elapsedTicks > 0) {
            collectAngles(nominalPath, origin, velocity, elapsedTicks - 1, finalTick, seeds);
        }
        collectAngles(nominalPath, origin, velocity, elapsedTicks + 1, finalTick, seeds);
        if (seeds.isEmpty()) {
            return null;
        }
        // Rank analytic seeds cheaply before running full multi-grid refinement.
        List<ScoredSeed> ranked = new ArrayList<>(seeds.size());
        for (AngleSeed seed : seeds) {
            int minTick = Math.max(elapsedTicks + 1, (int) seed.hitTick() - 6);
            int maxTick = Math.min(finalTick, (int) seed.hitTick() + 8);
            LaunchCandidate quick = evaluate(pearlPaths, seed.yaw(), seed.pitch(), origin, velocity, elapsedTicks, minTick, maxTick, COARSE_SAMPLES, false);
            double quickScore = quick != null ? quick.robustnessScore() : 0.0;
            ranked.add(new ScoredSeed(seed, quickScore));
        }
        ranked.sort(Comparator.comparingDouble(ScoredSeed::score).reversed()
                .thenComparingDouble(s -> s.seed().hitTick()));
        int limit = Math.min(MAX_REFINED_SEEDS, ranked.size());
        List<LaunchCandidate> candidates = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            LaunchCandidate candidate = refine(pearlPaths, ranked.get(i).seed(), origin, velocity, elapsedTicks, finalTick, checkWorldPath);
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
                if (candidate.robustnessScore() >= best.robustnessScore() - 0.08
                        && candidate.hitFraction() >= best.hitFraction() - 0.05
                        && candidate.meanHitTick() < best.meanHitTick()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    public static int interceptTick(ProjectileState state, Vec3d position, Vec3d velocity, int launchDelay, int finalTick) {
        int clampedFinal = Math.min(80, Math.max(1, finalTick));
        double[][] nominalPath = TrajectoryNumerics.sampleProjectile(state, clampedFinal);
        return pathInterceptTick(nominalPath, position, velocity, launchDelay, clampedFinal);
    }

    public static double hitFraction(ProjectileState state, Vec3d origin, Vec3d velocity, int elapsedTicks, float yaw, float pitch, int finalTick) {
        int clampedFinal = Math.min(80, Math.max(1, finalTick));
        double[][][] pearlPaths = precomputePearlPaths(state, clampedFinal);
        Vec3d direction = LaunchTrajectoryMath.direction(yaw, pitch);
        int hits = 0;
        for (int sample = 0; sample < SPREAD_SAMPLES; sample++) {
            double[][] path = pearlPaths.length == 1 ? pearlPaths[0] : pearlPaths[sample];
            if (pathInterceptTick(path, origin, launchVelocity(direction, velocity, sample), elapsedTicks, clampedFinal) > 0) {
                hits++;
            }
        }
        return (double) hits / (double) SPREAD_SAMPLES;
    }

    private static double[][][] precomputePearlPaths(ProjectileState state, int finalTick) {
        if (state.exactVelocity()) {
            return new double[][][] {TrajectoryNumerics.sampleProjectile(state, finalTick)};
        }
        double[][][] paths = new double[SPREAD_SAMPLES][][];
        for (int sample = 0; sample < SPREAD_SAMPLES; sample++) {
            Vec3d sampledVel = sample == 0
                    ? state.velocity()
                    : new Vec3d(
                            state.velocity().x + PROJECTILE_SPREAD[sample][0] * 1.5,
                            state.velocity().y + PROJECTILE_SPREAD[sample][1] * 1.5,
                            state.velocity().z + PROJECTILE_SPREAD[sample][2] * 1.5);
            paths[sample] = TrajectoryNumerics.sampleProjectile(new ProjectileState(state.position(), sampledVel, true), finalTick);
        }
        return paths;
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
                    Vec3d point = new Vec3d(
                            start[0] + (end[0] - start[0]) * crossing,
                            start[1] + (end[1] - start[1]) * crossing,
                            start[2] + (end[2] - start[2]) * crossing);
                    Vec3d relative = point.subtract(origin).subtract(velocity.multiply(launchAge));
                    if (!(relative.lengthSquared() > 1.0E-8)) {
                        break;
                    }
                    Vec3d direction = relative.normalize();
                    float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
                    float pitch = (float) Math.toDegrees(-Math.asin(MathHelper.clamp(direction.y, -1.0, 1.0)));
                    boolean duplicate = false;
                    for (AngleSeed existing : seeds) {
                        if (Math.abs(MathHelper.wrapDegrees(existing.yaw() - yaw)) < 1.25F
                                && Math.abs(existing.pitch() - pitch) < 1.25F) {
                            duplicate = true;
                            break;
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

    private static LaunchCandidate refine(double[][][] pearlPaths, AngleSeed seed, Vec3d origin, Vec3d velocity, int launchDelay, int finalTick, boolean checkWorldPath) {
        int minTick = Math.max(launchDelay + 1, (int) seed.hitTick() - 6);
        int horizon = Math.min(finalTick, (int) seed.hitTick() + 8);
        LaunchCandidate coarse = null;
        for (float yawOffset : COARSE_YAW) {
            for (float pitchOffset : COARSE_PITCH) {
                coarse = better(coarse, evaluate(pearlPaths, seed.yaw() + yawOffset, seed.pitch() + pitchOffset, origin, velocity, launchDelay, minTick, horizon, COARSE_SAMPLES, false));
            }
        }
        if (coarse == null) {
            return null;
        }
        LaunchCandidate fine = evaluate(pearlPaths, coarse.yaw(), coarse.pitch(), origin, velocity, launchDelay, minTick, horizon, FINE_SAMPLES, true);
        for (float yawOffset : FINE_YAW) {
            for (float pitchOffset : FINE_PITCH) {
                if (yawOffset == 0.0F && pitchOffset == 0.0F) {
                    continue;
                }
                fine = better(fine, evaluate(pearlPaths, coarse.yaw() + yawOffset, coarse.pitch() + pitchOffset, origin, velocity, launchDelay, minTick, horizon, FINE_SAMPLES, true));
            }
        }
        if (fine == null) {
            return null;
        }
        LaunchCandidate best = evaluate(pearlPaths, fine.yaw(), fine.pitch(), origin, velocity, launchDelay, minTick, horizon, SPREAD_SAMPLES, true);
        for (float yawOffset : ULTRA_FINE_YAW) {
            for (float pitchOffset : ULTRA_FINE_PITCH) {
                if (yawOffset == 0.0F && pitchOffset == 0.0F) {
                    continue;
                }
                best = better(best, evaluate(pearlPaths, fine.yaw() + yawOffset, fine.pitch() + pitchOffset, origin, velocity, launchDelay, minTick, horizon, SPREAD_SAMPLES, true));
            }
        }
        if (best == null) {
            return null;
        }
        return (!checkWorldPath || isPathClear(origin, velocity, best, launchDelay)) ? best : null;
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

    private static LaunchCandidate evaluate(double[][][] pearlPaths, float yaw, float pitch, Vec3d origin, Vec3d velocity, int launchDelay, int minTick, int finalTick, int sampleCount, boolean includeJitter) {
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        Vec3d direction = LaunchTrajectoryMath.direction(yaw, pitch);
        double dx0 = direction.x;
        double dy0 = direction.y;
        double dz0 = direction.z;
        double ox = origin.x;
        double oy = origin.y;
        double oz = origin.z;
        double ivx = velocity.x;
        double ivy = velocity.y;
        double ivz = velocity.z;
        int hits = 0;
        int earlierHits = 0;
        int laterHits = 0;
        int hitTickSum = 0;
        boolean exact = pearlPaths.length == 1;
        for (int sample = 0; sample < sampleCount; sample++) {
            double[][] path = exact ? pearlPaths[0] : pearlPaths[sample];
            double[] spread = LAUNCH_SPREAD[sample];
            double vx = (dx0 + spread[0]) * 1.5 + ivx;
            double vy = (dy0 + spread[1]) * 1.5 + ivy;
            double vz = (dz0 + spread[2]) * 1.5 + ivz;
            int hit = pathInterceptTickRange(path, ox, oy, oz, vx, vy, vz, launchDelay, minTick, finalTick);
            if (hit > 0) {
                hits++;
                hitTickSum += hit;
            }
            if (includeJitter) {
                if (pathInterceptTickRange(path, ox, oy, oz, vx, vy, vz, launchDelay - 1, Math.max(1, minTick - 1), finalTick) > 0) {
                    earlierHits++;
                }
                if (pathInterceptTickRange(path, ox, oy, oz, vx, vy, vz, launchDelay + 1, minTick, finalTick) > 0) {
                    laterHits++;
                }
            }
        }
        if (hits == 0) {
            return null;
        }
        double denom = sampleCount;
        double fraction = hits / denom;
        // Prioritize true on-tick pearl-to-wind-charge intercept probability first,
        // with +-1 tick server jitter tolerance as secondary robustness tie-breaker.
        double score = fraction * 2.0 + (includeJitter ? (0.35 * earlierHits + 0.45 * laterHits) / denom : 0.0);
        return new LaunchCandidate(yaw, pitch, fraction, score, Math.round((float) hitTickSum / (float) hits));
    }

    private static boolean isPathClear(Vec3d origin, Vec3d inheritedVelocity, LaunchCandidate candidate, int launchDelay) {
        if (CLIENT.world == null || CLIENT.player == null) {
            return true;
        }
        Vec3d velocity = LaunchTrajectoryMath.direction(candidate.yaw(), candidate.pitch()).multiply(1.5).add(inheritedVelocity);
        Vec3d end = origin.add(velocity.multiply(Math.max(1, candidate.meanHitTick() - launchDelay)));
        return CLIENT.world.raycast(new RaycastContext(origin, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, CLIENT.player)).getType() == HitResult.Type.MISS;
    }

    private static int pathInterceptTick(double[][] pearlPath, Vec3d origin, Vec3d velocity, int launchDelay, int finalTick) {
        return pathInterceptTickRange(pearlPath, origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z, launchDelay, launchDelay + 1, finalTick);
    }

    private static int pathInterceptTickRange(double[][] pearlPath, double ox, double oy, double oz, double vx, double vy, double vz, int launchDelay, int startTick, int finalTick) {
        if (launchDelay < 0) {
            return -1;
        }
        int first = Math.max(launchDelay + 1, startTick);
        int limit = Math.min(finalTick, pearlPath.length - 1);
        for (int tick = first; tick <= limit; tick++) {
            double[] prev = pearlPath[tick - 1];
            double[] next = pearlPath[tick];
            double dx = next[0] - prev[0];
            double dy = next[1] - prev[1];
            double dz = next[2] - prev[2];
            int launchAge = tick - 1 - launchDelay;
            double centerX = ox + vx * launchAge;
            double centerY = oy + vy * launchAge;
            double centerZ = oz + vz * launchAge;
            // Pearl age during tick's ProjectileUtil.getCollision is (tick - 1), before super.tick() increments age.
            double expansion = TrajectoryTolerance.collisionExpansion(tick - 1);
            // In vanilla MC 1.21.11, only the Ender Pearl raycasts into the Wind Charge's hitbox
            // (ExplosiveProjectileEntity.canHit() == true; EnderPearlEntity.canHit() == false).
            if (SegmentBoxIntersection.intersects(
                    prev[0], prev[1], prev[2], dx, dy, dz,
                    centerX - 0.15625 - expansion, centerY - 0.15 - expansion, centerZ - 0.15625 - expansion,
                    centerX + 0.15625 + expansion, centerY - 0.15 + 0.3125 + expansion, centerZ + 0.15625 + expansion)) {
                return tick;
            }
        }
        return -1;
    }

    private static Vec3d launchVelocity(Vec3d direction, Vec3d inheritedVelocity, int sample) {
        return new Vec3d(
                (direction.x + LAUNCH_SPREAD[sample][0]) * 1.5 + inheritedVelocity.x,
                (direction.y + LAUNCH_SPREAD[sample][1]) * 1.5 + inheritedVelocity.y,
                (direction.z + LAUNCH_SPREAD[sample][2]) * 1.5 + inheritedVelocity.z);
    }

    private record AngleSeed(float yaw, float pitch, float hitTick) {
    }

    private record ScoredSeed(AngleSeed seed, double score) {
    }
}
