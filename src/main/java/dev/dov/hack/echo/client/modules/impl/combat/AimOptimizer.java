package dev.dov.hack.echo.client.modules.impl.combat;

import dev.dov.hack.echo.client.event.EventBus;
import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.MouseMovementEvent;
import dev.dov.hack.echo.client.event.world.EntityHitboxEvent;
import dev.dov.hack.echo.client.handlers.LatencyTracker;
import dev.dov.hack.echo.client.handlers.MouseSensitivity;
import dev.dov.hack.echo.client.handlers.rotation.AimState;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.keyboard.KeybindingCache;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.HotbarSlotsSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.utils.Rotation;
import dev.dov.hack.echo.client.utils.strings.Concat;
import dev.dov.hack.echo.client.utils.targeting.EntityRaycasts;
import dev.dov.hack.echo.client.utils.targeting.TargetSelection;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Active mouse-movement aim optimizer designed for natural 1.9+ PvP tracking:
 * 1. Adaptive One-Euro input smoothing with phase-lead latency reduction that
 *    removes micro-stutters while keeping fast turns responsive.
 * 2. Subtle crosshair guidance applying gentle, movement-proportional rotational
 *    micro-adjustments and halo slowdown only while actively moving the mouse.
 * 3. Optimal 1.9+ hitbox calculation targeting the closest point on the
 *    opponent's hitbox measured from eye level to maximize 3-block reach.
 * 4. Vanilla mouse-sensitivity GCD quantization with fractional carry.
 */
public class AimOptimizer extends Module {
    private static final CharSequence SECTION_AIM = CatalogText.literal("Aim");
    private static final CharSequence SECTION_TARGETING = CatalogText.literal("Targeting");
    private static final CharSequence SECTION_CONDITIONS = CatalogText.literal("Conditions");
    private static final CharSequence SUFFIX_PERCENT = CatalogText.literal("%");

    private static final double MIN_STEP_EPSILON = 1.0E-5;
    private static final double MIN_INPUT_EPSILON = 1.0E-6;
    private static final long IDLE_RESET_NANOS = 60_000_000L;
    private static final double MAX_LEAD_TICKS = 6.0;
    private static final double MAX_HORIZONTAL_LEAD_BLOCKS = 1.15;
    private static final double SLOWDOWN_HALO_MIN_DEG = 4.0;
    private static final double SLOWDOWN_HALO_MAX_DEG = 14.0;
    private static final double REVERSAL_COUNT_THRESHOLD = 1.5;
    private static final double DISCONTINUITY_DISTANCE_SQ = 2.25;
    private static final double OPTIMAL_HITBOX_INSET = 0.03;
    private static final double CLOSE_COMFORT_RANGE_SQ = 2.2 * 2.2;

    private static FloatSetting strength;
    private static FloatSetting slowdown;
    private static FloatSetting smoothing;
    private static FloatSetting prediction;
    private static FloatSetting multipoint;
    private static BooleanSetting gcd;
    private static BooleanSetting throughWalls;
    private static BooleanSetting playersOnly;
    private static ChoiceSetting targetPriority;
    private static FloatSetting range;
    private static FloatSetting fov;
    private static BooleanSetting onClick;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;

    private long lastActiveMoveNanos;
    private double smoothedUserYawRate;
    private double smoothedUserPitchRate;
    private double filteredInputSpeed;
    private double prevRawCountX;
    private double prevRawCountY;
    private double mouseRemainderX;
    private double mouseRemainderY;
    private int lastTrackedTargetId = -1;
    private int lastSampledTargetTick = Integer.MIN_VALUE;
    private Vec3d smoothedTargetVelocity = Vec3d.ZERO;
    private LivingEntity cachedOccludedTarget;
    private Vec3d cachedOccludedLocalPoint;
    private Object trackedPlayer;
    private Object trackedWorld;

    public AimOptimizer() {
        super(new ModuleInfo(
                CatalogText.literal("Aim Optimizer"),
                CatalogText.literal("Smooths mouse input and guides aim toward the closest hitbox point"),
                ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        range.setAllowOutOfRange(true);
        fov.setAllowOutOfRange(true);

        strength.description(CatalogText.literal("Strength of subtle rotational guidance while moving the mouse near a target"));
        slowdown.description(CatalogText.literal("Gentle sensitivity damping over and near target hitboxes for steady tracking"));
        smoothing.description(CatalogText.literal("Adaptive input smoothing with latency reduction to remove micro-stutters"));
        prediction.description(CatalogText.literal("Leads moving target velocity and latency when computing the optimal hitbox point"));
        multipoint.description(CatalogText.literal("Blends toward the exact eye-to-nearest-hitbox edge for maximum 1.9+ 3-block reach"));
        gcd.description(CatalogText.literal("Quantizes output mouse deltas to vanilla sensitivity GCD steps"));
        throughWalls.description(CatalogText.literal("Allows crosshair guidance toward players occluded by blocks"));
        playersOnly.description(CatalogText.literal("Targets only other players; disable to allow mobs configured in Targets"));
        targetPriority.description(CatalogText.literal("Rule used to select the active target among candidates"));
        range.description(CatalogText.literal("Maximum distance from eyes to target hitbox for crosshair guidance"));
        fov.description(CatalogText.literal("Maximum angular cone from crosshair for crosshair guidance"));
        onClick.description(CatalogText.literal("Only optimizes aim while holding the attack button"));
        itemWhitelist.description(CatalogText.literal("Activates only with selected items. Empty allows every item."));
        hotbarWhitelist.description(CatalogText.literal("Activates only on selected hotbar slots. Empty allows every slot."));

        strength.tooltip(SECTION_AIM);
        slowdown.tooltip(SECTION_AIM);
        smoothing.tooltip(SECTION_AIM);
        prediction.tooltip(SECTION_AIM);
        multipoint.tooltip(SECTION_AIM);
        gcd.tooltip(SECTION_AIM);
        throughWalls.tooltip(SECTION_AIM);
        playersOnly.tooltip(SECTION_TARGETING);
        targetPriority.tooltip(SECTION_TARGETING);
        range.tooltip(SECTION_TARGETING);
        fov.tooltip(SECTION_TARGETING);
        onClick.tooltip(SECTION_CONDITIONS);
        itemWhitelist.tooltip(SECTION_CONDITIONS);
        hotbarWhitelist.tooltip(SECTION_CONDITIONS);
    }

    @Override
    public CharSequence getSuffix() {
        return Concat.sequences(Concat.integer(Math.round(strength.getValue())), SUFFIX_PERCENT);
    }

    @Override
    public void onEnable() {
        resetAllState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetAllState();
        super.onDisable();
    }

    @Subscribe(priority = EventPriority.NORMAL)
    private void onMouseMovementPre(MouseMovementEvent.Pre event) {
        synchronizeContext();
        if (!canProcessMouseMovement()) {
            resetVisibleState();
            return;
        }
        if (rotations().snapshot().state() != AimState.IDLE) {
            resetVisibleState();
            return;
        }
        if (!passesInputConditions()) {
            resetVisibleState();
            return;
        }

        double rawCountX = event.deltaX;
        double rawCountY = event.deltaY;
        boolean movingMouse = Math.abs(rawCountX) > MIN_INPUT_EPSILON || Math.abs(rawCountY) > MIN_INPUT_EPSILON;
        long nowNanos = System.nanoTime();
        if (!movingMouse) {
            // Never inject camera movement while the mouse is stationary; clear stale memory after a brief pause.
            if (lastActiveMoveNanos != 0L && (nowNanos - lastActiveMoveNanos) > IDLE_RESET_NANOS) {
                resetVisibleState();
            }
            return;
        }

        boolean fineScale = client.options.getPerspective().isFirstPerson() && client.player.isUsingSpyglass();
        double step = MouseSensitivity.angularStep(fineScale);
        if (step <= MIN_STEP_EPSILON) {
            resetVisibleState();
            return;
        }

        double dt = sampleActiveMoveDeltaSeconds(nowNanos);
        double signX = client.options.getInvertMouseX().getValue() ? -1.0 : 1.0;
        double signY = client.options.getInvertMouseY().getValue() ? -1.0 : 1.0;

        double rawYawDelta = rawCountX * signX * step;
        double rawPitchDelta = rawCountY * signY * step;
        double smoothNorm = MathHelper.clamp(smoothing.getValue() / 100.0, 0.0, 1.0);

        // Core Mechanic 1: Adaptive input smoothing & phase-lead latency reduction runs on all active mouse movement.
        double[] smoothedUser = applyInputSmoothingAndLead(
                rawCountX, rawCountY, rawYawDelta, rawPitchDelta, step, dt, smoothNorm);
        double outYawDelta = smoothedUser[0];
        double outPitchDelta = smoothedUser[1];

        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        Vec3d eyes = client.player.getCameraPosVec(tickDelta);
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        LivingEntity target = acquireTarget(eyes, currentYaw, currentPitch, tickDelta);
        if (target == null) {
            resetTargetTracking();
            cachedOccludedTarget = null;
            cachedOccludedLocalPoint = null;
        } else {
            updateTargetVelocity(target);
            Box baseBox = interpolatedHitbox(target, tickDelta);
            Box predictedBox = buildPredictedHitbox(target, baseBox);
            Vec3d aimPoint = selectOptimalReachPointWithFallback(
                    target, predictedBox, baseBox, eyes, currentYaw, currentPitch);
            if (aimPoint != null) {
                double[] guided = applySubtleCrosshairGuidance(
                        Math.hypot(rawYawDelta, rawPitchDelta),
                        outYawDelta,
                        outPitchDelta,
                        eyes,
                        currentYaw,
                        currentPitch,
                        predictedBox,
                        aimPoint,
                        dt);
                outYawDelta = guided[0];
                outPitchDelta = guided[1];
            }
        }

        // Core Mechanic 4: Clamp pitch and quantize output deltas onto vanilla mouse-sensitivity GCD steps.
        double clampedPitchDelta = MathHelper.clamp(currentPitch + outPitchDelta, -90.0, 90.0) - currentPitch;
        double outCountX = (outYawDelta / step) * signX;
        double outCountY = (clampedPitchDelta / step) * signY;
        writeQuantizedDeltas(event, outCountX, outCountY, currentPitch + clampedPitchDelta, signY);
    }

    /**
     * One-Euro style speed-adaptive input smoothing with clamped phase-lead
     * feed-forward to remove micro-stutters while reducing perceived input lag.
     */
    private double[] applyInputSmoothingAndLead(
            double rawCountX,
            double rawCountY,
            double rawYawDelta,
            double rawPitchDelta,
            double step,
            double dt,
            double smoothNorm) {
        if (smoothNorm <= 0.0) {
            smoothedUserYawRate = 0.0;
            smoothedUserPitchRate = 0.0;
            filteredInputSpeed = 0.0;
            prevRawCountX = rawCountX;
            prevRawCountY = rawCountY;
            return new double[] {rawYawDelta, rawPitchDelta};
        }

        double adjustedYaw = rawYawDelta;
        double adjustedPitch = rawPitchDelta;
        if (Math.abs(rawCountX) <= REVERSAL_COUNT_THRESHOLD && rawCountX * prevRawCountX < 0.0) {
            adjustedYaw *= 1.0 - 0.55 * smoothNorm;
        }
        if (Math.abs(rawCountY) <= REVERSAL_COUNT_THRESHOLD && rawCountY * prevRawCountY < 0.0) {
            adjustedPitch *= 1.0 - 0.55 * smoothNorm;
        }
        prevRawCountX = rawCountX;
        prevRawCountY = rawCountY;

        double instYawRate = adjustedYaw / dt;
        double instPitchRate = adjustedPitch / dt;
        double instSpeed = Math.hypot(instYawRate, instPitchRate);

        double speedAlpha = dt / (dt + 0.015);
        filteredInputSpeed += (instSpeed - filteredInputSpeed) * speedAlpha;

        // Low cutoff at micro-jitter speeds; rapidly opens cutoff on fast PvP turns.
        double minCutoffHz = lerp(42.0, 12.0, smoothNorm);
        double cutoffHz = minCutoffHz + 0.08 * filteredInputSpeed;
        double tau = 1.0 / (2.0 * Math.PI * cutoffHz);
        double alpha = dt / (dt + tau);

        smoothedUserYawRate += (instYawRate - smoothedUserYawRate) * alpha;
        smoothedUserPitchRate += (instPitchRate - smoothedUserPitchRate) * alpha;

        // Phase-lead compensation counters smoothing lag during rapid acceleration/flicks.
        double leadScale = smoothstep(30.0, 180.0, instSpeed) * (0.35 + 0.25 * smoothNorm);
        double leadYaw = MathHelper.clamp(
                (instYawRate - smoothedUserYawRate) * leadScale,
                -Math.abs(instYawRate) * 0.30,
                Math.abs(instYawRate) * 0.30);
        double leadPitch = MathHelper.clamp(
                (instPitchRate - smoothedUserPitchRate) * leadScale,
                -Math.abs(instPitchRate) * 0.30,
                Math.abs(instPitchRate) * 0.30);

        double outYaw = (smoothedUserYawRate + leadYaw) * dt;
        double outPitch = (smoothedUserPitchRate + leadPitch) * dt;
        if (rawCountX == 0.0 && Math.abs(outYaw) < step * 0.05) {
            smoothedUserYawRate = 0.0;
            outYaw = 0.0;
        }
        if (rawCountY == 0.0 && Math.abs(outPitch) < step * 0.05) {
            smoothedUserPitchRate = 0.0;
            outPitch = 0.0;
        }
        return new double[] {outYaw, outPitch};
    }

    /**
     * Core Mechanic 2: Gentle, movement-proportional crosshair guidance and
     * halo slowdown toward the optimal closest hitbox point while moving the mouse.
     */
    private double[] applySubtleCrosshairGuidance(
            double rawUserMag,
            double userYaw,
            double userPitch,
            Vec3d eyes,
            float currentYaw,
            float currentPitch,
            Box predictedBox,
            Vec3d aimPoint,
            double dt) {
        double userMag = Math.hypot(userYaw, userPitch);
        double userSpeedDeg = userMag / dt;

        Rotation targetRot = Rotation.lookingAt(eyes, aimPoint);
        double yawErr = MathHelper.wrapDegrees(targetRot.yaw() - currentYaw);
        double pitchErr = targetRot.pitch() - currentPitch;
        double errMag = Math.hypot(yawErr, pitchErr);

        double proximity = computeHitboxProximity(eyes, currentYaw, currentPitch, predictedBox);
        double awayCos = directionalCosine(userYaw, userPitch, yawErr, pitchErr);
        double awayYield = computeAwayYieldScale(awayCos, userSpeedDeg);
        double baseSlowdown = MathHelper.clamp((slowdown.getValue() / 100.0) * proximity * awayYield, 0.0, 0.85);

        if (errMag <= 1.0E-5) {
            double stayScale = 1.0 - baseSlowdown;
            return new double[] {userYaw * stayScale, userPitch * stayScale};
        }

        double dirX = yawErr / errMag;
        double dirY = pitchErr / errMag;

        double along = userYaw * dirX + userPitch * dirY;
        double perpX = userYaw - along * dirX;
        double perpY = userPitch - along * dirY;

        double perpScale = 1.0 - baseSlowdown;
        boolean closingGap = along > 0.0 && errMag > 1.2;
        double alongScale = closingGap ? 1.0 - baseSlowdown * 0.25 : perpScale;

        double slowedAlong = along * alongScale;
        double slowedPerpX = perpX * perpScale;
        double slowedPerpY = perpY * perpScale;

        double strengthNorm = MathHelper.clamp(strength.getValue() / 100.0, 0.0, 1.0);
        double intentGate = computeIntentGate(awayCos, userSpeedDeg, proximity);

        // Straighten minor off-axis wobble into the closest hitbox edge without killing flick feel.
        double trimFraction = 0.55 * strengthNorm * intentGate;
        double trimmedPerpX = slowedPerpX * (1.0 - trimFraction);
        double trimmedPerpY = slowedPerpY * (1.0 - trimFraction);

        double steerForwardBoost = 0.0;
        if (along > 0.0 && trimFraction > 0.0) {
            steerForwardBoost = Math.hypot(slowedPerpX, slowedPerpY) * trimFraction * 0.55;
        }

        boolean overHitbox = proximity >= 0.999;
        double deadzoneLow = overHitbox ? 0.18 : 0.05;
        double deadzoneHigh = overHitbox ? 0.95 : 0.35;
        double deadzoneScale = smoothstep(deadzoneLow, deadzoneHigh, errMag);

        double propBoost = rawUserMag * strengthNorm * 0.65;
        double trackBoost = Math.min(
                rawUserMag * 1.4,
                dt * (18.0 + strengthNorm * 85.0) * smoothstep(0.0, 0.25, rawUserMag));
        double guidanceBoost = Math.max(propBoost, trackBoost * 0.60)
                * intentGate * deadzoneScale * lerp(0.45, 1.0, proximity);

        double remainingAlong = Math.max(0.0, errMag - slowedAlong);
        double totalAssistAlong = Math.min(remainingAlong, steerForwardBoost + guidanceBoost);

        double finalAlong = slowedAlong + totalAssistAlong;
        return new double[] {
                finalAlong * dirX + trimmedPerpX,
                finalAlong * dirY + trimmedPerpY
        };
    }

    private void writeQuantizedDeltas(
            MouseMovementEvent.Pre event,
            double outCountX,
            double outCountY,
            double resultingPitch,
            double signY) {
        if (!gcd.getValue()) {
            mouseRemainderX = 0.0;
            mouseRemainderY = 0.0;
            event.deltaX = outCountX;
            event.deltaY = outCountY;
            return;
        }

        double totalX = outCountX + mouseRemainderX;
        double totalY = outCountY + mouseRemainderY;
        long wholeX = Math.round(totalX);
        long wholeY = Math.round(totalY);
        mouseRemainderX = totalX - wholeX;
        mouseRemainderY = totalY - wholeY;

        if (Math.abs(resultingPitch) >= 89.999 && Math.signum(mouseRemainderY * signY) == Math.signum(resultingPitch)) {
            mouseRemainderY = 0.0;
        }
        event.deltaX = (double) wholeX;
        event.deltaY = (double) wholeY;
    }

    private LivingEntity acquireTarget(Vec3d eyes, float yaw, float pitch, float tickDelta) {
        double maxRange = Math.max(0.5, range.getValue());
        double maxRangeSq = maxRange * maxRange;
        boolean enforcePlayers = playersOnly.getValue();
        List<LivingEntity> candidates = client.world
                .getEntitiesByClass(
                        LivingEntity.class,
                        client.player.getBoundingBox().expand(maxRange + 1.0),
                        entity -> isLivingCandidate(entity, enforcePlayers))
                .stream()
                .filter(TargetSelection::isValidTarget)
                .filter(entity -> interpolatedHitbox(entity, tickDelta).squaredMagnitude(eyes) <= maxRangeSq)
                .filter(entity -> isWithinFov(entity, eyes, yaw, pitch, tickDelta))
                .toList();

        LivingEntity selected = EntityRaycasts.selectTarget(client.player, candidates, eyes, targetPriority);
        LivingEntity resolved = TargetSelection.resolveTarget(selected);
        if (resolved == null || !isLivingCandidate(resolved, enforcePlayers)) {
            return null;
        }
        if (interpolatedHitbox(resolved, tickDelta).squaredMagnitude(eyes) > maxRangeSq) {
            return null;
        }
        return isWithinFov(resolved, eyes, yaw, pitch, tickDelta) ? resolved : null;
    }

    private boolean isWithinFov(LivingEntity target, Vec3d eyes, float yaw, float pitch, float tickDelta) {
        float configuredFov = fov.getValue();
        if (configuredFov >= 360.0F) {
            return true;
        }
        Box box = interpolatedHitbox(target, tickDelta);
        Vec3d lookDir = rotationDirection(yaw, pitch);
        if (box.contains(eyes) || box.raycast(eyes, eyes.add(lookDir.multiply(range.getValue() + 2.0))).isPresent()) {
            return true;
        }
        Vec3d edgePoint = closestPointToRay(eyes, lookDir, box);
        Rotation edgeRot = Rotation.lookingAt(eyes, edgePoint);
        double yawDiff = MathHelper.wrapDegrees(edgeRot.yaw() - yaw);
        double pitchDiff = edgeRot.pitch() - pitch;
        return Math.hypot(yawDiff, pitchDiff) <= (double) configuredFov * 0.5;
    }

    private void updateTargetVelocity(LivingEntity target) {
        int id = target.getId();
        int tick = target.age;
        if (id != lastTrackedTargetId || Math.abs(tick - lastSampledTargetTick) > 2) {
            lastTrackedTargetId = id;
            lastSampledTargetTick = tick;
            Vec3d initial = target.getEntityPos().subtract(target.lastX, target.lastY, target.lastZ);
            smoothedTargetVelocity = initial.lengthSquared() > DISCONTINUITY_DISTANCE_SQ ? Vec3d.ZERO : initial;
            return;
        }
        if (tick == lastSampledTargetTick) {
            return;
        }
        lastSampledTargetTick = tick;
        Vec3d instant = target.getEntityPos().subtract(target.lastX, target.lastY, target.lastZ);
        if (instant.lengthSquared() > DISCONTINUITY_DISTANCE_SQ) {
            smoothedTargetVelocity = Vec3d.ZERO;
            return;
        }
        smoothedTargetVelocity = smoothedTargetVelocity.lerp(instant, 0.45);
    }

    private Box buildPredictedHitbox(LivingEntity target, Box baseBox) {
        double predPercent = prediction.getValue();
        if (predPercent <= 0.0) {
            return baseBox;
        }
        double pingMillis = Math.max(0, LatencyTracker.milliseconds());
        double oneWayPingTicks = Math.min(4.0, (pingMillis / 1000.0) * 20.0 * 0.5);
        double leadTicks = MathHelper.clamp((0.65 + oneWayPingTicks) * (predPercent / 100.0), 0.0, MAX_LEAD_TICKS);
        if (leadTicks <= 0.0) {
            return baseBox;
        }

        double leadX = smoothedTargetVelocity.x * leadTicks;
        double leadZ = smoothedTargetVelocity.z * leadTicks;
        double horizSq = leadX * leadX + leadZ * leadZ;
        if (horizSq > MAX_HORIZONTAL_LEAD_BLOCKS * MAX_HORIZONTAL_LEAD_BLOCKS) {
            double scale = MAX_HORIZONTAL_LEAD_BLOCKS / Math.sqrt(horizSq);
            leadX *= scale;
            leadZ *= scale;
        }

        double leadY = target.isOnGround()
                ? 0.0
                : MathHelper.clamp(
                        smoothedTargetVelocity.y * leadTicks - 0.5 * 0.08 * leadTicks * leadTicks,
                        -0.65,
                        0.75);
        return baseBox.offset(leadX, leadY, leadZ);
    }

    private Vec3d selectOptimalReachPointWithFallback(
            LivingEntity target,
            Box predictedBox,
            Box baseBox,
            Vec3d eyes,
            float referenceYaw,
            float referencePitch) {
        Vec3d predictedPoint = selectOptimalReachPoint(target, predictedBox, eyes, referenceYaw, referencePitch);
        if (predictedPoint != null) {
            return predictedPoint;
        }
        if (predictedBox != baseBox) {
            return selectOptimalReachPoint(target, baseBox, eyes, referenceYaw, referencePitch);
        }
        return null;
    }

    /**
     * Core Mechanic 3: Computes the exact closest point on the target hitbox
     * measured from the attacker's eye level (maximizing 1.9+ 3-block reach).
     */
    private Vec3d selectOptimalReachPoint(
            LivingEntity target,
            Box box,
            Vec3d eyes,
            float referenceYaw,
            float referencePitch) {
        Box sampleBox = insetBox(box, OPTIMAL_HITBOX_INSET);
        Vec3d eyeNearest = clampToBox(eyes, sampleBox);
        Vec3d center = sampleBox.getCenter();
        Vec3d eyeLevelCenter = new Vec3d(center.x, MathHelper.clamp(eyes.y, sampleBox.minY, sampleBox.maxY), center.z);

        // At Multipoint = 100% (default), target the exact closest point from eye level.
        double edgeWeight = MathHelper.clamp(multipoint.getValue() / 100.0, 0.0, 1.0);
        Vec3d preferred = eyeLevelCenter.lerp(eyeNearest, edgeWeight);

        // In point-blank range while already aiming inside the box, gently respect current ray to avoid micro-tugging.
        Vec3d lookDir = rotationDirection(referenceYaw, referencePitch);
        if (eyes.squaredDistanceTo(eyeNearest) < CLOSE_COMFORT_RANGE_SQ
                && sampleBox.raycast(eyes, eyes.add(lookDir.multiply(range.getValue() + 1.0))).isPresent()) {
            Vec3d rayNearest = closestPointToRay(eyes, lookDir, sampleBox);
            preferred = preferred.lerp(rayNearest, 0.15);
        }

        if (throughWalls.getValue() || isPointVisible(eyes, preferred)) {
            cachedOccludedTarget = null;
            cachedOccludedLocalPoint = null;
            return preferred;
        }

        if (cachedOccludedTarget == target && cachedOccludedLocalPoint != null) {
            Vec3d cachedWorld = cachedOccludedLocalPoint.add(sampleBox.minX, sampleBox.minY, sampleBox.minZ);
            if (containsInclusive(cachedWorld, sampleBox) && isPointVisible(eyes, cachedWorld)) {
                return cachedWorld;
            }
        }

        Vec3d bestGridPoint = findClosestVisibleGridPoint(sampleBox, preferred, eyes, referenceYaw, referencePitch);
        if (bestGridPoint != null) {
            cachedOccludedTarget = target;
            cachedOccludedLocalPoint = bestGridPoint.subtract(sampleBox.minX, sampleBox.minY, sampleBox.minZ);
            return bestGridPoint;
        }

        cachedOccludedTarget = null;
        cachedOccludedLocalPoint = null;
        return null;
    }

    private Vec3d findClosestVisibleGridPoint(
            Box box,
            Vec3d preferred,
            Vec3d eyes,
            float referenceYaw,
            float referencePitch) {
        Vec3d best = null;
        double bestScore = Double.MAX_VALUE;
        for (int xi = 0; xi < 3; xi++) {
            double x = lerp(box.minX, box.maxX, xi * 0.5);
            for (int yi = 0; yi < 5; yi++) {
                double y = lerp(box.minY, box.maxY, yi * 0.25);
                for (int zi = 0; zi < 3; zi++) {
                    double z = lerp(box.minZ, box.maxZ, zi * 0.5);
                    Vec3d candidate = new Vec3d(x, y, z);
                    if (!isPointVisible(eyes, candidate)) {
                        continue;
                    }
                    double eyeDist = candidate.distanceTo(eyes);
                    double preferredDist = candidate.distanceTo(preferred);
                    Rotation rot = Rotation.lookingAt(eyes, candidate);
                    double angularErr = Math.hypot(
                            MathHelper.wrapDegrees(rot.yaw() - referenceYaw),
                            rot.pitch() - referencePitch);
                    // Prioritize minimum eye-to-hitbox reach distance first, then closeness to optimal edge and view ray.
                    double score = eyeDist * 20.0 + preferredDist * 6.0 + angularErr * 0.15;
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private double computeHitboxProximity(Vec3d eyes, float yaw, float pitch, Box box) {
        Vec3d lookDir = rotationDirection(yaw, pitch);
        if (box.contains(eyes) || box.raycast(eyes, eyes.add(lookDir.multiply(range.getValue() + 2.0))).isPresent()) {
            return 1.0;
        }
        Vec3d edgePoint = closestPointToRay(eyes, lookDir, box);
        Rotation edgeRot = Rotation.lookingAt(eyes, edgePoint);
        double edgeYaw = MathHelper.wrapDegrees(edgeRot.yaw() - yaw);
        double edgePitch = edgeRot.pitch() - pitch;
        double edgeAngle = Math.hypot(edgeYaw, edgePitch);
        double haloAngle = MathHelper.clamp((double) fov.getValue() * 0.25, SLOWDOWN_HALO_MIN_DEG, SLOWDOWN_HALO_MAX_DEG);
        return 1.0 - smoothstep(0.0, haloAngle, edgeAngle);
    }

    private boolean canProcessMouseMovement() {
        return !isOutsideWorld()
                && client.player.isAlive()
                && MinecraftRenderState.getScreen() == null
                && !MinecraftRenderState.hasOverlay()
                && client.mouse != null
                && client.mouse.isCursorLocked();
    }

    private boolean passesInputConditions() {
        if (!itemWhitelist.acceptsHeldItem() || !hotbarWhitelist.permitsCurrentSlot()) {
            return false;
        }
        return !onClick.getValue() || KeybindingCache.isPressed(client.options.attackKey);
    }

    private boolean isPointVisible(Vec3d eyes, Vec3d point) {
        if (client.player == null || client.world == null) {
            return true;
        }
        HitResult hit = client.world.raycast(new RaycastContext(
                eyes, point, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));
        return hit == null || hit.getType() == HitResult.Type.MISS || hit.getPos().squaredDistanceTo(point) < 1.0E-6;
    }

    private double sampleActiveMoveDeltaSeconds(long nowNanos) {
        if (lastActiveMoveNanos == 0L) {
            lastActiveMoveNanos = nowNanos;
            return 1.0 / 120.0;
        }
        double dt = MathHelper.clamp((double) (nowNanos - lastActiveMoveNanos) / 1.0E9, 0.0005, 0.05);
        lastActiveMoveNanos = nowNanos;
        return dt;
    }

    private void synchronizeContext() {
        if (trackedPlayer != client.player || trackedWorld != client.world) {
            trackedPlayer = client.player;
            trackedWorld = client.world;
            resetAllState();
        }
    }

    private void resetAllState() {
        resetVisibleState();
        resetTargetTracking();
        cachedOccludedTarget = null;
        cachedOccludedLocalPoint = null;
    }

    private void resetVisibleState() {
        lastActiveMoveNanos = 0L;
        smoothedUserYawRate = 0.0;
        smoothedUserPitchRate = 0.0;
        filteredInputSpeed = 0.0;
        prevRawCountX = 0.0;
        prevRawCountY = 0.0;
        mouseRemainderX = 0.0;
        mouseRemainderY = 0.0;
    }

    private void resetTargetTracking() {
        lastTrackedTargetId = -1;
        lastSampledTargetTick = Integer.MIN_VALUE;
        smoothedTargetVelocity = Vec3d.ZERO;
    }

    private static double computeIntentGate(double awayCos, double userSpeedDeg, double proximity) {
        if (awayCos >= 0.0) {
            return lerp(0.50, 1.0, awayCos);
        }
        if (proximity <= 0.25 || userSpeedDeg > 130.0) {
            return 0.0;
        }
        double angleFade = smoothstep(-0.35, 0.0, awayCos);
        double speedFade = 1.0 - smoothstep(60.0, 130.0, userSpeedDeg);
        return angleFade * speedFade * 0.40 * proximity;
    }

    private static Box interpolatedHitbox(LivingEntity entity, float tickDelta) {
        EntityHitboxEvent event = new EntityHitboxEvent(
                entity, entity.getBoundingBox().expand(entity.getTargetingMargin()));
        EventBus.post(event);
        Vec3d lerped = entity.getLerpedPos(tickDelta);
        return event.getBoundingBox().offset(
                lerped.x - entity.getX(),
                lerped.y - entity.getY(),
                lerped.z - entity.getZ());
    }

    private static Box insetBox(Box box, double fraction) {
        double dx = Math.min(0.03, box.getLengthX() * fraction);
        double dy = Math.min(0.05, box.getLengthY() * fraction);
        double dz = Math.min(0.03, box.getLengthZ() * fraction);
        return new Box(
                box.minX + dx, box.minY + dy, box.minZ + dz,
                box.maxX - dx, box.maxY - dy, box.maxZ - dz);
    }

    private static Vec3d closestPointToRay(Vec3d eyes, Vec3d lookDir, Box box) {
        Vec3d point = box.getCenter();
        for (int iteration = 0; iteration < 2; iteration++) {
            double projection = Math.max(0.0, point.subtract(eyes).dotProduct(lookDir));
            point = clampToBox(eyes.add(lookDir.multiply(projection)), box);
        }
        return point;
    }

    private static Vec3d clampToBox(Vec3d point, Box box) {
        return new Vec3d(
                MathHelper.clamp(point.x, box.minX, box.maxX),
                MathHelper.clamp(point.y, box.minY, box.maxY),
                MathHelper.clamp(point.z, box.minZ, box.maxZ));
    }

    private static Vec3d rotationDirection(float yawDegrees, float pitchDegrees) {
        double yawRad = Math.toRadians(yawDegrees);
        double pitchRad = Math.toRadians(pitchDegrees);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3d(
                -Math.sin(yawRad) * cosPitch,
                -Math.sin(pitchRad),
                Math.cos(yawRad) * cosPitch);
    }

    private static double directionalCosine(double ax, double ay, double bx, double by) {
        double mag = Math.hypot(ax, ay) * Math.hypot(bx, by);
        return mag <= 1.0E-6 ? 0.0 : MathHelper.clamp((ax * bx + ay * by) / mag, -1.0, 1.0);
    }

    private static double computeAwayYieldScale(double awayCos, double userSpeedDeg) {
        if (awayCos >= 0.0 || userSpeedDeg <= 45.0) {
            return 1.0;
        }
        double awayFactor = (-awayCos) * smoothstep(45.0, 180.0, userSpeedDeg);
        return lerp(1.0, 0.25, awayFactor);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0 : 0.0;
        }
        double t = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static boolean containsInclusive(Vec3d point, Box box) {
        return point.x >= box.minX && point.x <= box.maxX
                && point.y >= box.minY && point.y <= box.maxY
                && point.z >= box.minZ && point.z <= box.maxZ;
    }

    private static boolean isLivingCandidate(LivingEntity entity, boolean enforcePlayers) {
        if (entity == client.player || entity.isRemoved() || !entity.isAlive()) {
            return false;
        }
        return !enforcePlayers || entity instanceof PlayerEntity;
    }

    public static void appendBootstrapSettings(AimOptimizer Module) {
        CatalogSettings.append(Module, strength);
        CatalogSettings.append(Module, slowdown);
        CatalogSettings.append(Module, smoothing);
        CatalogSettings.append(Module, prediction);
        CatalogSettings.append(Module, multipoint);
        CatalogSettings.append(Module, gcd);
        CatalogSettings.append(Module, throughWalls);
        CatalogSettings.append(Module, playersOnly);
        CatalogSettings.append(Module, targetPriority);
        CatalogSettings.append(Module, range);
        CatalogSettings.append(Module, fov);
        CatalogSettings.append(Module, onClick);
        CatalogSettings.append(Module, itemWhitelist);
        CatalogSettings.append(Module, hotbarWhitelist);
    }

    public static void assignBootstrapSettings(
            AimOptimizer Module,
            FloatSetting preparedStrength,
            FloatSetting preparedSlowdown,
            FloatSetting preparedSmoothing,
            FloatSetting preparedPrediction,
            FloatSetting preparedMultipoint,
            BooleanSetting preparedGcd,
            BooleanSetting preparedThroughWalls,
            BooleanSetting preparedPlayersOnly,
            ChoiceSetting preparedTargetPriority,
            FloatSetting preparedRange,
            FloatSetting preparedFov,
            BooleanSetting preparedOnClick,
            ItemListSetting preparedItemWhitelist,
            HotbarSlotsSetting preparedHotbarWhitelist) {
        strength = preparedStrength;
        slowdown = preparedSlowdown;
        smoothing = preparedSmoothing;
        prediction = preparedPrediction;
        multipoint = preparedMultipoint;
        gcd = preparedGcd;
        throughWalls = preparedThroughWalls;
        playersOnly = preparedPlayersOnly;
        targetPriority = preparedTargetPriority;
        range = preparedRange;
        fov = preparedFov;
        onClick = preparedOnClick;
        itemWhitelist = preparedItemWhitelist;
        hotbarWhitelist = preparedHotbarWhitelist;
    }
}
