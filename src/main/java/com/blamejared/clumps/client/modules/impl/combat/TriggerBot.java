package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.interaction.AttackAttemptEvent;
import com.blamejared.clumps.client.event.interaction.BlockBreakingEvent;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.event.world.EntityHitboxEvent;
import com.blamejared.clumps.client.handlers.AttackCooldownTracker;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.LatencyTracker;
import com.blamejared.clumps.client.handlers.MouseSensitivity;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.rotation.RotationManager;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.impl.world.GhostHand;
import com.blamejared.clumps.client.modules.internals.Targets;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.HotbarSlotsSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.utils.CooldownTimer;
import com.blamejared.clumps.client.utils.Rotation;
import com.blamejared.clumps.client.utils.interaction.EntityInteractions;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.strings.Concat;
import com.blamejared.clumps.client.utils.targeting.AimPointMode;
import com.blamejared.clumps.client.utils.targeting.AimPointSettings;
import com.blamejared.clumps.client.utils.targeting.AttackRange;
import com.blamejared.clumps.client.utils.targeting.EntityPredicates;
import com.blamejared.clumps.client.utils.targeting.EntityRaycasts;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class TriggerBot extends Module {
    private static final CharSequence CRIT_NONE = CatalogText.literal("None");
    private static final CharSequence CRIT_PRECISE = CatalogText.literal("pCrit");
    private static final CharSequence CRIT_NORMAL = CatalogText.literal("Normal");
    private static final CharSequence ALIGN_OFF = CatalogText.literal("Off");
    private static final CharSequence ALIGN_VISIBLE = CatalogText.literal("Visible");
    private static final CharSequence EMPTY_SUFFIX = Concat.strings();
    private static final CharSequence SECTION_TIMING = CatalogText.literal("Timing");
    private static final CharSequence SECTION_CONDITIONS = CatalogText.literal("Conditions");
    private static final CharSequence SECTION_AIM = CatalogText.literal("Aim");

    private static final long SHIELD_BREAK_FOLLOWUP_TICKS = 12L;

    private static ChoiceSetting critMode;
    private static BooleanSetting critOnlyOnSpace;
    private static BooleanSetting attackShields;
    private static BooleanSetting hurtTimeCheck;
    private static BooleanSetting noSweepOrPickHits;
    private static FloatRangeSetting cooldown;
    private static FloatRangeSetting critCooldown;
    private static FloatRangeSetting critRange;
    private static FloatSetting attackDistance;
    private static FloatRangeSetting reactionDelay;
    private static FloatRangeSetting attackDelay;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;

    private static final BooleanSetting pingHurtTime = new BooleanSetting(
            CatalogText.literal("Ping HurtTime"), true);
    private static final BooleanSetting holdToAttack = new BooleanSetting(
            CatalogText.literal("Hold Attack"), false);
    private static final BooleanSetting dontBreakBlocks = new BooleanSetting(
            CatalogText.literal("Don't Break Blocks"), false);
    private static final ChoiceSetting aimAlign = new ChoiceSetting(
            CatalogText.literal("Aim Align"), ALIGN_OFF, ALIGN_OFF, ALIGN_VISIBLE);
    private static final FloatSetting alignSpeed = new FloatSetting(
            CatalogText.literal("Align Speed"), 4.5f, 1.0f, 10.0f, 0.5f, ignored -> !aimAlign.isSelected(ALIGN_OFF));
    private static final FloatSetting alignFov = new FloatSetting(
            CatalogText.literal("Align FOV"), 18.0f, 5.0f, 60.0f, 1.0f, CatalogText.literal(" deg"),
            ignored -> !aimAlign.isSelected(ALIGN_OFF));

    private CooldownTimer reactionTimer;
    private CooldownTimer attackTimer;
    private ClientPlayerEntity trackedPlayer;
    private ClientWorld trackedWorld;
    private int acquiredTargetId;
    private long acquiredLastSeenMs;
    private long lastAttackTick;
    private long lastSetbackTick;
    private long lastShieldBreakTick;
    private int lastShieldBreakTargetId;
    private long reactionDelayMillis;
    private long attackDelayMillis;
    private double sampledGroundCooldown;
    private double sampledCritCooldown;

    public TriggerBot() {
        super(new ModuleInfo(
                CatalogText.literal("Trigger Bot"),
                CatalogText.literal("Attacks when aiming at targets"),
                ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        reactionTimer = new CooldownTimer();
        attackTimer = new CooldownTimer();
        resetCombatState();
        sampleAttackCooldowns();

        critRange.setAllowOutOfRange(true);
        attackDistance.setAllowOutOfRange(true);
        attackDistance.displayAs(0.0f, CatalogText.literal("Vanilla Reach"));
        attackDelay.description(CatalogText.literal("Delay between attacks"));
        reactionDelay.description(CatalogText.literal("Time on target before attacking"));
        attackDistance.description(CatalogText.literal("Only attack when crosshair hit is within this distance (0 = vanilla reach)"));
        pingHurtTime.description(CatalogText.literal("Leads target hurt-time by half your ping (capped at 2 ticks) for snappier trades"));
        holdToAttack.description(CatalogText.literal("Only triggers while holding the Attack key"));
        dontBreakBlocks.description(CatalogText.literal("Prevents breaking blocks while holding the Attack key for Trigger Bot"));
        aimAlign.description(CatalogText.literal("Smoothly aligns visible aim onto targets within Align FOV"));
        alignSpeed.description(CatalogText.literal("Maximum smooth angular alignment speed"));
        alignFov.description(CatalogText.literal("Maximum angle from crosshair to assist alignment"));

        critOnlyOnSpace.setVisibilityPredicate(ignored -> !critMode.isSelected(CRIT_NONE));
        critRange.setVisibilityPredicate(ignored -> !critMode.isSelected(CRIT_NONE));
        critCooldown.setVisibilityPredicate(ignored -> !critMode.isSelected(CRIT_NONE));
        pingHurtTime.setVisibilityPredicate(ignored -> hurtTimeCheck.getValue());
        dontBreakBlocks.setVisibilityPredicate(ignored -> holdToAttack.getValue());

        cooldown.tooltip(SECTION_TIMING);
        critCooldown.tooltip(SECTION_TIMING);
        reactionDelay.tooltip(SECTION_TIMING);
        attackDelay.tooltip(SECTION_TIMING);
        pingHurtTime.tooltip(SECTION_TIMING);
        itemWhitelist.tooltip(SECTION_CONDITIONS);
        hotbarWhitelist.tooltip(SECTION_CONDITIONS);
        holdToAttack.tooltip(SECTION_CONDITIONS);
        dontBreakBlocks.tooltip(SECTION_CONDITIONS);
        noSweepOrPickHits.tooltip(SECTION_CONDITIONS);
        aimAlign.tooltip(SECTION_AIM);
        alignSpeed.tooltip(SECTION_AIM);
        alignFov.tooltip(SECTION_AIM);
    }

    @Override
    public CharSequence getSuffix() {
        return critMode.isSelected(CRIT_NONE) ? EMPTY_SUFFIX : critMode.getValue();
    }

    @Override
    public void onEnable() {
        trackedPlayer = client.player;
        trackedWorld = client.world;
        resetCombatState();
        sampleAttackCooldowns();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetCombatState();
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void onWorldContext(WorldTickContextEvent event) {
        trackedPlayer = client.player;
        trackedWorld = client.world;
        resetCombatState();
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onAttackAttempt(AttackAttemptEvent event) {
        if (!shouldPreventBlockBreaking()) {
            return;
        }
        if (event.getTarget() == null && client.crosshairTarget instanceof BlockHitResult) {
            event.cancel();
        }
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onBlockBreaking(BlockBreakingEvent.Pre event) {
        if (!shouldPreventBlockBreaking() || !isAttackKeyHeld()) {
            return;
        }
        if (client.interactionManager != null && client.interactionManager.isBreakingBlock()) {
            client.interactionManager.cancelBlockBreaking();
        }
        event.cancel();
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onEntityAttackPost(EntityAttackEvent.Post event) {
        if (isOutsideWorld() || event.getPlayer() != client.player) {
            return;
        }
        if (event.getTarget() instanceof PlayerEntity target
                && InventoryItems.holdsAxe()
                && (EntityPredicates.isBlocking(target)
                || EntityPredicates.isUsingBlockingItem(target)
                || target.isBlocking())) {
            lastShieldBreakTargetId = target.getId();
            lastShieldBreakTick = client.player.age;
        }
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (aimAlign.isSelected(ALIGN_OFF)) {
            if (rotations().isOwnedBy(this)) {
                rotations().cancel(this);
            }
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || MinecraftRenderState.hasOverlay()) {
            rotations().cancel(this);
            return;
        }
        if (!passesLoadoutConditions() || isPlayerBusy() || isYieldingToOtherModules()
                || !rotations().canRequest(this, RotationPriority.LOW)) {
            rotations().cancel(this);
            return;
        }

        double maxReach = effectiveReach();
        LivingEntity target = selectAlignmentTarget(maxReach);
        if (target == null) {
            rotations().cancel(this);
            return;
        }

        RotationManager rotationManager = rotations();
        Rotation effective = rotationManager.effectiveRotation();
        float gcdStep = currentGcdStep();
        if (isCenteredOnTarget(target, effective, maxReach, gcdStep)) {
            if (rotationManager.isOwnedBy(this)) {
                rotationManager.cancel(this);
            }
            return;
        }

        float speed = alignSpeed.getValue() * 20.0F;
        boolean aimed = rotationManager.aim(
                this,
                RotationPriority.LOW,
                target,
                new AimPointSettings(AimPointMode.CLOSEST, 65.0F, false, isPiercingBlocksActive()),
                new RotationSettings(RotationVisibility.VISIBLE, AimTransition.NATURAL, speed, speed * 0.85F));
        if (!aimed) {
            rotations().cancel(this);
        }
    }

    @Subscribe
    private void onAttackKey(AttackKeyEvent event) {
        synchronizeContext();
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || MinecraftRenderState.hasOverlay()) {
            return;
        }
        if (!passesLoadoutConditions()) {
            clearTargetAcquisition();
            return;
        }
        if (!canTriggerThisTick(event)) {
            return;
        }

        TargetHit targetHit = resolveTargetHit(attackDistance.getValue());
        long nowMs = System.currentTimeMillis();
        if (targetHit == null) {
            expireTargetAcquisitionIfNeeded(nowMs);
            return;
        }

        LivingEntity target = targetHit.target();
        if (!isEntityTargetable(target)) {
            expireTargetAcquisitionIfNeeded(nowMs);
            return;
        }
        LivingEntity resolved = TargetSelection.resolveTarget(target);
        if (resolved == null || resolved != target) {
            expireTargetAcquisitionIfNeeded(nowMs);
            return;
        }

        updateTargetAcquisition(target.getId(), nowMs);

        if (!isRotationReady()) {
            return;
        }

        boolean targetBlocking = isActivelyBlocking(target);
        if (targetBlocking && !attackShields.getValue()) {
            return;
        }
        boolean axeShieldBreak = targetBlocking && attackShields.getValue()
                && InventoryItems.isAxe(client.player.getMainHandStack());

        if (!passesHurtTime(target, axeShieldBreak)) {
            return;
        }

        double squaredDistance = client.player.getCameraPosVec(1.0F).squaredDistanceTo(targetHit.hit().getPos());
        if (!passesCooldown(squaredDistance, axeShieldBreak)) {
            return;
        }
        if (!reactionTimer.hasElapsedMillis((double) reactionDelayMillis)) {
            return;
        }
        if (!attackTimer.hasElapsedMillis((double) attackDelayMillis)) {
            return;
        }
        if (!axeShieldBreak && !passesCritWindow(squaredDistance)) {
            return;
        }
        if (noSweepOrPickHits.getValue() && (EntityRaycasts.wouldSweepAttack() || EntityRaycasts.wouldPickHit())) {
            return;
        }

        if (!actions().requestAttackClick(this, event)) {
            return;
        }
        rotations().acknowledgeRotation();
        if (axeShieldBreak) {
            lastShieldBreakTargetId = target.getId();
            lastShieldBreakTick = client.player.age;
        }
        attackTimer.reset();
        attackDelayMillis = sampleDelayMillis(attackDelay);
        sampleAttackCooldowns();
        lastAttackTick = client.player.age;
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        handleReceivedPacket(event.getPacket());
    }

    private void handleReceivedPacket(Packet<?> packet) {
        if (packet instanceof BundleS2CPacket bundle) {
            for (Packet<?> inner : bundle.getPackets()) {
                handleReceivedPacket(inner);
            }
            return;
        }
        if (packet instanceof PlayerPositionLookS2CPacket) {
            clearTargetAcquisition();
            if (client.player != null) {
                lastSetbackTick = client.player.age;
            }
        } else if (packet instanceof PlayerRespawnS2CPacket) {
            resetCombatState();
        }
    }

    private boolean passesLoadoutConditions() {
        return itemWhitelist.acceptsHeldItem() && hotbarWhitelist.permitsCurrentSlot();
    }

    private boolean isAttackKeyHeld() {
        return KeybindingCache.isPressed(client.options.attackKey)
                || client.options.attackKey.isPressed();
    }

    private boolean shouldPreventBlockBreaking() {
        return isEnabled()
                && !isOutsideWorld()
                && MinecraftRenderState.getScreen() == null
                && !MinecraftRenderState.hasOverlay()
                && holdToAttack.getValue()
                && dontBreakBlocks.getValue()
                && passesLoadoutConditions()
                && !GhostHand.isTargetingGhostBlock();
    }

    private boolean isPlayerBusy() {
        if (client.player.isUsingItem() || EntityInteractions.isBlocking() || EntityInteractions.isUsingOffhand()) {
            return true;
        }
        if (client.interactionManager == null) {
            return true;
        }
        return client.interactionManager.isBreakingBlock()
                && !(client.crosshairTarget instanceof EntityHitResult);
    }

    private boolean isYieldingToOtherModules() {
        return DeferredActionQueue.hasPendingActions()
                || Targets.shieldBreakActive
                || AutoMaceSwap.hasPendingSwap()
                || MaceSwap.hasPendingSwap();
    }

    private boolean canTriggerThisTick(AttackKeyEvent event) {
        if (event.hasClicks() || event.getClickCount() > 0) {
            return false;
        }
        if (holdToAttack.getValue()
                && !KeybindingCache.isPressed(client.options.attackKey)
                && !client.options.attackKey.isPressed()) {
            return false;
        }
        if (client.attackCooldown > 0 || client.player.isSpectator() || !client.player.isAlive()) {
            return false;
        }
        long age = client.player.age;
        if (age <= lastSetbackTick || age == lastAttackTick) {
            return false;
        }
        return !isPlayerBusy() && !isYieldingToOtherModules();
    }

    private TargetHit resolveTargetHit(float distance) {
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        double vanillaReach = Math.max(0.0, AttackRange.getEffectiveMaxRange(client.player));
        double maxReach = distance > 0.0F ? Math.min((double) distance, vanillaReach) : vanillaReach;
        boolean piercing = isPiercingActive();
        boolean piercingBlocks = isPiercingBlocksActive();

        EntityHitResult entityHit = null;
        if (client.crosshairTarget instanceof EntityHitResult crosshairHit) {
            entityHit = crosshairHit;
        } else if (!piercing) {
            if (distance <= 0.0F) {
                HitResult direct = client.player.getAttackRange().getHitResult(
                        client.player, 1.0F, net.minecraft.predicate.entity.EntityPredicates.CAN_HIT);
                if (direct instanceof EntityHitResult directEntity) {
                    entityHit = directEntity;
                }
            } else {
                entityHit = EntityRaycasts.raycastEntity(client.player, maxReach);
            }
        }

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            Vec3d hitPos = entityHit.getPos();
            boolean fromCrosshair = entityHit == client.crosshairTarget;
            boolean withinCustomDistance = distance <= 0.0F || eye.squaredDistanceTo(hitPos) <= maxReach * maxReach;
            if (fromCrosshair) {
                if (withinCustomDistance) {
                    return new TargetHit(entityHit, target, maxReach);
                }
            } else if (withinCustomDistance
                    && (piercingBlocks || !isOccludedByBlock(eye, hitPos))) {
                return new TargetHit(entityHit, target, maxReach);
            }
        }

        SpearReach spearReach = findEnabledSpearReach();
        if (spearReach != null) {
            EntityHitResult spearHit = spearReach.findExtendedReachTarget();
            if (spearHit != null && spearHit.getEntity() instanceof LivingEntity target) {
                Vec3d hitPos = spearHit.getPos();
                double spearDistance = eye.distanceTo(hitPos);
                if ((distance <= 0.0F || spearDistance <= (double) distance)
                        && (piercingBlocks || !isOccludedByBlock(eye, hitPos))) {
                    return new TargetHit(spearHit, target, Math.max(vanillaReach, spearDistance + 0.5));
                }
            }
        }
        return null;
    }

    private boolean isRotationReady() {
        RotationManager rotationManager = rotations();
        if (rotationManager.isSilentRotationActive() && !rotationManager.isRotationReady()) {
            return false;
        }
        Rotation effective = rotationManager.effectiveRotation();
        return effective != null
                && Float.isFinite(effective.yaw())
                && Float.isFinite(effective.pitch())
                && Math.abs(effective.pitch()) <= 90.0F;
    }

    private boolean passesHurtTime(LivingEntity target, boolean axeShieldBreak) {
        boolean recentShieldBreak = lastShieldBreakTargetId == target.getId()
                && (long) client.player.age - lastShieldBreakTick <= SHIELD_BREAK_FOLLOWUP_TICKS;
        int leadTicks = TriggerBotTiming.effectiveHurtLeadTicks(
                LatencyTracker.milliseconds(), pingHurtTime.getValue());
        int remainingCooldown = AttackCooldownTracker.remainingAttackCooldown(target);
        boolean nonFireWindow = AttackCooldownTracker.hasNonFireHurtWindow(target);
        return TriggerBotTiming.passesHurtTime(
                hurtTimeCheck.getValue(),
                target.hurtTime,
                remainingCooldown,
                recentShieldBreak,
                axeShieldBreak,
                nonFireWindow,
                leadTicks);
    }

    private boolean passesCooldown(double squaredDistance, boolean axeShieldBreak) {
        boolean inCritRange = TriggerBotTiming.isWithinRange(
                squaredDistance, critRange.getLower(), critRange.getUpper());
        boolean useCritCooldown = !critMode.isSelected(CRIT_NONE)
                && inCritRange
                && EntityRaycasts.isCriticalState()
                && !axeShieldBreak;
        float progress = client.player.getAttackCooldownProgress(0.5F);
        return TriggerBotTiming.passesCooldown(
                progress,
                useCritCooldown,
                sampledGroundCooldown,
                sampledCritCooldown,
                critCooldown.getUpper(),
                axeShieldBreak);
    }

    private boolean passesCritWindow(double squaredDistance) {
        TriggerBotTiming.CritPolicy policy = currentCritPolicy();
        boolean inCritRange = TriggerBotTiming.isWithinRange(
                squaredDistance, critRange.getLower(), critRange.getUpper());
        boolean jumpPressed = KeybindingCache.isPressed(client.options.jumpKey)
                || client.options.jumpKey.isPressed();
        boolean criticalFall = EntityRaycasts.isCriticalState();
        boolean fallbackAllowed = EntityRaycasts.allowsNonCriticalHit()
                || (critOnlyOnSpace.getValue() && !jumpPressed && !client.player.isOnGround());
        return TriggerBotTiming.allowsCriticalTiming(
                policy,
                inCritRange,
                critOnlyOnSpace.getValue(),
                jumpPressed,
                client.player.isOnGround(),
                criticalFall,
                client.player.isSprinting(),
                fallbackAllowed);
    }

    private TriggerBotTiming.CritPolicy currentCritPolicy() {
        if (critMode.isSelected(CRIT_PRECISE)) {
            return TriggerBotTiming.CritPolicy.PRECISE;
        }
        if (critMode.isSelected(CRIT_NORMAL)) {
            return TriggerBotTiming.CritPolicy.NORMAL;
        }
        return TriggerBotTiming.CritPolicy.NONE;
    }

    private void updateTargetAcquisition(int targetId, long nowMs) {
        if (!TriggerBotTiming.isTargetAcquisitionStillValid(
                acquiredTargetId, targetId, acquiredLastSeenMs, nowMs, TriggerBotTiming.TARGET_GRACE_MILLIS)) {
            acquiredTargetId = targetId;
            reactionTimer.reset();
            reactionDelayMillis = sampleDelayMillis(reactionDelay);
        }
        acquiredLastSeenMs = nowMs;
    }

    private void expireTargetAcquisitionIfNeeded(long nowMs) {
        if (acquiredTargetId >= 0
                && nowMs - acquiredLastSeenMs > TriggerBotTiming.TARGET_GRACE_MILLIS) {
            clearTargetAcquisition();
        }
    }

    private void clearTargetAcquisition() {
        if (acquiredTargetId >= 0 && reactionTimer != null) {
            reactionTimer.reset();
        }
        acquiredTargetId = -1;
        acquiredLastSeenMs = 0L;
    }

    private boolean isActivelyBlocking(LivingEntity target) {
        return target instanceof PlayerEntity player
                && EntityPredicates.isBlocking(player)
                && !EntityPredicates.isOutsideBlockingArc(player);
    }

    private boolean isEntityTargetable(LivingEntity target) {
        return target != null && target != client.player
                && !target.isDead() && target.isAlive()
                && !target.isRemoved() && !target.isInvulnerable()
                && !target.isSpectator();
    }

    private LivingEntity selectAlignmentTarget(double maxReach) {
        if (client.world == null || client.player == null) {
            return null;
        }
        double searchReach = maxReach + 0.25;
        double searchReachSq = searchReach * searchReach;
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        float maxFov = alignFov.getValue();
        List<LivingEntity> candidates = client.world.getEntitiesByClass(
                LivingEntity.class,
                client.player.getBoundingBox().expand(searchReach + 1.0),
                entity -> isAlignmentCandidate(entity, eye, searchReachSq, maxFov));
        if (candidates.isEmpty()) {
            return null;
        }
        LivingEntity best = candidates.stream()
                .min(Comparator.comparingDouble(entity -> angularDistance(eye, entity)))
                .orElse(null);
        LivingEntity resolved = TargetSelection.resolveTarget(best);
        return isAlignmentCandidate(resolved, eye, searchReachSq, maxFov) ? resolved : null;
    }

    private boolean isAlignmentCandidate(LivingEntity entity, Vec3d eye, double searchReachSq, float maxFov) {
        if (entity == null || entity == client.player || !isEntityTargetable(entity)
                || !TargetSelection.isValidTarget(entity)) {
            return false;
        }
        if (entity.getBoundingBox().squaredMagnitude(eye) > searchReachSq) {
            return false;
        }
        if (!EntityRaycasts.isWithinFov(client.player, entity, maxFov, maxFov)) {
            return false;
        }
        return isPiercingBlocksActive()
                || !isOccludedByBlock(eye, entity.getBoundingBox().getCenter())
                || !isOccludedByBlock(eye, entity.getEyePos());
    }

    private boolean isCenteredOnTarget(LivingEntity target, Rotation rotation, double maxReach, float gcdStep) {
        if (client.player == null || target == null || rotation == null) {
            return false;
        }
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        Vec3d end = raycastEnd(eye, rotation, maxReach);
        Box box = resolveTargetBox(target);
        double shrinkX = Math.min(0.06, box.getLengthX() * 0.2);
        double shrinkY = Math.min(0.10, box.getLengthY() * 0.15);
        double shrinkZ = Math.min(0.06, box.getLengthZ() * 0.2);
        Box core = box.expand(-shrinkX, -shrinkY, -shrinkZ);
        if (core.contains(eye) || core.raycast(eye, end).isPresent()) {
            return true;
        }
        if (gcdStep > 1.0E-5F) {
            Rotation desired = Rotation.lookingAt(eye, box.getCenter());
            float yawDiff = Math.abs(MathHelper.wrapDegrees(desired.yaw() - rotation.yaw()));
            float pitchDiff = Math.abs(desired.pitch() - rotation.pitch());
            return yawDiff <= gcdStep * 0.75F && pitchDiff <= gcdStep * 0.75F
                    && (box.contains(eye) || box.raycast(eye, end).isPresent());
        }
        return false;
    }

    private Vec3d raycastEnd(Vec3d eye, Rotation rotation, double maxReach) {
        Vec3d direction = client.player.getRotationVector(rotation.pitch(), rotation.yaw());
        Vec3d end = eye.add(direction.multiply(Math.max(0.5, maxReach)));
        if (!isPiercingBlocksActive() && client.world != null) {
            BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                    eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));
            if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
                end = blockHit.getPos();
            }
        }
        return end;
    }

    private double angularDistance(Vec3d eye, LivingEntity target) {
        Rotation desired = Rotation.lookingAt(eye, target.getBoundingBox().getCenter());
        float yawDiff = Math.abs(MathHelper.wrapDegrees(desired.yaw() - client.player.getYaw()));
        float pitchDiff = Math.abs(desired.pitch() - client.player.getPitch());
        return Math.hypot(yawDiff, pitchDiff);
    }

    private double effectiveReach() {
        double vanilla = Math.max(0.0, AttackRange.getEffectiveMaxRange(client.player));
        float distance = attackDistance.getValue();
        return distance > 0.0F ? Math.min((double) distance, vanilla) : vanilla;
    }

    private Box resolveTargetBox(LivingEntity target) {
        EntityHitboxEvent hitboxEvent = new EntityHitboxEvent(target, target.getBoundingBox().expand(target.getTargetingMargin()));
        EventBus.post(hitboxEvent);
        return hitboxEvent.getBoundingBox();
    }

    private boolean isOccludedByBlock(Vec3d eye, Vec3d point) {
        if (client.world == null || client.player == null) {
            return false;
        }
        double distSq = eye.squaredDistanceTo(point);
        if (distSq <= 1.0E-6) {
            return false;
        }
        BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                eye, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
        return blockHit != null && blockHit.getType() != HitResult.Type.MISS
                && blockHit.getPos().squaredDistanceTo(eye) + 1.0E-3 < distSq;
    }

    private void sampleAttackCooldowns() {
        sampledGroundCooldown = (double) cooldown.sampleGaussian() / 100.0;
        sampledCritCooldown = (double) critCooldown.sampleGaussian() / 100.0;
    }

    private long sampleDelayMillis(FloatRangeSetting setting) {
        return Math.max(0L, Math.round(setting.sampleGaussian()));
    }

    private void resetCombatState() {
        if (reactionTimer != null) {
            reactionTimer.reset();
        }
        if (attackTimer != null) {
            attackTimer.reset();
        }
        acquiredTargetId = -1;
        acquiredLastSeenMs = 0L;
        lastAttackTick = -1L;
        lastSetbackTick = -1L;
        lastShieldBreakTick = -1L;
        lastShieldBreakTargetId = -1;
        reactionDelayMillis = 0L;
        attackDelayMillis = 0L;
    }

    private void synchronizeContext() {
        if (trackedPlayer != client.player || trackedWorld != client.world
                || (client.player != null && (long) client.player.age < lastAttackTick)) {
            trackedPlayer = client.player;
            trackedWorld = client.world;
            resetCombatState();
        }
    }

    private float currentGcdStep() {
        boolean fineScale = client.options.getPerspective().isFirstPerson() && client.player.isUsingSpyglass();
        return MouseSensitivity.angularStep(fineScale);
    }

    private boolean isPiercingActive() {
        if (ClumpsClient.ModuleManager == null) {
            return false;
        }
        Module piercing = ClumpsClient.ModuleManager.findFeature(Piercing.class);
        return piercing != null && piercing.isEnabled();
    }

    private boolean isPiercingBlocksActive() {
        return isPiercingActive() && Piercing.blocks != null && Piercing.blocks.getValue();
    }

    private SpearReach findEnabledSpearReach() {
        if (ClumpsClient.ModuleManager == null) {
            return null;
        }
        Module feature = ClumpsClient.ModuleManager.findFeature(SpearReach.class);
        return feature instanceof SpearReach spearReach && spearReach.isEnabled() ? spearReach : null;
    }

    private record TargetHit(EntityHitResult hit, LivingEntity target, double maxReach) {}

    public static void appendBootstrapSettings(TriggerBot Module) {
        CatalogSettings.append(Module, critMode);
        CatalogSettings.append(Module, critOnlyOnSpace);
        CatalogSettings.append(Module, attackShields);
        CatalogSettings.append(Module, hurtTimeCheck);
        CatalogSettings.append(Module, pingHurtTime);
        CatalogSettings.append(Module, noSweepOrPickHits);
        CatalogSettings.append(Module, holdToAttack);
        CatalogSettings.append(Module, dontBreakBlocks);
        CatalogSettings.append(Module, cooldown);
        CatalogSettings.append(Module, critCooldown);
        CatalogSettings.append(Module, critRange);
        CatalogSettings.append(Module, attackDistance);
        CatalogSettings.append(Module, reactionDelay);
        CatalogSettings.append(Module, attackDelay);
        CatalogSettings.append(Module, aimAlign);
        CatalogSettings.append(Module, alignSpeed);
        CatalogSettings.append(Module, alignFov);
        CatalogSettings.append(Module, itemWhitelist);
        CatalogSettings.append(Module, hotbarWhitelist);
    }

    public static void assignBootstrapSettings(
            TriggerBot Module,
            FloatRangeSetting preparedCooldown,
            ChoiceSetting preparedCritMode,
            FloatRangeSetting preparedReactionDelay,
            BooleanSetting preparedCritOnlyOnSpace,
            FloatRangeSetting preparedAttackDelay,
            ItemListSetting preparedItemWhitelist,
            FloatSetting preparedAttackDistance,
            BooleanSetting preparedNoSweepOrPickHits,
            HotbarSlotsSetting preparedHotbarWhitelist,
            BooleanSetting preparedHurtTimeCheck,
            BooleanSetting preparedAttackShields,
            FloatRangeSetting preparedCritRange,
            FloatRangeSetting preparedCritCooldown) {
        cooldown = preparedCooldown;
        critMode = preparedCritMode;
        reactionDelay = preparedReactionDelay;
        critOnlyOnSpace = preparedCritOnlyOnSpace;
        attackDelay = preparedAttackDelay;
        itemWhitelist = preparedItemWhitelist;
        attackDistance = preparedAttackDistance;
        noSweepOrPickHits = preparedNoSweepOrPickHits;
        hotbarWhitelist = preparedHotbarWhitelist;
        hurtTimeCheck = preparedHurtTimeCheck;
        attackShields = preparedAttackShields;
        critRange = preparedCritRange;
        critCooldown = preparedCritCooldown;
    }
}
