package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.MultiChoiceSetting;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.BlockRaycasts;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
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
import net.minecraft.world.RaycastContext;

public class MLG extends Module {
    private static final float MAX_ROTATION_STEP = 250.0F;
    private static final int SNEAK_LOOKAHEAD_TICKS = 5;
    private static final Item[] fallBreakingItems = {
            Items.WATER_BUCKET, Items.LADDER, Items.VINE, Items.SLIME_BLOCK, Items.HAY_BLOCK, Items.HONEY_BLOCK
    };
    private static final CharSequence[] itemsOptionSet = {
            CatalogText.literal("Water Bucket"), CatalogText.literal("Ladder"), CatalogText.literal("Vine"), CatalogText.literal("Slime Block"), CatalogText.literal("Hay Bale"), CatalogText.literal("Honey Block")
    };
    private static IntSetting fallDistanceThreshold;
    private static IntSetting distanceToGround;
    private static MultiChoiceSetting items;
    private static BooleanSetting autoAim;
    private static BooleanSetting silent;
    private BlockHitResult placementHit;
    private Item placementItem;
    private int placementSlot;
    private boolean disablePending;

    public MLG() {
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("MLG"), com.blamejared.clumps.client.init.CatalogText.literal("Uses hotbar items to break your fall"), com.blamejared.clumps.client.modules.ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        silent.setVisibilityPredicate(ignored -> autoAim.getValue());
        items.setSelected(CatalogText.literal("Water Bucket"), true);
        distanceToGround.setAllowOutOfRange(true);
        fallDistanceThreshold.setAllowOutOfRange(true);
    }

    @Override
    public void onEnable() {
        placementHit = null;
        disablePending = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        placementHit = null;
        DeferredActionQueue.finish(this, true);
        if (silent.getValue() && rotations().isOwnedBy(this)) {
            disablePending = true;
            return;
        }
        disablePending = false;
        rotations().cancel(this);
        super.onDisable();
    }

    @Subscribe
    private void afterMouseMovement(MouseMovementEvent.Post event) {
        if (disablePending) {
            finishPendingDisable();
            return;
        }
        if (isOutsideWorld() || currentScreen() != null) {
            placementHit = null;
            releasePlacement();
            return;
        }
        placementHit = findLandingHit();
        if (placementHit == null) {
            releasePlacement();
            return;
        }
        if (!autoAim.getValue()) {
            return;
        }
        if (!rotations().aim(this, RotationPriority.HIGH, adjustPlacementPoint(placementHit), RotationSettings.uniform(silent.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, AimTransition.EASED, MAX_ROTATION_STEP))) {
            releasePlacement();
        }
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (placementHit == null || isOutsideWorld() || event.slot != placementSlot) {
            return;
        }
        if (DeferredActionQueue.selectedSlot() != placementSlot
                && DeferredActionQueue.ensureSelected(this, placementSlot, -1, true)) {
            event.addClick();
        }
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (placementHit == null || isOutsideWorld() || currentScreen() != null) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(placementItem)) {
            return;
        }
        if (!BlockRaycasts.matchesHit(placementHit, true)) {
            return;
        }
        if (!client.player.isSneaking()) {
            return;
        }
        event.addClick();
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || placementHit == null) {
            return;
        }
        double fallingSpeed = Math.max(0.1, -client.player.getVelocity().y);
        if (client.player.getY() - placementHit.getPos().y <= fallingSpeed * SNEAK_LOOKAHEAD_TICKS) {
            event.sneak = true;
        }
    }

    private BlockHitResult findLandingHit() {
        if (client.player.isOnGround() || client.player.isTouchingWater() || client.player.getVelocity().y >= 0.0) {
            return null;
        }
        Box bounds = client.player.getBoundingBox().contract(0.001, 0.0, 0.001);
        Vec3d center = bounds.getCenter();
        double rayDepth = Math.max(distanceToGround.getValue(), bounds.minY - client.world.getBottomY());
        BlockHitResult highestHit = null;
        double[][] origins = {
                {center.x, center.z}, {bounds.minX, bounds.minZ}, {bounds.minX, bounds.maxZ}, {bounds.maxX, bounds.minZ}, {bounds.maxX, bounds.maxZ}
        };
        for (double[] origin : origins) {
            Vec3d from = new Vec3d(origin[0], bounds.minY, origin[1]);
            Vec3d to = from.subtract(0.0, rayDepth, 0.0);
            BlockHitResult hit = client.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, client.player));
            if (hit.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            if (highestHit == null || hit.getPos().y > highestHit.getPos().y) {
                highestHit = hit;
            }
        }
        if (highestHit == null) {
            return findSideSupportedHit();
        }
        if (client.player.fallDistance < fallDistanceThreshold.getValue()) {
            return null;
        }
        if (bounds.minY - highestHit.getPos().y > distanceToGround.getValue()) {
            return null;
        }
        if (alreadyBreaksFall(highestHit.getBlockPos())) {
            return null;
        }
        if (resolvePlacementItem(highestHit.getBlockPos())) {
            return highestHit;
        }
        return findSideSupportedHit();
    }

    private BlockHitResult findSideSupportedHit() {
        if (!resolvePlacementItem(null)) {
            return null;
        }
        for (int depth = 1; depth <= distanceToGround.getValue(); depth++) {
            BlockPos position = client.player.getBlockPos().down(depth);
            if (!client.world.getBlockState(position).isReplaceable()) {
                return null;
            }
            Direction support = findHorizontalSupport(position);
            if (support != null) {
                return new BlockHitResult(Vec3d.ofCenter(position).offset(support, 0.5), support.getOpposite(), position.offset(support), false);
            }
        }
        return null;
    }

    private boolean alreadyBreaksFall(BlockPos position) {
        BlockState state = client.world.getBlockState(position);
        return !state.getFluidState().isEmpty()
                || state.isOf(Blocks.SLIME_BLOCK)
                || state.isOf(Blocks.HAY_BLOCK)
                || state.isOf(Blocks.HONEY_BLOCK)
                || client.world.getBlockState(position.up()).isIn(BlockTags.CLIMBABLE);
    }

    private boolean resolvePlacementItem(BlockPos landingBlock) {
        for (int index = 0; index < fallBreakingItems.length; index++) {
            Item item = fallBreakingItems[index];
            boolean requiresSideSupport = item == Items.LADDER || item == Items.VINE;
            if (landingBlock == null) {
                if (!requiresSideSupport) {
                    continue;
                }
            } else if (requiresSideSupport && findHorizontalSupport(landingBlock.up()) == null) {
                continue;
            }
            if (!items.isSelected(itemsOptionSet[index])) {
                continue;
            }
            int slot = InventoryItems.findStackInHotbar(stack -> matchesItem(item, stack));
            if (slot == -1) {
                continue;
            }
            placementItem = item;
            placementSlot = slot;
            return true;
        }
        return false;
    }

    private Direction findHorizontalSupport(BlockPos position) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos neighbor = position.offset(direction);
            if (client.world.getBlockState(neighbor).isSideSolidFullSquare(client.world, neighbor, direction.getOpposite())) {
                return direction;
            }
        }
        return null;
    }

    private Vec3d adjustPlacementPoint(BlockHitResult hit) {
        Vec3d point = hit.getPos();
        if (hit.getSide() != Direction.UP) {
            return point;
        }
        Vec3d forward = client.player.getRotationVector(0.0F, client.player.getYaw());
        BlockPos block = hit.getBlockPos();
        double x = MathHelper.clamp(point.x + forward.x * 0.3, block.getX() + 0.05, block.getX() + 0.95);
        double z = MathHelper.clamp(point.z + forward.z * 0.3, block.getZ() + 0.05, block.getZ() + 0.95);
        return new Vec3d(x, point.y, z);
    }

    private void releasePlacement() {
        DeferredActionQueue.finish(this, true);
        if (silent.getValue()) {
            rotations().beginReturn(this);
        } else {
            rotations().cancel(this);
        }
    }

    private void finishPendingDisable() {
        if (rotations().beginReturn(this)) {
            return;
        }
        disablePending = false;
        super.onDisable();
    }

    private static boolean matchesItem(Item item, ItemStack stack) {
        return stack.isOf(item);
    }

    private static Screen currentScreen() {
        try {
            Class.forName(Screen.class.getName(), true, Screen.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        return MinecraftClient.getInstance().currentScreen;
    }

    
    public static void appendBootstrapSettings(MLG Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, MLG.fallDistanceThreshold);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, MLG.distanceToGround);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, MLG.items);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, MLG.autoAim);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, MLG.silent);
    }

    
    public static java.lang.CharSequence[] bootstrapItemsOptionSet() {
        return itemsOptionSet;
    }

    
    public static void assignBootstrapSettings(MLG Module, com.blamejared.clumps.client.settings.IntSetting preparedDistanceToGround, com.blamejared.clumps.client.settings.MultiChoiceSetting preparedItems, com.blamejared.clumps.client.settings.IntSetting preparedFallDistanceThreshold, com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim, com.blamejared.clumps.client.settings.BooleanSetting preparedSilent) {
        MLG.distanceToGround = preparedDistanceToGround;
        MLG.items = preparedItems;
        MLG.fallDistanceThreshold = preparedFallDistanceThreshold;
        MLG.autoAim = preparedAutoAim;
        MLG.silent = preparedSilent;
    }
}
