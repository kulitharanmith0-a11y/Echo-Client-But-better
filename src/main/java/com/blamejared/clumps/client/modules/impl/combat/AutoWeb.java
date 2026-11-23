package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
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
import com.blamejared.clumps.client.utils.targeting.EntityRaycasts;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoWeb extends Module {
    private static final CharSequence STYLE_LEGIT = CatalogText.literal("Legit");
    private static final CharSequence STYLE_BLATANT = CatalogText.literal("Blatant");
    private static final CharSequence MODE_KEY = CatalogText.literal("On Key");
    private static final CharSequence MODE_ALWAYS = CatalogText.literal("Always");
    private static final CharSequence SWAP_LEGIT = CatalogText.literal("Legit");
    private static final CharSequence SWAP_SILENT = CatalogText.literal("Silent");

    private static final CharSequence SECTION_SWAP = CatalogText.literal("Swap");
    private static final CharSequence SECTION_PLACEMENT = CatalogText.literal("Placement");
    private static final CharSequence SECTION_ROTATION = CatalogText.literal("Rotation");
    private static final CharSequence SECTION_ESCAPE = CatalogText.literal("Escape");

    private static final float AIM_TOLERANCE_DEGREES = 10.0F;
    private static final int WATER_PICKUP_MIN_TICKS = 2;
    private static final int WATER_PICKUP_FORCE_TICKS = 7;
    private static final int WATER_PICKUP_TIMEOUT_TICKS = 16;

    public static ChoiceSetting style;
    public static ChoiceSetting mode;
    public static KeybindSetting activateKey;
    public static ChoiceSetting swapMode;
    public static BooleanSetting swapBack;
    public static IntSetting swapDelay;
    public static FloatSetting range;
    public static FloatSetting fov;
    public static IntSetting predictTicks;
    public static IntSetting placeDelay;
    public static BooleanSetting doubleWeb;
    public static BooleanSetting avoidSelf;
    public static BooleanSetting onlyGrounded;
    public static BooleanSetting silentRotations;
    public static FloatSetting rotationSpeed;
    public static BooleanSetting autoClearSelf;

    private PlacementTarget activePlacement;
    private Vec3d waterAimPoint;
    private BlockPos placedWaterPos;
    private long placedWaterTick = -1L;
    private int cooldownTicks;
    private boolean disablePending;

    // Legit hotbar swap state machine
    private int swapWaitTicks;
    private int returnSlot = -1;
    private int returnWaitTicks;
    private boolean pendingLegitPlace;
    private boolean pendingBlatantPlace;
    private boolean pendingClearPlace;
    private boolean pendingClearPickup;

    public AutoWeb() {
        super(new ModuleInfo(
                CatalogText.literal("Auto Web"),
                CatalogText.literal("Places cobwebs on targets or crosshair blocks and clears self-webs"),
                ModuleCategory.COMBAT));
    }

    private static boolean isLegitStyle() {
        return style != null && style.isSelected(STYLE_LEGIT);
    }

    private static boolean isLegitSwap() {
        return swapMode == null || swapMode.isSelected(SWAP_LEGIT);
    }

    @Override
    public void postInitialize() {
        resetState();
        if (style != null && !style.isSelected(STYLE_LEGIT) && !style.isSelected(STYLE_BLATANT)) {
            style.setValueSilently(STYLE_LEGIT);
        }
        if (swapMode != null && !swapMode.isSelected(SWAP_LEGIT) && !swapMode.isSelected(SWAP_SILENT)) {
            swapMode.setValueSilently(SWAP_LEGIT);
        }

        mode.setVisibilityPredicate(ignored -> !isLegitStyle());
        activateKey.setVisibilityPredicate(ignored -> isLegitStyle() || mode.isSelected(MODE_KEY));
        swapDelay.setVisibilityPredicate(ignored -> isLegitSwap());
        range.setVisibilityPredicate(ignored -> !isLegitStyle());
        fov.setVisibilityPredicate(ignored -> !isLegitStyle());
        predictTicks.setVisibilityPredicate(ignored -> !isLegitStyle());
        doubleWeb.setVisibilityPredicate(ignored -> !isLegitStyle());
        avoidSelf.setVisibilityPredicate(ignored -> !isLegitStyle());
        onlyGrounded.setVisibilityPredicate(ignored -> !isLegitStyle());
        silentRotations.setVisibilityPredicate(ignored -> !isLegitStyle() || autoClearSelf.getValue());
        rotationSpeed.setVisibilityPredicate(ignored -> !isLegitStyle() || autoClearSelf.getValue());

        style.description(CatalogText.literal("Legit places on the block you look at when pressing Activate Key; Blatant auto-targets enemies"));
        activateKey.description(CatalogText.literal("Press or hold to place a cobweb"));
        swapMode.description(CatalogText.literal("Legit visibly moves your hotbar slot to cobwebs; Silent swaps and restores instantly"));
        swapBack.description(CatalogText.literal("Return to your previous hotbar slot after placing"));
        swapDelay.description(CatalogText.literal("Ticks to hold the swapped slot before placing and returning in Legit swap mode"));
        range.description(CatalogText.literal("Maximum distance to web targets"));
        fov.description(CatalogText.literal("Maximum angle from crosshair to target"));
        predictTicks.description(CatalogText.literal("Ticks ahead to predict moving targets"));
        placeDelay.description(CatalogText.literal("Minimum ticks between cobweb placements"));
        doubleWeb.description(CatalogText.literal("Also places a cobweb at target head height after feet are webbed"));
        avoidSelf.description(CatalogText.literal("Prevents placing cobwebs on blocks intersecting yourself"));
        onlyGrounded.description(CatalogText.literal("Only places cobwebs on targets standing on the ground"));
        autoClearSelf.description(CatalogText.literal("Uses a hotbar water bucket to break a cobweb trapping you and picks the water back up"));

        swapMode.tooltip(SECTION_SWAP);
        swapBack.tooltip(SECTION_SWAP);
        swapDelay.tooltip(SECTION_SWAP);
        range.tooltip(SECTION_PLACEMENT);
        fov.tooltip(SECTION_PLACEMENT);
        predictTicks.tooltip(SECTION_PLACEMENT);
        placeDelay.tooltip(SECTION_PLACEMENT);
        doubleWeb.tooltip(SECTION_PLACEMENT);
        avoidSelf.tooltip(SECTION_PLACEMENT);
        onlyGrounded.tooltip(SECTION_PLACEMENT);
        silentRotations.tooltip(SECTION_ROTATION);
        rotationSpeed.tooltip(SECTION_ROTATION);
        autoClearSelf.tooltip(SECTION_ESCAPE);
    }

    @Override
    public CharSequence getSuffix() {
        return style != null ? style.getValue() : STYLE_LEGIT;
    }

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        activePlacement = null;
        waterAimPoint = null;
        placedWaterPos = null;
        placedWaterTick = -1L;
        if (returnSlot != -1 && client.player != null && swapBack.getValue()) {
            selectSlotDirect(returnSlot);
        }
        resetSwapState();
        DeferredActionQueue.finish(this, true);
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
        Vec3d aimPoint = waterAimPoint != null
                ? waterAimPoint
                : (activePlacement != null ? activePlacement.hitResult().getPos() : null);
        if (aimPoint == null) {
            releaseRotation();
            return;
        }
        float speed = rotationSpeed.getValue() * 20.0F;
        RotationSettings settings = new RotationSettings(
                silentRotations.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                AimTransition.EASED,
                speed,
                speed);
        boolean accepted = rotations().aim(this, RotationPriority.HIGH, aimPoint, settings);
        if (!accepted) {
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
            clearActivePlacement();
            return;
        }
        if (!client.player.isAlive() || client.player.isUsingItem()) {
            clearActivePlacement();
            return;
        }
        if (activateKey.isListeningForKey()) {
            clearActivePlacement();
            return;
        }
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) {
            clearActivePlacement();
            return;
        }

        // Complete any pending legit swap-back when idle
        if (returnSlot != -1 && returnWaitTicks == 0
                && !pendingLegitPlace && !pendingBlatantPlace && !pendingClearPlace && !pendingClearPickup
                && placedWaterPos == null) {
            int slotToRestore = returnSlot;
            returnSlot = -1;
            if (swapBack.getValue() && slotToRestore >= 0 && slotToRestore <= 8
                    && client.player.getInventory().getSelectedSlot() != slotToRestore) {
                selectSlotDirect(slotToRestore);
            }
        }

        // Priority 1: Pick back up water placed for self-web clear
        if (placedWaterPos != null) {
            if (handleWaterPickup()) {
                return;
            }
        }

        // Priority 2: Auto-clear cobweb trapping self using water bucket
        if (autoClearSelf.getValue() && handleSelfWebClear()) {
            return;
        }
        waterAimPoint = null;
        pendingClearPlace = false;
        pendingClearPickup = false;

        // Priority 3: Cobweb placement (Legit manual crosshair OR Blatant auto-target)
        if (isLegitStyle()) {
            activePlacement = null;
            handleLegitWebPlace();
        } else {
            pendingLegitPlace = false;
            handleBlatantWebPlace();
        }
    }

    private void handleLegitWebPlace() {
        boolean triggered = isActivateKeyPressed();
        if (!pendingLegitPlace) {
            if (!triggered || cooldownTicks > 0) {
                return;
            }
        }
        BlockHitResult hit = getManualCrosshairBlock();
        if (hit == null) {
            if (!triggered) {
                abortPendingLegitPlace();
            }
            return;
        }
        BlockPos placePos = BlockPlacement.placementPosition(hit);
        if (placePos == null || isCobweb(placePos) || !client.world.getBlockState(placePos).isReplaceable()) {
            if (!triggered) {
                abortPendingLegitPlace();
            }
            return;
        }
        int webSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.COBWEB));
        if (webSlot == -1) {
            abortPendingLegitPlace();
            return;
        }

        if (!isLegitSwap()) {
            // Silent swap: switch -> place -> restore in one tick
            int prevSlot = client.player.getInventory().getSelectedSlot();
            if (prevSlot != webSlot && !selectSlotDirect(webSlot)) {
                return;
            }
            if (!client.player.getMainHandStack().isOf(Items.COBWEB)) {
                if (swapBack.getValue() && prevSlot != webSlot) {
                    selectSlotDirect(prevSlot);
                }
                return;
            }
            ActionResult result = BlockPlacement.interactMainHand(this, hit, true);
            if (swapBack.getValue() && prevSlot != webSlot) {
                selectSlotDirect(prevSlot);
            }
            if (result.isAccepted()) {
                cooldownTicks = Math.max(1, placeDelay.getValue());
            }
            return;
        }

        // Legit swap: visibly select cobweb slot, wait swapDelay ticks, place, then swap back if enabled
        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (currentSlot != webSlot) {
            if (returnSlot == -1) {
                returnSlot = currentSlot;
            }
            if (!selectSlotDirect(webSlot)) {
                return;
            }
            pendingLegitPlace = true;
            swapWaitTicks = Math.max(1, swapDelay.getValue());
            return;
        }
        if (pendingLegitPlace && swapWaitTicks > 0) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(Items.COBWEB)) {
            return;
        }
        ActionResult result = BlockPlacement.interactMainHand(this, hit, true);
        if (result.isAccepted()) {
            cooldownTicks = Math.max(1, placeDelay.getValue());
            pendingLegitPlace = false;
            scheduleSwapBack(webSlot);
        } else if (!triggered) {
            abortPendingLegitPlace();
        }
    }

    private void abortPendingLegitPlace() {
        pendingLegitPlace = false;
        if (returnSlot != -1 && swapBack.getValue()) {
            returnWaitTicks = Math.max(1, swapDelay.getValue());
        } else {
            returnSlot = -1;
        }
    }

    private void handleBlatantWebPlace() {
        if (!isBlatantTriggerActive() && !pendingBlatantPlace) {
            clearActivePlacement();
            return;
        }
        if (cooldownTicks > 0 && !pendingBlatantPlace) {
            activePlacement = null;
            releaseRotation();
            return;
        }
        int webSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.COBWEB));
        if (webSlot == -1) {
            pendingBlatantPlace = false;
            clearActivePlacement();
            return;
        }

        PlacementTarget targetPlacement = findBestWebPlacement();
        if (targetPlacement == null) {
            pendingBlatantPlace = false;
            clearActivePlacement();
            if (returnSlot != -1 && swapBack.getValue()) {
                returnWaitTicks = Math.max(1, swapDelay.getValue());
            }
            return;
        }

        activePlacement = targetPlacement;
        if (!isAimedAt(targetPlacement.hitResult().getPos())) {
            return;
        }

        if (!isLegitSwap()) {
            int prevSlot = client.player.getInventory().getSelectedSlot();
            if (prevSlot != webSlot && !selectSlotDirect(webSlot)) {
                return;
            }
            if (!client.player.getMainHandStack().isOf(Items.COBWEB)) {
                if (swapBack.getValue() && prevSlot != webSlot) {
                    selectSlotDirect(prevSlot);
                }
                return;
            }
            ActionResult result = BlockPlacement.interactMainHand(this, targetPlacement.hitResult(), true);
            if (swapBack.getValue() && prevSlot != webSlot) {
                selectSlotDirect(prevSlot);
            }
            if (result.isAccepted()) {
                cooldownTicks = Math.max(1, placeDelay.getValue());
                activePlacement = null;
                pendingBlatantPlace = false;
            }
            return;
        }

        // Legit swap in Blatant mode: visibly move to cobweb slot before placing
        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (currentSlot != webSlot) {
            if (returnSlot == -1) {
                returnSlot = currentSlot;
            }
            if (!selectSlotDirect(webSlot)) {
                return;
            }
            pendingBlatantPlace = true;
            swapWaitTicks = Math.max(1, swapDelay.getValue());
            return;
        }
        if (pendingBlatantPlace && swapWaitTicks > 0) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(Items.COBWEB)) {
            return;
        }
        ActionResult result = BlockPlacement.interactMainHand(this, targetPlacement.hitResult(), true);
        if (result.isAccepted()) {
            cooldownTicks = Math.max(1, placeDelay.getValue());
            activePlacement = null;
            pendingBlatantPlace = false;
            scheduleSwapBack(webSlot);
        }
    }

    private boolean handleSelfWebClear() {
        BlockPos selfWeb = findSelfCobwebPos();
        if (selfWeb == null) {
            pendingClearPlace = false;
            return false;
        }
        int bucketSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.WATER_BUCKET));
        if (bucketSlot == -1) {
            pendingClearPlace = false;
            return false;
        }
        Vec3d aim = new Vec3d(selfWeb.getX() + 0.5, selfWeb.getY() + 0.05, selfWeb.getZ() + 0.5);
        activePlacement = null;
        waterAimPoint = aim;
        if (!isAimedAt(aim) || !rotations().isRotationReady()) {
            return true;
        }

        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (isLegitSwap()) {
            if (currentSlot != bucketSlot) {
                if (returnSlot == -1) {
                    returnSlot = currentSlot;
                }
                if (!selectSlotDirect(bucketSlot)) {
                    return true;
                }
                pendingClearPlace = true;
                swapWaitTicks = Math.max(1, swapDelay.getValue());
                return true;
            }
            if (pendingClearPlace && swapWaitTicks > 0) {
                return true;
            }
        } else if (currentSlot != bucketSlot) {
            if (returnSlot == -1) {
                returnSlot = currentSlot;
            }
            if (!selectSlotDirect(bucketSlot)) {
                return true;
            }
        }

        if (!client.player.getMainHandStack().isOf(Items.WATER_BUCKET)) {
            return true;
        }
        if (useMainHandItem()) {
            BlockPos waterSource = findNearbyWaterSource(selfWeb);
            placedWaterPos = waterSource != null ? waterSource : selfWeb.up();
            placedWaterTick = client.player.age;
            pendingClearPlace = false;
            waterAimPoint = null;
            if (!isLegitSwap() && swapBack.getValue() && returnSlot != -1 && returnSlot != bucketSlot) {
                selectSlotDirect(returnSlot);
            }
        }
        return true;
    }

    private boolean handleWaterPickup() {
        long elapsed = client.player.age - placedWaterTick;
        if (elapsed > WATER_PICKUP_TIMEOUT_TICKS) {
            finishWaterPickup();
            return false;
        }
        BlockPos waterSource = findNearbyWaterSource(placedWaterPos);
        if (waterSource != null) {
            placedWaterPos = waterSource;
        }
        boolean webStillPresent = findSelfCobwebPos() != null;
        // Wait briefly so water placed above a cobweb has time to flow down and break it
        if (elapsed < WATER_PICKUP_MIN_TICKS || (webStillPresent && elapsed < WATER_PICKUP_FORCE_TICKS)) {
            waterAimPoint = null;
            return true;
        }
        if (waterSource == null) {
            finishWaterPickup();
            return false;
        }
        int emptyBucketSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.BUCKET));
        if (emptyBucketSlot == -1) {
            finishWaterPickup();
            return false;
        }
        Vec3d aim = Vec3d.ofCenter(waterSource);
        activePlacement = null;
        waterAimPoint = aim;
        if (!isAimedAt(aim) || !rotations().isRotationReady()) {
            return true;
        }

        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (isLegitSwap()) {
            if (currentSlot != emptyBucketSlot) {
                if (!selectSlotDirect(emptyBucketSlot)) {
                    return true;
                }
                pendingClearPickup = true;
                swapWaitTicks = Math.max(1, swapDelay.getValue());
                return true;
            }
            if (pendingClearPickup && swapWaitTicks > 0) {
                return true;
            }
        } else if (currentSlot != emptyBucketSlot) {
            if (!selectSlotDirect(emptyBucketSlot)) {
                return true;
            }
        }

        if (!client.player.getMainHandStack().isOf(Items.BUCKET)) {
            return true;
        }
        if (useMainHandItem()) {
            cooldownTicks = Math.max(1, placeDelay.getValue());
            finishWaterPickup();
        }
        return true;
    }

    private void finishWaterPickup() {
        int currentSlot = client.player != null ? client.player.getInventory().getSelectedSlot() : -1;
        placedWaterPos = null;
        placedWaterTick = -1L;
        waterAimPoint = null;
        pendingClearPickup = false;
        if (returnSlot != -1 && swapBack.getValue()) {
            if (isLegitSwap()) {
                returnWaitTicks = Math.max(1, swapDelay.getValue());
            } else if (currentSlot != returnSlot) {
                selectSlotDirect(returnSlot);
                returnSlot = -1;
            } else {
                returnSlot = -1;
            }
        } else {
            returnSlot = -1;
        }
    }

    private BlockPos findNearbyWaterSource(BlockPos origin) {
        if (origin == null || client.world == null) {
            return null;
        }
        if (isWaterSource(origin)) {
            return origin.toImmutable();
        }
        if (isWaterSource(origin.up())) {
            return origin.up().toImmutable();
        }
        if (isWaterSource(origin.down())) {
            return origin.down().toImmutable();
        }
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (isWaterSource(pos)) {
                        return pos.toImmutable();
                    }
                }
            }
        }
        return null;
    }

    private boolean isWaterSource(BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        return state.isOf(Blocks.WATER) && state.getFluidState().isStill();
    }

    private BlockPos findSelfCobwebPos() {
        Box box = client.player.getBoundingBox();
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        int minY = MathHelper.floor(box.minY);
        int maxY = MathHelper.floor(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.world.getBlockState(pos).isOf(Blocks.COBWEB)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private PlacementTarget findBestWebPlacement() {
        double maxRange = range.getValue();
        double maxRangeSq = maxRange * maxRange;
        Vec3d eye = client.player.getEyePos();
        float maxFov = fov.getValue();

        List<PlayerEntity> targets = client.world.getEntitiesByClass(
                PlayerEntity.class,
                client.player.getBoundingBox().expand(maxRange + 1.5),
                player -> isValidWebTarget(player, eye, maxRangeSq, maxFov));
        if (targets.isEmpty()) {
            return null;
        }
        targets.sort(Comparator.comparingDouble(player -> player.squaredDistanceTo(client.player)));

        int lead = Math.max(0, predictTicks.getValue());
        for (PlayerEntity target : targets) {
            BlockPos actualFeet = target.getBlockPos();
            BlockPos predictedFeet = predictTargetFeet(target, lead);

            // If target's feet are already webbed and Double Web is enabled, place head web directly on top
            if (doubleWeb.getValue() && isCobweb(actualFeet) && !isCobweb(actualFeet.up())) {
                PlacementTarget headPlacement = tryBuildWebPlacement(actualFeet.up(), eye, maxRangeSq);
                if (headPlacement != null) {
                    return headPlacement;
                }
            }

            PlacementTarget feetPlacement = tryBuildWebPlacement(predictedFeet, eye, maxRangeSq);
            if (feetPlacement != null) {
                return feetPlacement;
            }
            if (!predictedFeet.equals(actualFeet)) {
                PlacementTarget actualFeetPlacement = tryBuildWebPlacement(actualFeet, eye, maxRangeSq);
                if (actualFeetPlacement != null) {
                    return actualFeetPlacement;
                }
            }
            if (doubleWeb.getValue() && isCobweb(predictedFeet) && !isCobweb(predictedFeet.up())) {
                PlacementTarget headPlacement = tryBuildWebPlacement(predictedFeet.up(), eye, maxRangeSq);
                if (headPlacement != null) {
                    return headPlacement;
                }
            }
        }
        return null;
    }

    private PlacementTarget tryBuildWebPlacement(BlockPos pos, Vec3d eye, double maxRangeSq) {
        if (pos == null || isCobweb(pos)) {
            return null;
        }
        BlockState state = client.world.getBlockState(pos);
        if (!state.isReplaceable()) {
            return null;
        }
        if (avoidSelf.getValue() && wouldWebSelf(pos)) {
            return null;
        }
        if (eye.squaredDistanceTo(Vec3d.ofCenter(pos)) > maxRangeSq) {
            return null;
        }
        // Cobwebs have an empty collision shape so BlockPlacement.find skips them as support,
        // even though vanilla allows stacking a second cobweb directly onto the top face of a cobweb.
        BlockPos below = pos.down();
        if (isCobweb(below)) {
            Vec3d topFaceCenter = Vec3d.ofCenter(below).add(0.0, 0.5, 0.0);
            if (eye.squaredDistanceTo(topFaceCenter) <= maxRangeSq) {
                BlockHitResult hit = new BlockHitResult(topFaceCenter, Direction.UP, below, false);
                return new PlacementTarget(pos, hit);
            }
        }
        PlacementTarget target = BlockPlacement.find(pos, true, true, true);
        if (target == null) {
            return null;
        }
        if (eye.squaredDistanceTo(target.hitResult().getPos()) > maxRangeSq) {
            return null;
        }
        return target;
    }

    private BlockPos predictTargetFeet(PlayerEntity target, int leadTicks) {
        if (leadTicks <= 0) {
            return target.getBlockPos();
        }
        double vx = target.getX() - target.lastX;
        double vz = target.getZ() - target.lastZ;
        double predX = target.getX() + vx * leadTicks;
        double predZ = target.getZ() + vz * leadTicks;
        BlockPos predicted = BlockPos.ofFloored(predX, target.getY() + 0.05, predZ);
        if (client.world.getBlockState(predicted).isReplaceable()) {
            return predicted;
        }
        return target.getBlockPos();
    }

    private boolean isValidWebTarget(PlayerEntity player, Vec3d eye, double maxRangeSq, float maxFov) {
        if (player == null || player == client.player || !player.isAlive() || player.isRemoved()
                || player.isSpectator() || !TargetSelection.isValidTarget(player)) {
            return false;
        }
        BlockPos feet = player.getBlockPos();
        if (onlyGrounded.getValue() && !player.isOnGround() && !isCobweb(feet)) {
            return false;
        }
        if (player.getBoundingBox().squaredMagnitude(eye) > maxRangeSq) {
            return false;
        }
        if (maxFov < 360.0F && !EntityRaycasts.isWithinFov(client.player, player, maxFov, maxFov)) {
            return false;
        }
        if (isCobweb(feet) && (!doubleWeb.getValue() || isCobweb(feet.up()))) {
            return false;
        }
        return true;
    }

    private BlockHitResult getManualCrosshairBlock() {
        if (client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return hit;
        }
        return null;
    }

    private boolean wouldWebSelf(BlockPos pos) {
        Box selfExpanded = client.player.getBoundingBox().expand(0.15, 0.0, 0.15);
        return selfExpanded.intersects(new Box(pos));
    }

    private boolean isCobweb(BlockPos pos) {
        return client.world.getBlockState(pos).isOf(Blocks.COBWEB);
    }

    private boolean isActivateKeyPressed() {
        InputKey key = activateKey.getKey();
        return key != null && key != InputKey.NONE && key.isPressed();
    }

    private boolean isBlatantTriggerActive() {
        if (mode.isSelected(MODE_ALWAYS)) {
            return true;
        }
        return isActivateKeyPressed();
    }

    private void scheduleSwapBack(int activeSlot) {
        if (swapBack.getValue() && returnSlot != -1 && returnSlot != activeSlot) {
            returnWaitTicks = Math.max(1, swapDelay.getValue());
        } else {
            returnSlot = -1;
            returnWaitTicks = 0;
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
        if (current == null) {
            return false;
        }
        Rotation desired = Rotation.lookingAt(client.player.getEyePos(), point);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(desired.yaw() - current.yaw()));
        float pitchDiff = Math.abs(desired.pitch() - current.pitch());
        return yawDiff <= AIM_TOLERANCE_DEGREES && pitchDiff <= AIM_TOLERANCE_DEGREES;
    }

    private void clearActivePlacement() {
        activePlacement = null;
        waterAimPoint = null;
        releaseRotation();
    }

    private void releaseRotation() {
        if (!rotations().isOwnedBy(this)) {
            return;
        }
        if (silentRotations.getValue()) {
            rotations().beginReturn(this);
        } else {
            rotations().cancel(this);
        }
    }

    private void resetSwapState() {
        swapWaitTicks = 0;
        returnSlot = -1;
        returnWaitTicks = 0;
        pendingLegitPlace = false;
        pendingBlatantPlace = false;
        pendingClearPlace = false;
        pendingClearPickup = false;
    }

    private void resetState() {
        activePlacement = null;
        waterAimPoint = null;
        placedWaterPos = null;
        placedWaterTick = -1L;
        cooldownTicks = 0;
        disablePending = false;
        resetSwapState();
    }

    public static void appendBootstrapSettings(AutoWeb module) {
        CatalogSettings.append(module, style);
        CatalogSettings.append(module, mode);
        CatalogSettings.append(module, activateKey);
        CatalogSettings.append(module, swapMode);
        CatalogSettings.append(module, swapBack);
        CatalogSettings.append(module, swapDelay);
        CatalogSettings.append(module, range);
        CatalogSettings.append(module, fov);
        CatalogSettings.append(module, predictTicks);
        CatalogSettings.append(module, placeDelay);
        CatalogSettings.append(module, doubleWeb);
        CatalogSettings.append(module, avoidSelf);
        CatalogSettings.append(module, onlyGrounded);
        CatalogSettings.append(module, silentRotations);
        CatalogSettings.append(module, rotationSpeed);
        CatalogSettings.append(module, autoClearSelf);
    }

    public static void assignBootstrapSettings(
            AutoWeb module,
            ChoiceSetting preparedStyle,
            ChoiceSetting preparedMode,
            KeybindSetting preparedActivateKey,
            ChoiceSetting preparedSwapMode,
            BooleanSetting preparedSwapBack,
            IntSetting preparedSwapDelay,
            FloatSetting preparedRange,
            FloatSetting preparedFov,
            IntSetting preparedPredictTicks,
            IntSetting preparedPlaceDelay,
            BooleanSetting preparedDoubleWeb,
            BooleanSetting preparedAvoidSelf,
            BooleanSetting preparedOnlyGrounded,
            BooleanSetting preparedSilentRotations,
            FloatSetting preparedRotationSpeed,
            BooleanSetting preparedAutoClearSelf) {
        style = preparedStyle;
        mode = preparedMode;
        activateKey = preparedActivateKey;
        swapMode = preparedSwapMode;
        swapBack = preparedSwapBack;
        swapDelay = preparedSwapDelay;
        range = preparedRange;
        fov = preparedFov;
        predictTicks = preparedPredictTicks;
        placeDelay = preparedPlaceDelay;
        doubleWeb = preparedDoubleWeb;
        avoidSelf = preparedAvoidSelf;
        onlyGrounded = preparedOnlyGrounded;
        silentRotations = preparedSilentRotations;
        rotationSpeed = preparedRotationSpeed;
        autoClearSelf = preparedAutoClearSelf;
    }
}
