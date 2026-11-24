package com.blamejared.clumps.client.handlers.rotation;

import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.world.EntityHitboxEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.utils.Rotation;
import com.blamejared.clumps.client.utils.targeting.AimPointMode;
import com.blamejared.clumps.client.utils.targeting.AimPointSettings;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class RotationRequest {
    private final RotationManager manager;
    private final Module owner;
    private final RotationPriority priority;
    private RotationSettings settings;
    private Rotation desiredRotation;
    private LivingEntity target;
    private int refreshedTick;
    private int refreshedClientTick;
    private boolean active = true;
    private boolean returning;
    private long previousUpdateNanos;
    private float yawVelocity;
    private float pitchVelocity;
    private LivingEntity cachedTarget;
    private Vec3d cachedLocalPoint;

    RotationRequest(RotationManager manager, Module owner, RotationPriority priority) {
        this.manager = manager;
        this.owner = owner;
        this.priority = priority;
    }

    
    public boolean aim(Vec3d point, RotationSettings settings) {
        return manager.aimAt(this, point, settings);
    }

    
    public boolean aim(LivingEntity target, AimPointSettings targeting, RotationSettings settings) {
        return manager.aimAt(this, target, targeting, settings);
    }

    
    public void refresh() {
        manager.refresh(this);
    }

    
    public void release() {
        manager.release(this);
    }

    
    public boolean isActive() {
        return active && manager.isCurrent(this);
    }

    Module owner() { return owner; }
    RotationPriority priority() { return priority; }
    RotationSettings settings() { return settings; }
    Rotation desiredRotation() { return desiredRotation; }
    LivingEntity target() { return target; }
    int refreshedTick() { return refreshedTick; }
    int refreshedClientTick() { return refreshedClientTick; }
    boolean returning() { return returning; }
    void settings(RotationSettings value) { settings = value; }
    void target(Rotation rotation, LivingEntity entity) { desiredRotation = rotation; target = entity; }
    void refreshedTick(int tick) { refreshedTick = tick; }
    void refreshedClientTick(int tick) { refreshedClientTick = tick; }
    void returning(boolean value) { returning = value; }

    Rotation transition(Rotation current, Rotation desired, RotationSettings settings, float elapsedSeconds) {
        return switch (settings.transition()) {
            case SNAP -> desired;
            case EASED -> eased(current, desired, settings.maxYawStep(), settings.maxPitchStep(), elapsedSeconds);
            case NATURAL -> natural(current, desired, settings.maxYawStep(), settings.maxPitchStep(), elapsedSeconds);
            case LEGIT_SMOOTH -> legitSmooth(current, desired, settings.maxYawStep(), settings.maxPitchStep(), elapsedSeconds);
        };
    }

    float elapsedSeconds(long now) {
        if (previousUpdateNanos == 0L) {
            previousUpdateNanos = now;
            return 0.016666668F;
        }
        float elapsed = Math.clamp((float) (now - previousUpdateNanos) / 1.0E9F, 0.001F, 0.1F);
        previousUpdateNanos = now;
        return elapsed;
    }

    void resetClock() { previousUpdateNanos = 0L; }

    void invalidate() {
        active = false;
        returning = false;
        refreshedTick = 0;
        refreshedClientTick = 0;
        previousUpdateNanos = 0L;
        yawVelocity = 0.0F;
        pitchVelocity = 0.0F;
        cachedTarget = null;
        cachedLocalPoint = null;
    }

    Optional<Vec3d> targetPoint(LivingEntity entity, AimPointSettings targeting, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == null || client.player == null || client.world == null) return Optional.empty();
        Box box = interpolatedHitbox(entity, tickDelta);
        Vec3d center = box.getCenter();
        Vec3d selected = switch (targeting.mode()) {
            case STRAIGHT -> new Vec3d(center.x, box.maxY - 0.1, center.z);
            case CLOSEST -> {
                Vec3d eyes = client.player.getCameraPosVec(tickDelta);
                yield new Vec3d(MathHelper.clamp(eyes.x, box.minX, box.maxX), MathHelper.clamp(eyes.y, box.minY, box.maxY), MathHelper.clamp(eyes.z, box.minZ, box.maxZ));
            }
            case RANDOM -> RotationMath.randomPoint(box);
            case CENTER -> center;
            case FEET -> new Vec3d(center.x, box.minY + 0.1, center.z);
        };
        if (targeting.randomize()) selected = randomizePoint(selected, box, targeting.mode());
        Vec3d preferred = center.lerp(selected, (double) targeting.pointWeight() / 100.0);
        if (visible(preferred, tickDelta)) {
            cachedTarget = null;
            cachedLocalPoint = null;
            return Optional.of(preferred);
        }
        if (cachedTarget == entity && cachedLocalPoint != null) {
            Vec3d previous = cachedLocalPoint.add(box.minX, box.minY, box.minZ);
            if (containsInclusive(previous, box) && visible(previous, tickDelta)) return Optional.of(previous);
        }
        Vec3d alternative = closestVisibleGridPoint(box, preferred, tickDelta);
        if (alternative != null) {
            cachedTarget = entity;
            cachedLocalPoint = alternative.subtract(box.minX, box.minY, box.minZ);
            return Optional.of(alternative);
        }
        cachedTarget = null;
        cachedLocalPoint = null;
        return targeting.allowOccluded() ? Optional.of(preferred) : Optional.empty();
    }

    private static Box interpolatedHitbox(LivingEntity entity, float tickDelta) {
        EntityHitboxEvent event = new EntityHitboxEvent(entity, entity.getBoundingBox().expand(entity.getTargetingMargin()));
        EventBus.post(event);
        Vec3d interpolated = entity.getLerpedPos(tickDelta);
        return event.getBoundingBox().offset(interpolated.x - entity.getX(), interpolated.y - entity.getY(), interpolated.z - entity.getZ());
    }

    private static Rotation eased(Rotation current, Rotation desired, float yawSpeed, float pitchSpeed, float elapsed) {
        float yaw = MathHelper.wrapDegrees(desired.yaw() - current.yaw());
        float pitch = desired.pitch() - current.pitch();
        float yawStep = Math.min(Math.abs(yaw), Math.max(yawSpeed * elapsed * 0.2F, Math.abs(yaw) * (1.0F - (float) Math.exp(-yawSpeed * elapsed * 0.1F))));
        float pitchStep = Math.min(Math.abs(pitch), Math.max(pitchSpeed * elapsed * 0.2F, Math.abs(pitch) * (1.0F - (float) Math.exp(-pitchSpeed * elapsed * 0.1F))));
        return new Rotation(current.yaw() + Math.signum(yaw) * yawStep, current.pitch() + Math.signum(pitch) * pitchStep);
    }

    private Rotation natural(Rotation current, Rotation desired, float yawSpeed, float pitchSpeed, float elapsed) {
        elapsed = MathHelper.clamp(elapsed, 0.001F, 0.1F);
        float yaw = MathHelper.wrapDegrees(desired.yaw() - current.yaw());
        float pitch = desired.pitch() - current.pitch();
        yawVelocity = RotationMath.smoothVelocity(yawVelocity, yaw, yawSpeed, elapsed);
        pitchVelocity = RotationMath.smoothVelocity(pitchVelocity, pitch, pitchSpeed, elapsed);
        return new Rotation(current.yaw() + yawVelocity * elapsed, current.pitch() + pitchVelocity * elapsed);
    }

    private Rotation legitSmooth(Rotation current, Rotation desired, float yawSpeed, float pitchSpeed, float elapsed) {
        elapsed = MathHelper.clamp(elapsed, 0.001F, 0.05F);
        float yawErr = MathHelper.wrapDegrees(desired.yaw() - current.yaw());
        float pitchErr = desired.pitch() - current.pitch();
        float dist = (float) Math.hypot(yawErr, pitchErr);

        // Smooth cubic ease-out inside the inner 3.0-degree core so aim glides organically onto
        // the hitbox without mechanical snapping or hard linear stops, while keeping fast catch-up.
        float coreT = MathHelper.clamp(dist / 3.0F, 0.0F, 1.0F);
        float smoothTaper = 0.25F + 0.75F * (coreT * coreT * (3.0F - 2.0F * coreT));

        // Proportional target velocity with smooth distance-scaled catch-up and hard cap
        float yawGain = Math.max(4.5F, yawSpeed * 0.095F);
        float pitchGain = Math.max(3.5F, pitchSpeed * 0.085F);
        float yawCap = Math.max(12.0F, yawSpeed * (1.15F + Math.min(0.85F, Math.abs(yawErr) / 35.0F)));
        float pitchCap = Math.max(10.0F, pitchSpeed * (1.05F + Math.min(0.65F, Math.abs(pitchErr) / 35.0F)));

        float desiredYawVel = MathHelper.clamp(yawErr * yawGain * smoothTaper, -yawCap, yawCap);
        float desiredPitchVel = MathHelper.clamp(pitchErr * pitchGain * smoothTaper, -pitchCap, pitchCap);

        // Critically damped velocity inertia: smooth acceleration out and gentle deceleration in
        float inertiaRate = dist > 6.0F ? 14.0F : 10.5F;
        float blend = 1.0F - (float) Math.exp(-elapsed * inertiaRate);
        yawVelocity += (desiredYawVel - yawVelocity) * blend;
        pitchVelocity += (desiredPitchVel - pitchVelocity) * blend;

        float nextYawDelta = yawVelocity * elapsed;
        float nextPitchDelta = pitchVelocity * elapsed;

        // Prevent overshoot jitter when very close to target
        if (Math.abs(nextYawDelta) > Math.abs(yawErr) && Math.signum(nextYawDelta) == Math.signum(yawErr)) {
            nextYawDelta = yawErr * 0.85F;
            yawVelocity *= 0.4F;
        }
        if (Math.abs(nextPitchDelta) > Math.abs(pitchErr) && Math.signum(nextPitchDelta) == Math.signum(pitchErr)) {
            nextPitchDelta = pitchErr * 0.85F;
            pitchVelocity *= 0.4F;
        }

        // Tiny human-hand micro-sway inside the hitbox so tracking never looks pixel-frozen
        if (dist > 0.08F && dist < 4.5F) {
            double t = System.nanoTime() * 1.0E-9;
            float microScale = MathHelper.clamp(dist / 3.0F, 0.15F, 1.0F) * elapsed * 4.2F;
            nextYawDelta += (float) Math.sin(t * 7.3) * 0.14F * microScale;
            nextPitchDelta += (float) Math.cos(t * 5.9) * 0.09F * microScale;
        }

        return new Rotation(
                current.yaw() + nextYawDelta,
                MathHelper.clamp(current.pitch() + nextPitchDelta, -90.0F, 90.0F));
    }

    private static Vec3d randomizePoint(Vec3d point, Box box, AimPointMode mode) {
        if (mode != AimPointMode.CLOSEST) return point.lerp(RotationMath.randomPoint(box), 0.5);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3d offset = new Vec3d(point.x + (random.nextDouble() - 0.5) * (box.maxX - box.minX) * 0.9, point.y + (random.nextDouble() - 0.5) * (box.maxY - box.minY) * 0.9, point.z + (random.nextDouble() - 0.5) * (box.maxZ - box.minZ) * 0.9);
        return point.lerp(new Vec3d(MathHelper.clamp(offset.x, box.minX, box.maxX), MathHelper.clamp(offset.y, box.minY, box.maxY), MathHelper.clamp(offset.z, box.minZ, box.maxZ)), 0.5);
    }

    private static Vec3d closestVisibleGridPoint(Box box, Vec3d preferred, float tickDelta) {
        Vec3d closest = null;
        double distance = Double.MAX_VALUE;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 3; z++) {
                    Vec3d point = new Vec3d(box.minX + (double) x / 2.0 * (box.maxX - box.minX), box.minY + (double) y / 4.0 * (box.maxY - box.minY), box.minZ + (double) z / 2.0 * (box.maxZ - box.minZ));
                    if (visible(point, tickDelta) && point.squaredDistanceTo(preferred) < distance) {
                        distance = point.squaredDistanceTo(preferred);
                        closest = point;
                    }
                }
            }
        }
        return closest;
    }

    private static boolean visible(Vec3d point, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return true;
        HitResult hit = client.world.raycast(new RaycastContext(client.player.getCameraPosVec(tickDelta), point, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));
        return hit == null || hit.getType() == HitResult.Type.MISS || hit.getPos().squaredDistanceTo(point) < 1.0E-6;
    }

    private static boolean containsInclusive(Vec3d point, Box box) {
        return point.x >= box.minX && point.x <= box.maxX && point.y >= box.minY
                && point.y <= box.maxY && point.z >= box.minZ && point.z <= box.maxZ;
    }
}
