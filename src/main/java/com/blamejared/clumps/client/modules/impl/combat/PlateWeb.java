package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.interaction.BlockInteractEvent;
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
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.utils.Rotation;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.placement.BlockPlacement;
import com.blamejared.clumps.client.utils.placement.PlacementTarget;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PlateWeb extends Module {
    private static final CharSequence TRIGGER_SELF = CatalogText.literal("Self Placed");
    private static final CharSequence TRIGGER_ANY = CatalogText.literal("Any Nearby");
    private static final CharSequence SWAP_LEGIT = CatalogText.literal("Legit");
    private static final CharSequence SWAP_SILENT = CatalogText.literal("Silent");

    private static final CharSequence SECTION_SWAP = CatalogText.literal("Swap");
    private static final CharSequence SECTION_PLACEMENT = CatalogText.literal("Placement");
    private static final CharSequence SECTION_ROTATION = CatalogText.literal("Rotation");

    private static final float AIM_TOLERANCE_DEGREES = 10.0F;
    private static final long PLATE_EXPIRE_TICKS = 24L;

    public static ChoiceSetting trigger;
    public static ChoiceSetting swapMode;
    public static BooleanSetting swapBack;
    public static IntSetting swapDelay;
    public static IntSetting placeDelay;
    public static FloatSetting range;
    public static BooleanSetting silentRotations;
    public static FloatSetting rotationSpeed;

    private record PendingPlate(BlockPos platePos, long readyAtTick, long expiresAtTick) {
    }

    private final Deque<PendingPlate> pendingPlates = new ArrayDeque<>();
    private PlacementTarget activePlacement;
    private BlockPos activePlatePos;
    private int swapWaitTicks;
    private int returnSlot = -1;
    private int returnWaitTicks;
    private boolean pendingLegitPlace;
    private boolean disablePending;

    public PlateWeb() {
        super(new ModuleInfo(
                CatalogText.literal("Plate Web"),
                CatalogText.literal("Automatically places a cobweb on top of placed pressure plates"),
                ModuleCategory.COMBAT));
    }

    private static boolean isLegitSwap() {
        return swapMode == null || swapMode.isSelected(SWAP_LEGIT);
    }

    private static boolean isAnyNearbyTrigger() {
        return trigger != null && trigger.isSelected(TRIGGER_ANY);
    }

    @Override
    public void postInitialize() {
        resetState();
        if (trigger != null && !trigger.isSelected(TRIGGER_SELF) && !trigger.isSelected(TRIGGER_ANY)) {
            trigger.setValueSilently(TRIGGER_SELF);
        }
        if (swapMode != null && !swapMode.isSelected(SWAP_LEGIT) && !swapMode.isSelected(SWAP_SILENT)) {
            swapMode.setValueSilently(SWAP_LEGIT);
        }

        swapDelay.setVisibilityPredicate(ignored -> isLegitSwap());

        trigger.description(CatalogText.literal("Self Placed triggers when you place a pressure plate; Any Nearby also reacts to nearby plates"));
        swapMode.description(CatalogText.literal("Legit visibly switches hotbar slot to cobwebs; Silent swaps and restores instantly"));
        swapBack.description(CatalogText.literal("Return to your previous hotbar slot after placing the cobweb"));
        swapDelay.description(CatalogText.literal("Ticks to wait after switching slots before placing and returning in Legit swap mode"));
        placeDelay.description(CatalogText.literal("Ticks to wait after a pressure plate is placed before placing the cobweb"));
        range.description(CatalogText.literal("Maximum distance to place a cobweb onto a pressure plate"));
        silentRotations.description(CatalogText.literal("Silently rotate toward the pressure plate when your crosshair is not already on it"));
        rotationSpeed.description(CatalogText.literal("Rotation speed when aiming at the pressure plate"));

        swapMode.tooltip(SECTION_SWAP);
        swapBack.tooltip(SECTION_SWAP);
        swapDelay.tooltip(SECTION_SWAP);
        placeDelay.tooltip(SECTION_PLACEMENT);
        range.tooltip(SECTION_PLACEMENT);
        silentRotations.tooltip(SECTION_ROTATION);
        rotationSpeed.tooltip(SECTION_ROTATION);
    }

    @Override
    public CharSequence getSuffix() {
        return swapMode != null ? swapMode.getValue() : SWAP_LEGIT;
    }

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        pendingPlates.clear();
        activePlacement = null;
        activePlatePos = null;
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
    private void onBlockInteract(BlockInteractEvent.Post event) {
        if (disablePending || isOutsideWorld() || client.player == null || event.getPlayer() != client.player) {
            return;
        }
        ItemStack stack = client.player.getStackInHand(event.getHand());
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        if (!(blockItem.getBlock() instanceof AbstractPressurePlateBlock)) {
            return;
        }
        BlockPos platePos = BlockPlacement.placementPosition(event.getHit());
        if (platePos == null) {
            return;
        }
        queuePlate(platePos);
    }

    @Subscribe
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (disablePending || !isAnyNearbyTrigger() || isOutsideWorld() || client.player == null) {
            return;
        }
        BlockState state = event.getState();
        if (state == null || !(state.getBlock() instanceof AbstractPressurePlateBlock)) {
            return;
        }
        BlockPos platePos = event.getPosition();
        if (platePos == null) {
            return;
        }
        double maxRange = range.getValue();
        Vec3d topCenter = new Vec3d(platePos.getX() + 0.5, platePos.getY() + 0.0625, platePos.getZ() + 0.5);
        if (client.player.getEyePos().squaredDistanceTo(topCenter) <= maxRange * maxRange) {
            queuePlate(platePos);
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
        if (activePlacement == null) {
            releaseRotation();
            return;
        }
        Vec3d aimPoint = activePlacement.hitResult().getPos();
        if (isCrosshairOnPlate(activePlatePos)) {
            releaseRotation();
            return;
        }
        float speed = rotationSpeed.getValue() * 20.0F;
        RotationSettings settings = new RotationSettings(
                silentRotations.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE,
                AimTransition.EASED,
                speed,
                speed);
        if (!rotations().aim(this, RotationPriority.HIGH, aimPoint, settings)) {
            releaseRotation();
        }
    }

    @Subscribe(priority = EventPriority.NORMAL)
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (disablePending) {
            return;
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
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) {
            clearActivePlacement();
            return;
        }

        // Finish pending legit swap-back once idle
        if (returnSlot != -1 && returnWaitTicks == 0 && !pendingLegitPlace && pendingPlates.isEmpty()) {
            int slotToRestore = returnSlot;
            returnSlot = -1;
            if (swapBack.getValue() && slotToRestore >= 0 && slotToRestore <= 8
                    && client.player.getInventory().getSelectedSlot() != slotToRestore) {
                selectSlotDirect(slotToRestore);
            }
        }

        if (pendingPlates.isEmpty()) {
            clearActivePlacement();
            return;
        }

        int webSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.COBWEB));
        if (webSlot == -1) {
            pendingPlates.clear();
            abortPendingLegitPlace();
            clearActivePlacement();
            return;
        }

        long nowTick = client.player.age;
        PendingPlate ready = pollNextReadyPlate(nowTick);
        if (ready == null) {
            clearActivePlacement();
            return;
        }

        PlacementTarget target = buildPlateWebPlacement(ready.platePos());
        if (target == null) {
            pendingPlates.remove(ready);
            clearActivePlacement();
            return;
        }

        activePlatePos = ready.platePos();
        activePlacement = target;

        if (!isCrosshairOnPlate(activePlatePos) && !isAimedAt(target.hitResult().getPos())) {
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
            ActionResult result = BlockPlacement.interactMainHand(this, target.hitResult(), true);
            if (swapBack.getValue() && prevSlot != webSlot) {
                selectSlotDirect(prevSlot);
            }
            if (result.isAccepted()) {
                pendingPlates.remove(ready);
                clearActivePlacement();
            }
            return;
        }

        // Legit swap: visibly select cobweb slot, wait swapDelay ticks, place, then restore
        int currentSlot = client.player.getInventory().getSelectedSlot();
        if (currentSlot != webSlot) {
            if (returnSlot == -1) {
                returnSlot = currentSlot;
            }
            if (!selectSlotDirect(webSlot)) {
                return;
            }
            pendingLegitPlace = true;
            swapWaitTicks = Math.max(0, swapDelay.getValue());
            if (swapWaitTicks > 0) {
                return;
            }
        }
        if (pendingLegitPlace && swapWaitTicks > 0) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(Items.COBWEB)) {
            return;
        }
        ActionResult result = BlockPlacement.interactMainHand(this, target.hitResult(), true);
        if (result.isAccepted()) {
            pendingPlates.remove(ready);
            pendingLegitPlace = false;
            clearActivePlacement();
            if (pendingPlates.isEmpty()) {
                scheduleSwapBack(webSlot);
            }
        }
    }

    private void queuePlate(BlockPos pos) {
        BlockPos immutable = pos.toImmutable();
        for (PendingPlate existing : pendingPlates) {
            if (existing.platePos().equals(immutable)) {
                return;
            }
        }
        long now = client.player != null ? client.player.age : 0L;
        long delay = Math.max(0, placeDelay != null ? placeDelay.getValue() : 1);
        pendingPlates.addLast(new PendingPlate(immutable, now + delay, now + PLATE_EXPIRE_TICKS));
    }

    private PendingPlate pollNextReadyPlate(long nowTick) {
        double maxRangeSq = range.getValue() * range.getValue();
        Vec3d eye = client.player.getEyePos();
        Iterator<PendingPlate> iterator = pendingPlates.iterator();
        while (iterator.hasNext()) {
            PendingPlate entry = iterator.next();
            if (nowTick > entry.expiresAtTick()) {
                iterator.remove();
                continue;
            }
            BlockPos platePos = entry.platePos();
            BlockPos webPos = platePos.up();
            BlockState aboveState = client.world.getBlockState(webPos);
            if (aboveState.isOf(Blocks.COBWEB) || !aboveState.isReplaceable()) {
                iterator.remove();
                continue;
            }
            if (nowTick < entry.readyAtTick()) {
                continue;
            }
            BlockState plateState = client.world.getBlockState(platePos);
            if (!(plateState.getBlock() instanceof AbstractPressurePlateBlock)) {
                // Allow a brief window after interact for server confirmation before dropping
                if (nowTick - entry.readyAtTick() >= 4L) {
                    iterator.remove();
                }
                continue;
            }
            Vec3d topCenter = new Vec3d(platePos.getX() + 0.5, platePos.getY() + 0.0625, platePos.getZ() + 0.5);
            if (eye.squaredDistanceTo(topCenter) > maxRangeSq) {
                continue;
            }
            return entry;
        }
        return null;
    }

    private PlacementTarget buildPlateWebPlacement(BlockPos platePos) {
        BlockPos webPos = platePos.up();
        if (!client.world.getBlockState(webPos).isReplaceable()) {
            return null;
        }
        double maxRangeSq = range.getValue() * range.getValue();
        Vec3d eye = client.player.getEyePos();

        // If player's crosshair is already on the pressure plate, reuse their natural hit point
        if (client.crosshairTarget instanceof BlockHitResult crosshair
                && crosshair.getType() == HitResult.Type.BLOCK
                && crosshair.getBlockPos().equals(platePos)
                && crosshair.getSide() == Direction.UP
                && eye.squaredDistanceTo(crosshair.getPos()) <= maxRangeSq) {
            return new PlacementTarget(webPos, crosshair);
        }

        // Pressure plates have an empty collision shape, so target their top outline face directly (y + 1/16)
        Vec3d topCenter = new Vec3d(platePos.getX() + 0.5, platePos.getY() + 0.0625, platePos.getZ() + 0.5);
        if (eye.squaredDistanceTo(topCenter) <= maxRangeSq) {
            BlockHitResult hit = new BlockHitResult(topCenter, Direction.UP, platePos, false);
            return new PlacementTarget(webPos, hit);
        }

        PlacementTarget fallback = BlockPlacement.find(webPos, true, true, true);
        if (fallback != null && eye.squaredDistanceTo(fallback.hitResult().getPos()) <= maxRangeSq) {
            return fallback;
        }
        return null;
    }

    private boolean isCrosshairOnPlate(BlockPos platePos) {
        return platePos != null
                && client.crosshairTarget instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(platePos)
                && hit.getSide() == Direction.UP;
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

    private void scheduleSwapBack(int activeSlot) {
        if (swapBack.getValue() && returnSlot != -1 && returnSlot != activeSlot) {
            returnWaitTicks = Math.max(1, swapDelay.getValue());
        } else {
            returnSlot = -1;
            returnWaitTicks = 0;
        }
    }

    private void abortPendingLegitPlace() {
        pendingLegitPlace = false;
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

    private void clearActivePlacement() {
        activePlacement = null;
        activePlatePos = null;
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
        pendingLegitPlace = false;
    }

    private void resetState() {
        pendingPlates.clear();
        activePlacement = null;
        activePlatePos = null;
        disablePending = false;
        resetSwapState();
    }

    public static CharSequence bootstrapTriggerSelf() {
        return TRIGGER_SELF;
    }

    public static CharSequence bootstrapTriggerAny() {
        return TRIGGER_ANY;
    }

    public static CharSequence bootstrapSwapLegit() {
        return SWAP_LEGIT;
    }

    public static CharSequence bootstrapSwapSilent() {
        return SWAP_SILENT;
    }

    public static void appendBootstrapSettings(PlateWeb module) {
        CatalogSettings.append(module, trigger);
        CatalogSettings.append(module, swapMode);
        CatalogSettings.append(module, swapBack);
        CatalogSettings.append(module, swapDelay);
        CatalogSettings.append(module, placeDelay);
        CatalogSettings.append(module, range);
        CatalogSettings.append(module, silentRotations);
        CatalogSettings.append(module, rotationSpeed);
    }

    public static void assignBootstrapSettings(
            PlateWeb module,
            ChoiceSetting preparedTrigger,
            ChoiceSetting preparedSwapMode,
            BooleanSetting preparedSwapBack,
            IntSetting preparedSwapDelay,
            IntSetting preparedPlaceDelay,
            FloatSetting preparedRange,
            BooleanSetting preparedSilentRotations,
            FloatSetting preparedRotationSpeed) {
        trigger = preparedTrigger;
        swapMode = preparedSwapMode;
        swapBack = preparedSwapBack;
        swapDelay = preparedSwapDelay;
        placeDelay = preparedPlaceDelay;
        range = preparedRange;
        silentRotations = preparedSilentRotations;
        rotationSpeed = preparedRotationSpeed;
    }
}
