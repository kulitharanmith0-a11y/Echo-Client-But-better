package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.interaction.BlockInteractEvent;
import com.blamejared.clumps.client.event.interaction.ItemUseEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.world.BlockUpdateEvent;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.handlers.action.ActionResultInfo;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.utils.Rotation;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.placement.BlockPlacement;
import com.blamejared.clumps.client.utils.placement.PlacementTarget;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AutoDrain extends Module {
    private static final CharSequence METHOD_AUTO = CatalogText.literal("Auto");
    private static final CharSequence METHOD_BUCKET = CatalogText.literal("Bucket");
    private static final CharSequence METHOD_WEBS = CatalogText.literal("Webs");
    private static final CharSequence METHOD_BLOCKS = CatalogText.literal("Blocks");

    private static final CharSequence MODE_ALWAYS = CatalogText.literal("Always");
    private static final CharSequence MODE_ON_KEY = CatalogText.literal("On Key");

    private static final CharSequence TARGET_ENEMY = CatalogText.literal("Enemy");
    private static final CharSequence TARGET_SELF_ENEMY = CatalogText.literal("Self + Enemy");
    private static final CharSequence TARGET_ANY = CatalogText.literal("Any Nearby");

    private static final CharSequence SWAP_LEGIT = CatalogText.literal("Legit");
    private static final CharSequence SWAP_SILENT = CatalogText.literal("Silent");

    private static final CharSequence SECTION_GENERAL = CatalogText.literal("General");
    private static final CharSequence SECTION_FLUIDS = CatalogText.literal("Fluids");
    private static final CharSequence SECTION_SWAP = CatalogText.literal("Swap");
    private static final CharSequence SECTION_ROTATION = CatalogText.literal("Rotation");

    private static final float AIM_TOLERANCE_DEGREES = 10.0F;
    private static final double ENEMY_FLUID_RADIUS_SQ = 3.5 * 3.5;
    private static final double SELF_FLUID_RADIUS_SQ = 2.6 * 2.6;
    private static final long ATTEMPT_COOLDOWN_TICKS = 8L;
    private static final long SELF_BUCKET_WINDOW_TICKS = 10L;
    private static final long SELF_FLUID_TTL_TICKS = 600L;
    private static final double SELF_FLUID_TRACK_RANGE_SQ = 6.5 * 6.5;

    private enum DrainKind {
        BUCKET,
        WEB,
        BLOCK
    }

    private record DrainPlan(
            BlockPos fluidPos,
            DrainKind kind,
            int hotbarSlot,
            Vec3d aimPoint,
            PlacementTarget placementTarget,
            double score) {
    }

    public static ChoiceSetting method;
    public static ChoiceSetting mode;
    public static KeybindSetting activateKey;
    public static ChoiceSetting targetFilter;
    public static BooleanSetting drainWater;
    public static BooleanSetting drainLava;
    public static BooleanSetting ignoreLakes;
    public static ChoiceSetting swapMode;
    public static BooleanSetting swapBack;
    public static IntSetting swapDelay;
    public static IntSetting drainDelay;
    public static FloatSetting range;
    public static BooleanSetting silentRotations;
    public static FloatSetting rotationSpeed;

    private final Map<BlockPos, Long> recentlyAttempted = new HashMap<>();
    private final Map<BlockPos, Long> selfPlacedFluids = new HashMap<>();
    private long lastSelfBucketTick = -100L;
    private boolean lastSelfBucketWasWater;
    private DrainPlan activePlan;
    private int cooldownTicks;
    private int swapWaitTicks;
    private int returnSlot = -1;
    private int returnWaitTicks;
    private boolean pendingLegitAction;
    private boolean disablePending;

    public AutoDrain() {
        super(new ModuleInfo(
                CatalogText.literal("Auto Drain"),
                CatalogText.literal("Automatically drains enemy water and lava using buckets, cobwebs, or blocks"),
                ModuleCategory.COMBAT));
    }

    private static boolean isLegitSwap() {
        return swapMode == null || swapMode.isSelected(SWAP_LEGIT);
    }

    @Override
    public void postInitialize() {
        resetState();
        activateKey.setVisibilityPredicate(ignored -> mode.isSelected(MODE_ON_KEY));
        swapDelay.setVisibilityPredicate(ignored -> isLegitSwap());

        method.description(CatalogText.literal("Auto prefers Empty Bucket, then Cobweb, then solid Blocks"));
        mode.description(CatalogText.literal("Always drains matching fluid sources automatically, or only while holding Activate Key"));
        activateKey.description(CatalogText.literal("Hold this key to drain nearby water and lava"));
        targetFilter.description(CatalogText.literal("Enemy only drains fluid placed near enemies; Self + Enemy also drains fluid touching you"));
        drainWater.description(CatalogText.literal("Drain water source blocks"));
        drainLava.description(CatalogText.literal("Drain lava source blocks"));
        ignoreLakes.description(CatalogText.literal("Skip rivers, oceans, infinite water sources, and large lava lakes"));
        swapMode.description(CatalogText.literal("Legit visibly switches hotbar slot; Silent swaps and restores instantly"));
        swapBack.description(CatalogText.literal("Return to your previous hotbar slot after draining"));
        swapDelay.description(CatalogText.literal("Ticks to wait after swapping before draining and returning in Legit swap mode"));
        drainDelay.description(CatalogText.literal("Cooldown ticks between consecutive drains"));
        range.description(CatalogText.literal("Maximum distance to drain water or lava sources"));
        silentRotations.description(CatalogText.literal("Silently rotate toward the fluid source before draining"));
        rotationSpeed.description(CatalogText.literal("Rotation speed when aiming at fluid sources"));

        method.tooltip(SECTION_GENERAL);
        mode.tooltip(SECTION_GENERAL);
        activateKey.tooltip(SECTION_GENERAL);
        targetFilter.tooltip(SECTION_GENERAL);
        drainDelay.tooltip(SECTION_GENERAL);
        range.tooltip(SECTION_GENERAL);
        drainWater.tooltip(SECTION_FLUIDS);
        drainLava.tooltip(SECTION_FLUIDS);
        ignoreLakes.tooltip(SECTION_FLUIDS);
        swapMode.tooltip(SECTION_SWAP);
        swapBack.tooltip(SECTION_SWAP);
        swapDelay.tooltip(SECTION_SWAP);
        silentRotations.tooltip(SECTION_ROTATION);
        rotationSpeed.tooltip(SECTION_ROTATION);
    }

    @Override
    public CharSequence getSuffix() {
        return method != null ? method.getValue() : METHOD_AUTO;
    }

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        activePlan = null;
        recentlyAttempted.clear();
        if (returnSlot != -1 && client.player != null && swapBack != null && swapBack.getValue()) {
            selectSlotDirect(returnSlot);
        }
        resetSwapState();
        DeferredActionQueue.finish(this, true);
        if (silentRotations != null && silentRotations.getValue() && rotations().isOwnedBy(this)) {
            disablePending = true;
            return;
        }
        disablePending = false;
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void onWorldContext(WorldTickContextEvent event) {
        selfPlacedFluids.clear();
        recentlyAttempted.clear();
        lastSelfBucketTick = -100L;
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onItemUse(ItemUseEvent.Pre event) {
        if (isOutsideWorld() || event.getPlayer() != client.player) {
            return;
        }
        recordSelfBucketUse(event.getStack(), event.getHit());
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onBlockInteract(BlockInteractEvent.Post event) {
        if (isOutsideWorld() || event.getPlayer() != client.player) {
            return;
        }
        ItemStack stack = client.player.getStackInHand(event.getHand());
        recordSelfBucketUse(stack, event.getHit());
    }

    @Subscribe
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (isOutsideWorld() || client.player == null) {
            return;
        }
        BlockPos pos = event.getPosition();
        BlockState state = event.getState();
        if (pos == null) {
            return;
        }
        FluidState fluid = state != null ? state.getFluidState() : null;
        if (fluid == null || fluid.isEmpty() || !fluid.isStill()) {
            selfPlacedFluids.remove(pos);
            return;
        }
        long age = client.player.age;
        if (age - lastSelfBucketTick <= SELF_BUCKET_WINDOW_TICKS) {
            boolean matchesType = lastSelfBucketWasWater ? fluid.isIn(FluidTags.WATER) : fluid.isIn(FluidTags.LAVA);
            if (matchesType && client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= SELF_FLUID_TRACK_RANGE_SQ) {
                selfPlacedFluids.put(pos.toImmutable(), age + SELF_FLUID_TTL_TICKS);
            }
        }
    }

    private void recordSelfBucketUse(ItemStack stack, HitResult hit) {
        if (stack == null || stack.isEmpty() || client.player == null || client.world == null) {
            return;
        }
        boolean isWaterBucket = stack.isOf(Items.WATER_BUCKET);
        boolean isLavaBucket = stack.isOf(Items.LAVA_BUCKET);
        if (!isWaterBucket && !isLavaBucket) {
            return;
        }
        long age = client.player.age;
        lastSelfBucketTick = age;
        lastSelfBucketWasWater = isWaterBucket;
        long expiresAt = age + SELF_FLUID_TTL_TICKS;

        BlockHitResult blockHit = hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK ? bhr : null;
        if (blockHit == null) {
            Rotation effective = rotations().effectiveRotation();
            Vec3d eye = client.player.getEyePos();
            Vec3d look = effective != null
                    ? Vec3d.fromPolar(effective.pitch(), effective.yaw())
                    : client.player.getRotationVec(1.0F);
            double reach = Math.max(4.5, client.player.getBlockInteractionRange());
            BlockHitResult ray = client.world.raycast(new RaycastContext(
                    eye,
                    eye.add(look.multiply(reach)),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    client.player));
            if (ray != null && ray.getType() == HitResult.Type.BLOCK) {
                blockHit = ray;
            }
        }
        if (blockHit != null) {
            BlockPos hitPos = blockHit.getBlockPos();
            selfPlacedFluids.put(hitPos.toImmutable(), expiresAt);
            selfPlacedFluids.put(hitPos.offset(blockHit.getSide()).toImmutable(), expiresAt);
        }
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (disablePending) {
            if (!rotations().beginReturn(this)) {
                disablePending = false;
                super.onDisable();
            }
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            releaseRotation();
            return;
        }
        if (activePlan == null || activePlan.aimPoint() == null) {
            releaseRotation();
            return;
        }
        float speed = rotationSpeed.getValue() * 20.0F;
        RotationSettings settings = new RotationSettings(
                silentRotations.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                AimTransition.EASED,
                speed,
                speed);
        if (!rotations().aim(this, RotationPriority.HIGH, activePlan.aimPoint(), settings)) {
            releaseRotation();
        }
    }

    @Subscribe(priority = EventPriority.NORMAL)
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (disablePending) {
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
        if (swapWaitTicks > 0) {
            swapWaitTicks--;
        }
        if (returnWaitTicks > 0) {
            returnWaitTicks--;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            clearActivePlan();
            return;
        }
        if (!client.player.isAlive() || client.player.isUsingItem()) {
            clearActivePlan();
            return;
        }
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) {
            clearActivePlan();
            return;
        }
        if (AutoWeb.isClearingSelfActive()) {
            abortPendingLegitAction();
            clearActivePlan();
            return;
        }

        pruneTrackedMaps(client.player.age);

        // Complete pending legit swap-back when idle
        if (returnSlot != -1 && returnWaitTicks == 0 && !pendingLegitAction) {
            int slotToRestore = returnSlot;
            returnSlot = -1;
            if (swapBack.getValue() && slotToRestore >= 0 && slotToRestore <= 8
                    && client.player.getInventory().getSelectedSlot() != slotToRestore) {
                selectSlotDirect(slotToRestore);
            }
        }

        if (!isActivationSatisfied()) {
            abortPendingLegitAction();
            clearActivePlan();
            return;
        }
        if (cooldownTicks > 0) {
            clearActivePlan();
            return;
        }
        if (!drainWater.getValue() && !drainLava.getValue()) {
            clearActivePlan();
            return;
        }

        DrainPlan plan = findBestDrainPlan();
        if (plan == null) {
            abortPendingLegitAction();
            clearActivePlan();
            return;
        }

        activePlan = plan;
        if (!isAimedAt(plan.aimPoint()) || !rotations().isRotationReady()) {
            return;
        }

        if (!isLegitSwap()) {
            int prevSlot = client.player.getInventory().getSelectedSlot();
            if (prevSlot != plan.hotbarSlot() && !selectSlotDirect(plan.hotbarSlot())) {
                return;
            }
            if (!matchesHeldPlanItem(plan)) {
                if (swapBack.getValue() && prevSlot != plan.hotbarSlot()) {
                    selectSlotDirect(prevSlot);
                }
                return;
            }
            boolean drained = executeDrain(plan);
            if (swapBack.getValue() && prevSlot != plan.hotbarSlot()) {
                selectSlotDirect(prevSlot);
            }
            if (drained) {
                onDrainSucceeded(plan);
            }
            return;
        }

        // Legit swap: visibly select target hotbar slot, wait swapDelay ticks, execute, then restore
        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (currentSlot != plan.hotbarSlot()) {
            if (returnSlot == -1) {
                returnSlot = currentSlot;
            }
            if (!selectSlotDirect(plan.hotbarSlot())) {
                return;
            }
            pendingLegitAction = true;
            swapWaitTicks = Math.max(0, swapDelay.getValue());
            if (swapWaitTicks > 0) {
                return;
            }
        }
        if (pendingLegitAction && swapWaitTicks > 0) {
            return;
        }
        if (!matchesHeldPlanItem(plan)) {
            return;
        }
        if (executeDrain(plan)) {
            pendingLegitAction = false;
            onDrainSucceeded(plan);
            scheduleSwapBack(plan.hotbarSlot());
        }
    }

    private void onDrainSucceeded(DrainPlan plan) {
        recentlyAttempted.put(plan.fluidPos().toImmutable(), client.player.age + ATTEMPT_COOLDOWN_TICKS);
        cooldownTicks = Math.max(1, drainDelay.getValue());
        clearActivePlan();
    }

    private boolean isActivationSatisfied() {
        if (!mode.isSelected(MODE_ON_KEY)) {
            return true;
        }
        InputKey key = activateKey != null ? activateKey.getKey() : InputKey.NONE;
        return key != null && key != InputKey.NONE && key.isPressed();
    }

    private DrainPlan findBestDrainPlan() {
        int bucketSlot = allowsMethod(DrainKind.BUCKET) ? findEmptyBucketSlot() : -1;
        int webSlot = allowsMethod(DrainKind.WEB) ? findCobwebSlot() : -1;
        int blockSlot = allowsMethod(DrainKind.BLOCK) ? findSolidBlockSlot() : -1;
        if (bucketSlot == -1 && webSlot == -1 && blockSlot == -1) {
            return null;
        }

        List<LivingEntity> enemies = collectNearbyEnemies();
        boolean requireEnemy = targetFilter.isSelected(TARGET_ENEMY);
        boolean allowSelf = targetFilter.isSelected(TARGET_SELF_ENEMY);
        boolean allowAny = targetFilter.isSelected(TARGET_ANY);
        if (requireEnemy && enemies.isEmpty()) {
            return null;
        }

        double maxRange = Math.max(2.0, range.getValue());
        double maxRangeSq = maxRange * maxRange;
        Vec3d eye = client.player.getEyePos();
        int reachBlocks = MathHelper.ceil(maxRange);
        BlockPos origin = BlockPos.ofFloored(eye);

        DrainPlan best = null;
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int dx = -reachBlocks; dx <= reachBlocks; dx++) {
            for (int dy = -reachBlocks; dy <= reachBlocks; dy++) {
                for (int dz = -reachBlocks; dz <= reachBlocks; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (recentlyAttempted.containsKey(cursor)) {
                        continue;
                    }
                    BlockState state = client.world.getBlockState(cursor);
                    if (!isTargetFluidSource(state, cursor)) {
                        continue;
                    }
                    Vec3d center = Vec3d.ofCenter(cursor);
                    double distSq = eye.squaredDistanceTo(center);
                    if (distSq > maxRangeSq) {
                        continue;
                    }

                    double relevanceScore = computeRelevanceScore(center, distSq, enemies, allowSelf, allowAny);
                    if (relevanceScore < 0.0) {
                        continue;
                    }

                    DrainPlan candidate = buildPlanForPos(cursor.toImmutable(), state, center, maxRangeSq, bucketSlot, webSlot, blockSlot, relevanceScore);
                    if (candidate != null && (best == null || candidate.score() < best.score())) {
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private DrainPlan buildPlanForPos(
            BlockPos pos,
            BlockState state,
            Vec3d center,
            double maxRangeSq,
            int bucketSlot,
            int webSlot,
            int blockSlot,
            double relevanceScore) {
        if (bucketSlot != -1 && canBucketReachSource(pos, center)) {
            return new DrainPlan(pos, DrainKind.BUCKET, bucketSlot, center, null, relevanceScore);
        }
        if (webSlot != -1 && state.isReplaceable()) {
            PlacementTarget target = BlockPlacement.find(pos, true, true, true);
            if (target != null && client.player.getEyePos().squaredDistanceTo(target.hitResult().getPos()) <= maxRangeSq) {
                return new DrainPlan(pos, DrainKind.WEB, webSlot, target.hitResult().getPos(), target, relevanceScore + 1.5);
            }
        }
        if (blockSlot != -1 && state.isReplaceable()
                && !BlockPlacement.hasBlockingEntity(pos)
                && !BlockPlacement.intersectsPlayer(pos)) {
            PlacementTarget target = BlockPlacement.find(pos, true, true, true);
            if (target != null && client.player.getEyePos().squaredDistanceTo(target.hitResult().getPos()) <= maxRangeSq) {
                return new DrainPlan(pos, DrainKind.BLOCK, blockSlot, target.hitResult().getPos(), target, relevanceScore + 3.0);
            }
        }
        return null;
    }

    private boolean canBucketReachSource(BlockPos fluidPos, Vec3d center) {
        Vec3d eye = client.player.getEyePos();
        Vec3d direction = center.subtract(eye).normalize();
        Vec3d end = eye.add(direction.multiply(Math.max(2.0, range.getValue())));
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                eye,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.SOURCE_ONLY,
                client.player));
        return hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(fluidPos);
    }

    private boolean isTargetFluidSource(BlockState state, BlockPos pos) {
        if (state == null) {
            return false;
        }
        if (AutoWeb.isSelfClearWaterPos(pos)) {
            return false;
        }
        if (!targetFilter.isSelected(TARGET_ANY) && selfPlacedFluids.containsKey(pos)) {
            return false;
        }
        FluidState fluid = state.getFluidState();
        if (fluid == null || fluid.isEmpty() || !fluid.isStill()) {
            return false;
        }
        boolean isWater = fluid.isIn(FluidTags.WATER);
        boolean isLava = fluid.isIn(FluidTags.LAVA);
        if (isWater && !drainWater.getValue()) {
            return false;
        }
        if (isLava && !drainLava.getValue()) {
            return false;
        }
        if (!isWater && !isLava) {
            return false;
        }
        if (ignoreLakes.getValue() && isPartOfBodyOfFluid(pos, isWater)) {
            return false;
        }
        return true;
    }

    private boolean isPartOfBodyOfFluid(BlockPos pos, boolean water) {
        int adjacentSources = 0;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            FluidState neighbor = client.world.getFluidState(pos.offset(dir));
            if (neighbor != null && !neighbor.isEmpty() && neighbor.isStill()
                    && neighbor.isIn(water ? FluidTags.WATER : FluidTags.LAVA)) {
                adjacentSources++;
                if (adjacentSources >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private double computeRelevanceScore(
            Vec3d fluidCenter,
            double playerDistSq,
            List<LivingEntity> enemies,
            boolean allowSelf,
            boolean allowAny) {
        double closestEnemyDistSq = Double.MAX_VALUE;
        for (LivingEntity enemy : enemies) {
            double enemyDistSq = enemy.getBoundingBox().getCenter().squaredDistanceTo(fluidCenter);
            if (enemyDistSq < closestEnemyDistSq) {
                closestEnemyDistSq = enemyDistSq;
            }
        }
        if (closestEnemyDistSq <= ENEMY_FLUID_RADIUS_SQ) {
            return closestEnemyDistSq;
        }
        if (allowSelf && playerDistSq <= SELF_FLUID_RADIUS_SQ) {
            return playerDistSq + 4.0;
        }
        if (allowAny) {
            return playerDistSq + 8.0;
        }
        return -1.0;
    }

    private List<LivingEntity> collectNearbyEnemies() {
        double searchRadius = Math.max(6.0, range.getValue() + 4.5);
        return client.world.getEntitiesByClass(
                LivingEntity.class,
                client.player.getBoundingBox().expand(searchRadius),
                entity -> entity != null && entity != client.player && entity.isAlive() && !entity.isRemoved()
                        && TargetSelection.isValidTarget(entity));
    }

    private boolean allowsMethod(DrainKind kind) {
        if (method == null || method.isSelected(METHOD_AUTO)) {
            return true;
        }
        return switch (kind) {
            case BUCKET -> method.isSelected(METHOD_BUCKET);
            case WEB -> method.isSelected(METHOD_WEBS);
            case BLOCK -> method.isSelected(METHOD_BLOCKS);
        };
    }

    private int findEmptyBucketSlot() {
        return InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.BUCKET));
    }

    private int findCobwebSlot() {
        return InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.COBWEB));
    }

    private int findSolidBlockSlot() {
        return InventoryItems.findStackInHotbar(this::isValidDrainBlock);
    }

    private boolean isValidDrainBlock(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        if (block == Blocks.COBWEB || block == Blocks.TNT || block == Blocks.RESPAWN_ANCHOR
                || block instanceof FallingBlock || block instanceof BlockEntityProvider) {
            return false;
        }
        return BlockPlacement.isDefaultStateFullCube(block);
    }

    private boolean matchesHeldPlanItem(DrainPlan plan) {
        ItemStack held = client.player.getMainHandStack();
        return switch (plan.kind()) {
            case BUCKET -> held.isOf(Items.BUCKET);
            case WEB -> held.isOf(Items.COBWEB);
            case BLOCK -> isValidDrainBlock(held);
        };
    }

    private boolean executeDrain(DrainPlan plan) {
        if (plan.kind() == DrainKind.BUCKET) {
            return useMainHandItem();
        }
        if (plan.placementTarget() == null) {
            return false;
        }
        ActionResult result = BlockPlacement.interactMainHand(this, plan.placementTarget().hitResult(), true);
        return result.isAccepted();
    }

    private boolean useMainHandItem() {
        if (client.player == null || client.interactionManager == null) {
            return false;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null) {
            ActionResultInfo info = coordinator.executeItemUse(this, Hand.MAIN_HAND);
            if (info.succeeded()) {
                client.player.swingHand(Hand.MAIN_HAND);
                return true;
            }
            return false;
        }
        ActionResult result = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        if (result.isAccepted()) {
            client.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private boolean isAimedAt(Vec3d point) {
        Rotation current = rotations().effectiveRotation();
        if (current == null || point == null) {
            return false;
        }
        Rotation desired = Rotation.lookingAt(client.player.getEyePos(), point);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(desired.yaw() - current.yaw()));
        float pitchDiff = Math.abs(desired.pitch() - current.pitch());
        return yawDiff <= AIM_TOLERANCE_DEGREES && pitchDiff <= AIM_TOLERANCE_DEGREES;
    }

    private void pruneTrackedMaps(long nowTick) {
        if (!recentlyAttempted.isEmpty()) {
            Iterator<Map.Entry<BlockPos, Long>> iterator = recentlyAttempted.entrySet().iterator();
            while (iterator.hasNext()) {
                if (nowTick >= iterator.next().getValue()) {
                    iterator.remove();
                }
            }
        }
        if (!selfPlacedFluids.isEmpty()) {
            Iterator<Map.Entry<BlockPos, Long>> iterator = selfPlacedFluids.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Long> entry = iterator.next();
                if (nowTick >= entry.getValue()) {
                    iterator.remove();
                    continue;
                }
                if (nowTick - lastSelfBucketTick > SELF_BUCKET_WINDOW_TICKS && client.world != null) {
                    FluidState fluid = client.world.getFluidState(entry.getKey());
                    if (fluid == null || fluid.isEmpty() || !fluid.isStill()) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private void scheduleSwapBack(int activeSlot) {
        if (swapBack.getValue() && returnSlot != -1 && returnSlot != activeSlot) {
            returnWaitTicks = Math.max(1, swapDelay.getValue());
        } else {
            returnSlot = -1;
            returnWaitTicks = 0;
        }
    }

    private void abortPendingLegitAction() {
        pendingLegitAction = false;
        if (returnSlot != -1 && swapBack != null && swapBack.getValue()) {
            returnWaitTicks = Math.max(1, swapDelay.getValue());
        } else {
            returnSlot = -1;
        }
    }

    private boolean selectSlotDirect(int slot) {
        if (client.player == null || slot < 0 || slot > 8) {
            return false;
        }
        if (client.player.getInventory().getSelectedSlot() == slot) {
            return true;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null) {
            ActionResultInfo info = coordinator.executeSlotSelect(this, slot, true);
            return info.succeeded();
        }
        client.player.getInventory().setSelectedSlot(slot);
        return client.player.getInventory().getSelectedSlot() == slot;
    }

    private void clearActivePlan() {
        activePlan = null;
        releaseRotation();
    }

    private void releaseRotation() {
        if (!rotations().isOwnedBy(this)) {
            return;
        }
        if (silentRotations != null && silentRotations.getValue()) {
            rotations().beginReturn(this);
        } else {
            rotations().cancel(this);
        }
    }

    private void resetSwapState() {
        swapWaitTicks = 0;
        returnSlot = -1;
        returnWaitTicks = 0;
        pendingLegitAction = false;
    }

    private void resetState() {
        recentlyAttempted.clear();
        activePlan = null;
        cooldownTicks = 0;
        disablePending = false;
        resetSwapState();
    }

    public static CharSequence bootstrapMethodAuto() {
        return METHOD_AUTO;
    }

    public static CharSequence bootstrapMethodBucket() {
        return METHOD_BUCKET;
    }

    public static CharSequence bootstrapMethodWebs() {
        return METHOD_WEBS;
    }

    public static CharSequence bootstrapMethodBlocks() {
        return METHOD_BLOCKS;
    }

    public static CharSequence bootstrapModeAlways() {
        return MODE_ALWAYS;
    }

    public static CharSequence bootstrapModeOnKey() {
        return MODE_ON_KEY;
    }

    public static CharSequence bootstrapTargetEnemy() {
        return TARGET_ENEMY;
    }

    public static CharSequence bootstrapTargetSelfEnemy() {
        return TARGET_SELF_ENEMY;
    }

    public static CharSequence bootstrapTargetAny() {
        return TARGET_ANY;
    }

    public static CharSequence bootstrapSwapLegit() {
        return SWAP_LEGIT;
    }

    public static CharSequence bootstrapSwapSilent() {
        return SWAP_SILENT;
    }

    public static void appendBootstrapSettings(AutoDrain module) {
        CatalogSettings.append(module, method);
        CatalogSettings.append(module, mode);
        CatalogSettings.append(module, activateKey);
        CatalogSettings.append(module, targetFilter);
        CatalogSettings.append(module, drainWater);
        CatalogSettings.append(module, drainLava);
        CatalogSettings.append(module, ignoreLakes);
        CatalogSettings.append(module, swapMode);
        CatalogSettings.append(module, swapBack);
        CatalogSettings.append(module, swapDelay);
        CatalogSettings.append(module, drainDelay);
        CatalogSettings.append(module, range);
        CatalogSettings.append(module, silentRotations);
        CatalogSettings.append(module, rotationSpeed);
    }

    public static void assignBootstrapSettings(
            AutoDrain module,
            ChoiceSetting preparedMethod,
            ChoiceSetting preparedMode,
            KeybindSetting preparedActivateKey,
            ChoiceSetting preparedTargetFilter,
            BooleanSetting preparedDrainWater,
            BooleanSetting preparedDrainLava,
            BooleanSetting preparedIgnoreLakes,
            ChoiceSetting preparedSwapMode,
            BooleanSetting preparedSwapBack,
            IntSetting preparedSwapDelay,
            IntSetting preparedDrainDelay,
            FloatSetting preparedRange,
            BooleanSetting preparedSilentRotations,
            FloatSetting preparedRotationSpeed) {
        method = preparedMethod;
        mode = preparedMode;
        activateKey = preparedActivateKey;
        targetFilter = preparedTargetFilter;
        drainWater = preparedDrainWater;
        drainLava = preparedDrainLava;
        ignoreLakes = preparedIgnoreLakes;
        swapMode = preparedSwapMode;
        swapBack = preparedSwapBack;
        swapDelay = preparedSwapDelay;
        drainDelay = preparedDrainDelay;
        range = preparedRange;
        silentRotations = preparedSilentRotations;
        rotationSpeed = preparedRotationSpeed;
    }
}
