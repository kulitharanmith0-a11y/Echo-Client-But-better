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
import net.minecraft.entity.vehicle.TntMinecartEntity;
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
    private static final CharSequence placeOption2 = CatalogText.literal("Fire");
    private static final CharSequence placeOption4 = CatalogText.literal("Rail");
    private static final CharSequence placeOption3 = CatalogText.literal("Cart");
    private static final CharSequence placeOption = CatalogText.literal("Shoot");
    private static KeybindSetting activateKey;
    private static MultiChoiceSetting place;
    private static BooleanSetting autoAim;
    private static BooleanSetting silentRotations;
    private BlockPos targetBlockPosition;
    private BlockPos supportBlockPosition;
    private Vec3d cachedTargetPosition;
    private Vec3d targetHitPosition;
    private long activationTick;
    private BlockHitResult pendingBlockHit;
    private int placementStep;
    private int selectedPlacementSlot;
    private boolean interactionSubmitted;
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
    }

    
    @Override
    public void onEnable() {
        targetBlockPosition = null;
        supportBlockPosition = null;
        cachedTargetPosition = null;
        targetHitPosition = null;
        pendingBlockHit = null;
        selectedPlacementSlot = -1;
        interactionSubmitted = false;
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
        if (placementStep == SHOOT) {
            if (selectedPlacementSlot == -1) {
                if (!InventoryItems.isChargedCrossbow(client.player.getOffHandStack())) return;
            } else if (DeferredActionQueue.selectedSlot() != selectedPlacementSlot) {
                return;
            }
            if (!canShootThroughFire()) return;
            event.addClick();
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
        if ((long) client.player.age - activationTick > TIMEOUT_TICKS) {
            abortSequence();
            return;
        }
        if (!prepareStep()) return;
        if (autoAim.getValue() && !rotations().aim(this, RotationPriority.HIGH, placementStep == SHOOT ? targetHitPosition : pendingBlockHit.getPos(), RotationSettings.uniform(silentRotations.getValue()
                        ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, AimTransition.EASED, AIM_SPEED))) {
            abortSequence();
        }
    }

    private void beginSequence() {
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
        if (place.isSelected(placeOption) && !hasCrossbow(findStepSlot(SHOOT))) return;
        supportBlockPosition = null;
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
            abortSequence();
            return false;
        }
        if (placementStep != CART) interactionSubmitted = false;
        pendingBlockHit = null;
        selectedPlacementSlot = findStepSlot(placementStep);
        boolean awaitingCart = placementStep == CART && selectedPlacementSlot == -1 && interactionSubmitted;
        if (!awaitingCart && (placementStep == SHOOT
                ? !hasCrossbow(selectedPlacementSlot) : selectedPlacementSlot == -1)) {
            abortSequence();
            return false;
        }
        if (placementStep == SHOOT) return true;
        pendingBlockHit = findStepHit();
        return pendingBlockHit != null;
    }

    private static int findStepSlot(int step) {
        return switch (step) {
            case FIRE -> InventoryItems.findStackInHotbar(XBow::isIgnitionItem);
            case RAIL -> InventoryItems.findStackInHotbar(InventoryItems::isRail);
            case CART -> InventoryItems.findStackInHotbar(XBow::isTntCart);
            default -> InventoryItems.findStackInHotbar(InventoryItems::isChargedCrossbow);
        };
    }

    private static boolean isIgnitionItem(ItemStack stack) { return stack.isOf(Items.FLINT_AND_STEEL); }
    private static boolean isTntCart(ItemStack stack) { return stack.isOf(Items.TNT_MINECART); }

    private static boolean hasCrossbow(int slot) {
        return slot != -1 || InventoryItems.isChargedCrossbow(client.player.getOffHandStack());
    }

    private int nextStep() {
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
        DeferredActionQueue.select(this, DeferredActionQueue.selectedSlot(), 1);
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
        cachedTargetPosition = null;
        targetHitPosition = null;
        pendingBlockHit = null;
        selectedPlacementSlot = -1;
        interactionSubmitted = false;
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
