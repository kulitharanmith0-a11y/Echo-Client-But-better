package com.blamejared.clumps.client.utils.placement;

import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.event.interaction.ItemUseEvent;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.handlers.action.ActionResultInfo;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.settings.Setting;
import com.blamejared.clumps.client.utils.CooldownTimer;
import com.blamejared.clumps.client.utils.MathRng;
import com.blamejared.clumps.client.utils.interaction.EntityInteractions;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class SystemAnchorController implements MinecraftAccess {
    private static final int HURT_TIME_THRESHOLD = 0;
    private static final long AIRPLACE_DAMAGE_TIMEOUT_MS = 500L;
    private static final long ANCHOR_DOUBLE_PLACE_GUARD_MS = 350L;
    private static final int DETONATION_SPAM_GUARD_TICKS = 5;
    private static final double SAFETY_RADIUS = 6.0;
    private static final double SAFETY_RADIUS_SQ = SAFETY_RADIUS * SAFETY_RADIUS;
    private static final double SAFE_ANCHOR_KEEP_DIST_SQ = 6.5 * 6.5;

    private final Predicate<Object> visibleWhen;
    public final BooleanSetting whileUse;
    public final BooleanSetting friendSafe;
    public final BooleanSetting stopOnKill;
    public final IntSetting placeChance;
    public final IntSetting switchDelay;
    public final IntSetting switchChance;
    public final IntSetting glowstoneDelay;
    public final IntSetting glowstoneChance;
    public final IntSetting explodeDelay;
    public final IntSetting explodeChance;
    public final IntSetting explodeSlot;
    public final BooleanSetting antiDamageTick;
    public final BooleanSetting onlyOwn;
    public final BooleanSetting onlyCharge;
    public final BooleanSetting safeAnchor;
    public final FloatRangeSetting safeBlockDelay;
    public final BooleanSetting doubleAnchor;
    public final KeybindSetting airplaceKey;

    private final CooldownTimer switchTimer = new CooldownTimer();
    private final CooldownTimer glowstoneTimer = new CooldownTimer();
    private final CooldownTimer explodeTimer = new CooldownTimer();
    private final Set<BlockPos> ownedAnchors = new HashSet<>();
    private final Set<BlockPos> airplacedAnchors = new HashSet<>();
    private final Map<BlockPos, Long> airplacedTimestamps = new HashMap<>();
    private final Set<BlockPos> airplaceDamageWindowCleared = new HashSet<>();
    private BlockPos pendingPlacedAnchor;
    private BlockPos safetyAnchor;
    private boolean safetyBlockPlaced;
    private int safetyTicks;
    private int sampledSafetyDelayTicks = -1;
    private boolean anchorPlacedDuringHold;
    private boolean glowstoneLockDuringHold;
    private BlockPos lastPlacedAnchorPos;
    private long lastAnchorPlaceMs;
    private int lastAnchorPlaceTick = -1;
    private boolean restoreUseKey;
    private BlockPos lastExplodedPos;
    private int recentDetonationTick = -100;
    private int selectedAutomationSlot = -1;
    private boolean interacting;

    public SystemAnchorController(Predicate<Object> visibleWhen) {
        this.visibleWhen = visibleWhen;
        whileUse = new BooleanSetting(CatalogText.literal("While Use"), true);
        friendSafe = new BooleanSetting(CatalogText.literal("Friend Safe"), true);
        stopOnKill = new BooleanSetting(CatalogText.literal("Stop on Kill"), false);
        placeChance = new IntSetting(CatalogText.literal("Place Chance"), 100, 0, 100, CatalogText.literal("%"));
        switchDelay = new IntSetting(CatalogText.literal("Switch Delay"), 70, 0, 1000, CatalogText.literal(" ms"));
        switchChance = new IntSetting(CatalogText.literal("Switch Chance"), 100, 0, 100, CatalogText.literal("%"));
        glowstoneDelay = new IntSetting(CatalogText.literal("Glowstone Delay"), 30, 0, 1000, CatalogText.literal(" ms"));
        glowstoneChance = new IntSetting(CatalogText.literal("Glowstone Chance"), 100, 0, 100, CatalogText.literal("%"));
        explodeDelay = new IntSetting(CatalogText.literal("Explode Delay"), 70, 0, 1000, CatalogText.literal(" ms"));
        explodeChance = new IntSetting(CatalogText.literal("Explode Chance"), 100, 0, 100, CatalogText.literal("%"));
        explodeSlot = new IntSetting(CatalogText.literal("Explode Slot"), 9, 1, 9);
        antiDamageTick = new BooleanSetting(CatalogText.literal("Anti Damage Tick"), true);
        onlyOwn = new BooleanSetting(CatalogText.literal("Only Own"), false);
        onlyCharge = new BooleanSetting(CatalogText.literal("Only Charge"), false);
        safeAnchor = new BooleanSetting(CatalogText.literal("Safe Anchor"), false);
        safeBlockDelay = new FloatRangeSetting(CatalogText.literal("Safe Block Delay"), 0.0F, 1.0F, 0.0F, 20.0F, 1.0F, CatalogText.literal(" ticks"));
        doubleAnchor = new BooleanSetting(CatalogText.literal("Double Anchor"), false);
        airplaceKey = new KeybindSetting(CatalogText.literal("Airplace Key"), InputKey.NONE);

        whileUse.description(CatalogText.literal("Pause while holding right click with food or a shield"));
        friendSafe.description(CatalogText.literal("Skip anchoring when a friend is within 6 blocks of the anchor"));
        stopOnKill.description(CatalogText.literal("Skip anchoring while a dead player body is nearby"));
        antiDamageTick.description(CatalogText.literal("Wait for enemy damage immunity to clear before exploding airplaced anchors"));
        onlyOwn.description(CatalogText.literal("Only charge and explode anchors you placed"));
        onlyCharge.description(CatalogText.literal("Charge anchors without exploding them"));
        safeAnchor.description(CatalogText.literal("After placing an anchor, aim at a block between you and the anchor while holding right click to place a safety block"));
        safeBlockDelay.description(CatalogText.literal("Delay in ticks before placing the glowstone safety block"));
        doubleAnchor.description(CatalogText.literal("Allow exploding and airplacing a fresh anchor while holding Airplace Key"));
        airplaceKey.description(CatalogText.literal("Hold while anchoring a charged anchor with Double Anchor enabled to airplace"));

        whileUse.setVisibilityPredicate(this.visibleWhen);
        friendSafe.setVisibilityPredicate(this.visibleWhen);
        stopOnKill.setVisibilityPredicate(this.visibleWhen);
        placeChance.setVisibilityPredicate(this.visibleWhen);
        switchDelay.setVisibilityPredicate(this.visibleWhen);
        switchChance.setVisibilityPredicate(this.visibleWhen);
        glowstoneDelay.setVisibilityPredicate(this.visibleWhen);
        glowstoneChance.setVisibilityPredicate(this.visibleWhen);
        explodeDelay.setVisibilityPredicate(this.visibleWhen);
        explodeChance.setVisibilityPredicate(this.visibleWhen);
        explodeSlot.setVisibilityPredicate(this.visibleWhen);
        antiDamageTick.setVisibilityPredicate(this.visibleWhen);
        onlyOwn.setVisibilityPredicate(this.visibleWhen);
        onlyCharge.setVisibilityPredicate(this.visibleWhen);
        safeAnchor.setVisibilityPredicate(this.visibleWhen);
        safeBlockDelay.setVisibilityPredicate(ignored -> this.visibleWhen.test(ignored) && safeAnchor.getValue());
        doubleAnchor.setVisibilityPredicate(this.visibleWhen);
        airplaceKey.setVisibilityPredicate(ignored -> this.visibleWhen.test(ignored) && doubleAnchor.getValue());
    }

    public List<Setting> getSettings() {
        return List.of(
                whileUse, friendSafe, stopOnKill,
                safeAnchor, safeBlockDelay,
                placeChance, switchDelay, switchChance,
                glowstoneDelay, glowstoneChance,
                explodeDelay, explodeChance, explodeSlot,
                antiDamageTick, onlyOwn, onlyCharge,
                doubleAnchor, airplaceKey);
    }

    public boolean isInteracting() {
        return interacting;
    }

    public void reset() {
        switchTimer.reset();
        glowstoneTimer.reset();
        explodeTimer.reset();
        ownedAnchors.clear();
        airplacedAnchors.clear();
        airplacedTimestamps.clear();
        airplaceDamageWindowCleared.clear();
        resetSafety();
        anchorPlacedDuringHold = false;
        glowstoneLockDuringHold = false;
        lastPlacedAnchorPos = null;
        lastAnchorPlaceMs = 0L;
        lastAnchorPlaceTick = -1;
        restoreUseKey = false;
        lastExplodedPos = null;
        recentDetonationTick = -100;
        selectedAutomationSlot = -1;
        interacting = false;
    }

    public void onItemUse(ItemUseEvent.Pre event) {
        if (CLIENT.player == null || CLIENT.world == null || event.getPlayer() != CLIENT.player) {
            return;
        }
        if (interacting || event.getHand() != Hand.MAIN_HAND) {
            return;
        }
        BlockHitResult hitResult = event.getBlockHit();
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hitResult.getBlockPos();
        if (safetyAnchor != null && !hasUnchargedSafetyAnchor()) {
            resetSafety();
        }
        // Classic Safe Anchor: cancel any non-anchor vanilla use while our uncharged safety anchor is active
        if (hasUnchargedSafetyAnchor() && !PlacementWorldQueries.isBlock(pos, Blocks.RESPAWN_ANCHOR)) {
            event.cancel();
            return;
        }
        // Prevent vanilla right-click hold from placing extra glowstone after charging or placing safety block
        if (event.getStack().isOf(Items.GLOWSTONE) && !PlacementWorldQueries.isBlock(pos, Blocks.RESPAWN_ANCHOR)
                && (glowstoneLockDuringHold || isHoldingSelectedGlowstone())) {
            event.cancel();
            return;
        }
        // Prevent placing a stray second anchor on the same tick, during the same RMB hold,
        // right after an anchor detonation while spamming, or onto an uncharged anchor while sneaking.
        if (event.getStack().isOf(Items.RESPAWN_ANCHOR)) {
            boolean recentDetonation = recentDetonationTick >= 0
                    && CLIENT.player.age - recentDetonationTick <= DETONATION_SPAM_GUARD_TICKS;
            boolean recentPlacement = lastAnchorPlaceTick == CLIENT.player.age
                    || (anchorPlacedDuringHold
                    && (recentDetonation
                    || (lastPlacedAnchorPos != null && PlacementWorldQueries.isBlock(lastPlacedAnchorPos, Blocks.RESPAWN_ANCHOR))
                    || System.currentTimeMillis() - lastAnchorPlaceMs < ANCHOR_DOUBLE_PLACE_GUARD_MS));
            if (recentPlacement || recentDetonation || PlacementWorldQueries.isUnchargedRespawnAnchor(pos)) {
                event.cancel();
                return;
            }
            if (!PlacementWorldQueries.isBlock(pos, Blocks.RESPAWN_ANCHOR)) {
                BlockPos placedPos = placementPosition(hitResult).toImmutable();
                ownedAnchors.add(placedPos);
                airplacedAnchors.remove(placedPos);
                airplacedTimestamps.remove(placedPos);
                airplaceDamageWindowCleared.remove(placedPos);
                pendingPlacedAnchor = placedPos;
                safetyAnchor = null;
                safetyBlockPlaced = false;
                safetyTicks = 0;
                sampledSafetyDelayTicks = -1;
                return;
            }
        }
        if (PlacementWorldQueries.isChargedRespawnAnchor(pos)) {
            ownedAnchors.remove(pos);
            airplacedAnchors.remove(pos);
            airplacedTimestamps.remove(pos);
            airplaceDamageWindowCleared.remove(pos);
        }
    }

    public void onItemUseComplete(ItemUseEvent.Post event) {
        if (CLIENT.player == null || CLIENT.world == null || event.getPlayer() != CLIENT.player
                || event.getHand() != Hand.MAIN_HAND || pendingPlacedAnchor == null) {
            return;
        }
        BlockPos placedPos = pendingPlacedAnchor;
        pendingPlacedAnchor = null;
        if (PlacementWorldQueries.isBlock(placedPos, Blocks.RESPAWN_ANCHOR)) {
            anchorPlacedDuringHold = true;
            lastPlacedAnchorPos = placedPos;
            lastAnchorPlaceMs = System.currentTimeMillis();
            lastAnchorPlaceTick = CLIENT.player.age;
            if (safeAnchor.getValue() && PlacementWorldQueries.isUnchargedRespawnAnchor(placedPos)) {
                safetyAnchor = placedPos;
                safetyBlockPlaced = false;
                safetyTicks = 0;
                sampledSafetyDelayTicks = -1;
            }
        }
    }

    public void onInputPost() {
        if (!restoreUseKey) {
            return;
        }
        restoreUseKey = false;
        if (MinecraftRenderState.getScreen() == null && isUseKeyHeld()) {
            CLIENT.options.useKey.setPressed(true);
        }
    }

    public void onTick(Module owner) {
        if (CLIENT.player == null || CLIENT.world == null || MinecraftRenderState.getScreen() != null) {
            resetSafety();
            return;
        }
        if (airplaceKey.isListeningForKey()) {
            return;
        }
        if (safetyAnchor != null && (!hasUnchargedSafetyAnchor()
                || CLIENT.player.squaredDistanceTo(Vec3d.ofCenter(safetyAnchor)) > SAFE_ANCHOR_KEEP_DIST_SQ)) {
            resetSafety();
        }
        if (!isUseKeyHeld()) {
            anchorPlacedDuringHold = false;
            glowstoneLockDuringHold = false;
            selectedAutomationSlot = -1;
            resetSafety();
            return;
        }
        if (CLIENT.player.isUsingItem()) {
            return;
        }
        if (whileUse.getValue() && isHoldingFoodOrShield()) {
            return;
        }
        if (stopOnKill.getValue() && isDeadBodyNearby()) {
            return;
        }
        if (friendSafe.getValue() && isFriendNearCrosshair()) {
            return;
        }

        // Classic Safe Anchor (ported directly from AnchorController): after placing an uncharged anchor,
        // looking at any non-anchor block while holding RMB immediately switches to glowstone and places 1 safety block.
        if (hasUnchargedSafetyAnchor()
                && CLIENT.crosshairTarget instanceof BlockHitResult safetyHit
                && safetyHit.getType() == HitResult.Type.BLOCK
                && !PlacementWorldQueries.isBlock(safetyHit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
            suppressUseKey();
            if (!safetyBlockPlaced) {
                if (sampledSafetyDelayTicks < 0) {
                    sampledSafetyDelayTicks = sampleDelayTicks(safeBlockDelay);
                }
                if (safetyTicks++ >= sampledSafetyDelayTicks) {
                    placeSafetyBlock(owner, safetyHit);
                }
            }
            return;
        }
        safetyTicks = 0;
        sampledSafetyDelayTicks = -1;

        if (CLIENT.crosshairTarget instanceof EntityHitResult) {
            suppressUseKey();
            return;
        }
        if (!(CLIENT.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos targetPos = hit.getBlockPos().toImmutable();
        boolean insideDetonationGuard = recentDetonationTick >= 0
                && CLIENT.player.age - recentDetonationTick <= DETONATION_SPAM_GUARD_TICKS;
        if (lastExplodedPos != null) {
            boolean pointingAtExplodedBase = (targetPos.equals(lastExplodedPos.down()) && hit.getSide() == Direction.UP)
                    || targetPos.equals(lastExplodedPos);
            if (pointingAtExplodedBase
                    && (insideDetonationGuard
                    || PlacementWorldQueries.isBlock(lastExplodedPos, Blocks.RESPAWN_ANCHOR)
                    || airplacedAnchors.contains(lastExplodedPos))) {
                suppressUseKey();
                if (!PlacementWorldQueries.isBlock(targetPos, Blocks.RESPAWN_ANCHOR)) {
                    return;
                }
            }
        }

        if (!PlacementWorldQueries.isBlock(targetPos, Blocks.RESPAWN_ANCHOR)) {
            if ((glowstoneLockDuringHold || isHoldingSelectedGlowstone())
                    && CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                suppressUseKey();
            } else if ((anchorPlacedDuringHold || insideDetonationGuard)
                    && CLIENT.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
                suppressUseKey();
            }
            return;
        }
        if (onlyOwn.getValue()) {
            boolean isOwned = ownedAnchors.contains(targetPos);
            boolean isLastExploded = targetPos.equals(lastExplodedPos);
            if (!isOwned && !isLastExploded) {
                return;
            }
        }

        suppressUseKey();

        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null && (coordinator.isActionSent() || coordinator.isSlotPending())) {
            return;
        }

        if (PlacementWorldQueries.isUnchargedRespawnAnchor(targetPos)) {
            handleUnchargedAnchor(owner, hit);
            return;
        }

        if (PlacementWorldQueries.isChargedRespawnAnchor(targetPos)) {
            handleChargedAnchor(owner, hit, targetPos);
        }
    }

    private void placeSafetyBlock(Module owner, BlockHitResult hit) {
        if (safetyBlockPlaced) {
            return;
        }
        int slot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.GLOWSTONE));
        if (slot == -1) {
            return;
        }
        if (CLIENT.player.getInventory().getSelectedSlot() != slot) {
            InventoryItems.selectHotbarSlot(slot);
        }
        selectedAutomationSlot = slot;
        if (!CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            return;
        }
        suppressUseKey();
        if (interactBlock(owner, hit, true)) {
            safetyBlockPlaced = true;
            glowstoneLockDuringHold = true;
            safetyTicks = 0;
            sampledSafetyDelayTicks = -1;
            switchTimer.reset();
            glowstoneTimer.reset();
        }
    }

    private boolean isHoldingSelectedGlowstone() {
        return selectedAutomationSlot != -1
                && CLIENT.player != null
                && CLIENT.player.getInventory().getSelectedSlot() == selectedAutomationSlot
                && CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE);
    }

    private void handleUnchargedAnchor(Module owner, BlockHitResult hit) {
        if (!rollPercent(placeChance.getValue())) {
            return;
        }
        if (!CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            if (!switchTimer.hasElapsedMillis(switchDelay.getValue())) {
                return;
            }
            if (rollPercent(switchChance.getValue())) {
                int glowstoneSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.GLOWSTONE));
                if (glowstoneSlot == -1) {
                    return;
                }
                if (selectSlot(owner, glowstoneSlot)) {
                    selectedAutomationSlot = glowstoneSlot;
                    switchTimer.reset();
                }
            }
        }

        if (!CLIENT.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
            return;
        }
        if (!glowstoneTimer.hasElapsedMillis(glowstoneDelay.getValue())) {
            return;
        }
        if (rollPercent(glowstoneChance.getValue())) {
            if (interactBlock(owner, hit, true)) {
                glowstoneTimer.reset();
                glowstoneLockDuringHold = true;
                if (safetyAnchor != null && safetyAnchor.equals(hit.getBlockPos())) {
                    resetSafety();
                }
            }
        }
    }

    private void handleChargedAnchor(Module owner, BlockHitResult hit, BlockPos targetPos) {
        int configuredSlot = explodeSlot.getValue() - 1;
        boolean isAirplaced = airplacedAnchors.contains(targetPos) || isFloatingInAir(targetPos);
        boolean airplaceHeld = doubleAnchor.getValue()
                && airplaceKey.getKey() != InputKey.NONE
                && airplaceKey.getKey().isPressed();
        int targetSlot = configuredSlot;

        if (airplaceHeld && !isAirplaced) {
            int anchorSlot = InventoryItems.findStackInHotbar(stack -> stack.isOf(Items.RESPAWN_ANCHOR));
            if (anchorSlot != -1) {
                targetSlot = anchorSlot;
            }
        }

        if (CLIENT.player.getInventory().getSelectedSlot() != targetSlot) {
            if (!switchTimer.hasElapsedMillis(switchDelay.getValue())) {
                return;
            }
            if (rollPercent(switchChance.getValue()) && selectSlot(owner, targetSlot)) {
                switchTimer.reset();
            }
        }

        if (CLIENT.player.getInventory().getSelectedSlot() != targetSlot) {
            return;
        }
        if (shouldWaitForHurtTime(targetPos, isAirplaced)) {
            return;
        }
        if (!explodeTimer.hasElapsedMillis(explodeDelay.getValue())) {
            return;
        }
        if (!rollPercent(explodeChance.getValue())) {
            return;
        }

        explodeTimer.reset();
        if (onlyCharge.getValue()) {
            return;
        }

        boolean shouldAirplace = airplaceHeld && !isAirplaced && CLIENT.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR);
        if (shouldAirplace) {
            if (doAirplace(owner, hit)) {
                lastExplodedPos = targetPos;
                ownedAnchors.add(targetPos);
                airplacedAnchors.add(targetPos);
                airplacedTimestamps.put(targetPos, System.currentTimeMillis());
                airplaceDamageWindowCleared.remove(targetPos);
            }
            return;
        }

        if (interactBlock(owner, hit, true)) {
            lastExplodedPos = targetPos;
            recentDetonationTick = CLIENT.player.age;
            ownedAnchors.remove(targetPos);
            airplacedAnchors.remove(targetPos);
            airplacedTimestamps.remove(targetPos);
            airplaceDamageWindowCleared.remove(targetPos);
            if (targetPos.equals(lastPlacedAnchorPos)) {
                lastPlacedAnchorPos = null;
            }
        }
    }

    private boolean doAirplace(Module owner, BlockHitResult hit) {
        if (CLIENT.player == null || CLIENT.interactionManager == null || hit == null) {
            return false;
        }
        if (CLIENT.player.isUsingItem() || !CLIENT.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
            return false;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null && (coordinator.isSlotPending() || coordinator.isActionSent() || coordinator.isMovementSent())) {
            return false;
        }
        interacting = true;
        try {
            boolean first;
            boolean second;
            if (coordinator != null) {
                ActionResultInfo r1 = coordinator.executeBlockUse(owner, Hand.MAIN_HAND, hit, false);
                ActionResultInfo r2 = coordinator.executeBlockUse(owner, Hand.MAIN_HAND, hit, false);
                first = r1.succeeded();
                second = r2.succeeded();
            } else {
                first = CLIENT.interactionManager.interactBlock(CLIENT.player, Hand.MAIN_HAND, hit).isAccepted();
                second = CLIENT.interactionManager.interactBlock(CLIENT.player, Hand.MAIN_HAND, hit).isAccepted();
            }
            if (first || second) {
                CLIENT.player.swingHand(Hand.MAIN_HAND);
                return true;
            }
            return false;
        } finally {
            interacting = false;
        }
    }

    private void suppressUseKey() {
        restoreUseKey = true;
        if (CLIENT.options != null) {
            CLIENT.options.useKey.setPressed(false);
        }
    }

    private boolean isUseKeyHeld() {
        return (CLIENT.options != null && KeybindingCache.isPressed(CLIENT.options.useKey))
                || InputKey.MOUSE_RIGHT.isPressed();
    }

    private boolean hasUnchargedSafetyAnchor() {
        return safetyAnchor != null && PlacementWorldQueries.isUnchargedRespawnAnchor(safetyAnchor);
    }

    private void resetSafety() {
        pendingPlacedAnchor = null;
        safetyAnchor = null;
        safetyBlockPlaced = false;
        safetyTicks = 0;
        sampledSafetyDelayTicks = -1;
    }

    private static BlockPos placementPosition(BlockHitResult hit) {
        return new ItemPlacementContext(CLIENT.player, Hand.MAIN_HAND, CLIENT.player.getMainHandStack(), hit).getBlockPos();
    }

    private static int sampleDelayTicks(FloatRangeSetting setting) {
        return Math.max(0, Math.round(setting.sampleGaussian()));
    }

    private boolean selectSlot(Module owner, int slot) {
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null) {
            return coordinator.executeSlotSelect(owner, slot).succeeded();
        }
        return InventoryItems.selectHotbarSlot(owner, slot);
    }

    private boolean interactBlock(Module owner, BlockHitResult hit, boolean swing) {
        interacting = true;
        try {
            return BlockPlacement.interactMainHand(owner, hit, swing).isAccepted();
        } finally {
            interacting = false;
        }
    }

    private boolean isHoldingFoodOrShield() {
        ItemStack main = CLIENT.player.getMainHandStack();
        ItemStack off = CLIENT.player.getOffHandStack();
        return main.getComponents().contains(DataComponentTypes.FOOD)
                || main.getItem() instanceof ShieldItem
                || off.getComponents().contains(DataComponentTypes.FOOD)
                || off.getItem() instanceof ShieldItem;
    }

    private boolean isDeadBodyNearby() {
        for (PlayerEntity player : CLIENT.world.getPlayers()) {
            if (player == CLIENT.player) {
                continue;
            }
            if ((!player.isAlive() || player.getHealth() <= 0.0F || player.deathTime > 0)
                    && player.squaredDistanceTo(CLIENT.player) <= SAFETY_RADIUS_SQ) {
                return true;
            }
        }
        return false;
    }

    private boolean isFriendNearCrosshair() {
        if (CLIENT.crosshairTarget == null) {
            return false;
        }
        Vec3d hitPos = CLIENT.crosshairTarget.getPos();
        for (PlayerEntity player : CLIENT.world.getPlayers()) {
            if (player == CLIENT.player || !EntityInteractions.isFriend(player.getName().getString())) {
                continue;
            }
            if (player.getEntityPos().squaredDistanceTo(hitPos) < SAFETY_RADIUS_SQ) {
                return true;
            }
        }
        return false;
    }

    private boolean isFloatingInAir(BlockPos pos) {
        return CLIENT.world != null
                && CLIENT.world.getBlockState(pos.down()).isAir()
                && CLIENT.world.getBlockState(pos.north()).isAir()
                && CLIENT.world.getBlockState(pos.south()).isAir()
                && CLIENT.world.getBlockState(pos.east()).isAir()
                && CLIENT.world.getBlockState(pos.west()).isAir();
    }

    private PlayerEntity getTargetEnemy(BlockPos anchorPos) {
        Vec3d center = Vec3d.ofCenter(anchorPos);
        PlayerEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (PlayerEntity player : CLIENT.world.getPlayers()) {
            if (player == CLIENT.player || !player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            if (EntityInteractions.isFriend(player.getName().getString())) {
                continue;
            }
            double distSq = player.squaredDistanceTo(center);
            if (distSq <= SAFETY_RADIUS_SQ && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = player;
            }
        }
        return best;
    }

    private boolean shouldWaitForHurtTime(BlockPos targetPos, boolean isAirplaced) {
        if (!antiDamageTick.getValue() || !isAirplaced) {
            return false;
        }
        if (airplaceDamageWindowCleared.contains(targetPos)) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long airplaceTime = airplacedTimestamps.get(targetPos);
        if (airplaceTime == null) {
            airplacedTimestamps.put(targetPos, now);
            airplaceTime = now;
        }
        if (now - airplaceTime >= AIRPLACE_DAMAGE_TIMEOUT_MS) {
            airplaceDamageWindowCleared.add(targetPos);
            return false;
        }
        PlayerEntity target = getTargetEnemy(targetPos);
        if (target == null || target.hurtTime <= HURT_TIME_THRESHOLD) {
            airplaceDamageWindowCleared.add(targetPos);
            return false;
        }
        return true;
    }

    private static boolean rollPercent(int chance) {
        if (chance >= 100) {
            return true;
        }
        if (chance <= 0) {
            return false;
        }
        return MathRng.randomInt(1, 101) <= chance;
    }
}
