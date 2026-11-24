package com.blamejared.clumps.client.modules.impl.combat;

import static com.blamejared.clumps.client.init.CatalogText.literal;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.event.interaction.ItemUsageTickEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.ServerFallDistanceTracker;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.mixin.accessor.KeyBindingAccessor;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.internals.Targets;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.HotbarSlotsSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.settings.NamedRangeGroupSetting;
import com.blamejared.clumps.client.settings.range.NamedRange;
import com.blamejared.clumps.client.utils.CooldownTimer;
import com.blamejared.clumps.client.utils.interaction.ActionReadiness;
import com.blamejared.clumps.client.utils.interaction.EntityInteractions;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.simulation.FallSample;
import com.blamejared.clumps.client.utils.simulation.MovementSimulation;
import com.blamejared.clumps.client.utils.simulation.TickWindow;
import com.blamejared.clumps.client.utils.strings.Concat;
import com.blamejared.clumps.client.utils.targeting.AimPointMode;
import com.blamejared.clumps.client.utils.targeting.AimPointSettings;
import com.blamejared.clumps.client.utils.targeting.AttackEvaluation;
import com.blamejared.clumps.client.utils.targeting.AttackRange;
import com.blamejared.clumps.client.utils.targeting.EntityPredicates;
import com.blamejared.clumps.client.utils.targeting.EntityRaycasts;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Combined legit mace combat module unifying Auto Mace diving/aim/elytra logic
 * and Mace Swap priority enchantment selection, stun slam, and spear D-tap
 * without sending any raw/illegal packets.
 */
public class AutoMaceSwap extends Module {
    private static final CharSequence BLATANT = literal("Blatant");
    private static final CharSequence LATE = literal("Late");
    private static final CharSequence CLOSEST = literal("Closest");
    private static final CharSequence NATURAL = literal("Natural");
    private static final CharSequence RANGE_SEPARATOR = literal("-");
    private static final CharSequence RETURN_SLOT = literal("Slot");
    private static final CharSequence RETURN_ITEM = literal("Item");
    private static final CharSequence RETURN_ORIGINAL = literal("Original");

    private static final int EQUIPMENT_USE_TICK = 1;
    private static final int EQUIPMENT_MIN_FINISH_TICK = 2;
    private static final int EQUIPMENT_FINISH_TICK = 3;
    private static final int MINIMUM_PREDICTED_HIT_TICK = 3;
    private static final int AIM_PREDICTION_TICKS = 6;
    private static final int GLIDE_END_TIMEOUT_TICKS = 8;
    private static final int STUN_HIT_WINDOW_TICKS = 8;
    private static final int AXE_AWAIT_WINDOW_TICKS = 4;
    private static final int SHIELD_BROKEN_GRACE_TICKS = 8;
    private static final double OVERHEAD_SHIELD_HORIZONTAL_MARGIN = 0.85;
    private static final double SHIELD_ARC_LATENCY_DEGREES = 105.0;
    private static final double BLOCKING_OBSERVE_RANGE = 18.0;
    private static final long BLOCKING_OBSERVE_GRACE_MS = 300L;
    private static final double MINIMUM_SMASH_DISTANCE = 1.5;
    private static final float SMASH_THRESHOLD = 1.5f;
    private static final float BREACH_UPPER_BOUND = 7.0f;
    private static final float MAXIMUM_FALL_DISTANCE = 20.0f;

    private static final NamedRange breachRange = new NamedRange(literal("Breach"), SMASH_THRESHOLD, BREACH_UPPER_BOUND);
    private static final NamedRange windBurstRange = new NamedRange(literal("Wind Burst"), SMASH_THRESHOLD, MAXIMUM_FALL_DISTANCE);
    private static final NamedRange densityRange = new NamedRange(literal("Density"), SMASH_THRESHOLD, MAXIMUM_FALL_DISTANCE);
    private static final NamedRange fallbackRange = new NamedRange(literal("Fallback")).disabled();
    public static NamedRangeGroupSetting maceSwapRanges = new NamedRangeGroupSetting(
            literal("Priority Order"), breachRange, windBurstRange, densityRange, fallbackRange)
        .bounds(literal("Fall Distance"), 0.0f, MAXIMUM_FALL_DISTANCE, 0.1f, literal(" Blocks"))
        .band(literal("Any"), 0.0f, MAXIMUM_FALL_DISTANCE)
        .band(literal("Smash"), SMASH_THRESHOLD, MAXIMUM_FALL_DISTANCE)
        .band(literal("Ground"), 0.0f, SMASH_THRESHOLD);

    public static FloatSetting fallDistance;
    public static FloatRangeSetting cps;
    public static FloatRangeSetting delay;
    public static ItemListSetting itemWhitelist;
    public static HotbarSlotsSetting hotbarWhitelist;
    public static BooleanSetting stunSlam;
    public static BooleanSetting autoAxeStun;
    public static FloatRangeSetting stunReactionDelay;
    public static FloatRangeSetting stunSwitchDelay;
    public static BooleanSetting spearDTap;
    public static FloatSetting minimumDamage;
    public static ChoiceSetting returnTo;
    public static HotbarSlotsSetting returnSlots;
    public static ItemListSetting returnItems;
    public static BooleanSetting autoAim;
    public static ChoiceSetting aimMath;
    public static ChoiceSetting aimVector;
    public static ChoiceSetting targetPriority;
    public static BooleanSetting random;
    public static BooleanSetting silentRotations;
    public static BooleanSetting throughWalls;
    public static FloatSetting horizontalSpeed;
    public static FloatSetting verticalSpeed;
    public static FloatSetting range;
    public static FloatSetting yawFOV;
    public static FloatSetting pitchFOV;
    public static FloatSetting multipoint;
    public static BooleanSetting automaticallyUnequipElytra;
    public static FloatSetting elytraRange;
    public static ChoiceSetting unequipTiming;
    public static FloatSetting lateWindow;
    public static BooleanSetting reEquipElytra;

    private record BlockingObservation(long firstSeenMs, long lastSeenMs, long requiredDelayMs) {}

    private CooldownTimer attackTimer;
    private final Map<Integer, BlockingObservation> blockingObservations = new HashMap<>();
    private long attackDelayMillis;
    private boolean disablePending;

    private int originalSlot;
    private int equipmentSlot;
    private int equipmentStageTicks;
    private boolean reEquippingElytra;
    private boolean equipmentUseIssued;
    private boolean chestplateSwapObserved;
    private ActionReadiness actionReadiness;
    private ClientPlayerEntity trackedPlayer;
    private MaceFlightState flightState;
    private int glideEndWaitTicks;

    private boolean restoreUseKey;
    private long releaseWaitUntilTick;
    private int preSwapSlot;
    private int scheduledMaceSlot;
    private int scheduledTargetId;
    private long scheduledSwapTick;
    private int stunAwaitTargetId;
    private int stunAwaitMaceSlot;
    private long stunAwaitDeadlineTick;
    private int axeAwaitTargetId;
    private long axeAwaitDeadlineTick;
    private int stunBrokenTargetId;
    private long stunBrokenUntilTick;
    private boolean stunBrokenSawUnblock;
    private int pendingHotbarSlot;
    private int pendingTargetId;
    private int pendingAxeTargetId;
    private boolean pendingActionIsAxe;
    private boolean pendingStunFollowup;
    private boolean requireSameTarget;
    private boolean attackPending;
    private long lastSwapTick;
    private boolean wasSmashReady;
    private int lockedAimTargetId = -1;

    public AutoMaceSwap() {
        super(new ModuleInfo(literal("Auto Mace Swap"), literal("Automatically swaps to a mace and attacks"), ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        attackTimer = new CooldownTimer();
        attackDelayMillis = 0L;
        disablePending = false;
        originalSlot = -1;
        equipmentSlot = -1;
        actionReadiness = new ActionReadiness();
        flightState = MaceFlightState.IDLE;
        resetSwapState();
        configureSettings();
    }

    private void configureSettings() {
        delay.tooltip("Timing");
        cps.tooltip("Timing");
        horizontalSpeed.setAllowOutOfRange(true);
        verticalSpeed.setAllowOutOfRange(true);
        horizontalSpeed.tooltip("Timing");
        verticalSpeed.tooltip("Timing");
        itemWhitelist.tooltip("Conditions");
        hotbarWhitelist.tooltip("Conditions");
        maceSwapRanges.tooltip("Mace Priority");
        maceSwapRanges.description(literal("Top to bottom, the first entry with a usable mace inside its fall distance wins. Disabled entries block maces carrying them"));

        stunSlam.tooltip("Stun Slam");
        autoAxeStun.tooltip("Stun Slam");
        autoAxeStun.setVisibilityPredicate(ignored -> stunSlam.getValue());
        autoAxeStun.description(literal("Automatically swaps to a hotbar axe to break a blocking shield before the mace slam."));
        stunReactionDelay.tooltip("Stun Slam");
        stunReactionDelay.setVisibilityPredicate(ignored -> stunSlam.getValue());
        stunReactionDelay.description(literal("Milliseconds to wait after an enemy holds a shield before stunning with an axe."));
        stunSwitchDelay.tooltip("Stun Slam");
        stunSwitchDelay.setVisibilityPredicate(ignored -> stunSlam.getValue());
        stunSwitchDelay.description(literal("Total ticks from the shield disable to attack with the mace"));

        spearDTap.tooltip("Spear D-tap");
        minimumDamage.tooltip("Spear D-tap");
        minimumDamage.setVisibilityPredicate(ignored -> spearDTap.getValue());

        returnTo.tooltip("Return");
        returnSlots.tooltip("Return");
        returnItems.tooltip("Return");
        returnSlots.setVisibilityPredicate(ignored -> returnTo.isSelected(RETURN_SLOT));
        returnItems.setVisibilityPredicate(ignored -> returnTo.isSelected(RETURN_ITEM));

        aimMath.setVisibilityPredicate(ignored -> autoAim.getValue());
        aimVector.setVisibilityPredicate(ignored -> autoAim.getValue());
        targetPriority.setVisibilityPredicate(ignored -> autoAim.getValue());
        random.setVisibilityPredicate(ignored -> autoAim.getValue());
        silentRotations.setVisibilityPredicate(ignored -> autoAim.getValue());
        throughWalls.setVisibilityPredicate(ignored -> autoAim.getValue());
        horizontalSpeed.setVisibilityPredicate(ignored -> autoAim.getValue() && !aimMath.isSelected(BLATANT));
        verticalSpeed.setVisibilityPredicate(ignored -> autoAim.getValue() && !aimMath.isSelected(BLATANT));
        range.setVisibilityPredicate(ignored -> autoAim.getValue());
        yawFOV.setVisibilityPredicate(ignored -> autoAim.getValue());
        pitchFOV.setVisibilityPredicate(ignored -> autoAim.getValue());
        multipoint.setVisibilityPredicate(ignored -> autoAim.getValue());

        automaticallyUnequipElytra.description(literal("Equips a chestplate when the predicted fall can reach a mace target."));
        elytraRange.description(literal("Target distance within which your elytra swaps back to a chestplate (3.0 - 6.0 blocks)."));
        elytraRange.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue());
        unequipTiming.description(literal("Normal starts at the first predicted opportunity. Late keeps gliding until a hit is inside the Late Window."));
        unequipTiming.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue());
        lateWindow.description(literal("Maximum ticks from starting the armor swap to the predicted hit. Lower swaps later; higher gives more preparation time. Includes the stun follow-up when needed."));
        lateWindow.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue() && unequipTiming.isSelected(LATE));
        reEquipElytra.description(literal("Puts the elytra back on after a smash bounce or when the dive ends on the ground, in liquid, or on a climbable block."));
        reEquipElytra.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue());
        automaticallyUnequipElytra.tooltip("Elytra");
        elytraRange.tooltip("Elytra");
        unequipTiming.tooltip("Elytra");
        lateWindow.tooltip("Elytra");
        reEquipElytra.tooltip("Elytra");
    }

    @Override
    public CharSequence getSuffix() {
        return Concat.sequences(new CharSequence[] {
                Concat.formattedFloat(cps.getLower(), 1), RANGE_SEPARATOR, Concat.formattedFloat(cps.getUpper(), 1)});
    }

    @Override
    public void onEnable() {
        disablePending = false;
        DeferredActionQueue.finish(this, true);
        resetFlightState(true);
        resetSwapState();
        Targets.currentTarget = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (client == null) {
            throw new NullPointerException("object reference is null");
        }
        if (client.player != null) {
            restoreEquipmentSlot();
        }
        DeferredActionQueue.finish(this, true);
        resetFlightState(true);
        resetSwapState();
        Targets.currentTarget = null;
        if (silentRotations.getValue() && rotations().isOwnedBy(this)) {
            disablePending = true;
            return;
        }
        disablePending = false;
        rotations().cancel(this);
        super.onDisable();
    }

    public static boolean hasPendingSwap() {
        return DeferredActionQueue.contains(AutoMaceSwap.class);
    }

    public static boolean isStunSequenceRunning() {
        if (ClumpsClient.ModuleManager == null) {
            return false;
        }
        Module feature = ClumpsClient.ModuleManager.findFeature(AutoMaceSwap.class);
        if (!(feature instanceof AutoMaceSwap module) || !module.isEnabled() || module.isOutsideWorld()) {
            return false;
        }
        return module.isStunSequenceActive() || (stunSlam != null && stunSlam.getValue()
                && autoAxeStun != null && autoAxeStun.getValue()
                && !module.client.player.isOnGround() && !module.client.player.isGliding()
                && module.isSmashReady());
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (disablePending) {
            finishDeferredDisable();
            return;
        }
        if (!autoAim.getValue()) {
            if (rotations().isOwnedBy(this)) {
                releaseOrCancelRotation();
            }
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            rotations().beginReturn(this);
            return;
        }
        LivingEntity stunTarget = activeStunTarget();
        boolean equipmentUnequipping = hasEquipmentSwap(false)
                && (!silentRotations.getValue() || !client.player.isGliding());
        double approachSpeed = client.player.getVelocity().length();
        boolean airborneMace = !client.player.isGliding() && !client.player.isOnGround()
                && !client.player.isTouchingWater() && !client.player.isClimbing()
                && (client.player.getVelocity().y < 0.05 || approachSpeed >= 0.85
                || InventoryItems.mainHandStack().isOf(Items.MACE));
        if (stunTarget == null && !equipmentUnequipping && !airborneMace) {
            releaseOrCancelRotation();
            return;
        }
        boolean canAxeStunAim = stunSlam.getValue() && InventoryItems.holdsAxe()
                && InventoryItems.findMaceWithEnchantmentInHotbar(null, true) != -1;
        if (stunTarget == null && !equipmentUnequipping && !canUseMace() && !canAxeStunAim && !isStunSequenceActive()) {
            if (silentRotations.getValue() && rotations().isOwnedBy(this)
                    && (hasPendingSwap() || MaceSwap.hasPendingSwap() || SpearReach.hasPendingSwap())) {
                if (attackPending || stunAwaitTargetId != -1 || axeAwaitTargetId != -1) {
                    LivingEntity heldTarget = selectAimTarget();
                    if (heldTarget != null) {
                        rotations().aim(this, RotationPriority.HIGH, heldTarget,
                                effectiveAimPointSettings(heldTarget), rotationSettings(heldTarget));
                        return;
                    }
                }
                rotations().beginReturn(this);
                return;
            }
            releaseOrCancelRotation();
            return;
        }
        int predictionHorizon = approachSpeed >= 1.0 ? AIM_PREDICTION_TICKS + 2 : AIM_PREDICTION_TICKS;
        MaceImpactPrediction prediction = (stunTarget != null || client.player.isGliding())
                ? null : predictImpact(predictionHorizon);
        LivingEntity target = stunTarget != null ? stunTarget : selectAimTarget();
        if (target == null && prediction != null) {
            LivingEntity predictedTarget = TargetSelection.resolveTarget(prediction.target());
            Vec3d eye = client.player.getCameraPosVec(1.0F);
            double effectiveRange = effectiveAimRange();
            if (isWithinAimBounds(eye, effectiveRange * effectiveRange, predictedTarget)) {
                target = predictedTarget;
            }
        }
        if (target == null) {
            lockedAimTargetId = -1;
            releaseOrCancelRotation();
            return;
        }
        lockedAimTargetId = target.getId();
        boolean accepted;
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        Box targetBox = target.getBoundingBox();
        double reachMargin = MovementSimulation.attackRange() + 0.35;
        boolean outsideImmediateReach = targetBox.squaredMagnitude(eye) > reachMargin * reachMargin;
        double dx = target.getX() - eye.x;
        double dz = target.getZ() - eye.z;
        boolean directlyOverhead = (dx * dx + dz * dz) < 0.72 * 0.72 && eye.y > targetBox.maxY - 0.1;
        if (directlyOverhead) {
            // Aim smoothly at the upper center surface when directly overhead to prevent 180-degree yaw spin.
            Vec3d topPoint = new Vec3d(
                    MathHelper.clamp(eye.x, targetBox.minX + 0.08, targetBox.maxX - 0.08),
                    targetBox.maxY - 0.12,
                    MathHelper.clamp(eye.z, targetBox.minZ + 0.08, targetBox.maxZ - 0.08));
            accepted = rotations().aim(this, RotationPriority.HIGH, topPoint, rotationSettings(target));
        } else if (prediction != null && prediction.target() == target && prediction.impactTick() >= 1
                && outsideImmediateReach && approachSpeed >= 0.75) {
            int leadTicks = Math.min(2, prediction.impactTick());
            accepted = rotations().aim(this, RotationPriority.HIGH,
                    MovementSimulation.predictedAimPoint(target, leadTicks), rotationSettings(target));
        } else {
            accepted = rotations().aim(this, RotationPriority.HIGH, target,
                    effectiveAimPointSettings(target), rotationSettings(target));
        }
        if (!accepted) {
            releaseOrCancelRotation();
        }
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (trackedPlayer != client.player) {
            resetFlightState(true);
            resetSwapState();
            trackedPlayer = client.player;
        }
        if (disablePending || isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            if (isOutsideWorld() || !stunSlam.getValue()) {
                clearScheduledSwap();
                clearStunAwait(false);
            }
            wasSmashReady = false;
            return;
        }
        if (!client.player.isAlive()) {
            resetFlightState(true);
            resetSwapState();
            wasSmashReady = false;
            return;
        }
        boolean smashNow = isSmashReady();
        if (smashNow && !wasSmashReady) {
            // Fresh smash fall just crossed threshold: never block the opening smash with a stale CPS delay.
            attackDelayMillis = 0L;
        }
        wasSmashReady = smashNow;
        updateDive();

        if (!stunSlam.getValue()) {
            Targets.currentTarget = null;
            blockingObservations.clear();
            clearScheduledSwap();
            clearAxeAwait(true);
            clearStunAwait(true);
            clearShieldBrokenGrace();
        } else {
            updateShieldBrokenGrace();
            observeBlockingTargets();
            if (axeAwaitTargetId != -1
                    && (client.player.age > axeAwaitDeadlineTick || !isEntityValidForStun(axeAwaitTargetId)
                    || !isTargetStillAxeWorthy(axeAwaitTargetId))) {
                clearAxeAwait(true);
            }
            if (stunAwaitTargetId != -1
                    && (client.player.age > stunAwaitDeadlineTick || !isEntityValidForStun(stunAwaitTargetId))) {
                clearStunAwait(true);
            }
            if (scheduledMaceSlot != -1 && (long) client.player.age >= scheduledSwapTick) {
                if (DeferredActionQueue.hasPendingActions()
                        && !DeferredActionQueue.contains(this)
                        && !DeferredActionQueue.contains(ShieldBreaker.class)) {
                    clearScheduledSwap();
                } else if (!isEntityValidForStun(scheduledTargetId)) {
                    clearScheduledSwap();
                    DeferredActionQueue.finish(this, true);
                }
            }
            if (isStunSequenceActive()) {
                Targets.shieldBreakActive = true;
            }
        }
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onHotbarKey(HotbarKeyEvent event) {
        if (disablePending || isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (event.slot == 0) {
            if (stunSlam.getValue()) {
                processScheduledStunFollowup();
                if (stunAwaitTargetId != -1 && pendingHotbarSlot == -1 && !hasEquipmentSwap()) {
                    int refreshedMace = findStunFollowupMaceSlot();
                    if (refreshedMace != -1 && client.player.getInventory().getSelectedSlot() != refreshedMace) {
                        pendingHotbarSlot = refreshedMace;
                        pendingTargetId = stunAwaitTargetId;
                        pendingActionIsAxe = false;
                        pendingStunFollowup = true;
                        requireSameTarget = true;
                        attackPending = true;
                        lastSwapTick = client.player.age;
                    }
                }
            }
            boolean manualClickTriggered = false;
            if (!hasEquipmentSwap() && client.player.age > releaseWaitUntilTick
                    && !isStunSequenceActive()
                    && client.options.attackKey != null
                    && ((KeyBindingAccessor) client.options.attackKey).getTimesPressed() > 0) {
                if (client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
                    if (stunSlam.getValue() && autoAxeStun.getValue() && isBlockingTarget(target)
                            && !InventoryItems.holdsAxe() && canInitiateHotbarSwap()) {
                        manualClickTriggered = scheduleAxeStun(target, false);
                    } else if (!isBlockingTarget(target)) {
                        manualClickTriggered = requestMaceSwap(target);
                    }
                }
            }
            if (!manualClickTriggered && !hasEquipmentSwap() && pendingHotbarSlot == -1 && !attackPending
                    && !DeferredActionQueue.hasPendingActions() && scheduledMaceSlot == -1
                    && stunAwaitTargetId == -1 && axeAwaitTargetId == -1
                    && client.player.age > releaseWaitUntilTick) {
                if (!tryScheduleAutoAttack()) {
                    tryPreSwapOnFastApproach();
                }
            }
        }
        if (pendingHotbarSlot == -1 || event.slot != pendingHotbarSlot) {
            return;
        }
        if (client.player == null || (long) client.player.age - lastSwapTick > 2L) {
            resetPendingAttack();
            return;
        }
        boolean isAxe = pendingActionIsAxe;
        boolean isStunFollowup = pendingStunFollowup;
        pendingHotbarSlot = -1;
        if (isStunFollowup && DeferredActionQueue.topOwner() instanceof ShieldBreaker) {
            if (!DeferredActionQueue.contains(this) && preSwapSlot < 0) {
                preSwapSlot = client.player.getInventory().getSelectedSlot();
            }
            DeferredActionQueue.drop(DeferredActionQueue.topOwner());
        }
        if (DeferredActionQueue.contains(this) || client.player.getInventory().getSelectedSlot() != event.slot) {
            if (!DeferredActionQueue.contains(this) && preSwapSlot < 0) {
                preSwapSlot = client.player.getInventory().getSelectedSlot();
            }
            int ticks = isAxe ? Math.max(4, Math.round(stunSwitchDelay.getUpper()) + AXE_AWAIT_WINDOW_TICKS + 2)
                    : (isStunFollowup || stunAwaitTargetId != -1)
                            ? STUN_HIT_WINDOW_TICKS + Math.max(1, (int) delay.sampleGaussian())
                            : Math.max(0, (int) delay.sampleGaussian());
            boolean restore = !isAxe && returnTo.isSelected(RETURN_ORIGINAL);
            Runnable callback = isAxe ? null : this::onSwapEnded;
            if (!DeferredActionQueue.select(this, event.slot, ticks, restore, callback)) {
                resetPendingAttack();
                return;
            }
        }
        event.addClick();
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onAttackKey(AttackKeyEvent event) {
        if (disablePending || isOutsideWorld() || MinecraftRenderState.getScreen() != null || hasEquipmentSwap()) {
            return;
        }
        if (client.player.age <= releaseWaitUntilTick) {
            return;
        }
        if (stunAwaitTargetId != -1 && !attackPending) {
            int targetId = stunAwaitTargetId;
            if (client.player.age > stunAwaitDeadlineTick || !isEntityValidForStun(targetId)) {
                clearStunAwait(true);
            } else if (InventoryItems.mainHandStack().isOf(Items.MACE) && isCrosshairTargetValid(targetId)) {
                Targets.shieldBreakActive = true;
                triggerAttack(event, crosshairLivingTarget());
                return;
            }
            return;
        }
        if (axeAwaitTargetId != -1 && !attackPending) {
            int targetId = axeAwaitTargetId;
            if (client.player.age > axeAwaitDeadlineTick || !isEntityValidForStun(targetId)
                    || !isTargetStillAxeWorthy(targetId)) {
                clearAxeAwait(true);
            } else if (InventoryItems.holdsAxe() && isCrosshairTargetValid(targetId)) {
                Targets.shieldBreakActive = true;
                if (triggerAttack(event, crosshairLivingTarget())) {
                    pendingAxeTargetId = targetId;
                    clearAxeAwait(false);
                }
                return;
            }
            return;
        }
        if (attackPending) {
            boolean recent = (long) client.player.age - lastSwapTick <= 2L;
            int targetId = pendingTargetId;
            boolean checkTarget = requireSameTarget;
            boolean isAxe = pendingActionIsAxe;
            boolean isStunFollowup = pendingStunFollowup;
            resetPendingAttack();
            boolean holdingExpected = isAxe ? InventoryItems.holdsAxe()
                    : InventoryItems.mainHandStack().isOf(Items.MACE);
            LivingEntity resolvedTarget = crosshairLivingTarget();
            boolean validTarget = !checkTarget || isCrosshairTargetValid(targetId);
            if (recent && holdingExpected && validTarget && triggerAttack(event, resolvedTarget)) {
                if (isAxe) {
                    pendingAxeTargetId = targetId;
                    Targets.shieldBreakActive = true;
                } else if (!isStunFollowup) {
                    resetAttackTimer();
                } else {
                    enterStunAwait(targetId);
                }
            } else if (!isAxe && isStunFollowup && recent && holdingExpected && isEntityValidForStun(targetId)) {
                enterStunAwait(targetId);
            } else if (isAxe && recent && holdingExpected && isEntityValidForStun(targetId)
                    && isTargetStillAxeWorthy(targetId)) {
                enterAxeAwait(targetId);
            }
            return;
        }
        if (event.hasClicks() && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity target) {
            if (stunSlam.getValue() && isBlockingTarget(target)) {
                if (InventoryItems.holdsAxe()) {
                    pendingAxeTargetId = target.getId();
                    Targets.shieldBreakActive = true;
                    return;
                }
                if (autoAxeStun.getValue() && canInitiateHotbarSwap() && !suspendItemUse()) {
                    int axeSlot = InventoryItems.findStackInHotbar(InventoryItems::isAxe);
                    if (axeSlot != -1) {
                        preSwapSlot = client.player.getInventory().getSelectedSlot();
                        int ticks = Math.max(4, Math.round(stunSwitchDelay.getUpper()) + AXE_AWAIT_WINDOW_TICKS + 2);
                        if (DeferredActionQueue.select(this, axeSlot, ticks, false, null)) {
                            pendingAxeTargetId = target.getId();
                            Targets.shieldBreakActive = true;
                            return;
                        }
                    }
                }
            }
            int slot = findEligibleMace(target);
            if (slot != -1 && !suspendItemUse()) {
                preSwapSlot = client.player.getInventory().getSelectedSlot();
                DeferredActionQueue.select(this, slot, Math.max(0, (int) delay.sampleGaussian()),
                        returnTo.isSelected(RETURN_ORIGINAL), this::onSwapEnded);
            }
            return;
        }
        if (DeferredActionQueue.hasPendingActions() || !isSmashReady()) {
            return;
        }
        LivingEntity target = attackTarget();
        if (target == null || !isAttackTimerReady()) {
            return;
        }
        if (isBlockingTarget(target)) {
            if (stunSlam.getValue() && autoAxeStun.getValue() && InventoryItems.holdsAxe()
                    && checkStunReactionElapsed(target)) {
                if (triggerAttack(event, target)) {
                    blockingObservations.remove(target.getId());
                    pendingAxeTargetId = target.getId();
                    Targets.shieldBreakActive = true;
                }
            }
            return;
        }
        int bestMace = findTargetMaceSlot();
        boolean holdingMace = InventoryItems.mainHandStack().isOf(Items.MACE);
        if (holdingMace && (bestMace == -1 || bestMace == client.player.getInventory().getSelectedSlot())) {
            if (triggerAttack(event, target)) {
                resetAttackTimer();
            }
        }
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (!hasEquipmentSwap() || isOutsideWorld() || equipmentStageTicks != EQUIPMENT_USE_TICK
                || equipmentUseIssued || client.player.getInventory().getSelectedSlot() != equipmentSlot) {
            return;
        }
        boolean matches = reEquippingElytra ? InventoryItems.mainHandStack().isOf(Items.ELYTRA)
                : InventoryItems.holdsChestplateInMainHand();
        if (!matches) {
            return;
        }
        if (!event.hasClicks()) {
            event.addClick();
        }
        equipmentUseIssued = true;
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void afterShieldAttack(EntityAttackEvent.Post event) {
        if (isOutsideWorld() || !stunSlam.getValue() || event.getPlayer() != client.player) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (InventoryItems.mainHandStack().isOf(Items.MACE) && stunAwaitTargetId != -1) {
            if (target.getId() == stunAwaitTargetId || !isSmashReady()) {
                clearStunAwait(false);
                scheduleMaceReturn();
                resetAttackTimer();
            }
            return;
        }
        LivingEntity tracked = Targets.currentTarget;
        Targets.currentTarget = null;
        boolean matchesTracked = tracked != null && tracked.getId() == target.getId();
        boolean matchesOwnAxeStun = pendingAxeTargetId == target.getId();
        pendingAxeTargetId = -1;
        boolean matchesBlockingPlayer = target instanceof PlayerEntity player
                && (isBlockingTarget(player) || EntityPredicates.isUsingBlockingItem(player) || player.isBlocking());
        if (!matchesTracked && !matchesOwnAxeStun && !matchesBlockingPlayer) {
            return;
        }
        if (!InventoryItems.holdsAxe() || !TargetSelection.isValidTarget(target)) {
            return;
        }
        clearAxeAwait(false);
        markShieldBroken(target.getId());
        int slot = findStunFollowupMaceSlot();
        if (slot == -1) {
            return;
        }
        int ticks = Math.max(1, Math.round(stunSwitchDelay.sampleGaussian()));
        int airTicks = MovementSimulation.ticksUntilGround(MovementSimulation.fallSamples(0));
        double fallSpeed = Math.max(0.0, -client.player.getVelocity().y);
        if (fallSpeed >= 0.85 || (airTicks > 0 && airTicks <= Math.max(2, ticks))) {
            // Fast dive or near touchdown: slam on the very next tick (strictly >= 1 tick apart)
            // so the mace hits while still airborne with full fall distance.
            ticks = 1;
        }
        scheduledMaceSlot = slot;
        scheduledTargetId = target.getId();
        scheduledSwapTick = client.player.age + ticks;
        blockingObservations.remove(target.getId());
        Targets.shieldBreakActive = true;
    }

    @Subscribe
    private void onItemUsageTick(ItemUsageTickEvent event) {
        if (isOutsideWorld() || !spearDTap.getValue()) {
            return;
        }
        for (AttackEvaluation evaluation : event.getAttackEvaluations()) {
            if (!evaluation.isAvailable()) continue;
            float damage = evaluation.damage() ? evaluation.damageAmount() : 0.0f;
            if (damage >= minimumDamage.getValue()
                    && evaluation.target() instanceof LivingEntity target
                    && requestMaceSwap(target)) {
                break;
            }
        }
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerRespawnS2CPacket || packet instanceof PlayerPositionLookS2CPacket) {
            resetSwapState();
            Targets.currentTarget = null;
        }
    }

    private boolean tryScheduleAutoAttack() {
        if (client.player.isUsingItem() && InventoryItems.isSpear(client.player.getActiveItem().getItem())) {
            return false;
        }
        LivingEntity target = attackTarget();
        if (target == null) {
            return false;
        }
        if (isBlockingTarget(target)) {
            boolean stunWindowReady = isSmashReady() || predictStunSmashReady(target);
            if (stunWindowReady && stunSlam.getValue() && autoAxeStun.getValue()
                    && checkStunReactionElapsed(target)) {
                if (InventoryItems.holdsAxe() && !suspendItemUse()) {
                    blockingObservations.remove(target.getId());
                    pendingHotbarSlot = -1;
                    pendingTargetId = target.getId();
                    pendingActionIsAxe = true;
                    pendingStunFollowup = false;
                    requireSameTarget = true;
                    attackPending = true;
                    lastSwapTick = client.player.age;
                    Targets.shieldBreakActive = true;
                    return true;
                } else if (!InventoryItems.holdsAxe() && canInitiateHotbarSwap()) {
                    return scheduleAxeStun(target, true);
                }
            }
            return false;
        }
        if (!isSmashReady() || !isAttackTimerReady()) {
            return false;
        }
        if (requestMaceSwap(target)) {
            resetAttackTimer();
            return true;
        }
        int bestMace = findTargetMaceSlot();
        boolean holdingMace = InventoryItems.mainHandStack().isOf(Items.MACE);
        if (holdingMace && (bestMace == -1 || bestMace == client.player.getInventory().getSelectedSlot())
                && !DeferredActionQueue.hasPendingActions() && client.player.age > releaseWaitUntilTick
                && !suspendItemUse()) {
            pendingHotbarSlot = -1;
            pendingTargetId = target.getId();
            pendingActionIsAxe = false;
            pendingStunFollowup = false;
            requireSameTarget = true;
            attackPending = true;
            lastSwapTick = client.player.age;
            resetAttackTimer();
            return true;
        }
        return false;
    }

    private void tryPreSwapOnFastApproach() {
        if (client.player.isOnGround() || client.player.isGliding() || client.player.isUsingItem()) {
            return;
        }
        if (!stunSlam.getValue() || !autoAxeStun.getValue() || InventoryItems.holdsAxe()) {
            return;
        }
        double fallSpeed = Math.max(0.0, -client.player.getVelocity().y);
        double totalSpeed = client.player.getVelocity().length();
        if (fallSpeed < 0.40 && totalSpeed < 0.65 && !isSmashReady()) {
            return;
        }
        if (!canInitiateHotbarSwap()) {
            return;
        }
        // Only pre-swap early to an AXE for blocking targets so the axe is ready on the very first
        // tick entering 3.0-block reach (enabling Tick T axe break -> Tick T+1 airborne mace slam).
        // Never pre-swap early to a Mace for non-blocking hits so same-tick Sword/Axe/Spear -> Mace
        // attribute swapping preserves full base damage and attack cooldown progress.
        LivingEntity incoming = null;
        MaceImpactPrediction prediction = predictImpact(3);
        if (prediction != null && prediction.impactTick() >= 1 && prediction.impactTick() <= 3) {
            incoming = TargetSelection.resolveTarget(prediction.target());
        }
        if (incoming == null || !TargetSelection.isValidTarget(incoming) || !isBlockingTarget(incoming)) {
            incoming = findApproachingBlockingTarget();
        }
        if (incoming == null || !isBlockingTarget(incoming) || !checkStunReactionElapsed(incoming)) {
            return;
        }
        int axeSlot = InventoryItems.findStackInHotbar(InventoryItems::isAxe);
        if (axeSlot != -1 && !suspendItemUse()) {
            preSwapSlot = client.player.getInventory().getSelectedSlot();
            int ticks = Math.max(5, Math.round(stunSwitchDelay.getUpper()) + AXE_AWAIT_WINDOW_TICKS + 3);
            if (DeferredActionQueue.select(this, axeSlot, ticks, false, null)) {
                enterAxeAwait(incoming.getId());
                Targets.shieldBreakActive = true;
            }
        }
    }

    private LivingEntity findApproachingBlockingTarget() {
        if (client.world == null || client.player == null) {
            return null;
        }
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        double maxApproachSq = 7.5 * 7.5;
        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !TargetSelection.isValidTarget(player)) {
                continue;
            }
            if (!isBlockingTarget(player)) {
                continue;
            }
            double distSq = player.getBoundingBox().squaredMagnitude(eye);
            if (distSq <= maxApproachSq && distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = player;
            }
        }
        return closest;
    }

    private void processScheduledStunFollowup() {
        if (scheduledMaceSlot == -1 || (long) client.player.age < scheduledSwapTick) {
            return;
        }
        if (DeferredActionQueue.hasPendingActions()
                && !DeferredActionQueue.contains(this)
                && !DeferredActionQueue.contains(ShieldBreaker.class)) {
            clearScheduledSwap();
            return;
        }
        if (!isEntityValidForStun(scheduledTargetId)) {
            clearScheduledSwap();
            DeferredActionQueue.finish(this, true);
            return;
        }
        if (client.player.age <= releaseWaitUntilTick) {
            return;
        }
        if (suspendItemUse()) {
            return;
        }
        int bestSlot = findStunFollowupMaceSlot();
        int slot = bestSlot != -1 ? bestSlot : scheduledMaceSlot;
        int targetId = scheduledTargetId;
        clearScheduledSwap();
        prepareStunAttack(slot, targetId);
    }

    private boolean scheduleAxeStun(LivingEntity target, boolean checkReaction) {
        if (client.player != null && client.player.age <= releaseWaitUntilTick) {
            return false;
        }
        int axeSlot = InventoryItems.findStackInHotbar(InventoryItems::isAxe);
        if (axeSlot == -1 || !canInitiateHotbarSwap()) {
            return false;
        }
        if (checkReaction && !checkStunReactionElapsed(target)) {
            return false;
        }
        if (suspendItemUse()) {
            return false;
        }
        blockingObservations.remove(target.getId());
        preSwapSlot = client.player.getInventory().getSelectedSlot();
        pendingHotbarSlot = axeSlot;
        pendingTargetId = target.getId();
        pendingActionIsAxe = true;
        pendingStunFollowup = false;
        requireSameTarget = true;
        attackPending = true;
        lastSwapTick = client.player.age;
        Targets.shieldBreakActive = true;
        return true;
    }

    private void observeBlockingTargets() {
        if (client.world == null || client.player == null) {
            blockingObservations.clear();
            return;
        }
        long now = System.currentTimeMillis();
        double observeRange = Math.max(BLOCKING_OBSERVE_RANGE, range.getValue());
        double rangeSq = observeRange * observeRange;
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        List<PlayerEntity> players = client.world.getEntitiesByClass(
                PlayerEntity.class,
                client.player.getBoundingBox().expand(observeRange + 1.0),
                player -> player != client.player && TargetSelection.isValidTarget(player)
                        && !isRecentlyShieldBroken(player)
                        && isRawShieldBlocking(player)
                        && player.getBoundingBox().squaredMagnitude(eye) <= rangeSq);
        for (PlayerEntity player : players) {
            int id = player.getId();
            BlockingObservation existing = blockingObservations.get(id);
            if (existing == null) {
                long required = Math.max(0L, Math.round(stunReactionDelay.sampleGaussian()));
                blockingObservations.put(id, new BlockingObservation(now, now, required));
            } else {
                blockingObservations.put(id, new BlockingObservation(existing.firstSeenMs(), now, existing.requiredDelayMs()));
            }
        }
        Iterator<Map.Entry<Integer, BlockingObservation>> it = blockingObservations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, BlockingObservation> entry = it.next();
            if (now - entry.getValue().lastSeenMs() > BLOCKING_OBSERVE_GRACE_MS) {
                it.remove();
            }
        }
    }

    private boolean predictStunSmashReady(LivingEntity target) {
        if (client.player.isOnGround() || client.player.isGliding() || client.player.getVelocity().y >= -0.05) {
            return false;
        }
        int stunTicks = Math.max(1, Math.round(stunSwitchDelay.getUpper()));
        int lookahead = Math.max(2, stunTicks + 2);
        MaceImpactPrediction prediction = predictImpact(lookahead);
        if (prediction != null && prediction.target() == target) {
            return true;
        }
        double currentFall = Math.max(ServerFallDistanceTracker.fallDistance(client.player), client.player.fallDistance);
        double vy = client.player.getVelocity().y;
        double projectedDrop = 0.0;
        for (int i = 0; i < stunTicks; i++) {
            vy = (vy - 0.08) * 0.98;
            projectedDrop += -vy;
        }
        return currentFall + projectedDrop > fallDistance.getValue();
    }

    public boolean requestMaceSwap(LivingEntity target) {
        if (client.player != null && client.player.age <= releaseWaitUntilTick) {
            return false;
        }
        int slot = findEligibleMace(target);
        if (slot == -1) {
            return false;
        }
        if (suspendItemUse()) {
            return false;
        }
        preSwapSlot = client.player.getInventory().getSelectedSlot();
        pendingHotbarSlot = slot;
        pendingTargetId = target.getId();
        pendingActionIsAxe = false;
        requireSameTarget = true;
        attackPending = true;
        lastSwapTick = client.player.age;
        return true;
    }

    private int findEligibleMace(LivingEntity target) {
        if (isOutsideWorld() || attackPending || pendingHotbarSlot != -1) {
            return -1;
        }
        if (!canInitiateHotbarSwap()) {
            return -1;
        }
        if (!TargetSelection.isValidTarget(target) || isBlockingTarget(target)) {
            return -1;
        }
        int slot = findTargetMaceSlot();
        if (slot == -1 || client.player.getInventory().getSelectedSlot() == slot) {
            return -1;
        }
        return slot;
    }

    private boolean canInitiateHotbarSwap() {
        if (EntityInteractions.isUsingOffhand() || !itemWhitelist.acceptsHeldItem()
                || !hotbarWhitelist.permitsCurrentSlot()) {
            return false;
        }
        return !DeferredActionQueue.hasPendingActions();
    }

    private int findTargetMaceSlot() {
        int slot = MaceSwap.findPriorityMace(maceSwapRanges, densityRange, breachRange, windBurstRange);
        if (slot != -1) {
            return slot;
        }
        return isSmashReady() ? InventoryItems.findMaceWithEnchantmentInHotbar(null, true) : -1;
    }

    private int findStunFollowupMaceSlot() {
        int slot = findTargetMaceSlot();
        return slot != -1 ? slot : InventoryItems.findMaceWithEnchantmentInHotbar(null, true);
    }

    private void prepareStunAttack(int slot, int targetId) {
        if (!DeferredActionQueue.contains(this)) {
            preSwapSlot = client.player.getInventory().getSelectedSlot();
        }
        pendingHotbarSlot = client.player.getInventory().getSelectedSlot() == slot ? -1 : slot;
        pendingTargetId = targetId;
        pendingActionIsAxe = false;
        pendingStunFollowup = true;
        requireSameTarget = true;
        attackPending = true;
        lastSwapTick = client.player.age;
        Targets.shieldBreakActive = true;
    }

    private void scheduleMaceReturn() {
        int currentSlot = client.player.getInventory().getSelectedSlot();
        int ticks = Math.max(0, (int) delay.sampleGaussian());
        DeferredActionQueue.select(this, currentSlot, ticks,
                returnTo.isSelected(RETURN_ORIGINAL), this::onSwapEnded);
    }

    private boolean triggerAttack(AttackKeyEvent event, LivingEntity resolvedTarget) {
        if (client.attackCooldown > 0) {
            return false;
        }
        boolean vanillaCrosshairMatches = client.crosshairTarget instanceof EntityHitResult hit
                && (resolvedTarget == null || hit.getEntity() == resolvedTarget);
        if (vanillaCrosshairMatches) {
            if (actions().requestAttackClick(this, event)) {
                return true;
            }
            return event.hasClicks() && actions().canAttack(this, crosshairLivingTarget(), false);
        }
        if (resolvedTarget != null && resolvedTarget.isAlive() && !resolvedTarget.isRemoved()) {
            return actions().executeEntityAttack(this, resolvedTarget, true).succeeded();
        }
        return false;
    }

    private boolean isAttackTimerReady() {
        if (attackTimer.hasElapsedMillis((double) attackDelayMillis)) {
            return true;
        }
        if (client.player != null && !client.player.isOnGround() && client.attackCooldown <= 0
                && !actions().hasAttackedThisTick()) {
            double fallSpeed = Math.max(0.0, -client.player.getVelocity().y);
            double totalSpeed = client.player.getVelocity().length();
            if (fallSpeed >= 0.95 || totalSpeed >= 1.2) {
                return true;
            }
        }
        return false;
    }

    private void enterStunAwait(int targetId) {
        stunAwaitTargetId = targetId;
        stunAwaitMaceSlot = client.player.getInventory().getSelectedSlot();
        stunAwaitDeadlineTick = client.player.age + STUN_HIT_WINDOW_TICKS;
        Targets.shieldBreakActive = true;
        if (DeferredActionQueue.topOwner() instanceof ShieldBreaker) {
            DeferredActionQueue.drop(DeferredActionQueue.topOwner());
        }
        DeferredActionQueue.select(this, stunAwaitMaceSlot,
                STUN_HIT_WINDOW_TICKS + Math.max(1, (int) delay.sampleGaussian()),
                returnTo.isSelected(RETURN_ORIGINAL), this::onSwapEnded);
    }

    private void enterAxeAwait(int targetId) {
        axeAwaitTargetId = targetId;
        axeAwaitDeadlineTick = client.player.age + AXE_AWAIT_WINDOW_TICKS;
        Targets.shieldBreakActive = true;
    }

    private void clearAxeAwait(boolean restoreSlot) {
        boolean hadAwait = axeAwaitTargetId != -1;
        axeAwaitTargetId = -1;
        axeAwaitDeadlineTick = -1L;
        if (hadAwait && restoreSlot && DeferredActionQueue.contains(this)) {
            DeferredActionQueue.finish(this, true);
        }
    }

    private void markShieldBroken(int targetId) {
        stunBrokenTargetId = targetId;
        stunBrokenUntilTick = client.player == null ? -1L : client.player.age + SHIELD_BROKEN_GRACE_TICKS;
        stunBrokenSawUnblock = false;
    }

    private void clearShieldBrokenGrace() {
        stunBrokenTargetId = -1;
        stunBrokenUntilTick = -1L;
        stunBrokenSawUnblock = false;
    }

    private void updateShieldBrokenGrace() {
        if (stunBrokenTargetId == -1 || client.player == null || client.world == null) {
            return;
        }
        if (client.player.age > stunBrokenUntilTick) {
            clearShieldBrokenGrace();
            return;
        }
        if (!(client.world.getEntityById(stunBrokenTargetId) instanceof PlayerEntity player)
                || !player.isAlive() || player.isRemoved()) {
            clearShieldBrokenGrace();
            return;
        }
        boolean rawBlocking = isRawShieldBlocking(player);
        if (!rawBlocking) {
            stunBrokenSawUnblock = true;
        } else if (stunBrokenSawUnblock) {
            clearShieldBrokenGrace();
        }
    }

    private boolean isRecentlyShieldBroken(LivingEntity target) {
        return target != null && client.player != null
                && stunBrokenTargetId == target.getId()
                && client.player.age <= stunBrokenUntilTick;
    }

    private boolean isTargetStillAxeWorthy(int targetId) {
        if (targetId < 0 || client.world == null) {
            return false;
        }
        if (!(client.world.getEntityById(targetId) instanceof PlayerEntity player)) {
            return false;
        }
        return !isRecentlyShieldBroken(player) && isRawShieldBlocking(player);
    }

    private boolean isRawShieldBlocking(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        if (EntityPredicates.isBlocking(player) || player.isBlocking()) {
            return true;
        }
        boolean airborneFall = client.player != null && !client.player.isOnGround() && client.player.getVelocity().y < -0.1;
        return airborneFall && EntityPredicates.isUsingBlockingItem(player);
    }

    private boolean isWithinDiveBlockingArc(PlayerEntity player) {
        if (client.player == null || player == null) {
            return false;
        }
        double dx = client.player.getX() - player.getX();
        double dz = client.player.getZ() - player.getZ();
        double horizDistSq = dx * dx + dz * dz;
        if (horizDistSq <= OVERHEAD_SHIELD_HORIZONTAL_MARGIN * OVERHEAD_SHIELD_HORIZONTAL_MARGIN) {
            return true;
        }
        double invLen = 1.0 / Math.sqrt(horizDistSq);
        Vec3d direction = new Vec3d(dx * invLen, 0.0, dz * invLen);
        Vec3d facing = player.getRotationVector(0.0F, player.getHeadYaw());
        double angle = Math.acos(MathHelper.clamp(direction.dotProduct(facing), -1.0, 1.0));

        BlocksAttacksComponent blocking = player.getActiveItem().get(DataComponentTypes.BLOCKS_ATTACKS);
        if (blocking == null) {
            ItemStack heldItem = player.getStackInHand(player.getActiveHand());
            blocking = heldItem.get(DataComponentTypes.BLOCKS_ATTACKS);
        }
        double maxDegrees = 90.0;
        if (blocking != null && !blocking.damageReductions().isEmpty()) {
            maxDegrees = 0.0;
            for (BlocksAttacksComponent.DamageReduction reduction : blocking.damageReductions()) {
                maxDegrees = Math.max(maxDegrees, reduction.horizontalBlockingAngle());
            }
        }
        boolean airborne = !client.player.isOnGround() && client.player.getVelocity().y < -0.05;
        double effectiveMaxDegrees = airborne
                ? Math.max(SHIELD_ARC_LATENCY_DEGREES, maxDegrees + 15.0)
                : maxDegrees + 5.0;
        return angle <= Math.toRadians(effectiveMaxDegrees);
    }

    private boolean checkStunReactionElapsed(LivingEntity target) {
        long now = System.currentTimeMillis();
        BlockingObservation obs = blockingObservations.get(target.getId());
        if (obs == null) {
            long required = Math.max(0L, Math.round(stunReactionDelay.sampleGaussian()));
            blockingObservations.put(target.getId(), new BlockingObservation(now, now, required));
            return required <= 0L;
        }
        return (now - obs.firstSeenMs()) >= obs.requiredDelayMs();
    }

    private boolean isStunSequenceActive() {
        return pendingActionIsAxe || pendingAxeTargetId != -1
                || axeAwaitTargetId != -1 || scheduledMaceSlot != -1 || stunAwaitTargetId != -1;
    }

    private LivingEntity activeStunTarget() {
        int targetId = stunAwaitTargetId != -1 ? stunAwaitTargetId
                : scheduledTargetId != -1 ? scheduledTargetId
                : axeAwaitTargetId != -1 ? axeAwaitTargetId
                : pendingAxeTargetId != -1 ? pendingAxeTargetId
                : pendingActionIsAxe ? pendingTargetId : -1;
        if (targetId < 0 || client.world == null) {
            return null;
        }
        if (client.world.getEntityById(targetId) instanceof LivingEntity living && TargetSelection.isValidTarget(living)) {
            return living;
        }
        return null;
    }

    private boolean isEntityValidForStun(int entityId) {
        if (entityId < 0 || client.world == null) {
            return false;
        }
        return client.world.getEntityById(entityId) instanceof LivingEntity target
                && target.isAlive() && !target.isRemoved() && TargetSelection.isValidTarget(target);
    }

    private void clearStunAwait(boolean restoreSlot) {
        boolean hadAwait = stunAwaitTargetId != -1;
        stunAwaitTargetId = -1;
        stunAwaitMaceSlot = -1;
        stunAwaitDeadlineTick = -1L;
        if (hadAwait && restoreSlot && DeferredActionQueue.contains(this)) {
            DeferredActionQueue.finish(this, true);
        }
    }

    public int getStunDelayTicks(LivingEntity target) {
        if (!isEnabled() || !stunSlam.getValue() || !(target instanceof PlayerEntity player)) {
            return 0;
        }
        if (!isBlockingTarget(player)) {
            return 0;
        }
        return Math.max(1, Math.round(stunSwitchDelay.getUpper()));
    }

    public static boolean acceptsCurrentSlot() {
        return itemWhitelist.acceptsHeldItem() && hotbarWhitelist.permitsCurrentSlot();
    }

    private boolean isBlockingTarget(LivingEntity target) {
        if (!(target instanceof PlayerEntity player) || isRecentlyShieldBroken(player)) {
            return false;
        }
        return isRawShieldBlocking(player) && isWithinDiveBlockingArc(player);
    }

    private boolean isCrosshairTargetValid(int targetId) {
        if (targetId < 0) {
            return false;
        }
        LivingEntity target = crosshairLivingTarget();
        return target != null && target.getId() == targetId
                && target.isAlive() && !target.isRemoved() && TargetSelection.isValidTarget(target);
    }

    private boolean suspendItemUse() {
        boolean wasUsing = client.player != null && client.player.isUsingItem();
        if (client.player != null && (wasUsing || client.options.useKey.isPressed())) {
            client.options.useKey.setPressed(false);
            if (wasUsing) {
                EntityInteractions.stopUsingItem();
                releaseWaitUntilTick = client.player.age;
            }
        }
        if (!restoreUseKey && KeybindingCache.isPressed(client.options.useKey)) {
            restoreUseKey = true;
        }
        return wasUsing && client.player != null && client.player.age <= releaseWaitUntilTick;
    }

    private void onSwapEnded() {
        if (!returnTo.isSelected(RETURN_ORIGINAL)) {
            int slot = returnTo.isSelected(RETURN_SLOT) ? returnSlots.chooseSelectedSlot()
                    : InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
            if (slot == -1 && preSwapSlot >= 0) {
                slot = preSwapSlot;
            }
            if (slot != -1) {
                InventoryItems.selectHotbarSlot(slot);
            }
        }
        preSwapSlot = -1;
        restoreItemUse();
    }

    private void restoreItemUse() {
        if (!restoreUseKey) {
            return;
        }
        restoreUseKey = false;
        if (client.options.useKey == null) {
            return;
        }
        InputUtil.Key key = ((KeyBindingAccessor) client.options.useKey).getBoundKey();
        if (key == null) {
            return;
        }
        InputKey input = key.getCategory() == InputUtil.Type.MOUSE
                ? InputKey.fromMouseCode(key.getCode()) : InputKey.fromCode(key.getCode());
        if (input.isPressed()) {
            client.options.useKey.setPressed(true);
        }
    }

    private void resetSwapState() {
        restoreUseKey = false;
        releaseWaitUntilTick = -1L;
        preSwapSlot = -1;
        blockingObservations.clear();
        clearScheduledSwap();
        clearAxeAwait(false);
        clearStunAwait(false);
        clearShieldBrokenGrace();
        resetPendingAttack();
        pendingAxeTargetId = -1;
    }

    private void clearScheduledSwap() {
        scheduledMaceSlot = -1;
        scheduledTargetId = -1;
        scheduledSwapTick = -1L;
    }

    private void resetPendingAttack() {
        pendingHotbarSlot = -1;
        pendingTargetId = -1;
        pendingActionIsAxe = false;
        pendingStunFollowup = false;
        requireSameTarget = true;
        attackPending = false;
        lastSwapTick = -1L;
    }

    private void updateDive() {
        if (!automaticallyUnequipElytra.getValue()) {
            restoreEquipmentSlot();
            resetFlightState(true);
            return;
        }
        updateReequipReadiness();
        if (hasEquipmentSwap(false)) {
            updateUnequip();
            return;
        }
        if (hasEquipmentSwap(true)) {
            updateReequip();
            return;
        }
        if (!client.player.isGliding()) {
            tryReequip();
            resetFlightState(false);
            return;
        }
        if (flightState == MaceFlightState.WAITING_FOR_GLIDE_END) {
            if (++glideEndWaitTicks > GLIDE_END_TIMEOUT_TICKS) {
                flightState = MaceFlightState.ABORTED_THIS_FLIGHT;
            }
            return;
        }
        if (flightState == MaceFlightState.ABORTED_THIS_FLIGHT) {
            return;
        }
        if (DeferredActionQueue.hasPendingActions() || client.player.isUsingItem()
                || client.player.isTouchingWater() || client.player.isInLava()) {
            return;
        }
        if (InventoryItems.findMaceWithEnchantmentInHotbar(null, true) == -1) {
            return;
        }
        int chestplate = InventoryItems.findChestplateInHotbar();
        if (chestplate == -1 || !shouldStartUnequip()) {
            return;
        }
        startEquipmentSwap(chestplate, false);
        if (!hasEquipmentSwap()) {
            flightState = MaceFlightState.ABORTED_THIS_FLIGHT;
        }
    }

    private void updateUnequip() {
        if (client.player.getInventory().getSelectedSlot() != equipmentSlot) {
            boolean observed = observeChestplateSwap();
            clearEquipmentSwapState();
            finishUnequip(observed);
            return;
        }
        observeChestplateSwap();
        if (!client.player.isGliding() && !chestplateSwapObserved && equipmentStageTicks < EQUIPMENT_USE_TICK) {
            restoreEquipmentSlot();
            resetFlightState(false);
            return;
        }
        if (++equipmentStageTicks < EQUIPMENT_FINISH_TICK
                && !(chestplateSwapObserved && !client.player.isGliding() && equipmentStageTicks > EQUIPMENT_USE_TICK)) {
            return;
        }
        boolean observed = chestplateSwapObserved;
        restoreEquipmentSlot();
        finishUnequip(observed);
    }

    private boolean observeChestplateSwap() {
        if (chestplateSwapObserved || equipmentSlot < 0) {
            return chestplateSwapObserved;
        }
        chestplateSwapObserved = !InventoryItems.wearsElytra()
                && client.player.getInventory().getStack(equipmentSlot).isOf(Items.ELYTRA);
        if (chestplateSwapObserved && reEquipElytra.getValue()) {
            actionReadiness.beginWaiting();
            updateReequipReadiness();
        }
        return chestplateSwapObserved;
    }

    private void finishUnequip(boolean observed) {
        flightState = observed ? MaceFlightState.WAITING_FOR_GLIDE_END : MaceFlightState.ABORTED_THIS_FLIGHT;
        glideEndWaitTicks = 0;
        if (observed) {
            attackDelayMillis = 0L;
        } else {
            actionReadiness.reset();
        }
        if (!client.player.isGliding()) {
            resetFlightState(false);
        }
    }

    private void updateReequip() {
        if (client.player.getInventory().getSelectedSlot() != equipmentSlot) {
            clearEquipmentSwapState();
            actionReadiness.reset();
            return;
        }
        if (++equipmentStageTicks < EQUIPMENT_FINISH_TICK) {
            return;
        }
        restoreEquipmentSlot();
        actionReadiness.reset();
    }

    private void updateReequipReadiness() {
        if (!reEquipElytra.getValue() || InventoryItems.wearsElytra()) {
            actionReadiness.reset();
            return;
        }
        if (client.player.isGliding()) {
            return;
        }
        boolean supported = client.player.isOnGround() || client.player.isTouchingWater()
                || client.player.isInLava() || client.player.isClimbing();
        actionReadiness.update(client.player.getY() - client.player.lastY, supported);
    }

    private void tryReequip() {
        if (!actionReadiness.isReady() || DeferredActionQueue.hasPendingActions() || client.player.isUsingItem()) {
            return;
        }
        int elytra = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.ELYTRA));
        if (elytra == -1) {
            actionReadiness.reset();
            return;
        }
        startEquipmentSwap(elytra, true);
    }

    private void startEquipmentSwap(int slot, boolean reEquip) {
        if (hasEquipmentSwap() || DeferredActionQueue.hasPendingActions()) {
            return;
        }
        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (currentSlot != slot && !DeferredActionQueue.select(this, slot, -1, false)) {
            return;
        }
        originalSlot = currentSlot;
        equipmentSlot = slot;
        equipmentStageTicks = 0;
        reEquippingElytra = reEquip;
        equipmentUseIssued = false;
        chestplateSwapObserved = false;
    }

    private boolean shouldStartUnequip() {
        if (unequipTiming.isSelected(LATE)) {
            return shouldStartLateUnequip();
        }
        List<FallSample> immediate = MovementSimulation.fallSamples(0);
        List<FallSample> delayed = MovementSimulation.fallSamples(1);
        if (immediate.size() <= 1 || delayed.size() <= 1) {
            return false;
        }
        double targetRange = MathHelper.clamp(elytraRange.getValue(), 3.0, 6.0);
        double maxApproach = targetRange + client.player.getVelocity().length() * EQUIPMENT_FINISH_TICK;
        double rangeSquared = maxApproach * maxApproach;
        Vec3d eye = client.player.getEyePos();
        Box bounds = MovementSimulation.sampleBounds(immediate).union(MovementSimulation.sampleBounds(delayed))
                .union(client.player.getBoundingBox().expand(maxApproach));
        double minimumFall = Math.max(MINIMUM_SMASH_DISTANCE, fallDistance.getValue());
        for (LivingEntity target : predictionTargets(bounds)) {
            if (target.getBoundingBox().squaredMagnitude(eye) > rangeSquared) {
                continue;
            }
            if (!MovementSimulation.isHorizontallyReachable(immediate, target, targetRange)
                    && !MovementSimulation.isHorizontallyReachable(delayed, target, targetRange)) {
                continue;
            }
            if (MovementSimulation.firstImpactTick(immediate, target, minimumFall) >= EQUIPMENT_USE_TICK
                    && MovementSimulation.firstImpactTick(delayed, target, minimumFall) >= EQUIPMENT_USE_TICK) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldStartLateUnequip() {
        List<FallSample> first = MovementSimulation.fallSamples(1);
        List<FallSample> second = MovementSimulation.fallSamples(2);
        if (first.size() <= 1 || second.size() <= 1) {
            return false;
        }
        double targetRange = MathHelper.clamp(elytraRange.getValue(), 3.0, 6.0);
        double maxApproach = targetRange + client.player.getVelocity().length() * (EQUIPMENT_FINISH_TICK + 1);
        double maxApproachSquared = maxApproach * maxApproach;
        Vec3d eye = client.player.getEyePos();
        Box bounds = MovementSimulation.sampleBounds(first).union(MovementSimulation.sampleBounds(second))
                .union(client.player.getBoundingBox().expand(maxApproach));
        double minimumFall = Math.max(MINIMUM_SMASH_DISTANCE, fallDistance.getValue());
        int lastTick = Math.round(lateWindow.getValue());
        for (LivingEntity target : predictionTargets(bounds)) {
            if (target.getBoundingBox().squaredMagnitude(eye) > maxApproachSquared) {
                continue;
            }
            int followupTicks = getStunDelayTicks(target);
            TickWindow window = new TickWindow(EQUIPMENT_MIN_FINISH_TICK, lastTick, followupTicks);
            if (MovementSimulation.canHitWithinWindow(first, target, minimumFall, targetRange, window)
                    && MovementSimulation.canHitWithinWindow(second, target, minimumFall, targetRange, window)) {
                return true;
            }
        }
        return false;
    }

    private MaceImpactPrediction predictImpact(int latestTick) {
        List<FallSample> samples = MovementSimulation.fallSamples(0);
        if (samples.size() <= 1) {
            return null;
        }
        double minimumFall = fallDistance.getValue();
        MaceImpactPrediction best = null;
        for (LivingEntity target : predictionTargets(MovementSimulation.sampleBounds(samples))) {
            int tick = MovementSimulation.firstImpactTick(samples, target, minimumFall);
            if (tick < 0 || tick > latestTick) {
                continue;
            }
            if (best == null || tick < best.impactTick()) {
                best = new MaceImpactPrediction(target, tick);
            }
        }
        return best;
    }

    private List<LivingEntity> predictionTargets(Box bounds) {
        return client.world.getEntitiesByClass(LivingEntity.class, bounds.expand(MovementSimulation.attackRange() + 1.0), this::isPredictionTarget);
    }

    private boolean isPredictionTarget(LivingEntity entity) {
        return entity != client.player && entity.isAlive() && !entity.isRemoved() && !entity.isInvulnerable()
                && TargetSelection.isValidTarget(entity) && entity.getY() <= client.player.getY() + 1.0;
    }

    private boolean hasEquipmentSwap() {
        return equipmentSlot >= 0;
    }

    private boolean hasEquipmentSwap(boolean reEquip) {
        return hasEquipmentSwap() && reEquippingElytra == reEquip;
    }

    private void restoreEquipmentSlot() {
        if (!hasEquipmentSwap()) {
            return;
        }
        if (originalSlot >= 0 && client.player.getInventory().getSelectedSlot() == equipmentSlot) {
            InventoryItems.selectHotbarSlot(originalSlot);
        }
        clearEquipmentSwapState();
    }

    private void clearEquipmentSwapState() {
        if (hasEquipmentSwap()) {
            DeferredActionQueue.finish(this, false);
        }
        originalSlot = -1;
        equipmentSlot = -1;
        equipmentStageTicks = 0;
        reEquippingElytra = false;
        equipmentUseIssued = false;
        chestplateSwapObserved = false;
    }

    private void resetFlightState(boolean clearEquipmentState) {
        flightState = MaceFlightState.IDLE;
        glideEndWaitTicks = 0;
        chestplateSwapObserved = false;
        if (!clearEquipmentState) {
            return;
        }
        actionReadiness.reset();
        clearEquipmentSwapState();
    }

    private double effectiveAimRange() {
        double base = range.getValue();
        if (client.player == null || client.player.isOnGround()) {
            return base;
        }
        double speed = client.player.getVelocity().length();
        return base + Math.min(6.0, speed * 2.5);
    }

    private LivingEntity selectAimTarget() {
        double targetRange = effectiveAimRange();
        double rangeSquared = targetRange * targetRange;
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        // Sticky target lock while airborne diving so passing overhead never drops or swaps target mid-dive
        if (lockedAimTargetId != -1 && !client.player.isOnGround()
                && client.world.getEntityById(lockedAimTargetId) instanceof LivingEntity locked
                && isWithinAimBounds(eye, rangeSquared, locked)) {
            return locked;
        }
        Box searchBox = client.player.getBoundingBox().expand(targetRange + 1.0);
        List<LivingEntity> candidates = client.world.getEntitiesByClass(
                LivingEntity.class, searchBox, entity -> isAimCandidate(eye, rangeSquared, entity));
        LivingEntity target = TargetSelection.resolveTarget(
                EntityRaycasts.selectTarget(client.player, candidates, eye, targetPriority));
        return isWithinAimBounds(eye, rangeSquared, target) ? target : null;
    }

    private AimPointSettings aimPointSettings() {
        return new AimPointSettings(aimVector.isSelected(CLOSEST) ? AimPointMode.CLOSEST : AimPointMode.STRAIGHT, multipoint.getValue(), random.getValue(), throughWalls.getValue());
    }

    private AimPointSettings effectiveAimPointSettings(LivingEntity target) {
        AimPointMode mode = aimVector.isSelected(CLOSEST) ? AimPointMode.CLOSEST : AimPointMode.STRAIGHT;
        boolean useRandom = random.getValue();
        LivingEntity stunTarget = activeStunTarget();
        boolean highSpeedDive = client.player != null && !client.player.isOnGround()
                && (client.player.getVelocity().y <= -0.85 || client.player.getVelocity().length() >= 1.0);
        if ((stunTarget != null && target != null && stunTarget.getId() == target.getId()) || highSpeedDive) {
            mode = AimPointMode.CLOSEST;
            useRandom = false;
        } else if (silentRotations.getValue() && target != null && client.player != null && mode == AimPointMode.STRAIGHT) {
            Vec3d eye = client.player.getCameraPosVec(1.0F);
            double dx = target.getX() - eye.x;
            double dz = target.getZ() - eye.z;
            double dy = (target.getY() + target.getHeight() * 0.5) - eye.y;
            if (dx * dx + dz * dz < 0.64 && Math.abs(dy) > 1.0) {
                mode = AimPointMode.CLOSEST;
            }
        }
        return new AimPointSettings(mode, multipoint.getValue(), useRandom, throughWalls.getValue());
    }

    private RotationSettings rotationSettings(LivingEntity target) {
        boolean silent = silentRotations.getValue();
        float hSpeed = horizontalSpeed.getValue() * 32.0F;
        float vSpeed = verticalSpeed.getValue() * 36.0F;
        AimTransition transition = aimTransition();
        if (transition != AimTransition.SNAP && client.player != null) {
            double speed = client.player.getVelocity().length();
            double fallSpeed = Math.max(0.0, -client.player.getVelocity().y);
            float velocityBoost = (float) MathHelper.clamp(1.0 + Math.max(speed - 0.35, fallSpeed - 0.30) * 1.35, 1.0, 3.0);
            if (silent) {
                boolean stunActive = activeStunTarget() != null || isStunSequenceActive();
                boolean urgent = stunActive || isSmashReady() || hasEquipmentSwap(false) || attackPending;
                if (!urgent && target != null) {
                    double reach = currentWeaponReach() + 1.5;
                    urgent = target.getBoundingBox().squaredMagnitude(client.player.getCameraPosVec(1.0F)) <= reach * reach;
                }
                float scale = (stunActive ? 4.8F : urgent ? 2.6F : 1.75F) * velocityBoost;
                hSpeed *= scale;
                vSpeed *= scale;
                if (stunActive) {
                    transition = AimTransition.EASED;
                }
            } else {
                boolean stunActive = activeStunTarget() != null || isStunSequenceActive();
                float visibleBoost = MathHelper.clamp(velocityBoost * (stunActive ? 1.65F : 1.0F), 1.0F, 3.8F);
                hSpeed *= visibleBoost;
                vSpeed *= visibleBoost;
            }
        }
        return new RotationSettings(silent ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                transition, hSpeed, vSpeed);
    }

    private LivingEntity attackTarget() {
        LivingEntity target = crosshairLivingTarget();
        if (target == null || !TargetSelection.isValidTarget(target)) {
            return null;
        }
        LivingEntity locked = TargetSelection.resolveTarget(target);
        if (locked == null || locked != target || target.isInvulnerable() || target.isDead() || !target.isAlive()) {
            return null;
        }
        return target;
    }

    private void resetAttackTimer() {
        float sampledCps = cps.sampleGaussian();
        attackDelayMillis = sampledCps <= 0.0F ? 0L : (long) (1000.0F / sampledCps);
        attackTimer.reset();
    }

    private boolean isSmashReady() {
        return !client.player.isGliding() && ServerFallDistanceTracker.fallDistance(client.player) > fallDistance.getValue();
    }

    private double currentWeaponReach() {
        if (client.player == null) {
            return 3.0;
        }
        double vanillaReach = Math.max(0.0, client.player.getEntityInteractionRange());
        ItemStack held = client.player.getMainHandStack();
        if (held != null && !held.isEmpty() && InventoryItems.isSpear(held.getItem())) {
            return Math.max(vanillaReach, AttackRange.getEffectiveMaxRange(client.player));
        }
        return vanillaReach;
    }

    private LivingEntity crosshairLivingTarget() {
        double allowedReach = currentWeaponReach();
        if (client.player != null
                && client.crosshairTarget instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity target
                && target.isAlive() && !target.isRemoved()) {
            Vec3d eye = client.player.getCameraPosVec(1.0F);
            if (eye.squaredDistanceTo(entityHit.getPos()) <= allowedReach * allowedReach + 1.0E-4) {
                return target;
            }
        }
        // High fall speed / grand velocity fallback: if vanilla crosshairTarget is 1-frame stale,
        // verify a live raycast along our active effective rotation strictly within current weapon reach.
        if (autoAim.getValue() && client.player != null && client.world != null && rotations().isOwnedBy(this)) {
            return raycastEffectiveRotationTarget(allowedReach);
        }
        return null;
    }

    private LivingEntity raycastEffectiveRotationTarget(double reach) {
        var effective = rotations().effectiveRotation();
        if (effective == null || reach <= 0.0) {
            return null;
        }
        Vec3d start = client.player.getCameraPosVec(1.0F);
        Vec3d look = Vec3d.fromPolar(effective.pitch(), effective.yaw());
        Vec3d end = start.add(look.multiply(reach));
        Vec3d limitedEnd = end;
        if (!throughWalls.getValue()) {
            BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                    start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
            if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                limitedEnd = blockHit.getPos();
            }
        }
        double maxDistSq = start.squaredDistanceTo(limitedEnd);
        EntityHitResult hit = ProjectileUtil.raycast(
                client.player,
                start,
                limitedEnd,
                client.player.getBoundingBox().stretch(limitedEnd.subtract(start)).expand(1.0),
                entity -> entity instanceof LivingEntity living && !entity.isSpectator()
                        && entity.canHit() && living.isAlive() && !living.isRemoved()
                        && TargetSelection.isValidTarget(living),
                maxDistSq);
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private boolean canUseMace() {
        ItemStack held = InventoryItems.mainHandStack();
        if (held.isOf(Items.MACE)) {
            return true;
        }
        if (!acceptsCurrentSlot()) {
            return false;
        }
        return InventoryItems.findMaceWithEnchantmentInHotbar(null, true) != -1;
    }

    private void releaseOrCancelRotation() {
        if (silentRotations.getValue()) {
            rotations().beginReturn(this);
        } else {
            rotations().cancel(this);
        }
    }

    private void finishDeferredDisable() {
        if (rotations().beginReturn(this)) {
            return;
        }
        disablePending = false;
        super.onDisable();
    }

    private AimTransition aimTransition() {
        if (aimMath.isSelected(NATURAL)) {
            return AimTransition.NATURAL;
        }
        return aimMath.isSelected(BLATANT) ? AimTransition.SNAP : AimTransition.EASED;
    }

    private boolean isWithinAimBounds(Vec3d eye, double rangeSquared, LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isDead() || entity.isRemoved()
                || !TargetSelection.isValidTarget(entity)) {
            return false;
        }
        return entity.getBoundingBox().squaredMagnitude(eye) <= rangeSquared && isWithinFov(entity);
    }

    private boolean isWithinFov(LivingEntity entity) {
        if (client.player == null || entity == null) {
            return false;
        }
        float halfYaw = yawFOV.getValue() * 0.5F;
        float halfPitch = pitchFOV.getValue() * 0.5F;
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        Box box = entity.getBoundingBox();
        Vec3d closest = new Vec3d(
                MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(eye.y, box.minY, box.maxY),
                MathHelper.clamp(eye.z, box.minZ, box.maxZ));
        float camYaw = client.player.getYaw();
        float camPitch = client.player.getPitch();
        boolean inCameraFov = isPointWithinFov(eye, entity.getEyePos(), camYaw, camPitch, halfYaw, halfPitch)
                || isPointWithinFov(eye, box.getCenter(), camYaw, camPitch, halfYaw, halfPitch)
                || isPointWithinFov(eye, closest, camYaw, camPitch, halfYaw, halfPitch);
        if (inCameraFov) {
            return true;
        }
        if (silentRotations.getValue() && rotations().isOwnedBy(this) && rotations().snapshot().target() == entity) {
            var effective = rotations().effectiveRotation();
            float effYaw = effective.yaw();
            float effPitch = effective.pitch();
            return isPointWithinFov(eye, entity.getEyePos(), effYaw, effPitch, halfYaw, halfPitch)
                    || isPointWithinFov(eye, box.getCenter(), effYaw, effPitch, halfYaw, halfPitch)
                    || isPointWithinFov(eye, closest, effYaw, effPitch, halfYaw, halfPitch);
        }
        return false;
    }

    private boolean isPointWithinFov(Vec3d eye, Vec3d point, float yaw, float pitch, float halfYaw, float halfPitch) {
        Vec3d delta = point.subtract(eye);
        if (delta.lengthSquared() < 1.0E-8) {
            return true;
        }
        Vec3d dir = delta.normalize();
        Vec3d forward = Vec3d.fromPolar(pitch, yaw);
        Vec3d right = Vec3d.fromPolar(0.0F, yaw + 90.0F);
        Vec3d up = Vec3d.fromPolar(pitch - 90.0F, yaw);
        double f = dir.dotProduct(forward);
        double r = Math.abs(dir.dotProduct(right));
        double u = Math.abs(dir.dotProduct(up));
        double horizAngle = Math.toDegrees(Math.atan2(r, f));
        double vertAngle = Math.toDegrees(Math.atan2(u, Math.hypot(f, r)));
        return horizAngle <= (double) halfYaw + 1.0E-4 && vertAngle <= (double) halfPitch + 1.0E-4;
    }

    private boolean isAimCandidate(Vec3d eye, double rangeSquared, LivingEntity entity) {
        return entity != client.player && isWithinAimBounds(eye, rangeSquared, entity);
    }

    public static void appendBootstrapSettings(AutoMaceSwap Module) {
        CatalogSettings.append(Module, fallDistance);
        CatalogSettings.append(Module, cps);
        CatalogSettings.append(Module, delay);
        CatalogSettings.append(Module, itemWhitelist);
        CatalogSettings.append(Module, hotbarWhitelist);
        CatalogSettings.append(Module, maceSwapRanges);
        CatalogSettings.append(Module, stunSlam);
        CatalogSettings.append(Module, autoAxeStun);
        CatalogSettings.append(Module, stunReactionDelay);
        CatalogSettings.append(Module, stunSwitchDelay);
        CatalogSettings.append(Module, spearDTap);
        CatalogSettings.append(Module, minimumDamage);
        CatalogSettings.append(Module, returnTo);
        CatalogSettings.append(Module, returnSlots);
        CatalogSettings.append(Module, returnItems);
        CatalogSettings.append(Module, autoAim);
        CatalogSettings.append(Module, aimMath);
        CatalogSettings.append(Module, aimVector);
        CatalogSettings.append(Module, targetPriority);
        CatalogSettings.append(Module, random);
        CatalogSettings.append(Module, silentRotations);
        CatalogSettings.append(Module, throughWalls);
        CatalogSettings.append(Module, horizontalSpeed);
        CatalogSettings.append(Module, verticalSpeed);
        CatalogSettings.append(Module, range);
        CatalogSettings.append(Module, yawFOV);
        CatalogSettings.append(Module, pitchFOV);
        CatalogSettings.append(Module, multipoint);
        CatalogSettings.append(Module, automaticallyUnequipElytra);
        CatalogSettings.append(Module, elytraRange);
        CatalogSettings.append(Module, unequipTiming);
        CatalogSettings.append(Module, lateWindow);
        CatalogSettings.append(Module, reEquipElytra);
    }

    public static void assignBootstrapSettings(
            AutoMaceSwap Module,
            FloatSetting preparedFallDistance,
            FloatRangeSetting preparedCps,
            FloatRangeSetting preparedDelay,
            ItemListSetting preparedItemWhitelist,
            HotbarSlotsSetting preparedHotbarWhitelist,
            BooleanSetting preparedStunSlam,
            BooleanSetting preparedAutoAxeStun,
            FloatRangeSetting preparedStunReactionDelay,
            FloatRangeSetting preparedStunSwitchDelay,
            BooleanSetting preparedSpearDTap,
            FloatSetting preparedMinimumDamage,
            ChoiceSetting preparedReturnTo,
            HotbarSlotsSetting preparedReturnSlots,
            ItemListSetting preparedReturnItems,
            BooleanSetting preparedAutoAim,
            ChoiceSetting preparedAimMath,
            ChoiceSetting preparedAimVector,
            ChoiceSetting preparedTargetPriority,
            BooleanSetting preparedRandom,
            BooleanSetting preparedSilentRotations,
            BooleanSetting preparedThroughWalls,
            FloatSetting preparedHorizontalSpeed,
            FloatSetting preparedVerticalSpeed,
            FloatSetting preparedRange,
            FloatSetting preparedYawFOV,
            FloatSetting preparedPitchFOV,
            FloatSetting preparedMultipoint,
            BooleanSetting preparedAutomaticallyUnequipElytra,
            FloatSetting preparedElytraRange,
            ChoiceSetting preparedUnequipTiming,
            FloatSetting preparedLateWindow,
            BooleanSetting preparedReEquipElytra) {
        fallDistance = preparedFallDistance;
        cps = preparedCps;
        delay = preparedDelay;
        itemWhitelist = preparedItemWhitelist;
        hotbarWhitelist = preparedHotbarWhitelist;
        stunSlam = preparedStunSlam;
        autoAxeStun = preparedAutoAxeStun;
        stunReactionDelay = preparedStunReactionDelay;
        stunSwitchDelay = preparedStunSwitchDelay;
        spearDTap = preparedSpearDTap;
        minimumDamage = preparedMinimumDamage;
        returnTo = preparedReturnTo;
        returnSlots = preparedReturnSlots;
        returnItems = preparedReturnItems;
        autoAim = preparedAutoAim;
        aimMath = preparedAimMath;
        aimVector = preparedAimVector;
        targetPriority = preparedTargetPriority;
        random = preparedRandom;
        silentRotations = preparedSilentRotations;
        throughWalls = preparedThroughWalls;
        horizontalSpeed = preparedHorizontalSpeed;
        verticalSpeed = preparedVerticalSpeed;
        range = preparedRange;
        yawFOV = preparedYawFOV;
        pitchFOV = preparedPitchFOV;
        multipoint = preparedMultipoint;
        automaticallyUnequipElytra = preparedAutomaticallyUnequipElytra;
        elytraRange = preparedElytraRange;
        unequipTiming = preparedUnequipTiming;
        lateWindow = preparedLateWindow;
        reEquipElytra = preparedReEquipElytra;
    }
}
