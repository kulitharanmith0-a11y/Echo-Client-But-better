package com.blamejared.clumps.client.utils.placement;

import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.event.interaction.ItemUseEvent;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.handlers.action.ActionResultInfo;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.HotbarSlotsSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.settings.Setting;
import com.blamejared.clumps.client.utils.MathRng;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AnchorController implements MinecraftAccess {
    private static final int PLACEMENT_CONFIRMATION_TICKS = 20;
    private static final int MAX_ANCHOR_CHARGES = 4;
    private static final int EXPLOSION_EFFECT_WINDOW_TICKS = 20;
    private static final int DETONATION_RETRY_TICKS = 20;
    private static final List<Item> DEFAULT_WHITELIST = List.of(
            Items.RESPAWN_ANCHOR, Items.GLOWSTONE, Items.WOODEN_SWORD, Items.STONE_SWORD,
            Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);

    private final Predicate<Object> visibleWhen;
    private Module owner;
    public final BooleanSetting autoCharge;
    public final IntSetting chargeChance;
    public final FloatRangeSetting charges;
    public final FloatRangeSetting glowstoneDelay;
    public final IntSetting glowstoneChance;
    public final BooleanSetting autoExplode;
    public final IntSetting explodeSlot;
    public final FloatRangeSetting explodeDelay;
    public final IntSetting explodeChance;
    public final BooleanSetting safeAnchor;
    public final FloatRangeSetting safeBlockDelay;
    public final ItemListSetting itemWhitelist;
    public final HotbarSlotsSetting hotbarWhitelist;
    public final FloatRangeSetting switchDelay;
    public final IntSetting switchChance;

    private int switchTicks;
    private int glowstoneTicks;
    private int explodeTicks;
    private int safetyTicks;
    private boolean interacting;
    private boolean restoreUseKey;
    private int selectedAutomationSlot;
    private BlockPos pendingSafetyAnchor;
    private BlockPos safetyAnchor;
    private boolean safetyBlockPlaced;
    private BlockPos recentExplodedAnchor;
    private int recentDetonationTick;
    private BlockPos chargeTarget;
    private int targetCharges;
    private volatile BlockPos pendingPlacement;
    private volatile BlockPos rejectedPlacement;
    private int placementTick;
    private Vec3d lastExplosionPosition;
    private int lastExplosionTick;
    private int lastInteractTick;

    public AnchorController(Predicate<Object> visibleWhen) {
        autoCharge = new BooleanSetting("Auto Charge", true);
        chargeChance = new IntSetting("Charge Chance", 100, 0, 100, "%");
        charges = new FloatRangeSetting("Charges", 1.0F, 1.0F, 1.0F, 4.0F, 1.0F, " charges");
        glowstoneDelay = new FloatRangeSetting("Glowstone Delay", 1.0F, 1.0F, 0.0F, 20.0F, 1.0F, " ticks");
        glowstoneChance = new IntSetting("Glowstone Chance", 100, 0, 100, "%");
        autoExplode = new BooleanSetting("Auto Explode", true);
        explodeSlot = new IntSetting("Explode Slot", 1, 1, 9);
        explodeDelay = new FloatRangeSetting("Explode Delay", 1.0F, 1.0F, 0.0F, 20.0F, 1.0F, " ticks");
        explodeChance = new IntSetting("Explode Chance", 100, 0, 100, "%");
        safeAnchor = new BooleanSetting("Safe Anchor", false);
        safeBlockDelay = new FloatRangeSetting("Safe Block Delay", 1.0F, 1.0F, 0.0F, 20.0F, 1.0F, " ticks");
        itemWhitelist = new ItemListSetting("Item Whitelist");
        hotbarWhitelist = new HotbarSlotsSetting("Hotbar Whitelist");
        switchDelay = new FloatRangeSetting("Switch Delay", 1.0F, 1.0F, 0.0F, 20.0F, 1.0F, " ticks");
        switchChance = new IntSetting("Switch Chance", 100, 0, 100, "%");
        selectedAutomationSlot = -1;
        lastInteractTick = -1;
        this.visibleWhen = visibleWhen;
        for (Item item : DEFAULT_WHITELIST) {
            itemWhitelist.setEntrySelected(item, true);
        }
        autoCharge.setVisibilityPredicate(this.visibleWhen);
        chargeChance.setVisibilityPredicate(this::showChargeChance);
        charges.setVisibilityPredicate(this::showCharges);
        glowstoneDelay.setVisibilityPredicate(this::showGlowstoneDelay);
        glowstoneChance.setVisibilityPredicate(this::showGlowstoneChance);
        autoExplode.setVisibilityPredicate(this.visibleWhen);
        explodeSlot.setVisibilityPredicate(this.visibleWhen);
        explodeDelay.setVisibilityPredicate(this::showExplodeDelay);
        explodeChance.setVisibilityPredicate(this::showExplodeChance);
        safeAnchor.setVisibilityPredicate(this.visibleWhen);
        safeBlockDelay.setVisibilityPredicate(this::showSafeBlockDelay);
        switchDelay.setVisibilityPredicate(this.visibleWhen);
        switchChance.setVisibilityPredicate(this.visibleWhen);
        itemWhitelist.setVisibilityPredicate(this.visibleWhen);
        hotbarWhitelist.setVisibilityPredicate(this.visibleWhen);
    }

    public void setOwner(Module owner) {
        this.owner = owner;
    }

    public List<Setting> getSettings() {
        return List.of(
                autoCharge, chargeChance, charges, glowstoneDelay, glowstoneChance,
                autoExplode, explodeSlot, explodeDelay, explodeChance,
                safeAnchor, safeBlockDelay, switchDelay, switchChance,
                itemWhitelist, hotbarWhitelist);
    }

    public void reset() {
        switchTicks = 0;
        glowstoneTicks = 0;
        explodeTicks = 0;
        safetyTicks = 0;
        interacting = false;
        selectedAutomationSlot = -1;
        chargeTarget = null;
        pendingPlacement = null;
        rejectedPlacement = null;
        lastExplosionPosition = null;
        lastInteractTick = -1;
        resetSafety();
    }

    public boolean onItemUse(ItemUseEvent.Pre event) {
        if (event.getPlayer() != CLIENT.player || event.getHand() != Hand.MAIN_HAND) {
            return false;
        }
        if (interacting || !isAllowedHeldItem()) {
            return false;
        }
        BlockHitResult hit = event.getBlockHit();
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (safetyAnchor != null && !hasUnchargedSafetyAnchor()) {
            resetSafety();
        }
        if (hasUnchargedSafetyAnchor() && !PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            event.cancel();
            return true;
        }
        if (event.getStack().isOf(Items.RESPAWN_ANCHOR)
                && !PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            resetDelays();
            pendingPlacement = placementPosition(hit).toImmutable();
            rejectedPlacement = null;
            placementTick = CLIENT.player.age;
            pendingSafetyAnchor = safeAnchor.getValue() ? placementPosition(hit) : null;
            safetyAnchor = null;
            safetyBlockPlaced = false;
            return false;
        }
        if (!PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            return false;
        }
        boolean needsCharge = needsMoreCharges(hit.getBlockPos());
        if (needsCharge && !autoCharge.getValue()) {
            return false;
        }
        if (!needsCharge && !autoExplode.getValue()) {
            return false;
        }
        event.cancel();
        return true;
    }

    public void onItemUseComplete(ItemUseEvent.Post event) {
        if (event.getPlayer() != CLIENT.player || event.getHand() != Hand.MAIN_HAND || pendingSafetyAnchor == null) {
            return;
        }
        safetyAnchor = pendingSafetyAnchor;
        if (!safeAnchor.getValue() || !PlacementWorldQueries.isUnchargedRespawnAnchor(safetyAnchor)) {
            safetyAnchor = null;
        }
        pendingSafetyAnchor = null;
    }

    public void onBlockUpdate(BlockUpdateS2CPacket packet) {
        BlockPos position = packet.getPos();
        if (!position.equals(pendingPlacement) && !position.equals(rejectedPlacement)) {
            return;
        }
        boolean anchorPresent = packet.getState().isOf(Blocks.RESPAWN_ANCHOR);
        CLIENT.execute(() -> applyBlockUpdate(position, anchorPresent));
    }

    public void onExplosion(Vec3d position) {
        CLIENT.execute(() -> rememberExplosion(position));
    }

    private boolean shouldDelayCharging(BlockPos position) {
        if (CLIENT.player.age - placementTick > PLACEMENT_CONFIRMATION_TICKS) {
            pendingPlacement = null;
            rejectedPlacement = null;
            return false;
        }
        if (position.equals(rejectedPlacement)) {
            return true;
        }
        return position.equals(pendingPlacement)
                && (isNearRecentExplosion(position) || wouldIntersectMovingEntity(position));
    }

    private boolean isNearRecentExplosion(BlockPos position) {
        return lastExplosionPosition != null
                && CLIENT.player.age - lastExplosionTick <= EXPLOSION_EFFECT_WINDOW_TICKS
                && lastExplosionPosition.squaredDistanceTo(Vec3d.ofCenter(position)) <= 36.0;
    }

    static boolean wouldIntersectMovingEntity(BlockPos position) {
        Box placement = new Box(position);
        for (Entity entity : CLIENT.world.getOtherEntities(CLIENT.player, placement.expand(3.0))) {
            if (!entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            Vec3d displacement = entity.getEntityPos().subtract(entity.lastX, entity.lastY, entity.lastZ);
            if (entity.getBoundingBox().expand(1.0 + displacement.length() * 5.0).intersects(placement)) {
                return true;
            }
        }
        return false;
    }

    private void resetDelays() {
        switchTicks = 0;
        glowstoneTicks = 0;
        explodeTicks = 0;
        safetyTicks = 0;
    }

    public void onInputPre() {
        if (MinecraftRenderState.getScreen() != null || CLIENT.player == null || CLIENT.world == null) {
            return;
        }
        if (recentExplodedAnchor != null && !PlacementWorldQueries.isBlock(recentExplodedAnchor, Blocks.RESPAWN_ANCHOR)) {
            recentExplodedAnchor = null;
        }
        if (!KeybindingCache.isPressed(CLIENT.options.useKey)) {
            selectedAutomationSlot = -1;
            resetSafety();
        } else if (safetyAnchor != null && !hasUnchargedSafetyAnchor()) {
            resetSafety();
        }
        if (hasUnchargedSafetyAnchor() && !safetyBlockPlaced
                && CLIENT.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && !PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            if (safetyTicks++ >= sampleDelay(safeBlockDelay)) {
                placeSafetyBlock(hit);
            }
            return;
        }
        switchTicks++;
        glowstoneTicks++;
        explodeTicks++;
        if (!(CLIENT.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (!KeybindingCache.isPressed(CLIENT.options.useKey) || !isAllowedHeldItem()) {
            return;
        }
        if (!PlacementWorldQueries.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            if (isHoldingSelectedGlowstone()) {
                suppressUseKey();
            }
            return;
        }
        if (hit.getBlockPos().equals(recentExplodedAnchor) && CLIENT.player.age - recentDetonationTick <= DETONATION_RETRY_TICKS) {
            suppressUseKey();
            return;
        }
        suppressUseKey();
        if (needsMoreCharges(hit.getBlockPos())) {
            chargeAnchor(hit);
        } else {
            explodeAnchor(hit);
        }
    }

    private void chargeAnchor(BlockHitResult hit) {
        if (!passesChance(chargeChance.getValue())) {
            return;
        }
        if (switchTicks < sampleDelay(switchDelay)) {
            return;
        }
        boolean swappedThisTick = false;
        if (!CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            if (!passesChance(switchChance.getValue())) {
                return;
            }
            int glowSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.GLOWSTONE));
            if (glowSlot == -1 || !selectSlotSafely(glowSlot)) {
                return;
            }
            switchTicks = 0;
            selectedAutomationSlot = glowSlot;
            swappedThisTick = true;
        }
        if (!autoCharge.getValue() || !CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            return;
        }
        int requiredGlowDelay = sampleDelay(glowstoneDelay);
        if (swappedThisTick && requiredGlowDelay > 0) {
            glowstoneTicks = 0;
            return;
        }
        if (glowstoneTicks < requiredGlowDelay || shouldDelayCharging(hit.getBlockPos())) {
            return;
        }
        if (!passesChance(glowstoneChance.getValue())) {
            return;
        }
        if (interact(hit).isAccepted()) {
            glowstoneTicks = 0;
            explodeTicks = 0;
            switchTicks = 0;
        }
    }

    private void explodeAnchor(BlockHitResult hit) {
        int slot = explodeSlot.getValue() - 1;
        boolean swappedThisTick = false;
        if (CLIENT.player.getInventory().getSelectedSlot() != slot) {
            if (switchTicks < sampleDelay(switchDelay) || !passesChance(switchChance.getValue())) {
                return;
            }
            if (!selectSlotSafely(slot)) {
                return;
            }
            switchTicks = 0;
            selectedAutomationSlot = slot;
            swappedThisTick = true;
        }
        if (CLIENT.player.getInventory().getSelectedSlot() != slot || !autoExplode.getValue()) {
            return;
        }
        if (CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE)
                && CLIENT.world.getBlockState(hit.getBlockPos()).get(RespawnAnchorBlock.CHARGES) < MAX_ANCHOR_CHARGES) {
            return;
        }
        int requiredExplodeDelay = sampleDelay(explodeDelay);
        if (swappedThisTick && requiredExplodeDelay > 0) {
            explodeTicks = 0;
            return;
        }
        if (explodeTicks < requiredExplodeDelay || !passesChance(explodeChance.getValue())) {
            return;
        }
        if (interact(hit).isAccepted()) {
            explodeTicks = 0;
            recentExplodedAnchor = hit.getBlockPos();
            recentDetonationTick = CLIENT.player.age;
        }
    }

    public void onInputPost() {
        if (!restoreUseKey) {
            return;
        }
        restoreUseKey = false;
        if (MinecraftRenderState.getScreen() == null && KeybindingCache.isPressed(CLIENT.options.useKey)) {
            CLIENT.options.useKey.setPressed(true);
        }
    }

    private void suppressUseKey() {
        restoreUseKey = true;
        CLIENT.options.useKey.setPressed(false);
    }

    private boolean selectSlotSafely(int slot) {
        if (CLIENT.player == null || slot < 0 || slot > 8) {
            return false;
        }
        if (CLIENT.player.getInventory().getSelectedSlot() == slot) {
            return true;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null) {
            ActionResultInfo info = coordinator.executeSlotSelect(owner, slot, true);
            return info.succeeded() && CLIENT.player.getInventory().getSelectedSlot() == slot;
        }
        InventoryItems.selectHotbarSlot(slot);
        return CLIENT.player.getInventory().getSelectedSlot() == slot;
    }

    private ActionResult interact(BlockHitResult hit) {
        if (CLIENT.player == null || lastInteractTick == CLIENT.player.age) {
            return ActionResult.PASS;
        }
        if (!CLIENT.player.canInteractWithBlockAt(hit.getBlockPos(), 0.0)) {
            return ActionResult.PASS;
        }
        interacting = true;
        suppressUseKey();
        try {
            ActionResult result = BlockPlacement.interactMainHand(owner, hit, true);
            if (result.isAccepted()) {
                lastInteractTick = CLIENT.player.age;
            }
            return result;
        } finally {
            interacting = false;
        }
    }

    private void placeSafetyBlock(BlockHitResult hit) {
        int slot = InventoryItems.findStackInHotbar(AnchorController::isSafetyBlock);
        if (slot == -1) {
            return;
        }
        if (!selectSlotSafely(slot)) {
            return;
        }
        selectedAutomationSlot = slot;
        if (!CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            return;
        }
        ActionResult result = interact(hit);
        safetyBlockPlaced = result.isAccepted();
    }

    public boolean isInteracting() {
        return interacting;
    }

    private boolean hasUnchargedSafetyAnchor() {
        return safetyAnchor != null && PlacementWorldQueries.isUnchargedRespawnAnchor(safetyAnchor);
    }

    private void resetSafety() {
        pendingSafetyAnchor = null;
        safetyAnchor = null;
        safetyBlockPlaced = false;
        safetyTicks = 0;
    }

    static BlockPos placementPosition(BlockHitResult hit) {
        return new ItemPlacementContext(CLIENT.player, Hand.MAIN_HAND, CLIENT.player.getMainHandStack(), hit).getBlockPos();
    }

    private boolean needsMoreCharges(BlockPos position) {
        int currentCharges = CLIENT.world.getBlockState(position).get(RespawnAnchorBlock.CHARGES);
        if (currentCharges == 0) {
            chargeTarget = null;
            return true;
        }
        if (!autoCharge.getValue()) {
            return false;
        }
        if (!position.equals(chargeTarget)) {
            chargeTarget = position.toImmutable();
            int minimum = Math.round(charges.getLower());
            int maximum = Math.round(charges.getUpper());
            targetCharges = Math.clamp((long) MathRng.randomInt(minimum, maximum + 1), 1, MAX_ANCHOR_CHARGES);
        }
        return currentCharges < targetCharges;
    }

    private boolean isHoldingSelectedGlowstone() {
        return selectedAutomationSlot != -1
                && CLIENT.player.getInventory().getSelectedSlot() == selectedAutomationSlot
                && CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE);
    }

    private boolean isAllowedHeldItem() {
        if (selectedAutomationSlot != -1 && CLIENT.player.getInventory().getSelectedSlot() == selectedAutomationSlot) {
            return true;
        }
        return itemWhitelist.acceptsHeldItem() && hotbarWhitelist.permitsCurrentSlot();
    }

    private static int sampleDelay(FloatRangeSetting setting) {
        return Math.max(0, Math.round(setting.sampleGaussian()));
    }

    private static boolean passesChance(int percent) {
        return percent >= 100 || MathRng.randomInt(1, 100) <= percent;
    }

    static boolean isSafetyBlock(ItemStack stack) {
        return stack.isOf(Items.GLOWSTONE);
    }

    private void rememberExplosion(Vec3d position) {
        if (CLIENT.player == null) {
            return;
        }
        lastExplosionPosition = position;
        lastExplosionTick = CLIENT.player.age;
    }

    private void applyBlockUpdate(BlockPos position, boolean anchorPresent) {
        if (!position.equals(pendingPlacement) && !position.equals(rejectedPlacement)) {
            return;
        }
        pendingPlacement = null;
        rejectedPlacement = anchorPresent ? null : position;
    }

    private boolean showSafeBlockDelay(Object context) { return visibleWhen.test(context) && safeAnchor.getValue(); }
    private boolean showExplodeChance(Object context) { return visibleWhen.test(context) && autoExplode.getValue(); }
    private boolean showExplodeDelay(Object context) { return visibleWhen.test(context) && autoExplode.getValue(); }
    private boolean showGlowstoneChance(Object context) { return visibleWhen.test(context) && autoCharge.getValue(); }
    private boolean showGlowstoneDelay(Object context) { return visibleWhen.test(context) && autoCharge.getValue(); }
    private boolean showCharges(Object context) { return visibleWhen.test(context) && autoCharge.getValue(); }
    private boolean showChargeChance(Object context) { return visibleWhen.test(context) && autoCharge.getValue(); }
}
