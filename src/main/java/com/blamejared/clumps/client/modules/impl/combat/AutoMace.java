package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.modules.impl.combat.MaceFlightState;
import com.blamejared.clumps.client.modules.impl.combat.MaceImpactPrediction;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.handlers.ServerFallDistanceTracker;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.utils.CooldownTimer;
import com.blamejared.clumps.client.utils.interaction.ActionReadiness;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.simulation.FallSample;
import com.blamejared.clumps.client.utils.simulation.MovementSimulation;
import com.blamejared.clumps.client.utils.simulation.TickWindow;
import com.blamejared.clumps.client.utils.strings.Concat;
import com.blamejared.clumps.client.utils.targeting.AimPointMode;
import com.blamejared.clumps.client.utils.targeting.AimPointSettings;
import com.blamejared.clumps.client.utils.targeting.EntityRaycasts;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import java.util.List;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AutoMace extends Module {
    private static final CharSequence BLATANT = CatalogText.literal("Blatant");
    private static final CharSequence LATE = CatalogText.literal("Late");
    private static final CharSequence CLOSEST = CatalogText.literal("Closest");
    private static final CharSequence NATURAL = CatalogText.literal("Natural");
    private static final CharSequence RANGE_SEPARATOR = CatalogText.literal("-");
    private static final int EQUIPMENT_USE_TICK = 2;
    private static final int EQUIPMENT_FINISH_TICK = 4;
    private static final int MINIMUM_PREDICTED_HIT_TICK = 3;
    private static final int AIM_PREDICTION_TICKS = 6;
    private static final int GLIDE_END_TIMEOUT_TICKS = 8;
    private static final double MINIMUM_SMASH_DISTANCE = 1.5;

    private static FloatSetting fallDistance;
    private static FloatRangeSetting cps;
    private static BooleanSetting workWithMaceSwap;
    private static BooleanSetting autoAim;
    private static ChoiceSetting aimMath;
    private static ChoiceSetting aimVector;
    private static ChoiceSetting targetPriority;
    private static BooleanSetting random;
    private static BooleanSetting silentRotations;
    private static BooleanSetting throughWalls;
    private static FloatSetting horizontalSpeed;
    private static FloatSetting verticalSpeed;
    private static FloatSetting range;
    private static FloatSetting yawFOV;
    private static FloatSetting pitchFOV;
    private static FloatSetting multipoint;
    private static BooleanSetting automaticallyUnequipElytra;
    private static ChoiceSetting unequipTiming;
    private static FloatSetting lateWindow;
    private static BooleanSetting reEquipElytra;

    private CooldownTimer attackTimer;
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
    private boolean wasSmashReady;

    public AutoMace() {
        super(new ModuleInfo(CatalogText.literal("Auto Mace"), CatalogText.literal("Automatically attacks while holding a mace"), ModuleCategory.COMBAT));
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
        configureSettings();
    }

    private void configureSettings() {
        workWithMaceSwap.description(CatalogText.literal("With the mace swap Module enabled it will allow you to hold allowed items to automatically swap to a mace."));
        aimMath.setVisibilityPredicate(ignored -> autoAim.getValue());
        aimVector.setVisibilityPredicate(ignored -> autoAim.getValue());
        targetPriority.setVisibilityPredicate(ignored -> autoAim.getValue());
        random.setVisibilityPredicate(ignored -> autoAim.getValue());
        silentRotations.setVisibilityPredicate(ignored -> autoAim.getValue());
        throughWalls.setVisibilityPredicate(ignored -> autoAim.getValue());
        horizontalSpeed.setVisibilityPredicate(ignored -> autoAim.getValue() && !aimMath.isSelected(BLATANT));
        verticalSpeed.setVisibilityPredicate(ignored -> autoAim.getValue() && !aimMath.isSelected(BLATANT));
        range.setVisibilityPredicate(ignored -> autoAim.getValue() || automaticallyUnequipElytra.getValue());
        yawFOV.setVisibilityPredicate(ignored -> autoAim.getValue());
        pitchFOV.setVisibilityPredicate(ignored -> autoAim.getValue());
        multipoint.setVisibilityPredicate(ignored -> autoAim.getValue());
        cps.tooltip("Timing");
        horizontalSpeed.tooltip("Timing");
        verticalSpeed.tooltip("Timing");
        automaticallyUnequipElytra.description(CatalogText.literal("Equips a chestplate when the predicted fall can reach a mace target."));
        unequipTiming.description(CatalogText.literal("Normal starts at the first predicted opportunity. Late keeps gliding until a hit is inside the Late Window."));
        unequipTiming.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue());
        lateWindow.description(CatalogText.literal("Maximum ticks from starting the armor swap to the predicted hit. Lower swaps later; higher gives more preparation time. Includes the stun follow-up when needed."));
        lateWindow.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue() && unequipTiming.isSelected(LATE));
        reEquipElytra.description(CatalogText.literal("Puts the elytra back on after a smash bounce or when the dive ends on the ground, in liquid, or on a climbable block."));
        reEquipElytra.setVisibilityPredicate(ignored -> automaticallyUnequipElytra.getValue());
        automaticallyUnequipElytra.tooltip("Elytra");
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
        resetFlightState(true);
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
        resetFlightState(true);
        if (silentRotations.getValue() && rotations().isOwnedBy(this)) {
            disablePending = true;
            return;
        }
        disablePending = false;
        rotations().cancel(this);
        super.onDisable();
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
        double approachSpeed = client.player.getVelocity().length();
        if (client.player.isGliding() || client.player.isOnGround()
                || (client.player.getVelocity().y >= 0.05 && approachSpeed < 0.85) || hasEquipmentSwap()) {
            releaseOrCancelRotation();
            return;
        }
        if (!canUseMace()) {
            if (silentRotations.getValue() && rotations().isOwnedBy(this)
                    && (MaceSwap.hasPendingSwap() || AutoMaceSwap.hasPendingSwap() || SpearReach.hasPendingSwap())) {
                return;
            }
            releaseOrCancelRotation();
            return;
        }
        boolean ready = isSmashReady();
        int horizon = approachSpeed >= 1.0 ? AIM_PREDICTION_TICKS + 2 : AIM_PREDICTION_TICKS;
        MaceImpactPrediction prediction = predictImpact(horizon);
        if (!ready && prediction == null) {
            releaseOrCancelRotation();
            return;
        }
        LivingEntity target = selectAimTarget();
        if (target == null && prediction != null) {
            target = TargetSelection.resolveTarget(prediction.target());
        }
        if (target == null) {
            releaseOrCancelRotation();
            return;
        }
        boolean accepted;
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        double reachMargin = MovementSimulation.attackRange() + 0.35;
        boolean outsideImmediateReach = target.getBoundingBox().squaredMagnitude(eye) > reachMargin * reachMargin;
        if (prediction != null && prediction.target() == target && prediction.impactTick() >= 1
                && (outsideImmediateReach || !ready)) {
            int leadTicks = Math.min(2, prediction.impactTick());
            accepted = rotations().aim(this, RotationPriority.HIGH, MovementSimulation.predictedAimPoint(target, leadTicks), rotationSettings());
        } else {
            accepted = rotations().aim(this, RotationPriority.HIGH, target, aimPointSettings(), rotationSettings());
        }
        if (!accepted) {
            releaseOrCancelRotation();
        }
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (trackedPlayer != client.player) {
            resetFlightState(true);
            trackedPlayer = client.player;
        }
        if (disablePending || isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            wasSmashReady = false;
            return;
        }
        if (!client.player.isAlive()) {
            resetFlightState(true);
            wasSmashReady = false;
            return;
        }
        boolean smashNow = isSmashReady();
        if (smashNow && !wasSmashReady) {
            attackDelayMillis = 0L;
        }
        wasSmashReady = smashNow;
        updateDive();
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onHotbarKey(HotbarKeyEvent event) {
        if (event.slot != 0 || disablePending || isOutsideWorld()
                || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (!workWithMaceSwap.getValue() || InventoryItems.mainHandStack().isOf(Items.MACE)
                || hasEquipmentSwap() || DeferredActionQueue.hasPendingActions()) {
            return;
        }
        if (client.player.isUsingItem() && InventoryItems.isSpear(client.player.getActiveItem().getItem())) {
            return;
        }
        if (!isSmashReady() || !isAttackTimerReady()) {
            return;
        }
        LivingEntity target = attackTarget();
        if (target == null) {
            return;
        }
        AutoMaceSwap combinedSwap = (AutoMaceSwap) ClumpsClient.ModuleManager.findFeature(AutoMaceSwap.class);
        if (combinedSwap != null && combinedSwap.isEnabled()) {
            if (combinedSwap.requestMaceSwap(target)) {
                resetAttackTimer();
            }
            return;
        }
        MaceSwap swap = (MaceSwap) ClumpsClient.ModuleManager.findFeature(MaceSwap.class);
        if (swap == null || !swap.isEnabled() || !swap.requestMaceSwap(target)) {
            return;
        }
        resetAttackTimer();
    }

    @Subscribe
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null
                || hasEquipmentSwap() || DeferredActionQueue.hasPendingActions() || !isSmashReady()) {
            return;
        }
        LivingEntity target = attackTarget();
        if (target == null || !canUseMace() || !isAttackTimerReady()) {
            return;
        }
        boolean vanillaCrosshairMatches = client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() == target;
        if (vanillaCrosshairMatches) {
            event.addClick();
            resetAttackTimer();
        } else if (client.attackCooldown <= 0 && actions().executeEntityAttack(this, target, true).succeeded()) {
            resetAttackTimer();
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
        if (++equipmentStageTicks < EQUIPMENT_FINISH_TICK) {
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
        if (!observed) {
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
        Box bounds = MovementSimulation.sampleBounds(immediate).union(MovementSimulation.sampleBounds(delayed));
        double minimumFall = Math.max(MINIMUM_SMASH_DISTANCE, fallDistance.getValue());
        double targetRange = range.getValue();
        for (LivingEntity target : predictionTargets(bounds)) {
            if (!MovementSimulation.isHorizontallyReachable(immediate, target, targetRange)
                    && !MovementSimulation.isHorizontallyReachable(delayed, target, targetRange)) {
                continue;
            }
            if (MovementSimulation.firstImpactTick(immediate, target, minimumFall) >= MINIMUM_PREDICTED_HIT_TICK
                    && MovementSimulation.firstImpactTick(delayed, target, minimumFall) >= MINIMUM_PREDICTED_HIT_TICK) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldStartLateUnequip() {
        List<FallSample> first = MovementSimulation.fallSamples(2);
        List<FallSample> second = MovementSimulation.fallSamples(3);
        if (first.size() <= 1 || second.size() <= 1) {
            return false;
        }
        Box bounds = MovementSimulation.sampleBounds(first).union(MovementSimulation.sampleBounds(second));
        double minimumFall = Math.max(MINIMUM_SMASH_DISTANCE, fallDistance.getValue());
        int lastTick = Math.round(lateWindow.getValue());
        AutoMaceSwap combinedSwap = (AutoMaceSwap) ClumpsClient.ModuleManager.findFeature(AutoMaceSwap.class);
        MaceSwap swap = (MaceSwap) ClumpsClient.ModuleManager.findFeature(MaceSwap.class);
        for (LivingEntity target : predictionTargets(bounds)) {
            int followupTicks = combinedSwap != null && combinedSwap.isEnabled()
                    ? combinedSwap.getStunDelayTicks(target)
                    : (swap == null ? 0 : swap.getStunDelayTicks(target));
            TickWindow window = new TickWindow(EQUIPMENT_FINISH_TICK, lastTick, followupTicks);
            if (MovementSimulation.canHitWithinWindow(first, target, minimumFall, range.getValue(), window)
                    && MovementSimulation.canHitWithinWindow(second, target, minimumFall, range.getValue(), window)) {
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
        DeferredActionQueue.finish(this, false);
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
        return base + Math.min(6.0, client.player.getVelocity().length() * 2.5);
    }

    private LivingEntity selectAimTarget() {
        double targetRange = effectiveAimRange();
        double rangeSquared = targetRange * targetRange;
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        List<LivingEntity> candidates = client.world.getEntitiesByClass(LivingEntity.class, client.player.getBoundingBox().expand(targetRange + 1.0), entity -> isAimCandidate(eye, rangeSquared, entity));
        LivingEntity target = TargetSelection.resolveTarget(
                EntityRaycasts.selectTarget(client.player, candidates, eye, targetPriority));
        if (target == null || target.getBoundingBox().squaredMagnitude(eye) > rangeSquared) {
            return null;
        }
        return isWithinFov(target) ? target : null;
    }

    private AimPointSettings aimPointSettings() {
        boolean highSpeedDive = client.player != null && !client.player.isOnGround()
                && (client.player.getVelocity().y <= -0.85 || client.player.getVelocity().length() >= 1.0);
        AimPointMode mode = (highSpeedDive || aimVector.isSelected(CLOSEST)) ? AimPointMode.CLOSEST : AimPointMode.STRAIGHT;
        boolean useRandom = !highSpeedDive && random.getValue();
        return new AimPointSettings(mode, multipoint.getValue(), useRandom, throughWalls.getValue());
    }

    private RotationSettings rotationSettings() {
        boolean silent = silentRotations.getValue();
        float hSpeed = horizontalSpeed.getValue() * 20.0F;
        float vSpeed = verticalSpeed.getValue() * 20.0F;
        AimTransition transition = aimTransition();
        if (transition != AimTransition.SNAP && client.player != null) {
            double speed = client.player.getVelocity().length();
            double fallSpeed = Math.max(0.0, -client.player.getVelocity().y);
            float velocityBoost = (float) MathHelper.clamp(1.0 + Math.max(speed - 0.5, fallSpeed - 0.45) * 1.15, 1.0, 2.8);
            if (silent) {
                float scale = (isSmashReady() ? 2.4F : 1.6F) * velocityBoost;
                hSpeed *= scale;
                vSpeed *= scale;
            } else if (fallSpeed >= 0.7 || speed >= 0.85) {
                float visibleBoost = MathHelper.clamp(velocityBoost, 1.0F, 2.0F);
                hSpeed *= visibleBoost;
                vSpeed *= visibleBoost;
            }
        }
        return new RotationSettings(silent ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, transition, hSpeed, vSpeed);
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

    private LivingEntity crosshairLivingTarget() {
        if (client.crosshairTarget instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity target
                && target.isAlive() && !target.isRemoved()) {
            return target;
        }
        if (autoAim.getValue() && client.player != null && client.world != null && rotations().isOwnedBy(this)) {
            return raycastEffectiveRotationTarget();
        }
        return null;
    }

    private LivingEntity raycastEffectiveRotationTarget() {
        var effective = rotations().effectiveRotation();
        if (effective == null) {
            return null;
        }
        double reach = client.player.getEntityInteractionRange();
        Vec3d start = client.player.getCameraPosVec(1.0F);
        Vec3d look = Vec3d.fromPolar(effective.pitch(), effective.yaw());
        Vec3d end = start.add(look.multiply(reach));
        Vec3d limitedEnd = end;
        if (!throughWalls.getValue()) {
            BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                    start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));
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
        if (!workWithMaceSwap.getValue()) {
            return false;
        }
        AutoMaceSwap combinedSwap = (AutoMaceSwap) ClumpsClient.ModuleManager.findFeature(AutoMaceSwap.class);
        if (combinedSwap != null && combinedSwap.isEnabled()) {
            if (!AutoMaceSwap.acceptsCurrentSlot()) {
                return false;
            }
            return InventoryItems.findMaceWithEnchantmentInHotbar(null, true) != -1;
        }
        MaceSwap swap = (MaceSwap) ClumpsClient.ModuleManager.findFeature(MaceSwap.class);
        if (swap == null || !swap.isEnabled()) {
            return false;
        }
        if (!MaceSwap.itemWhitelist.acceptsHeldItem() || !MaceSwap.hotbarWhitelist.permitsCurrentSlot()) {
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

    private boolean isWithinFov(LivingEntity entity) {
        return EntityRaycasts.isWithinFov(client.player, entity, yawFOV.getValue(), pitchFOV.getValue());
    }

    private boolean isAimCandidate(Vec3d eye, double rangeSquared, LivingEntity entity) {
        return entity != client.player && !entity.isDead() && entity.isAlive() && TargetSelection.isValidTarget(entity)
                && eye.squaredDistanceTo(entity.getEntityPos()) <= rangeSquared && isWithinFov(entity);
    }

    
    public static void appendBootstrapSettings(AutoMace Module) {
        CatalogSettings.append(Module, fallDistance);
        CatalogSettings.append(Module, cps);
        CatalogSettings.append(Module, workWithMaceSwap);
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
        CatalogSettings.append(Module, unequipTiming);
        CatalogSettings.append(Module, lateWindow);
        CatalogSettings.append(Module, reEquipElytra);
    }

    
    public static void assignBootstrapSettings(AutoMace Module, FloatRangeSetting preparedCps, BooleanSetting preparedWorkWithMaceSwap, FloatSetting preparedMultipoint, FloatSetting preparedPitchFOV, ChoiceSetting preparedTargetPriority, FloatSetting preparedRange, BooleanSetting preparedReEquipElytra, BooleanSetting preparedSilentRotations, FloatSetting preparedFallDistance, BooleanSetting preparedAutomaticallyUnequipElytra, BooleanSetting preparedAutoAim, BooleanSetting preparedRandom, FloatSetting preparedVerticalSpeed, BooleanSetting preparedThroughWalls, FloatSetting preparedYawFOV, ChoiceSetting preparedUnequipTiming, FloatSetting preparedHorizontalSpeed, FloatSetting preparedLateWindow, ChoiceSetting preparedAimMath, ChoiceSetting preparedAimVector) {
        cps = preparedCps;
        workWithMaceSwap = preparedWorkWithMaceSwap;
        multipoint = preparedMultipoint;
        pitchFOV = preparedPitchFOV;
        targetPriority = preparedTargetPriority;
        range = preparedRange;
        reEquipElytra = preparedReEquipElytra;
        silentRotations = preparedSilentRotations;
        fallDistance = preparedFallDistance;
        automaticallyUnequipElytra = preparedAutomaticallyUnequipElytra;
        autoAim = preparedAutoAim;
        random = preparedRandom;
        verticalSpeed = preparedVerticalSpeed;
        throughWalls = preparedThroughWalls;
        yawFOV = preparedYawFOV;
        unequipTiming = preparedUnequipTiming;
        horizontalSpeed = preparedHorizontalSpeed;
        lateWindow = preparedLateWindow;
        aimMath = preparedAimMath;
        aimVector = preparedAimVector;
    }
}
