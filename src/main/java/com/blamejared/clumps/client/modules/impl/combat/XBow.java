package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.rotation.RotationManager;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.settings.MultiChoiceSetting;
import com.blamejared.clumps.client.utils.Rotation;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.placement.BlockPlacement;
import com.blamejared.clumps.client.utils.placement.PlacementTarget;
import com.blamejared.clumps.client.utils.targeting.BlockAimRotation;
import com.blamejared.clumps.client.utils.targeting.BlockRaycasts;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class XBow extends Module {
    private static final float AIM_SPEED = 350.0F;
    private static final int TIMEOUT_TICKS = 40;
    private static final double FIRE_MARGIN = 0.2;
    private static final double SEARCH_STEP = 0.1;
    private static final double CART_INSET = 0.05;
    private static final int FIRE = 0;
    private static final int RAIL = 1;
    private static final int CART = 2;
    private static final int SHOOT = 3;
    private static final int SLAB = 4;
    private static final CharSequence placeOption2 = CatalogText.literal("Fire");
    private static final CharSequence placeOption4 = CatalogText.literal("Rail");
    private static final CharSequence placeOption3 = CatalogText.literal("Cart");
    private static final CharSequence placeOption = CatalogText.literal("Shoot");
    private static final String TIMING_TOOLTIP = "Timing";
    private static KeybindSetting activateKey;
    private static MultiChoiceSetting place;
    private static BooleanSetting autoAim;
    private static BooleanSetting silentRotations;
    private static final BooleanSetting safe = new BooleanSetting(CatalogText.literal("Safe"), false);
    private static final IntSetting railDelay = new IntSetting(CatalogText.literal("Rail Delay"), 0, 0, 10, CatalogText.literal(" ticks"));
    private static final IntSetting cartDelay = new IntSetting(CatalogText.literal("Cart Delay"), 0, 0, 10, CatalogText.literal(" ticks"));
    private static final IntSetting fireDelay = new IntSetting(CatalogText.literal("Fire Delay"), 0, 0, 10, CatalogText.literal(" ticks"));
    private static final IntSetting shootDelay = new IntSetting(CatalogText.literal("Shoot Delay"), 0, 0, 10, CatalogText.literal(" ticks"));
    private static final IntSetting slabDelay = new IntSetting(CatalogText.literal("Slab Delay"), 0, 0, 10, CatalogText.literal(" ticks"));
    private static final IntSetting switchBackDelay = new IntSetting(CatalogText.literal("Switch Back Delay"), 1, 0, 10, CatalogText.literal(" ticks"));
    private BlockPos targetBlockPosition;
    private BlockPos supportBlockPosition;
    private BlockPos safeSlabPosition;
    private Vec3d cachedTargetPosition;
    private Vec3d targetHitPosition;
    private long activationTick;
    private long stepEnteredTick;
    private int lastTrackedStep;
    private BlockHitResult pendingBlockHit;
    private int placementStep;
    private int selectedPlacementSlot;
    private int lockedCrossbowSlot = -1;
    private long lastShotTick = -10L;
    private boolean interactionSubmitted;
    private boolean shotFired;
    private boolean slabPlaced;
    private boolean activationKeyHeld;
    private boolean waitingForRotationRelease;

    public XBow() {
        super(new ModuleInfo(CatalogText.literal("XBow"), CatalogText.literal("Lights a fire, lays a TNT cart and shoots your crossbow through it"), ModuleCategory.COMBAT));
    }

    public static void appendBootstrapSettings(XBow Module) {
        CatalogSettings.append(Module, activateKey);
        CatalogSettings.append(Module, place);
        CatalogSettings.append(Module, autoAim);
        CatalogSettings.append(Module, silentRotations);
        CatalogSettings.append(Module, safe);
        CatalogSettings.append(Module, railDelay);
        CatalogSettings.append(Module, cartDelay);
        CatalogSettings.append(Module, fireDelay);
        CatalogSettings.append(Module, shootDelay);
        CatalogSettings.append(Module, slabDelay);
        CatalogSettings.append(Module, switchBackDelay);
    }

    public static CharSequence bootstrapPlaceOption2() { return placeOption2; }
    public static CharSequence bootstrapPlaceOption4() { return placeOption4; }
    public static CharSequence bootstrapPlaceOption3() { return placeOption3; }
    public static CharSequence bootstrapPlaceOption() { return placeOption; }

    public static void assignBootstrapSettings(XBow Module, MultiChoiceSetting preparedPlace, KeybindSetting preparedActivateKey, BooleanSetting preparedAutoAim, BooleanSetting preparedSilentRotations) {
        place = preparedPlace;
        activateKey = preparedActivateKey;
        autoAim = preparedAutoAim;
        silentRotations = preparedSilentRotations;
    }

    @Override
    public void postInitialize() {
        place.setSelected(placeOption2, true);
        place.setSelected(placeOption4, true);
        place.setSelected(placeOption3, true);
        place.setSelected(placeOption, true);
        silentRotations.setVisibilityPredicate(ignored -> autoAim.getValue());
        railDelay.tooltip(TIMING_TOOLTIP);
        cartDelay.tooltip(TIMING_TOOLTIP);
        fireDelay.tooltip(TIMING_TOOLTIP);
        shootDelay.tooltip(TIMING_TOOLTIP);
        slabDelay.tooltip(TIMING_TOOLTIP);
        switchBackDelay.tooltip(TIMING_TOOLTIP);
        railDelay.setVisibilityPredicate(ignored -> place.isSelected(placeOption4));
        cartDelay.setVisibilityPredicate(ignored -> place.isSelected(placeOption3));
        fireDelay.setVisibilityPredicate(ignored -> place.isSelected(placeOption2));
        shootDelay.setVisibilityPredicate(ignored -> place.isSelected(placeOption));
        slabDelay.setVisibilityPredicate(ignored -> safe.getValue());
        safe.description(CatalogText.literal("Places a protective slab in front of you after shooting to reduce TNT cart self-damage"));
        railDelay.description(CatalogText.literal("Ticks to wait before placing the rail"));
        cartDelay.description(CatalogText.literal("Ticks to wait after placing the rail before placing the TNT cart"));
        fireDelay.description(CatalogText.literal("Ticks to wait before lighting the fire"));
        shootDelay.description(CatalogText.literal("Ticks to wait before firing the crossbow through the fire"));
        slabDelay.description(CatalogText.literal("Ticks to wait after shooting before placing the protective slab"));
        switchBackDelay.description(CatalogText.literal("Ticks to wait after finishing before switching back to your previous slot"));
        lastTrackedStep = -1;
        stepEnteredTick = -1L;
    }

    
    @Override
    public void onEnable() {
        targetBlockPosition = null;
        supportBlockPosition = null;
        safeSlabPosition = null;
        cachedTargetPosition = null;
        targetHitPosition = null;
        pendingBlockHit = null;
        selectedPlacementSlot = -1;
        lockedCrossbowSlot = -1;
        lastShotTick = -10L;
        interactionSubmitted = false;
        shotFired = false;
        slabPlaced = false;
        activationKeyHeld = false;
        waitingForRotationRelease = false;
        super.onEnable();
    }

    
    @Override
    public void onDisable() {
        clearPlacementState();
        DeferredActionQueue.finish(this, true);
        if (silentRotations.getValue()) {
            if (rotations().isOwnedBy(this)) {
                waitingForRotationRelease = true;
                return;
            }
        }
        waitingForRotationRelease = false;
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        boolean pressed = activateKey.getKey().isPressed();
        boolean risingEdge = pressed && !activationKeyHeld;
        activationKeyHeld = pressed;
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (targetBlockPosition == null) {
            if (risingEdge) beginSequence();
            return;
        }
        if (!isStepDelayElapsed(placementStep)) return;
        if (placementStep == SHOOT) {
            if (selectedPlacementSlot == -1) {
                if (!InventoryItems.isChargedCrossbow(client.player.getOffHandStack())) return;
            } else if (DeferredActionQueue.selectedSlot() != selectedPlacementSlot
                    || !InventoryItems.isChargedCrossbow(client.player.getInventory().getStack(selectedPlacementSlot))) {
                return;
            }
            if (!canShootThroughFire()) return;
            // Ensure exactly one click is sent so holding/pressing with 2 loaded crossbows never fires both.
            event.setClickCount(1);
            lastShotTick = client.player.age;
            shotFired = true;
            if (safe.getValue() && findStepSlot(SLAB) != -1) {
                safeSlabPosition = findSafeSlabPosition(targetBlockPosition);
                if (safeSlabPosition != null) {
                    pendingBlockHit = null;
                    interactionSubmitted = false;
                    return;
                }
            }
            finishSequence();
            return;
        }
        if (pendingBlockHit == null || DeferredActionQueue.selectedSlot() != selectedPlacementSlot
                || !BlockRaycasts.matchesHit(pendingBlockHit, placementStep != CART)) return;
        if (placementStep == CART) {
            if (interactionSubmitted) return;
            interactionSubmitted = true;
        }
        event.addClick();
        if (placementStep == SLAB) {
            slabPlaced = true;
            finishSequence();
        }
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (targetBlockPosition == null || isOutsideWorld() || selectedPlacementSlot == -1
                || event.slot != selectedPlacementSlot) return;
        if (DeferredActionQueue.selectedSlot() != selectedPlacementSlot
                && DeferredActionQueue.ensureSelected(this, selectedPlacementSlot, -1, true)) {
            event.addClick();
        }
    }

    @Subscribe
    private void afterMouseMovement(MouseMovementEvent.Post event) {
        if (waitingForRotationRelease) {
            finishDisableAfterReturn();
            return;
        }
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            if (targetBlockPosition != null) abortSequence();
            else releaseRotation();
            return;
        }
        if (targetBlockPosition == null) {
            releaseRotation();
            return;
        }
        int maxTicks = TIMEOUT_TICKS + railDelay.getValue() + cartDelay.getValue()
                + fireDelay.getValue() + shootDelay.getValue() + slabDelay.getValue();
        if ((long) client.player.age - activationTick > maxTicks) {
            if (shotFired) finishSequence();
            else abortSequence();
            return;
        }
        if (!prepareStep()) return;
        if (autoAim.getValue() && !rotations().aim(this, RotationPriority.HIGH, placementStep == SHOOT ? targetHitPosition : pendingBlockHit.getPos(), RotationSettings.uniform(silentRotations.getValue()
                        ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, AimTransition.EASED, AIM_SPEED))) {
            if (shotFired) finishSequence();
            else abortSequence();
        }
    }

    private void beginSequence() {
        if ((long) client.player.age - lastShotTick < 3L) return;
        if (!(place.isSelected(placeOption2) || place.isSelected(placeOption4)
                || place.isSelected(placeOption3) || place.isSelected(placeOption))) return;
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos target = AbstractRailBlock.isRail(client.world, hit.getBlockPos())
                ? hit.getBlockPos() : BlockPlacement.placementPosition(hit);
        Vec3d hitPoint = hit.getPos();
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        double reachSquared = MathHelper.square(client.player.getBlockInteractionRange());
        boolean railPresent = AbstractRailBlock.isRail(client.world, target);
        if (place.isSelected(placeOption4) && !railPresent) {
            if (!BlockPlacement.isReplaceable(target) || !Block.hasTopRim(client.world, target.down())) return;
            if (findStepSlot(RAIL) == -1) return;
            BlockHitResult railHit = BlockAimRotation.findHit(target, hitPoint);
            if (railHit == null || eye.squaredDistanceTo(railHit.getPos()) > reachSquared) return;
        }
        if (place.isSelected(placeOption3)) {
            if (!railPresent && !place.isSelected(placeOption4)) return;
            if (findStepSlot(CART) == -1) return;
            if (eye.squaredDistanceTo(BlockAimRotation.insetBottomPoint(target, hitPoint)) > reachSquared) return;
        }
        lockedCrossbowSlot = -1;
        if (place.isSelected(placeOption)) {
            int chosen = chooseSingleCrossbowSlot();
            if (!hasCrossbow(chosen)) return;
            lockedCrossbowSlot = chosen;
        }
        supportBlockPosition = null;
        safeSlabPosition = null;
        shotFired = false;
        slabPlaced = false;
        targetHitPosition = Vec3d.add(target, 0.5, 0.35, 0.5);
        if (place.isSelected(placeOption2)) {
            if (findStepSlot(FIRE) == -1) return;
            if (!findAdjacentFirePosition(target, eye, reachSquared)
                    && !findFirePositionAlongShot(target, eye, reachSquared)) return;
        }
        targetBlockPosition = target;
        cachedTargetPosition = hitPoint;
        activationTick = client.player.age;
    }

    private boolean findAdjacentFirePosition(BlockPos target, Vec3d eye, double reachSquared) {
        Vec3d offset = eye.subtract(Vec3d.ofCenter(target));
        Direction bestDirection = null;
        double bestAlignment = 0.5;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            double alignment = direction.getOffsetX() * offset.x + direction.getOffsetZ() * offset.z;
            if (alignment <= bestAlignment) continue;
            BlockPos candidate = target.offset(direction);
            if (!AbstractFireBlock.canPlaceAt(client.world, candidate, client.player.getHorizontalFacing())) continue;
            if (client.player.getBoundingBox().intersects(new Box(candidate))) continue;
            PlacementTarget placement = BlockPlacement.find(candidate, true);
            if (placement == null || eye.squaredDistanceTo(placement.hitResult().getPos()) > reachSquared) continue;
            bestDirection = direction;
            bestAlignment = alignment;
        }
        if (bestDirection == null) return false;
        supportBlockPosition = target.offset(bestDirection);
        targetHitPosition = Vec3d.add(target, 0.5 + bestDirection.getOffsetX() * 0.3, 0.35, 0.5 + bestDirection.getOffsetZ() * 0.3);
        return true;
    }

    private boolean findFirePositionAlongShot(BlockPos target, Vec3d eye, double reachSquared) {
        Vec3d delta = targetHitPosition.subtract(eye);
        double distance = delta.length();
        Vec3d direction = delta.multiply(1.0 / distance);
        Set<BlockPos> visited = new HashSet<>();
        for (double along = distance; along > 0.0; along -= SEARCH_STEP) {
            BlockPos candidate = BlockPos.ofFloored(eye.add(direction.multiply(along)));
            if (tryFirePosition(candidate, target, eye, reachSquared, visited)
                    || tryFirePosition(candidate.down(), target, eye, reachSquared, visited)) return true;
        }
        return false;
    }

    private boolean tryFirePosition(BlockPos candidate, BlockPos target, Vec3d eye, double reachSquared, Set<BlockPos> visited) {
        if (candidate.equals(target) || !visited.add(candidate)) return false;
        Box bounds = new Box(candidate).expand(FIRE_MARGIN);
        if (!bounds.contains(eye) && bounds.raycast(eye, targetHitPosition).isEmpty()) return false;
        if (!AbstractFireBlock.canPlaceAt(client.world, candidate, client.player.getHorizontalFacing())) return false;
        PlacementTarget placement = BlockPlacement.find(candidate, true);
        if (placement == null || eye.squaredDistanceTo(placement.hitResult().getPos()) > reachSquared) return false;
        supportBlockPosition = candidate;
        return true;
    }

    private boolean prepareStep() {
        placementStep = nextStep();
        if (placementStep == -1) {
            if (shotFired) finishSequence();
            else abortSequence();
            return false;
        }
        if (placementStep != lastTrackedStep) {
            lastTrackedStep = placementStep;
            stepEnteredTick = client.player.age;
        }
        if (placementStep != CART) interactionSubmitted = false;
        pendingBlockHit = null;
        selectedPlacementSlot = placementStep == SHOOT ? resolveLockedCrossbowSlot() : findStepSlot(placementStep);
        boolean awaitingCart = placementStep == CART && selectedPlacementSlot == -1 && interactionSubmitted;
        if (!awaitingCart && (placementStep == SHOOT
                ? !hasLockedCrossbow(selectedPlacementSlot) : selectedPlacementSlot == -1)) {
            if (placementStep == SLAB || shotFired) finishSequence();
            else abortSequence();
            return false;
        }
        if (placementStep == SHOOT) return true;
        pendingBlockHit = findStepHit();
        if (pendingBlockHit == null && placementStep == SLAB) {
            finishSequence();
            return false;
        }
        return pendingBlockHit != null;
    }

    private static int findStepSlot(int step) {
        return switch (step) {
            case FIRE -> InventoryItems.findStackInHotbar(XBow::isIgnitionItem);
            case RAIL -> InventoryItems.findStackInHotbar(InventoryItems::isRail);
            case CART -> InventoryItems.findStackInHotbar(XBow::isTntCart);
            case SLAB -> InventoryItems.findStackInHotbar(XBow::isSlabItem);
            default -> InventoryItems.findStackInHotbar(InventoryItems::isChargedCrossbow);
        };
    }

    private static boolean isIgnitionItem(ItemStack stack) { return stack.isOf(Items.FLINT_AND_STEEL); }
    private static boolean isTntCart(ItemStack stack) { return stack.isOf(Items.TNT_MINECART); }
    private static boolean isSlabItem(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();
        return block instanceof SlabBlock || block.getDefaultState().isIn(BlockTags.SLABS);
    }

    private BlockPos findSafeSlabPosition(BlockPos cartPos) {
        if (cartPos == null || client.player == null || client.world == null) return null;
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        double reachSquared = MathHelper.square(client.player.getBlockInteractionRange());
        Vec3d fromCartToPlayer = client.player.getEntityPos().subtract(Vec3d.ofCenter(cartPos));
        Direction towardPlayer = Direction.getFacing(fromCartToPlayer.x, 0.0, fromCartToPlayer.z);
        Direction towardCart = towardPlayer.getOpposite();
        BlockPos feet = client.player.getBlockPos();
        BlockPos[] candidates = new BlockPos[]{
                feet.offset(towardCart),
                cartPos.offset(towardPlayer),
                feet.offset(towardCart).down()
        };
        for (BlockPos candidate : candidates) {
            if (candidate == null || candidate.equals(cartPos)) continue;
            if (!BlockPlacement.isReplaceable(candidate)) continue;
            if (BlockPlacement.intersectsPlayer(candidate)) continue;
            if (BlockPlacement.hasBlockingEntity(candidate)) continue;
            BlockHitResult hit = findSlabHit(candidate, eye, reachSquared);
            if (hit != null) return candidate;
        }
        return null;
    }

    private BlockHitResult findSlabHit(BlockPos candidate, Vec3d eye, double reachSquared) {
        if (candidate == null || !BlockPlacement.isReplaceable(candidate)) return null;
        BlockPos below = candidate.down();
        if (BlockPlacement.supportsFace(below, Direction.UP, false)) {
            Vec3d topCenter = new Vec3d(below.getX() + 0.5, below.getY() + 1.0, below.getZ() + 0.5);
            BlockHitResult bottomSlabHit = new BlockHitResult(topCenter, Direction.UP, below, false);
            if (eye.squaredDistanceTo(topCenter) <= reachSquared
                    && BlockPlacement.facesEye(bottomSlabHit, eye)
                    && BlockPlacement.canSeeHit(eye, bottomSlabHit)) {
                return bottomSlabHit;
            }
        }
        PlacementTarget placement = BlockPlacement.find(candidate, true);
        return placement != null && eye.squaredDistanceTo(placement.hitResult().getPos()) <= reachSquared
                ? placement.hitResult() : null;
    }

    private static int chooseSingleCrossbowSlot() {
        int held = DeferredActionQueue.selectedSlot();
        if (held >= 0 && held < 9 && InventoryItems.isChargedCrossbow(client.player.getInventory().getStack(held))) {
            return held;
        }
        return InventoryItems.findStackInHotbar(InventoryItems::isChargedCrossbow);
    }

    private int resolveLockedCrossbowSlot() {
        if (lockedCrossbowSlot >= 0 && lockedCrossbowSlot < 9) {
            return lockedCrossbowSlot;
        }
        return -1;
    }

    private boolean hasLockedCrossbow(int slot) {
        if (slot >= 0 && slot < 9) {
            return InventoryItems.isChargedCrossbow(client.player.getInventory().getStack(slot));
        }
        return InventoryItems.isChargedCrossbow(client.player.getOffHandStack());
    }

    private static boolean hasCrossbow(int slot) {
        return slot != -1 || InventoryItems.isChargedCrossbow(client.player.getOffHandStack());
    }

    private int nextStep() {
        if (shotFired) {
            if (safe.getValue() && !slabPlaced && safeSlabPosition != null
                    && BlockPlacement.isReplaceable(safeSlabPosition)
                    && !BlockPlacement.intersectsPlayer(safeSlabPosition)) {
                return SLAB;
            }
            return -1;
        }
        boolean railPresent = AbstractRailBlock.isRail(client.world, targetBlockPosition);
        if (place.isSelected(placeOption4) && !railPresent) return RAIL;
        boolean cartPresent = findCart(targetBlockPosition) != null;
        if (place.isSelected(placeOption3) && railPresent && !cartPresent) return CART;
        if (place.isSelected(placeOption2) && !client.world.getBlockState(supportBlockPosition).isIn(BlockTags.FIRE)) return FIRE;
        return place.isSelected(placeOption) && cartPresent ? SHOOT : -1;
    }

    private BlockHitResult findStepHit() {
        Vec3d eye = client.player.getCameraPosVec(1.0F);
        double reachSquared = MathHelper.square(client.player.getBlockInteractionRange());
        if (placementStep == SLAB) {
            return findSlabHit(safeSlabPosition, eye, reachSquared);
        }
        if (placementStep == FIRE) {
            if (!AbstractFireBlock.canPlaceAt(client.world, supportBlockPosition, client.player.getHorizontalFacing())) return null;
            PlacementTarget placement = BlockPlacement.find(supportBlockPosition, true);
            return placement != null && eye.squaredDistanceTo(placement.hitResult().getPos()) <= reachSquared
                    ? placement.hitResult() : null;
        }
        if (placementStep == RAIL) {
            if (!BlockPlacement.isReplaceable(targetBlockPosition)
                    || !Block.hasTopRim(client.world, targetBlockPosition.down())) return null;
            BlockHitResult hit = BlockAimRotation.findHit(targetBlockPosition, cachedTargetPosition);
            return hit != null && eye.squaredDistanceTo(hit.getPos()) <= reachSquared ? hit : null;
        }
        BlockHitResult hit = new BlockHitResult(BlockAimRotation.insetBottomPoint(targetBlockPosition, cachedTargetPosition), Direction.UP, targetBlockPosition, false);
        return eye.squaredDistanceTo(hit.getPos()) <= reachSquared ? hit : null;
    }

    private boolean canShootThroughFire() {
        Vec3d start = client.player.getCameraPosVec(1.0F).subtract(0.0, 0.1, 0.0);
        Rotation rotation = rotations().effectiveRotation();
        Vec3d direction = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
        Vec3d end = start.add(direction.multiply(client.player.getBlockInteractionRange() + 2.0));
        if (place.isSelected(placeOption2)) {
            Box fireBounds = new Box(supportBlockPosition).expand(FIRE_MARGIN);
            if (!fireBounds.contains(start) && fireBounds.raycast(start, end).isEmpty()) return false;
        }
        TntMinecartEntity cart = findCart(targetBlockPosition);
        return cart != null && cart.getBoundingBox().contract(CART_INSET).raycast(start, end).isPresent();
    }

    private TntMinecartEntity findCart(BlockPos position) {
        List<TntMinecartEntity> carts = client.world.getNonSpectatingEntities(TntMinecartEntity.class, new Box(position));
        return carts.isEmpty() ? null : carts.getFirst();
    }

    private void finishSequence() {
        // Keep the fired crossbow selected for at least 1 tick so the queued click fires only that slot
        // and never switches back onto a second loaded crossbow before doItemUse executes.
        int delay = Math.max(1, switchBackDelay.getValue());
        DeferredActionQueue.select(this, DeferredActionQueue.selectedSlot(), delay);
        clearPlacementState();
        releaseRotation();
    }

    private void abortSequence() {
        clearPlacementState();
        DeferredActionQueue.finish(this, true);
        releaseRotation();
    }

    private void clearPlacementState() {
        targetBlockPosition = null;
        supportBlockPosition = null;
        safeSlabPosition = null;
        cachedTargetPosition = null;
        targetHitPosition = null;
        pendingBlockHit = null;
        selectedPlacementSlot = -1;
        lockedCrossbowSlot = -1;
        lastTrackedStep = -1;
        stepEnteredTick = -1L;
        interactionSubmitted = false;
        shotFired = false;
        slabPlaced = false;
    }

    private boolean isStepDelayElapsed(int step) {
        int delay = switch (step) {
            case RAIL -> railDelay.getValue();
            case CART -> cartDelay.getValue();
            case FIRE -> fireDelay.getValue();
            case SHOOT -> shootDelay.getValue();
            case SLAB -> slabDelay.getValue();
            default -> 0;
        };
        if (delay <= 0 || stepEnteredTick < 0L) {
            return true;
        }
        return (long) client.player.age - stepEnteredTick >= delay;
    }

    private void releaseRotation() {
        if (silentRotations.getValue()) rotations().beginReturn(this);
        else rotations().cancel(this);
    }

    private void finishDisableAfterReturn() {
        if (rotations().beginReturn(this)) return;
        waitingForRotationRelease = false;
        super.onDisable();
    }
}
