package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.config.NameRegistry;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.network.PacketSendEvent;
import com.blamejared.clumps.client.modules.impl.utility.PearlGrapplePhase;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.EntityPositionTracker;
import com.blamejared.clumps.client.handlers.LatencyTracker;
import com.blamejared.clumps.client.handlers.PositionSample;
import com.blamejared.clumps.client.utils.Rotation;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationManager;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.handlers.VelocitySample;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.mixin.accessor.MinecraftClientAccessor;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.CrosshairHits;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import com.blamejared.clumps.client.utils.trajectory.MovingTargetLaunch;
import com.blamejared.clumps.client.utils.trajectory.MovingTargetTrajectorySearch;
import com.blamejared.clumps.client.utils.trajectory.TargetPath;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PearlGrapple extends Module {
    private static final double EYE_OFFSET = 0.1;
    private static final float MAX_TARGET_DISTANCE = 48.0f;
    private static final double REQUIRED_HIT_FRACTION = 0.62;
    private static final double FORCED_MIN_HIT_FRACTION = 0.35;
    private static final double MANUAL_HIT_TOLERANCE = 0.12;
    private static final int AIM_WAIT_TICKS = 10;
    private static final double AIM_POINT_DISTANCE = 100.0;
    private static final float MIN_ROTATION_SPEED = 180.0f;
    private static final float MAX_ROTATION_SPEED = 1200.0f;
    private static final double TELEPORT_DISTANCE = 8.0;
    private static final int TELEPORT_SAMPLE_TICKS = 5;
    private static final double FAST_TARGET_SPEED = 0.8;
    private static final int WIND_CHARGE_WAIT_TICKS = 20;
    private static final float WIND_CHARGE_PITCH = 50.0f;
    private static final double VELOCITY_PACKET_THRESHOLD_SQUARED = 0.1;
    private static final double RISE_VELOCITY = 0.6;
    private static final int REQUIRED_RISE_SAMPLES = 3;
    private static final double HIGH_LATENCY_SPEED = 0.5;
    private static final int HIGH_LATENCY_MILLIS = 150;
    private static final double ORDINARY_MOVING_HIT_FRACTION = 0.45;
    private static final double AIRBORNE_HIT_FRACTION = 0.40;
    private static final double PERTURBED_HIT_FRACTION = 0.20;
    private static final double PREDICTION_PERTURBATION_TICKS = 0.65;
    private static final double AIRBORNE_PERTURBATION_TICKS = 0.35;
    private static final double FAST_FALL_VELOCITY = -0.85;
    private static final double FALLING_HIT_FRACTION = 0.50;
    private static final int PEARL_FOLLOW_SIM_TICKS = 110;
    private static final int FOLLOW_LANDING_EXPIRE_TICKS = 35;
    private static final double ENEMY_PEARL_OWNER_PROXIMITY_SQ = 4.0 * 4.0;
    private static final double SELF_PEARL_EXCLUDE_SQ = 2.6 * 2.6;

    private static final CharSequence MODE_GRAPPLE = CatalogText.literal("Grapple");
    private static final CharSequence MODE_FOLLOW = CatalogText.literal("Pearl Follow");
    private static final CharSequence MODE_BOTH = CatalogText.literal("Both");
    private static final CharSequence SECTION_FOLLOW = CatalogText.literal("Pearl Follow");
    private static final CharSequence NO_PEARL = CatalogText.literal("No ender pearl in hotbar");
    private static final CharSequence PEARL_COOLDOWN = CatalogText.literal("Ender pearl is on cooldown");
    private static final CharSequence NO_TARGET = CatalogText.literal("No target in range");
    private static final CharSequence TARGET_LEFT_RANGE = CatalogText.literal("Target left range");
    private static final CharSequence BLOCKED_PATH = CatalogText.literal("Pearl path is blocked");
    private static final CharSequence NO_RELIABLE_SHOT = CatalogText.literal("No reliable shot");

    private static final ChoiceSetting mode = new ChoiceSetting(
            CatalogText.literal("Mode"), MODE_GRAPPLE, MODE_GRAPPLE, MODE_FOLLOW, MODE_BOTH);
    private static final KeybindSetting grappleKey = new KeybindSetting(
            CatalogText.literal("Grapple Key"), InputKey.NONE);
    private static final BooleanSetting followOnKey = new BooleanSetting(
            CatalogText.literal("Follow Only On Key"), true);
    private static final KeybindSetting followKey = new KeybindSetting(
            CatalogText.literal("Follow Key"), InputKey.NONE);
    private static final FloatSetting followRange = new FloatSetting(
            CatalogText.literal("Follow Range"), 44.0f, 8.0f, 64.0f, 1.0f, CatalogText.literal(" blocks"));
    private static final IntSetting followDelay = new IntSetting(
            CatalogText.literal("Follow Delay"), 1, 0, 8, CatalogText.literal(" ticks"));

    private static FloatSetting distance;
    private static IntSetting maxPredictTicks;
    private static BooleanSetting silentRotations;
    private PearlGrapplePhase phase;
    private PlayerEntity target;
    private int selectedAtPlayerTick;
    private int selectedAtTrackerTick;
    private boolean delayedForWindCharge;
    private int riseStartTrackerTick;
    private TargetPath targetPath;
    private MovingTargetTrajectorySearch.Result targetMotion;
    private MovingTargetLaunch bestLaunch;
    private int pathUpdatedPlayerTick;
    private Vec3d lastSentPosition;
    private Vec3d sentVelocity;
    private boolean sentOnGround;
    private boolean sentMovementThisTick;

    // Pearl Follow tracking state
    private final Map<Integer, Long> seenEnemyPearls = new HashMap<>();
    private int pendingFollowPearlId = -1;
    private Vec3d pendingFollowLanding;
    private long pendingFollowReadyTick;
    private long pendingFollowExpiresTick;
    private MovingTargetLaunch followLaunch;
    private boolean grappleKeyWasPressed;
    private boolean activeOneShotGrapple;

    public PearlGrapple() {
        super(new ModuleInfo(CatalogText.literal("Pearl Grapple"), CatalogText.literal("Throws an ender pearl that hits the best player target"), ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        phase = PearlGrapplePhase.SELECT;
        riseStartTrackerTick = -1;
        pathUpdatedPlayerTick = -1;
        sentVelocity = Vec3d.ZERO;
        sentOnGround = true;
        clearFollowState();
        grappleKeyWasPressed = false;
        activeOneShotGrapple = false;

        mode.description(CatalogText.literal("Grapple: one-shot target grapple. Pearl Follow: throw a pearl to enemy pearl landings. Both: passive follow + Grapple Key"));
        grappleKey.description(CatalogText.literal("Press to throw a direct grapple pearl at the best target while in Both mode"));
        grappleKey.setVisibilityPredicate(ignored -> mode.isSelected(MODE_BOTH));
        followOnKey.description(CatalogText.literal("Only follow enemy pearls while Follow Key is held so pearls are not wasted automatically"));
        followOnKey.setVisibilityPredicate(ignored -> isFollowModeEnabled());
        followKey.description(CatalogText.literal("Hold this key when an enemy throws a pearl to throw yours to their landing spot"));
        followKey.setVisibilityPredicate(ignored -> isFollowModeEnabled() && followOnKey.getValue());
        followRange.description(CatalogText.literal("Maximum distance to an enemy pearl landing spot to follow"));
        followRange.setVisibilityPredicate(ignored -> isFollowModeEnabled());
        followDelay.description(CatalogText.literal("Ticks to wait after detecting an enemy pearl before throwing your follow pearl"));
        followDelay.setVisibilityPredicate(ignored -> isFollowModeEnabled());
        maxPredictTicks.description(CatalogText.literal("How many ticks of pearl flight and target movement are simulated"));

        followOnKey.tooltip(SECTION_FOLLOW);
        followKey.tooltip(SECTION_FOLLOW);
        followRange.tooltip(SECTION_FOLLOW);
        followDelay.tooltip(SECTION_FOLLOW);
    }

    @Override
    public CharSequence getSuffix() {
        return mode.getValue();
    }

    private static boolean isFollowModeEnabled() {
        return mode.isSelected(MODE_FOLLOW) || mode.isSelected(MODE_BOTH);
    }

    private void clearFollowState() {
        pendingFollowPearlId = -1;
        pendingFollowLanding = null;
        pendingFollowReadyTick = 0L;
        pendingFollowExpiresTick = 0L;
        followLaunch = null;
    }

    @Override
    public void onEnable() {
        PearlGrapplePhase selected = PearlGrapplePhase.SELECT;
        phase = selected;
        initializePearlPlayerType();
        target = null;
        targetPath = null;
        targetMotion = null;
        bestLaunch = null;
        pathUpdatedPlayerTick = -1;
        initializePearlClientType();
        Vec3d position;
        if (client.player != null) {
            initializePearlClientType();
            position = client.player.getEntityPos();
        } else position = null;
        initializePearlVectorType();
        lastSentPosition = position;
        initializePearlClientType();
        Vec3d movement;
        if (client.player != null) {
            initializePearlClientType();
            Vec3d current = client.player.getEntityPos();
            initializePearlClientType();
            double previousX = client.player.lastX;
            initializePearlClientType();
            double previousY = client.player.lastY;
            initializePearlClientType();
            double previousZ = client.player.lastZ;
            movement = current.subtract(previousX, previousY, previousZ);
        } else movement = Vec3d.ZERO;
        initializePearlVectorType();
        sentVelocity = movement;
        initializePearlClientType();
        boolean onGround;
        if (client.player != null) {
            initializePearlClientType();
            onGround = client.player.isOnGround();
        } else onGround = true;
        sentOnGround = onGround;
        sentMovementThisTick = false;
        seenEnemyPearls.clear();
        clearFollowState();
        grappleKeyWasPressed = false;
        activeOneShotGrapple = mode.isSelected(MODE_GRAPPLE);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, true);
        initializePearlRotationType();
        RotationManager rotations = rotations();
        initializePearlRotationType();
        rotations.cancel(this);
        initializePearlPlayerType();
        target = null;
        targetPath = null;
        targetMotion = null;
        bestLaunch = null;
        seenEnemyPearls.clear();
        clearFollowState();
        activeOneShotGrapple = false;
        super.onDisable();
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            if (mode.isSelected(MODE_GRAPPLE)) {
                setEnabled(false);
            } else {
                clearFollowState();
                seenEnemyPearls.clear();
                activeOneShotGrapple = false;
            }
            return;
        }
        if (MinecraftRenderState.getScreen() != null) return;

        if (mode.isSelected(MODE_BOTH) && !activeOneShotGrapple) {
            boolean pressed = grappleKey.getKey() != InputKey.NONE && grappleKey.getKey().isPressed();
            if (pressed && !grappleKeyWasPressed) {
                activeOneShotGrapple = true;
                phase = PearlGrapplePhase.SELECT;
                target = null;
                bestLaunch = null;
            }
            grappleKeyWasPressed = pressed;
        }

        if (activeOneShotGrapple) {
            switch (phase) {
                case SELECT -> select();
                case AIM -> aim();
                case RESTORE -> finishGrappleShot();
            }
            return;
        }

        if (isFollowModeEnabled()) {
            tickPearlFollow();
        }
    }

    private void finishGrappleShot() {
        if (mode.isSelected(MODE_GRAPPLE)) {
            setEnabled(false);
            return;
        }
        DeferredActionQueue.finish(this, true);
        rotations().cancel(this);
        activeOneShotGrapple = false;
        target = null;
        targetPath = null;
        targetMotion = null;
        bestLaunch = null;
        phase = PearlGrapplePhase.SELECT;
    }

    private void tickPearlFollow() {
        long nowTick = client.player.age;
        pruneSeenPearls(nowTick);
        scanForEnemyPearls(nowTick);

        if (pendingFollowLanding == null) {
            followLaunch = null;
            if (rotations().isOwnedBy(this)) {
                rotations().cancel(this);
            }
            return;
        }
        if (nowTick > pendingFollowExpiresTick) {
            clearFollowState();
            DeferredActionQueue.finish(this, true);
            rotations().cancel(this);
            return;
        }
        if (followOnKey.getValue()) {
            InputKey key = followKey.getKey();
            if (key == null || key == InputKey.NONE || !key.isPressed()) {
                followLaunch = null;
                if (rotations().isOwnedBy(this)) {
                    rotations().cancel(this);
                }
                return;
            }
        }
        if (nowTick < pendingFollowReadyTick || isPearlCoolingDown()) {
            return;
        }
        int pearlSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.ENDER_PEARL));
        if (pearlSlot == -1) {
            return;
        }
        Vec3d origin = lastSentPosition != null ? lastSentPosition : client.player.getEntityPos();
        Vec3d eye = origin.add(0.0, currentPoseEyeHeight() - EYE_OFFSET, 0.0);
        Vec3d inherited = new Vec3d(sentVelocity.x, sentOnGround ? 0.0 : sentVelocity.y, sentVelocity.z);
        MovingTargetLaunch launch = MovingTargetTrajectorySearch.findLaunchToPoint(
                eye, inherited, pendingFollowLanding, Math.max(maxPredictTicks.getValue(), 80));
        if (launch == null) {
            return;
        }
        followLaunch = launch;
        if (!silentRotations.getValue()) {
            float yawDiff = Math.abs(MathHelper.wrapDegrees(launch.yaw() - client.player.getYaw()));
            float pitchDiff = Math.abs(launch.pitch() - client.player.getPitch());
            if (yawDiff > 2.5F || pitchDiff > 2.5F) {
                return;
            }
        }
        if (client.player.getInventory().getSelectedSlot() != pearlSlot
                && !DeferredActionQueue.ensureSelected(this, pearlSlot, 0, true)) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
            return;
        }
        if (silentRotations.getValue() && rotations().override(this, RotationPriority.HIGH, new Rotation(launch.yaw(), launch.pitch())).map(scope -> {
                    scope.commit();
                    return true;
                }).isEmpty()) {
            return;
        }
        CrosshairHits.withItemUseHit(() -> actions().executeDoItemUse(this));
        rotations().cancel(this);
        DeferredActionQueue.finish(this, true);
        clearFollowState();
    }

    private void scanForEnemyPearls(long nowTick) {
        double maxFollowDist = followRange.getValue();
        double maxFollowDistSq = maxFollowDist * maxFollowDist;
        List<EnderPearlEntity> pearls = client.world.getEntitiesByClass(
                EnderPearlEntity.class,
                client.player.getBoundingBox().expand(maxFollowDist + 12.0),
                pearl -> pearl != null && pearl.isAlive() && !pearl.isRemoved() && !seenEnemyPearls.containsKey(pearl.getId()));
        for (EnderPearlEntity pearl : pearls) {
            if (!isEnemyPearl(pearl)) {
                continue;
            }
            seenEnemyPearls.put(pearl.getId(), nowTick + 120L);
            Vec3d landing = MovingTargetTrajectorySearch.predictPearlLanding(
                    pearl.getEntityPos(), pearl.getVelocity(), PEARL_FOLLOW_SIM_TICKS);
            if (landing == null) {
                continue;
            }
            if (client.player.getEyePos().squaredDistanceTo(landing) > maxFollowDistSq) {
                continue;
            }
            pendingFollowPearlId = pearl.getId();
            pendingFollowLanding = landing;
            pendingFollowReadyTick = nowTick + Math.max(0, followDelay.getValue());
            pendingFollowExpiresTick = nowTick + FOLLOW_LANDING_EXPIRE_TICKS;
        }
    }

    private boolean isEnemyPearl(EnderPearlEntity pearl) {
        Entity owner = pearl.getOwner();
        if (owner != null) {
            if (owner == client.player) {
                return false;
            }
            return owner instanceof PlayerEntity player && TargetSelection.isValidTarget(player);
        }
        // Fallback for servers that do not sync projectile owner UUIDs to the client
        if (pearl.age > 4) {
            return false;
        }
        Vec3d pos = pearl.getEntityPos();
        if (pos.squaredDistanceTo(client.player.getEyePos()) <= SELF_PEARL_EXCLUDE_SQ) {
            return false;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !TargetSelection.isValidTarget(player)) {
                continue;
            }
            if (pos.squaredDistanceTo(player.getEyePos()) <= ENEMY_PEARL_OWNER_PROXIMITY_SQ) {
                return true;
            }
        }
        return false;
    }

    private void pruneSeenPearls(long nowTick) {
        if (seenEnemyPearls.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, Long>> iterator = seenEnemyPearls.entrySet().iterator();
        while (iterator.hasNext()) {
            if (nowTick >= iterator.next().getValue()) {
                iterator.remove();
            }
        }
    }

    private double currentPoseEyeHeight() {
        if (client.player == null) {
            return 1.62;
        }
        if (client.player.isGliding() || client.player.getPose() == EntityPose.GLIDING) {
            return 0.4;
        }
        return client.player.getEyeHeight(client.player.getPose());
    }

    private void select() {
        int slot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.ENDER_PEARL));
        if (slot == -1) {
            fail(NO_PEARL);
            return;
        }
        if (isPearlCoolingDown()) {
            fail(PEARL_COOLDOWN);
            return;
        }
        target = selectTarget();
        if (target == null) {
            fail(NO_TARGET);
            return;
        }
        if (client.player.getInventory().getSelectedSlot() != slot
                && !DeferredActionQueue.ensureSelected(this, slot, -1, true)) {
            setEnabled(false);
            return;
        }
        selectedAtPlayerTick = client.player.age;
        selectedAtTrackerTick = EntityPositionTracker.currentTick();
        delayedForWindCharge = false;
        riseStartTrackerTick = -1;
        phase = PearlGrapplePhase.AIM;
    }

    private void aim() {
        int tick = client.player.age;
        if (!client.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
            fail(NO_PEARL);
            return;
        }
        if (!isValidTarget(target)) {
            fail(TARGET_LEFT_RANGE);
            return;
        }
        if (tick <= selectedAtPlayerTick || pathUpdatedPlayerTick != tick) return;
        if (tick - selectedAtPlayerTick < WIND_CHARGE_WAIT_TICKS && shouldWaitForWindCharge(target)) {
            delayedForWindCharge = true;
            return;
        }
        VelocitySample velocity = EntityPositionTracker.velocity(target);
        if (velocity != null && targetMotion != null && targetMotion.tick() < velocity.tick()) return;
        boolean receivedVelocity = velocity != null && velocity.tick() >= selectedAtTrackerTick;
        if (!receivedVelocity && tick - selectedAtPlayerTick < 30 && awaitingRiseSamples()) return;
        boolean force = tick - selectedAtPlayerTick >= AIM_WAIT_TICKS + (delayedForWindCharge ? WIND_CHARGE_WAIT_TICKS : 0);
        MovingTargetLaunch launch = acceptableLaunch(force);
        if (launch != null) throwPearl(launch);
        if (phase == PearlGrapplePhase.AIM && force) {
            fail(targetPath != null && MovingTargetTrajectorySearch.findLaunch(targetPath.withoutObstructions()) != null
                && bestLaunch == null ? BLOCKED_PATH : NO_RELIABLE_SHOT);
        }
    }

    private MovingTargetLaunch acceptableLaunch(boolean force) {
        if (bestLaunch == null || (!targetMotion.reliable() && !force)) return null;
        MovingTargetLaunch launch = bestLaunch;
        if (!silentRotations.getValue()) {
            float yaw = client.player.getYaw();
            float pitch = client.player.getPitch();
            double fraction = MovingTargetTrajectorySearch.hitFraction(targetPath, yaw, pitch);
            double required = force ? FORCED_MIN_HIT_FRACTION : Math.min(REQUIRED_HIT_FRACTION, bestLaunch.hitFraction() - MANUAL_HIT_TOLERANCE);
            if (fraction <= 0.0 || fraction < required) return null;
            launch = new MovingTargetLaunch(yaw, pitch, fraction, bestLaunch.meanHitTick());
        }
        if (requiresRobustPrediction()) return remainsReliableWhenPerturbed(launch) ? launch : null;
        if (launch.hitFraction() < (force ? FORCED_MIN_HIT_FRACTION : REQUIRED_HIT_FRACTION)) return null;
        return launch;
    }

    private boolean awaitingRiseSamples() {
        if (riseStartTrackerTick < 0) return false;
        return EntityPositionTracker.positions(target).stream()
            .filter(sample -> sample.tick() >= riseStartTrackerTick).count() < REQUIRED_RISE_SAMPLES;
    }

    private boolean shouldWaitForWindCharge(PlayerEntity player) {
        VelocitySample velocity = EntityPositionTracker.velocity(player);
        if (velocity != null && velocity.tick() >= selectedAtTrackerTick
                && velocity.velocity().lengthSquared() > VELOCITY_PACKET_THRESHOLD_SQUARED) return false;
        if (targetMotion != null && targetMotion.velocity().y > RISE_VELOCITY) {
            if (riseStartTrackerTick < 0) riseStartTrackerTick = EntityPositionTracker.currentTick();
            return false;
        }
        boolean holdsWindCharge = player.getMainHandStack().isOf(Items.WIND_CHARGE)
            || player.getOffHandStack().isOf(Items.WIND_CHARGE);
        return holdsWindCharge && player.getPitch() >= WIND_CHARGE_PITCH;
    }

    private boolean requiresRobustPrediction() {
        double speed = LatencyTracker.milliseconds() > HIGH_LATENCY_MILLIS ? HIGH_LATENCY_SPEED : FAST_TARGET_SPEED;
        if (targetMotion.velocity().length() > speed) return true;
        Vec3d velocity = targetMotion.velocity();
        return sentVelocity.y < FAST_FALL_VELOCITY && !sentOnGround && velocity.horizontalLength() > Math.abs(velocity.y);
    }

    private boolean remainsReliableWhenPerturbed(MovingTargetLaunch launch) {
        boolean selfFalling = sentVelocity.y < FAST_FALL_VELOCITY && !sentOnGround;
        boolean targetGliding = target != null && (target.isGliding() || target.getPose() == EntityPose.GLIDING);
        boolean targetFastAirborne = targetMotion != null
                && (targetGliding || (targetMotion.airborne() && Math.abs(targetMotion.velocity().y) > 0.45));
        double minFraction = targetFastAirborne ? AIRBORNE_HIT_FRACTION
                : (selfFalling ? FALLING_HIT_FRACTION : ORDINARY_MOVING_HIT_FRACTION);
        if (launch.hitFraction() < minFraction) return false;
        double perturbTicks = targetFastAirborne ? AIRBORNE_PERTURBATION_TICKS : PREDICTION_PERTURBATION_TICKS;
        double age = predictionAge(targetMotion);
        int ticks = maxPredictTicks.getValue();
        for (double offset : new double[] {-perturbTicks, perturbTicks}) {
            Box[] boxes = MovingTargetTrajectorySearch.predictBoxes(target, targetMotion, age + offset, ticks);
            TargetPath perturbed = targetPath.withTargetBoxes(boxes);
            if (MovingTargetTrajectorySearch.hitFraction(perturbed, launch.yaw(), launch.pitch()) < PERTURBED_HIT_FRACTION) return false;
        }
        return true;
    }

    private void throwPearl(MovingTargetLaunch launch) {
        if (isPearlCoolingDown()) {
            fail(PEARL_COOLDOWN);
            return;
        }
        if (silentRotations.getValue() && rotations().override(this, RotationPriority.HIGH, new Rotation(launch.yaw(), launch.pitch())).map(scope -> {
                    scope.commit();
                    return true;
                }).isEmpty()) return;
        CrosshairHits.withItemUseHit(() -> actions().executeDoItemUse(this));
        rotations().cancel(this);
        phase = PearlGrapplePhase.RESTORE;
        if (!isPearlCoolingDown()) finishGrappleShot();
    }

    private void fail(CharSequence message) {
        NameRegistry.postNotification(this, message);
        finishGrappleShot();
    }

    private boolean isPearlCoolingDown() {
        return client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL));
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld() || silentRotations.getValue()) return;
        MovingTargetLaunch activeLaunch = (activeOneShotGrapple && phase == PearlGrapplePhase.AIM)
                ? bestLaunch
                : followLaunch;
        if (activeLaunch == null) {
            if (rotations().isOwnedBy(this)) {
                rotations().cancel(this);
            }
            return;
        }
        Vec3d point = client.player.getEyePos().add(Vec3d.fromPolar(activeLaunch.pitch(), activeLaunch.yaw()).multiply(AIM_POINT_DISTANCE));
        float yawDelta = Math.abs(MathHelper.wrapDegrees(activeLaunch.yaw() - client.player.getYaw()));
        float pitchDelta = Math.abs(activeLaunch.pitch() - client.player.getPitch());
        float speed = MathHelper.clamp(Math.max(yawDelta, pitchDelta) / 0.06f, MIN_ROTATION_SPEED, MAX_ROTATION_SPEED);
        rotations().aim(this, RotationPriority.HIGH, point, RotationSettings.uniform(RotationVisibility.VISIBLE, AimTransition.EASED, speed));
    }

    @Subscribe
    private void onPacketSend(PacketSendEvent event) {
        if (client.player == null) return;
        if (event.getPacket() instanceof PlayerMoveC2SPacket movement) {
            if (movement.changesPosition()) {
                Vec3d position = new Vec3d(movement.getX(0.0), movement.getY(0.0), movement.getZ(0.0));
                sentVelocity = lastSentPosition == null ? client.player.getVelocity() : position.subtract(lastSentPosition);
                lastSentPosition = position;
            } else {
                sentVelocity = Vec3d.ZERO;
            }
            sentOnGround = movement.isOnGround();
            sentMovementThisTick = true;
        } else if (event.getPacket() instanceof ClientTickEndC2SPacket) {
            if (!sentMovementThisTick) sentVelocity = Vec3d.ZERO;
            sentMovementThisTick = false;
            if (phase == PearlGrapplePhase.AIM && isValidTarget(target)) {
                targetMotion = estimateMotion(target);
                targetPath = buildTargetPath(target, targetMotion);
                bestLaunch = MovingTargetTrajectorySearch.findLaunch(targetPath);
                pathUpdatedPlayerTick = client.player.age;
            }
        } else if (event.getPacket() instanceof TeleportConfirmC2SPacket) {
            lastSentPosition = null;
            sentVelocity = Vec3d.ZERO;
        }
    }

    private TargetPath buildTargetPath(PlayerEntity player, MovingTargetTrajectorySearch.Result motion) {
        Vec3d origin = lastSentPosition != null ? lastSentPosition : client.player.getEntityPos();
        Vec3d eye = origin.add(0.0, currentPoseEyeHeight() - EYE_OFFSET, 0.0);
        Vec3d inherited = new Vec3d(sentVelocity.x, sentOnGround ? 0.0 : sentVelocity.y, sentVelocity.z);
        int ticks = maxPredictTicks.getValue();
        double age = predictionAge(motion);
        Box[] boxes = MovingTargetTrajectorySearch.predictBoxes(player, motion, age, ticks);
        return new TargetPath(eye, inherited, boxes, blockers(player));
    }

    private static MovingTargetTrajectorySearch.Result estimateMotion(PlayerEntity player) {
        int tick = EntityPositionTracker.currentTick();
        VelocitySample velocity = EntityPositionTracker.velocity(player);
        List<PositionSample> history = EntityPositionTracker.positions(player);
        return MovingTargetTrajectorySearch.estimateMotion(player, history, velocity, tick);
    }

    private static double predictionAge(MovingTargetTrajectorySearch.Result motion) {
        return (double) (EntityPositionTracker.currentTick() + 1 - motion.tick()) + (double) LatencyTracker.milliseconds() / 50.0;
    }

    private List<Box> blockers(PlayerEntity player) {
        return client.world.getOtherEntities(client.player, client.player.getBoundingBox().expand(targetDistance()), entity -> entity != player && entity.canBeHitByProjectile() && !(entity instanceof ProjectileEntity))
            .stream().map(Entity::getBoundingBox).toList();
    }

    private PlayerEntity selectTarget() {
        PlayerEntity best = null;
        double bestFraction = -1.0;
        double bestDistance = Double.MAX_VALUE;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (!isValidTarget(player)) continue;
            double playerDistance = client.player.distanceTo(player);
            MovingTargetTrajectorySearch.Result motion = estimateMotion(player);
            TargetPath path = buildTargetPath(player, motion);
            MovingTargetLaunch launch = MovingTargetTrajectorySearch.findLaunch(path);
            double fraction = launch == null ? 0.0 : launch.hitFraction();
            if (fraction > bestFraction || (fraction == bestFraction && playerDistance < bestDistance)) {
                best = player;
                bestFraction = fraction;
                bestDistance = playerDistance;
            }
        }
        return best;
    }

    private boolean isValidTarget(PlayerEntity player) {
        return player != null && player != client.player && TargetSelection.isValidTarget(player)
            && client.player.distanceTo(player) <= targetDistance() && !player.hasVehicle()
            && !player.isUsingRiptide() && !recentlyTeleported(player);
    }

    private static boolean recentlyTeleported(PlayerEntity player) {
        List<PositionSample> history = EntityPositionTracker.positions(player);
        for (int i = 1; i < history.size(); i++) {
            if (EntityPositionTracker.currentTick() - history.get(i).tick() > TELEPORT_SAMPLE_TICKS) continue;
            if (history.get(i).position().distanceTo(history.get(i - 1).position()) > TELEPORT_DISTANCE) return true;
        }
        return false;
    }

    private static float targetDistance() { return Math.min(distance.getValue(), MAX_TARGET_DISTANCE); }

    private static void initializePearlPlayerType() {
    }

    private static void initializePearlClientType() {
    }

    private static void initializePearlVectorType() {
    }

    private static void initializePearlRotationType() {
    }

    public static void appendBootstrapSettings(PearlGrapple Module) {
        CatalogSettings.append(Module, mode);
        CatalogSettings.append(Module, grappleKey);
        CatalogSettings.append(Module, distance);
        CatalogSettings.append(Module, maxPredictTicks);
        CatalogSettings.append(Module, silentRotations);
        CatalogSettings.append(Module, followOnKey);
        CatalogSettings.append(Module, followKey);
        CatalogSettings.append(Module, followRange);
        CatalogSettings.append(Module, followDelay);
    }

    public static void assignBootstrapSettings(PearlGrapple Module, IntSetting preparedMaxPredictTicks, FloatSetting preparedDistance, BooleanSetting preparedSilentRotations) {
        maxPredictTicks = preparedMaxPredictTicks;
        distance = preparedDistance;
        silentRotations = preparedSilentRotations;
    }
}
