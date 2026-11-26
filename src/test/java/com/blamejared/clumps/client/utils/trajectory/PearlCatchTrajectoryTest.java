package com.blamejared.clumps.client.utils.trajectory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

class PearlCatchTrajectoryTest {
    @Test
    void solverAchievesHighCatchAccuracyAcrossAnglesAndStaysFast() {
        float[][] scenarios = {
                {0.0F, -89.0F, 1.0F},
                {35.0F, -60.0F, 2.0F},
                {-120.0F, -45.0F, 2.0F},
                {70.0F, -30.0F, 3.0F},
                {15.0F, -20.0F, 1.0F}
        };
        Vec3d[] movements = {
                Vec3d.ZERO,
                new Vec3d(0.22, 0.0, 0.05),
                new Vec3d(-0.18, 0.12, 0.14),
                new Vec3d(0.10, 0.0, -0.20),
                new Vec3d(0.0, 0.0, 0.25)
        };
        Random rng = new Random(1337L);
        int totalTrials = 0;
        int totalHits = 0;
        long maxSolveNanos = 0L;

        // Warm up classloading and JIT before measuring solve latency
        ProjectileState warmupState = new ProjectileState(
                new Vec3d(0.0, 1.52, 0.0),
                LaunchTrajectoryMath.direction(0.0F, -45.0F).multiply(1.5),
                true);
        LaunchTrajectorySearch.findCatch(warmupState, new Vec3d(0.0, 1.62, 0.0), Vec3d.ZERO, 1, 60, false, false);

        for (int s = 0; s < scenarios.length; s++) {
            float throwYaw = scenarios[s][0];
            float throwPitch = scenarios[s][1];
            int delay = (int) scenarios[s][2];
            Vec3d inherited = movements[s];
            Vec3d pearlOrigin = new Vec3d(0.0, 1.52, 0.0);
            Vec3d eyeOrigin = new Vec3d(0.0, 1.62, 0.0).add(inherited.multiply(delay));
            Vec3d basePearlVel = LaunchTrajectoryMath.direction(throwYaw, throwPitch).multiply(1.5).add(inherited);
            ProjectileState exactState = new ProjectileState(pearlOrigin, basePearlVel, true);

            long t0 = System.nanoTime();
            LaunchCandidate candidate = LaunchTrajectorySearch.findCatch(exactState, eyeOrigin, inherited, delay, 60, false, false);
            long elapsed = System.nanoTime() - t0;
            maxSolveNanos = Math.max(maxSolveNanos, elapsed);
            assertNotNull(candidate, "Expected catch solution for scenario " + s);

            Vec3d wcDir = LaunchTrajectoryMath.direction(candidate.yaw(), candidate.pitch());
            for (int trial = 0; trial < 20; trial++) {
                double sx = 0.0172275 * (rng.nextDouble() - rng.nextDouble());
                double sy = 0.0172275 * (rng.nextDouble() - rng.nextDouble());
                double sz = 0.0172275 * (rng.nextDouble() - rng.nextDouble());
                Vec3d wcVel = new Vec3d(
                        (wcDir.x + sx) * 1.5 + inherited.x,
                        (wcDir.y + sy) * 1.5 + inherited.y,
                        (wcDir.z + sz) * 1.5 + inherited.z);
                totalTrials++;
                if (LaunchTrajectorySearch.interceptTick(exactState, eyeOrigin, wcVel, delay, 60) > 0) {
                    totalHits++;
                }
            }
        }

        double hitRate = (double) totalHits / (double) totalTrials;
        assertTrue(hitRate >= 0.85, "Expected >= 85% simulated catch rate, got " + (hitRate * 100.0) + "%");
        assertTrue(maxSolveNanos < 50_000_000L, "Expected fast solve (<50ms), got " + (maxSolveNanos / 1_000_000.0) + "ms");
    }

    @Test
    void segmentBoxIntersectionDetectsOriginInsideBox() {
        assertTrue(SegmentBoxIntersection.intersects(
                0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                -0.2, -0.2, -0.2, 0.2, 0.2, 0.2));
    }
}
