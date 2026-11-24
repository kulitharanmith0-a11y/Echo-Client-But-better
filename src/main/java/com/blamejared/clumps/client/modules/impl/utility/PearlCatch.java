package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.event.network.PacketSendEvent;
import com.blamejared.clumps.client.modules.impl.movement.ThrowInventoryOperations;
import com.blamejared.clumps.client.modules.impl.utility.PearlCatchCalculations;
import com.blamejared.clumps.client.modules.impl.utility.PearlCatchInstantPhase;
import com.blamejared.clumps.client.modules.impl.utility.PearlCatchMode;
import com.blamejared.clumps.client.modules.impl.utility.PearlCatchStage;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationScope;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.mixin.accessor.MinecraftClientAccessor;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.utils.WeakRefSet;
import com.blamejared.clumps.client.utils.Rotation;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.trajectory.LaunchCandidate;
import com.blamejared.clumps.client.utils.trajectory.LaunchTrajectoryMath;
import com.blamejared.clumps.client.utils.trajectory.LaunchTrajectorySearch;
import com.blamejared.clumps.client.utils.trajectory.ProjectileState;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PearlCatch extends Module {
    private static final double LAUNCH_HEIGHT_OFFSET = 0.1;
    private static final double SPAWN_MATCH_DISTANCE = 1.5;
    private static final double REQUIRED_HIT_FRACTION = 0.72;
    private static final double RELAXED_HIT_FRACTION = 0.50;
    private static final double LAST_CHANCE_HIT_FRACTION = 0.25;
    private static final int SPAWN_RECONCILE_WAIT_TICKS = 2;
    private static final int AIM_WAIT_TICKS = 8;
    private static final int RETRY_DELAY_TICKS = 10;
    private static final int MAX_RETRIES = 2;
    private static final int CONFIRM_GRACE_TICKS = 3;
    private static final float MIN_AIM_SPEED = 50.0F;
    private static final float MAX_AIM_SPEED = 420.0F;
    private static final float IMMEDIATE_AIM_SPEED = 1000.0F;
    private static final double VISIBLE_AIM_TOLERANCE = 0.06;
    private static final double AIM_DISTANCE = 100.0;
    private static final String HIGH_DELAY_TOOLTIP = "Timing";
    private static final String LEGIT_TOOLTIP = "Legit Mode";

    private static BooleanSetting autoAim;
    private static BooleanSetting silentAim;
    private static FloatRangeSetting highDelay;
    private static BooleanSetting retry;
    private static BooleanSetting switchBack;
    private static KeybindSetting highPearlCatch;
    private static KeybindSetting lowPearlCatch;
    private static KeybindSetting instantPearlCatch;
    private static final BooleanSetting legitMode = new BooleanSetting(CatalogText.literal("Legit Mode"), false);
    private static final FloatRangeSetting legitDelay = new FloatRangeSetting(CatalogText.literal("Legit Delay"), 1.0F, 2.0F, 1.0F, 10.0F, 1.0F, CatalogText.literal(" ticks"));
    private static final BooleanSetting legitAutoAim = new BooleanSetting(CatalogText.literal("Legit Auto Aim"), true);

    private PearlCatchStage normalStage;
    private PearlCatchInstantPhase instantPhase;
    private boolean previousHighPress;
    private boolean previousLowPress;
    private boolean previousInstantPress;
    private boolean legitSequenceActive;
    private boolean legitConsumedUntilRelease;
    private int normalStageTicks;
    private int pearlThrowTick;
    private int scheduledCatchTick;
    private int catchDelayTicks;
    private boolean lowTrajectory;
    private int pearlFlightTicks;
    private ProjectileState trajectory;
    private LaunchCandidate pendingPlan;
    private LaunchCandidate activePlan;
    private Vec3d planningOrigin;
    private Vec3d planningVelocity;
    private int planningElapsedTicks;
    private int plannedPlayerTick;
    private volatile boolean awaitingPearlSpawn;
    private volatile Vec3d pearlSpawnPosition;
    private volatile Vec3d pearlSpawnVelocity;
    private volatile int pearlSpawnPlayerAge;
    private volatile int pearlEntityId;
    private volatile boolean pearlDestroyed;
    private volatile boolean awaitingWindChargeSpawn;
    private volatile Vec3d windChargeSpawnPosition;
    private volatile Vec3d windChargeSpawnVelocity;
    private volatile boolean positionCorrectionReceived;
    private LaunchCandidate thrownPlan;
    private int windChargeThrowTick;
    private int retryCount;
    private boolean retryPending;
    private Vec3d capturedOrigin;
    private Vec3d capturedVelocity;
    private boolean capturedOnGround;
    private boolean movementPacketSent;
    private int originalSlot;
    private int windChargeSlot;
    private int pearlSlot;
    private boolean movedWindChargeToOffhand;
    private int instantPhaseTick;

    public PearlCatch() {
        super(new ModuleInfo(CatalogText.literal("Pearl Catch"), CatalogText.literal("Automatically catches ender pearls thrown by the player"), ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        normalStage = PearlCatchStage.IDLE;
        instantPhase = PearlCatchInstantPhase.IDLE;
        previousHighPress = false;
        previousLowPress = false;
        previousInstantPress = false;
        normalStageTicks = 0;
        pearlThrowTick = -1;
        scheduledCatchTick = -1;
        catchDelayTicks = 1;
        lowTrajectory = false;
        pearlFlightTicks = 0;
        plannedPlayerTick = -1;
        awaitingPearlSpawn = false;
        pearlSpawnPlayerAge = -1;
        pearlEntityId = -1;
        pearlDestroyed = false;
        awaitingWindChargeSpawn = false;
        positionCorrectionReceived = false;
        windChargeThrowTick = -1;
        retryCount = 0;
        retryPending = false;
        capturedVelocity = Vec3d.ZERO;
        capturedOnGround = true;
        movementPacketSent = false;
        originalSlot = -1;
        windChargeSlot = -1;
        pearlSlot = -1;
        movedWindChargeToOffhand = false;
        instantPhaseTick = -1;
        legitSequenceActive = false;
        legitConsumedUntilRelease = false;
        silentAim.setVisibilityPredicate(ignored -> autoAim.getValue());
        retry.setVisibilityPredicate(ignored -> autoAim.getValue());
        highDelay.tooltip(HIGH_DELAY_TOOLTIP);
        legitDelay.setVisibilityPredicate(ignored -> legitMode.getValue());
        legitAutoAim.setVisibilityPredicate(ignored -> legitMode.getValue());
        legitMode.tooltip(LEGIT_TOOLTIP);
        legitDelay.tooltip(LEGIT_TOOLTIP);
        legitAutoAim.tooltip(LEGIT_TOOLTIP);
        legitMode.description(CatalogText.literal("Hold Right Click while holding an Ender Pearl to throw the pearl then automatically throw a Wind Charge to catch it"));
        legitDelay.description(CatalogText.literal("Ticks after throwing the pearl before throwing the Wind Charge in Legit Mode"));
        legitAutoAim.description(CatalogText.literal("Smoothly aims visibly onto the pearl before throwing the Wind Charge in Legit Mode"));
    }

    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (isOutsideWorld() || client.currentScreen != null) return;
        boolean useHeld = isUseKeyHeld();
        if (!useHeld) {
            legitConsumedUntilRelease = false;
            if (legitSequenceActive && normalStage == PearlCatchStage.WAIT) {
                resetNormalSequence(switchBack.getValue());
            }
        }
        handleKeyPresses();
        if (instantPhase != PearlCatchInstantPhase.IDLE) {
            advanceInstantSequence();
            return;
        }
        switch (normalStage) {
            case THROW_PEARL -> handlePearlThrow();
            case WAIT -> handleWait();
            case CONFIRM -> handleConfirm();
            case SWITCH_BACK -> handleSwitchBack();
        }
    }

    @Subscribe
    public void onPacketSent(PacketSendEvent event) {
        if (client.player == null) return;
        if (event.getPacket() instanceof PlayerMoveC2SPacket movement) {
            if (movement.changesPosition()) {
                Vec3d position = new Vec3d(movement.getX(0.0), movement.getY(0.0), movement.getZ(0.0));
                capturedVelocity = capturedOrigin == null ? Vec3d.ZERO : position.subtract(capturedOrigin);
                capturedOrigin = position;
            } else {
                capturedVelocity = Vec3d.ZERO;
            }
            capturedOnGround = movement.isOnGround();
            movementPacketSent = true;
            return;
        }
        if (event.getPacket() instanceof ClientTickEndC2SPacket) {
            if (!movementPacketSent) capturedVelocity = Vec3d.ZERO;
            movementPacketSent = false;
            if (needsCatchPlan() && client.player.age >= scheduledCatchTick) {
                prepareCatchPlan();
                return;
            }
            if (needsCatchPlan() && usesVisibleAim()) activePlan = planForTick(scheduledCatchTick);
            return;
        }
        if (event.getPacket() instanceof TeleportConfirmC2SPacket) {
            capturedOrigin = null;
            capturedVelocity = Vec3d.ZERO;
            return;
        }
        if (event.getPacket() instanceof PlayerInteractItemC2SPacket interact
                && legitMode.getValue()
                && !legitConsumedUntilRelease
                && normalStage == PearlCatchStage.IDLE
                && instantPhase == PearlCatchInstantPhase.IDLE
                && isUseKeyHeld()
                && client.currentScreen == null) {
            ItemStack used = interact.getHand() == Hand.OFF_HAND
                    ? client.player.getOffHandStack()
                    : client.player.getMainHandStack();
            if (used.isOf(Items.ENDER_PEARL)
                    && !client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE))) {
                int windCharge = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
                if (windCharge != -1) {
                    beginLegitCatch();
                }
            }
        }
    }

    @Subscribe
    public void onPacketReceived(PacketReceiveEvent event) {
        if (normalStage == PearlCatchStage.IDLE && instantPhase == PearlCatchInstantPhase.IDLE) return;
        observeIncoming(event.getPacket());
    }

    private void observeIncoming(Packet<?> packet) {
        if (packet instanceof BundleS2CPacket bundle) {
            for (Packet<?> inner : bundle.getPackets()) observeIncoming(inner);
            return;
        }
        if (packet instanceof PlayerPositionLookS2CPacket) {
            positionCorrectionReceived = true;
            return;
        }
        if (packet instanceof EntitiesDestroyS2CPacket destroyed) {
            if (pearlEntityId != -1 && destroyed.getEntityIds().contains(pearlEntityId)) pearlDestroyed = true;
            return;
        }
        observeSpawn(packet);
    }

    private void observeSpawn(Packet<?> packet) {
        if (!(packet instanceof EntitySpawnS2CPacket spawn)) return;
        if (client.player == null || spawn.getEntityData() != client.player.getId()) return;
        Vec3d position = new Vec3d(spawn.getX(), spawn.getY(), spawn.getZ());
        if (awaitingPearlSpawn && spawn.getEntityType() == EntityType.ENDER_PEARL) {
            pearlSpawnPosition = position;
            pearlSpawnVelocity = spawn.getVelocity();
            pearlSpawnPlayerAge = client.player.age;
            pearlEntityId = spawn.getEntityId();
            awaitingPearlSpawn = false;
        } else if (awaitingWindChargeSpawn && spawn.getEntityType() == EntityType.WIND_CHARGE) {
            windChargeSpawnPosition = position;
            windChargeSpawnVelocity = spawn.getVelocity();
            awaitingWindChargeSpawn = false;
        }
    }

    @Subscribe
    public void afterMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld() || !usesVisibleAim() || !needsCatchPlan() || activePlan == null) return;
        Vec3d target = client.player.getEyePos().add(
                LaunchTrajectoryMath.direction(activePlan.yaw(), activePlan.pitch()).multiply(AIM_DISTANCE));
        boolean immediate = !retryPending && scheduledCatchTick - pearlThrowTick <= 1
                && scheduledCatchTick - client.player.age <= 1;
        RotationSettings settings = immediate
                ? RotationSettings.uniform(RotationVisibility.VISIBLE, AimTransition.EASED, IMMEDIATE_AIM_SPEED)
                : RotationSettings.uniform(RotationVisibility.VISIBLE, AimTransition.NATURAL, aimSpeed());
        rotations().aim(this, RotationPriority.HIGH, target, settings);
    }

    private float aimSpeed() {
        return PearlCatchCalculations.aimSpeed(activePlan.yaw(), activePlan.pitch(), client.player.getYaw(), client.player.getPitch(), scheduledCatchTick, client.player.age);
    }

    private boolean needsCatchPlan() {
        return normalStage == PearlCatchStage.WAIT
                || normalStage == PearlCatchStage.CONFIRM && retryPending
                || instantPhase == PearlCatchInstantPhase.THROW_FOLLOW_UP;
    }

    private boolean usesAutoAim() {
        return legitSequenceActive ? legitAutoAim.getValue() : autoAim.getValue();
    }

    private boolean usesSilentAim() {
        return !legitSequenceActive && silentAim.getValue();
    }

    private boolean usesVisibleAim() { return usesAutoAim() && !usesSilentAim(); }

    private boolean isUseKeyHeld() {
        return client.options != null
                && (KeybindingCache.isPressed(client.options.useKey) || client.options.useKey.isPressed());
    }

    private void beginLegitCatch() {
        Vec3d origin = sequenceOrigin();
        Vec3d velocity = sequenceVelocity();
        Rotation rotation = rotations().effectiveRotation();
        pearlSpawnPosition = null;
        pearlSpawnVelocity = null;
        pearlSpawnPlayerAge = -1;
        pearlEntityId = -1;
        pearlDestroyed = false;
        positionCorrectionReceived = false;
        retryCount = 0;
        awaitingPearlSpawn = true;
        float eyeHeight = currentEyeHeight();
        Vec3d launchPosition = new Vec3d(origin.x, origin.y + eyeHeight - LAUNCH_HEIGHT_OFFSET, origin.z);
        trajectory = new ProjectileState(launchPosition, LaunchTrajectoryMath.direction(rotation.yaw(), rotation.pitch()).multiply(1.5).add(velocity), false);
        pearlFlightTicks = LaunchTrajectorySearch.flightTicks(trajectory);
        pearlThrowTick = client.player.age;
        catchDelayTicks = Math.max(1, Math.round(legitDelay.sampleGaussian()));
        scheduledCatchTick = pearlThrowTick + catchDelayTicks;
        lowTrajectory = false;
        legitSequenceActive = true;
        legitConsumedUntilRelease = true;
        normalStage = PearlCatchStage.WAIT;
        normalStageTicks = 0;
    }

    @Override
    public void onDisable() {
        WeakRefSet.remove(this);
        legitConsumedUntilRelease = false;
        resetNormalSequence(false);
        resetInstantSequence();
        capturedOrigin = null;
        capturedVelocity = Vec3d.ZERO;
        super.onDisable();
    }

    private void handleKeyPresses() {
        boolean highPressed = highPearlCatch.getKey().isPressed();
        boolean lowPressed = lowPearlCatch.getKey().isPressed();
        boolean instantPressed = instantPearlCatch.getKey().isPressed();
        if (highPressed && !previousHighPress) startNormalSequence(PearlCatchMode.HIGH);
        if (lowPressed && !previousLowPress) startNormalSequence(PearlCatchMode.LOW);
        if (instantPressed && !previousInstantPress) startInstantSequence();
        previousHighPress = highPressed;
        previousLowPress = lowPressed;
        previousInstantPress = instantPressed;
    }

    private void startNormalSequence(PearlCatchMode mode) {
        if (normalStage != PearlCatchStage.IDLE || instantPhase != PearlCatchInstantPhase.IDLE) return;
        if (isThrowCoolingDown()) return;
        int pearl = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (pearl == -1) return;
        int windCharge = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (windCharge == -1) return;
        if (!DeferredActionQueue.ensureSelected(this, pearl, -1, false)) return;
        catchDelayTicks = mode == PearlCatchMode.HIGH ? Math.max(1, Math.round(highDelay.sampleGaussian())) : 1;
        lowTrajectory = mode == PearlCatchMode.LOW;
        normalStage = PearlCatchStage.THROW_PEARL;
        normalStageTicks = 0;
    }

    private void handlePearlThrow() {
        int pearl = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (pearl == -1 || !DeferredActionQueue.ensureSelected(this, pearl, -1, false)) {
            resetNormalSequence(false);
            return;
        }
        Vec3d origin = sequenceOrigin();
        Vec3d velocity = sequenceVelocity();
        Rotation rotation = rotations().effectiveRotation();
        pearlSpawnPosition = null;
        pearlSpawnVelocity = null;
        pearlSpawnPlayerAge = -1;
        pearlEntityId = -1;
        pearlDestroyed = false;
        positionCorrectionReceived = false;
        retryCount = 0;
        awaitingPearlSpawn = true;
        actions().executeDoItemUse(this);
        if (!client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))) {
            resetNormalSequence(true);
            return;
        }
        float eyeHeight = currentEyeHeight();
        Vec3d launchPosition = new Vec3d(origin.x, origin.y + eyeHeight - LAUNCH_HEIGHT_OFFSET, origin.z);
        trajectory = new ProjectileState(launchPosition, LaunchTrajectoryMath.direction(rotation.yaw(), rotation.pitch()).multiply(1.5).add(velocity), false);
        pearlFlightTicks = LaunchTrajectorySearch.flightTicks(trajectory);
        pearlThrowTick = client.player.age;
        scheduledCatchTick = pearlThrowTick + catchDelayTicks;
        normalStage = PearlCatchStage.WAIT;
    }

    private void prepareCatchPlan() {
        reconcilePearlSpawn();
        Vec3d origin = sequenceOrigin();
        planningOrigin = new Vec3d(origin.x, origin.y + currentEyeHeight(), origin.z);
        planningVelocity = sequenceVelocity();
        planningElapsedTicks = client.player.age - pearlThrowTick;
        pendingPlan = usesAutoAim() ? LaunchTrajectorySearch.findCatch(trajectory, planningOrigin, planningVelocity, planningElapsedTicks, pearlFlightTicks, lowTrajectory) : null;
        plannedPlayerTick = client.player.age;
        if (pendingPlan != null) activePlan = pendingPlan;
    }

    private LaunchCandidate planForTick(int tick) {
        reconcilePearlSpawn();
        Vec3d origin = sequenceOrigin();
        Vec3d velocity = sequenceVelocity();
        int ticksAhead = Math.max(0, tick - client.player.age);
        double px = origin.x;
        double py = origin.y + currentEyeHeight();
        double pz = origin.z;
        double vx = velocity.x;
        double vy = velocity.y;
        double vz = velocity.z;
        for (int i = 0; i < ticksAhead; i++) {
            px += vx;
            py += vy;
            pz += vz;
            if (!capturedOnGround) {
                vy = (vy - 0.08) * 0.98;
            }
        }
        Vec3d projectedEye = new Vec3d(px, py, pz);
        Vec3d projectedVel = new Vec3d(vx, capturedOnGround ? 0.0 : vy, vz);
        LaunchCandidate plan = LaunchTrajectorySearch.findCatch(trajectory, projectedEye, projectedVel, tick - pearlThrowTick, pearlFlightTicks, lowTrajectory);
        return plan != null ? plan : activePlan;
    }

    private LaunchCandidate usablePlan(int lateTicks, boolean lastChance) {
        LaunchCandidate plan = pendingPlan;
        if (plan == null) return null;
        double required = lastChance
                ? LAST_CHANCE_HIT_FRACTION
                : (lateTicks >= 3 ? RELAXED_HIT_FRACTION : REQUIRED_HIT_FRACTION);
        if (!usesVisibleAim()) {
            if (plan.hitFraction() < required) return null;
            return plan;
        }
        float yaw = client.player.getYaw();
        float pitch = client.player.getPitch();
        double hitFraction = LaunchTrajectorySearch.hitFraction(trajectory, planningOrigin, planningVelocity, planningElapsedTicks, yaw, pitch, pearlFlightTicks - 1);
        double visibleRequired = lastChance
                ? LAST_CHANCE_HIT_FRACTION
                : Math.max(required, plan.hitFraction() - VISIBLE_AIM_TOLERANCE);
        if (hitFraction < visibleRequired) return null;
        return new LaunchCandidate(yaw, pitch, hitFraction, hitFraction, plan.meanHitTick());
    }

    private void useWithPlan(LaunchCandidate plan) {
        if (!usesSilentAim()) {
            actions().executeDoItemUse(this);
            return;
        }
        rotations().override(this, RotationPriority.HIGH, new Rotation(plan.yaw(), plan.pitch()))
                .ifPresent(RotationScope::commit);
        useIgnoringCrosshair();
    }

    private void handleWait() {
        int slot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (slot == -1) {
            resetNormalSequence(true);
            return;
        }
        if (client.player.age < scheduledCatchTick || plannedPlayerTick != client.player.age) return;
        int lateTicks = client.player.age - scheduledCatchTick;
        if (!usesAutoAim()) {
            throwWindCharge(slot, null);
            return;
        }
        // On delayed/high catches, wait up to 2 ticks for exact server pearl velocity reconciliation
        // so the solver uses the exact server spread instead of estimating.
        if (!lowTrajectory && trajectory != null && !trajectory.exactVelocity()
                && lateTicks < SPAWN_RECONCILE_WAIT_TICKS
                && (client.player.age - pearlThrowTick) < Math.max(4, pearlFlightTicks - 4)) {
            return;
        }
        LaunchCandidate plan = usablePlan(lateTicks, lateTicks >= AIM_WAIT_TICKS);
        if (plan != null) throwWindCharge(slot, plan);
        else if (lateTicks >= AIM_WAIT_TICKS || client.player.age - pearlThrowTick >= pearlFlightTicks)
            resetNormalSequence(true);
    }

    private void handleConfirm() {
        int tick = client.player.age;
        if (positionCorrectionReceived || pearlDestroyed || tick - pearlThrowTick >= pearlFlightTicks) {
            resetNormalSequence(switchBack.getValue());
            return;
        }
        if (switchBack.getValue() && !retryPending && tick > windChargeThrowTick
                && DeferredActionQueue.contains(this)) DeferredActionQueue.finish(this, true);
        boolean missed = false;
        Vec3d position = windChargeSpawnPosition;
        Vec3d velocity = windChargeSpawnVelocity;
        if (position != null && velocity != null) {
            int delay = windChargeThrowTick - pearlThrowTick;
            missed = LaunchTrajectorySearch.interceptTick(trajectory, position, velocity, delay, pearlFlightTicks - 1) < 0;
        }
        int latencyTicks = pearlSpawnPlayerAge >= 0 ? Math.max(0, pearlSpawnPlayerAge - pearlThrowTick) : 2;
        boolean expired = tick > pearlThrowTick + thrownPlan.meanHitTick() + latencyTicks + CONFIRM_GRACE_TICKS;
        if (!missed && !expired) return;
        retryPending = true;
        if (!retry.getValue() || retryCount >= MAX_RETRIES) {
            resetNormalSequence(switchBack.getValue());
            return;
        }
        if (plannedPlayerTick != tick) return;
        if (client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE))) return;
        int slot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        LaunchCandidate plan = usablePlan(AIM_WAIT_TICKS, true);
        if (slot == -1 || plan == null) {
            if (tick - windChargeThrowTick > 18) resetNormalSequence(switchBack.getValue());
            return;
        }
        retryCount++;
        throwWindCharge(slot, plan);
    }

    private void reconcilePearlSpawn() {
        Vec3d position = pearlSpawnPosition;
        Vec3d velocity = pearlSpawnVelocity;
        if (trajectory == null || trajectory.exactVelocity() || position == null || velocity == null) return;
        if (position.distanceTo(trajectory.position()) > SPAWN_MATCH_DISTANCE) return;
        trajectory = new ProjectileState(position, velocity, true);
        pearlFlightTicks = LaunchTrajectorySearch.flightTicks(trajectory);
    }

    private float currentEyeHeight() {
        return client.player == null ? 1.62F : client.player.getEyeHeight(client.player.getPose());
    }

    private void throwWindCharge(int slot, LaunchCandidate plan) {
        if (!ensureWindChargeSlot(slot)) {
            resetNormalSequence(true);
            return;
        }
        windChargeSpawnPosition = null;
        windChargeSpawnVelocity = null;
        awaitingWindChargeSpawn = plan != null;
        if (plan != null) useWithPlan(plan);
        else actions().executeDoItemUse(this);
        activePlan = null;
        if (plan != null) {
            thrownPlan = plan;
            retryPending = false;
            windChargeThrowTick = client.player.age;
            scheduledCatchTick = windChargeThrowTick + RETRY_DELAY_TICKS;
            normalStage = PearlCatchStage.CONFIRM;
        } else if (switchBack.getValue()) {
            normalStage = PearlCatchStage.SWITCH_BACK;
            normalStageTicks = 0;
        } else resetNormalSequence(false);
    }

    private void useIgnoringCrosshair() {
        HitResult previousHit = client.crosshairTarget;
        Vec3d eye = client.player.getEyePos();
        client.crosshairTarget = BlockHitResult.createMissed(eye, Direction.UP, client.player.getBlockPos());
        actions().executeDoItemUse(this);
        client.crosshairTarget = previousHit;
    }

    private void handleSwitchBack() {
        normalStageTicks++;
        if (normalStageTicks < 1) return;
        resetNormalSequence(switchBack.getValue());
    }

    private boolean ensureWindChargeSlot(int slot) {
        if (DeferredActionQueue.selectedSlot(this) == slot) return true;
        if (DeferredActionQueue.select(this, slot, -1, false)) return true;
        return client.player.getInventory().getSelectedSlot() == slot;
    }

    private Vec3d sequenceOrigin() {
        return capturedOrigin != null ? capturedOrigin : client.player.getEntityPos();
    }

    private Vec3d sequenceVelocity() {
        return PearlCatchCalculations.restoredVelocity(capturedVelocity, capturedOnGround);
    }

    private void resetNormalSequence(boolean restore) {
        DeferredActionQueue.finish(this, restore);
        normalStage = PearlCatchStage.IDLE;
        normalStageTicks = 0;
        legitSequenceActive = false;
        pearlThrowTick = -1;
        scheduledCatchTick = -1;
        catchDelayTicks = 1;
        lowTrajectory = false;
        trajectory = null;
        pendingPlan = null;
        activePlan = null;
        plannedPlayerTick = -1;
        awaitingPearlSpawn = false;
        awaitingWindChargeSpawn = false;
        thrownPlan = null;
        windChargeThrowTick = -1;
        retryCount = 0;
        retryPending = false;
    }

    private void startInstantSequence() {
        if (normalStage != PearlCatchStage.IDLE || instantPhase != PearlCatchInstantPhase.IDLE) return;
        if (isThrowCoolingDown()) return;
        if (client.interactionManager == null || client.player.isSpectator()) return;
        int selected = client.player.getInventory().getSelectedSlot();
        int pearl = client.player.getMainHandStack().isOf(Items.ENDER_PEARL) ? selected
                : InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (pearl == -1) return;
        boolean swap = !client.player.getOffHandStack().isOf(Items.WIND_CHARGE);
        int windCharge = swap ? InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE) : -1;
        if (swap && windCharge == -1) return;
        WeakRefSet.add(this);
        originalSlot = selected;
        windChargeSlot = windCharge;
        pearlSlot = pearl;
        movedWindChargeToOffhand = swap;
        if (swap) enterInstantPhase(PearlCatchInstantPhase.SELECT_WIND_CHARGE);
        else if (pearl != selected) enterInstantPhase(PearlCatchInstantPhase.SELECT_PEARL);
        else enterInstantPhase(PearlCatchInstantPhase.THROW);
        instantPhaseTick = client.player.age;
        advanceInstantSequence();
    }

    private void advanceInstantSequence() {
        if (client.player.age < instantPhaseTick) return;
        switch (instantPhase) {
            case SELECT_WIND_CHARGE -> {
                InventoryItems.selectHotbarSlot(windChargeSlot);
                enterInstantPhase(PearlCatchInstantPhase.SWAP_OFFHAND);
            }
            case SWAP_OFFHAND -> {
                swapHands();
                enterInstantPhase(PearlCatchInstantPhase.SELECT_PEARL);
            }
            case SELECT_PEARL -> {
                InventoryItems.selectHotbarSlot(pearlSlot);
                enterInstantPhase(PearlCatchInstantPhase.THROW);
            }
            case THROW -> throwInstant();
            case THROW_FOLLOW_UP -> throwInstantFollowUp();
            case RESTORE_SELECT_WIND_CHARGE -> {
                InventoryItems.selectHotbarSlot(windChargeSlot);
                enterInstantPhase(PearlCatchInstantPhase.RESTORE_SWAP_OFFHAND);
            }
            case RESTORE_SWAP_OFFHAND -> {
                swapHands();
                enterInstantPhase(PearlCatchInstantPhase.RESTORE_SLOT);
            }
            case RESTORE_SLOT -> {
                InventoryItems.selectHotbarSlot(originalSlot);
                resetInstantSequence();
            }
            default -> resetInstantSequence();
        }
    }

    private void throwInstant() {
        if (!client.player.getMainHandStack().isOf(Items.ENDER_PEARL)
                || !client.player.getOffHandStack().isOf(Items.WIND_CHARGE) || isThrowCoolingDown()) {
            beginInstantRestoration();
            return;
        }
        Vec3d origin = sequenceOrigin();
        Vec3d velocity = sequenceVelocity();
        Rotation rotation = rotations().effectiveRotation();
        float eyeHeight = currentEyeHeight();
        Vec3d launchPosition = new Vec3d(origin.x, origin.y + eyeHeight - LAUNCH_HEIGHT_OFFSET, origin.z);
        ProjectileState launched = new ProjectileState(launchPosition, LaunchTrajectoryMath.direction(rotation.yaw(), rotation.pitch()).multiply(1.5).add(velocity), false);
        int flightTicks = LaunchTrajectorySearch.flightTicks(launched);
        Vec3d eye = new Vec3d(origin.x, origin.y + eyeHeight, origin.z);
        double hitFraction = LaunchTrajectorySearch.hitFraction(launched, eye, velocity, 0, rotation.yaw(), rotation.pitch(), flightTicks - 1);
        if (!autoAim.getValue() || hitFraction >= REQUIRED_HIT_FRACTION) {
            actions().executeDoItemUse(this);
            actions().executeDoItemUse(this);
            usePendingOffhand();
            beginInstantRestoration();
            return;
        }
        pearlSpawnPosition = null;
        pearlSpawnVelocity = null;
        pearlSpawnPlayerAge = -1;
        awaitingPearlSpawn = true;
        actions().executeDoItemUse(this);
        if (!client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))) {
            beginInstantRestoration();
            return;
        }
        trajectory = launched;
        pearlFlightTicks = flightTicks;
        lowTrajectory = true;
        pearlThrowTick = client.player.age;
        scheduledCatchTick = pearlThrowTick + 1;
        enterInstantPhase(PearlCatchInstantPhase.THROW_FOLLOW_UP);
    }

    private void throwInstantFollowUp() {
        int tick = client.player.age;
        if (plannedPlayerTick != tick) return;
        int lateTicks = tick - scheduledCatchTick;
        LaunchCandidate plan = usablePlan(lateTicks, lateTicks >= AIM_WAIT_TICKS);
        if (plan != null && client.player.getOffHandStack().isOf(Items.WIND_CHARGE)) {
            useWithPlan(plan);
            usePendingOffhand();
            finishInstantFollowUp();
        } else if (tick - scheduledCatchTick >= AIM_WAIT_TICKS || tick - pearlThrowTick >= pearlFlightTicks) {
            finishInstantFollowUp();
        }
    }

    private void finishInstantFollowUp() {
        trajectory = null;
        pendingPlan = null;
        activePlan = null;
        plannedPlayerTick = -1;
        awaitingPearlSpawn = false;
        beginInstantRestoration();
    }

    private void usePendingOffhand() { PearlCatchCalculations.usePendingOffhandWindCharge(client, this); }

    private void beginInstantRestoration() {
        if (movedWindChargeToOffhand) enterInstantPhase(PearlCatchInstantPhase.RESTORE_SELECT_WIND_CHARGE);
        else if (originalSlot != client.player.getInventory().getSelectedSlot())
            enterInstantPhase(PearlCatchInstantPhase.RESTORE_SLOT);
        else resetInstantSequence();
    }

    private void enterInstantPhase(PearlCatchInstantPhase phase) {
        instantPhase = phase;
        instantPhaseTick = client.player.age + 1;
    }

    private void swapHands() { ThrowInventoryOperations.swapHands(client); }

    private boolean isThrowCoolingDown() {
        return client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))
                || client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE));
    }

    private void resetInstantSequence() {
        WeakRefSet.remove(this);
        instantPhase = PearlCatchInstantPhase.IDLE;
        instantPhaseTick = -1;
        originalSlot = -1;
        windChargeSlot = -1;
        pearlSlot = -1;
        movedWindChargeToOffhand = false;
    }

    
    public static void appendBootstrapSettings(PearlCatch Module) {
        CatalogSettings.append(Module, autoAim);
        CatalogSettings.append(Module, silentAim);
        CatalogSettings.append(Module, highDelay);
        CatalogSettings.append(Module, retry);
        CatalogSettings.append(Module, switchBack);
        CatalogSettings.append(Module, highPearlCatch);
        CatalogSettings.append(Module, lowPearlCatch);
        CatalogSettings.append(Module, instantPearlCatch);
        CatalogSettings.append(Module, legitMode);
        CatalogSettings.append(Module, legitDelay);
        CatalogSettings.append(Module, legitAutoAim);
    }

    
    public static void assignBootstrapSettings(PearlCatch Module, KeybindSetting preparedLowPearlCatch, BooleanSetting preparedAutoAim, KeybindSetting preparedInstantPearlCatch, FloatRangeSetting preparedHighDelay, BooleanSetting preparedSwitchBack, BooleanSetting preparedRetry, KeybindSetting preparedHighPearlCatch, BooleanSetting preparedSilentAim) {
        lowPearlCatch = preparedLowPearlCatch;
        autoAim = preparedAutoAim;
        instantPearlCatch = preparedInstantPearlCatch;
        highDelay = preparedHighDelay;
        switchBack = preparedSwitchBack;
        retry = preparedRetry;
        highPearlCatch = preparedHighPearlCatch;
        silentAim = preparedSilentAim;
    }
}