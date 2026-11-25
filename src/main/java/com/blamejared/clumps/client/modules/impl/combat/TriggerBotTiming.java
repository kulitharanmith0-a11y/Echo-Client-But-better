package com.blamejared.clumps.client.modules.impl.combat;

/**
 * Pure combat timing and critical-hit evaluation helpers for {@link TriggerBot}.
 * Kept free of client runtime dependencies so timing edge cases are directly unit-testable.
 */
public final class TriggerBotTiming {
    public enum CritPolicy {
        NONE,
        NORMAL,
        PRECISE
    }

    public static final double MIN_VANILLA_CRIT_COOLDOWN = 0.901;
    public static final int MAX_PING_HURT_LEAD_TICKS = 2;
    public static final long TARGET_GRACE_MILLIS = 150L;

    private TriggerBotTiming() {
    }

    public static boolean isWithinRange(double squaredDistance, float boundA, float boundB) {
        double lower = Math.min(boundA, boundB);
        double upper = Math.max(boundA, boundB);
        return squaredDistance >= lower * lower && squaredDistance <= upper * upper;
    }

    public static double requiredCooldownProgress(
            boolean useCritCooldown,
            double sampledGroundCooldown,
            double sampledCritCooldown,
            float critUpperPercent) {
        double required = useCritCooldown ? sampledCritCooldown : sampledGroundCooldown;
        if (useCritCooldown && critUpperPercent > 90.0f) {
            required = Math.max(required, MIN_VANILLA_CRIT_COOLDOWN);
        }
        return Math.max(0.0, Math.min(1.0, required));
    }

    public static boolean passesCooldown(
            float currentProgress,
            boolean useCritCooldown,
            double sampledGroundCooldown,
            double sampledCritCooldown,
            float critUpperPercent,
            boolean axeShieldBreak) {
        if (axeShieldBreak) {
            // In vanilla Minecraft an axe disables a raised shield regardless of cooldown progress.
            return true;
        }
        double required = requiredCooldownProgress(
                useCritCooldown, sampledGroundCooldown, sampledCritCooldown, critUpperPercent);
        return (double) currentProgress >= required;
    }

    public static boolean allowsCriticalTiming(
            CritPolicy policy,
            boolean inCritRange,
            boolean critOnlyOnSpace,
            boolean jumpPressed,
            boolean onGround,
            boolean inCriticalFallState,
            boolean sprinting,
            boolean allowsNonCriticalFallback) {
        if (policy == CritPolicy.NONE || !inCritRange) {
            return true;
        }
        // Never waste attack cooldown on a ground swing on the exact tick the player jumps to crit.
        if (jumpPressed && onGround) {
            return false;
        }
        if (critOnlyOnSpace && !jumpPressed && !onGround) {
            return true;
        }
        if (policy == CritPolicy.PRECISE) {
            return (inCriticalFallState && !sprinting) || allowsNonCriticalFallback;
        }
        if (policy == CritPolicy.NORMAL) {
            // In vanilla Minecraft, sprinting cancels critical hits completely.
            // While airborne in crit range, wait for falling + !sprinting; allow normal ground trades when not jumping.
            if (inCriticalFallState) {
                return !sprinting;
            }
            return allowsNonCriticalFallback;
        }
        return true;
    }

    public static int effectiveHurtLeadTicks(int pingMillis, boolean pingCompensationEnabled) {
        if (!pingCompensationEnabled || pingMillis <= 0) {
            return 0;
        }
        int halfRttTicks = Math.round((pingMillis * 0.5f) / 50.0f);
        return Math.max(0, Math.min(MAX_PING_HURT_LEAD_TICKS, halfRttTicks));
    }

    public static boolean passesHurtTime(
            boolean hurtTimeCheckEnabled,
            int targetHurtTime,
            int remainingTrackedCooldown,
            boolean recentShieldBreakZeroHurt,
            boolean axeShieldBreak,
            boolean nonFireHurtWindow,
            int hurtLeadTicks) {
        if (!hurtTimeCheckEnabled) {
            return true;
        }
        if (recentShieldBreakZeroHurt && targetHurtTime <= 0 && !nonFireHurtWindow) {
            return true;
        }
        if (axeShieldBreak) {
            return !nonFireHurtWindow;
        }
        return remainingTrackedCooldown <= Math.max(0, hurtLeadTicks);
    }

    public static boolean isTargetAcquisitionStillValid(
            int acquiredTargetId,
            int candidateTargetId,
            long lastSeenMillis,
            long nowMillis,
            long graceMillis) {
        if (acquiredTargetId < 0 || acquiredTargetId != candidateTargetId) {
            return false;
        }
        return nowMillis - lastSeenMillis <= Math.max(0L, graceMillis);
    }
}
