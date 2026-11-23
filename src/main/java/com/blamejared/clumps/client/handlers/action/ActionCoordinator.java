package com.blamejared.clumps.client.handlers.action;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.KeybindingEvent;
import com.blamejared.clumps.client.event.input.SwapHandsKeyEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.network.PacketSendEvent;
import com.blamejared.clumps.client.handlers.ClientHandler;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.input.InputSimulator;
import com.blamejared.clumps.client.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import com.blamejared.clumps.client.mixin.accessor.MinecraftClientAccessor;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.impl.internals.Prevent;
import com.blamejared.clumps.client.modules.impl.utility.AutoTotem;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.utils.inventory.InventoryActionScheduler;
import com.blamejared.clumps.client.utils.inventory.SlotAction;
import com.blamejared.clumps.client.utils.inventory.SlotPackets;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class ActionCoordinator extends ClientHandler {
    private int trackedTick = -1;

    // Tick-phase boundaries
    private boolean inTick;
    private boolean movementSentInTick;
    private boolean tickEnded;

    // Granular per-tick packet/action matrix
    private boolean actionSentInTick;
    private boolean attackSentInTick;
    private Module attackOwnerThisTick;

    private boolean blockActionSentInTick;
    private boolean blockPlaceMainSentInTick;
    private boolean blockPlaceOffSentInTick;
    private BlockHitResult lastPlaceMainHit;
    private BlockHitResult lastPlaceOffHit;

    private boolean entityInteractSentInTick;
    private boolean useItemMainSentInTick;
    private boolean useItemOffSentInTick;
    private Module itemUseOwnerThisTick;

    private boolean inventoryActionSentInTick;
    private Module inventoryClickOwnerThisTick;

    private boolean releasingSentInTick;
    private boolean diggingSentInTick;
    private boolean digStartStopSentInTick;
    private boolean droppingSentInTick;
    private boolean swappingSentInTick;
    private Module offhandSwapOwnerThisTick;

    private boolean slotChangedThisTick;
    private Module slotOwnerThisTick;

    private boolean pendingAttackSwing;
    private boolean pendingDigSwing;
    private boolean swingSentInTick;
    private boolean suppressedActionInTick;

    // Server-synced hotbar slot deferral queue
    private int pendingSlot = -1;
    private int lastSentSlot = -1;
    private boolean flushingPendingSlot;

    private boolean postAttackSwitchBlocked;
    private int lastSetbackTick = -100;

    // Input-cycle synthetic action claims
    private Module syntheticAttackClaimOwner;
    private boolean syntheticAttackPending;
    private Module syntheticUseClaimOwner;
    private boolean syntheticUsePending;
    private boolean syntheticOffhandSwapPending;

    // Re-entrant execution frame for nested Minecraft calls
    private int activeDepth;
    private ActionType activeType;
    private Module activeOwner;
    private boolean activeAutomated;
    private boolean attackObservedInFrame;
    private boolean offhandPacketSentInFrame;
    private ActionResult observedUseResult;

    private ActionResultInfo lastResult;

    public static ActionCoordinator get() {
        return ClumpsClient.handlerManager == null ? null : ClumpsClient.handlerManager.getActionCoordinator();
    }

    public ActionResultInfo lastResult() {
        return lastResult;
    }

    public Module syntheticAttackOwner() {
        return syntheticAttackClaimOwner;
    }

    public Module syntheticUseOwner() {
        return syntheticUseClaimOwner;
    }

    public boolean isInTick() {
        return inTick;
    }

    public boolean isMovementSent() {
        return movementSentInTick;
    }

    public boolean isTickEnded() {
        return tickEnded;
    }

    public boolean isActionSent() {
        return actionSentInTick;
    }

    public boolean isAttackSent() {
        return attackSentInTick;
    }

    public boolean isBlockActionSent() {
        return blockActionSentInTick;
    }

    public boolean isBlockPlaceSent() {
        return blockPlaceMainSentInTick || blockPlaceOffSentInTick;
    }

    public boolean isEntityInteractSent() {
        return entityInteractSentInTick;
    }

    public boolean isUseItemSent() {
        return useItemMainSentInTick || useItemOffSentInTick;
    }

    public boolean isDiggingSent() {
        return diggingSentInTick;
    }

    public boolean isDigStartStopSent() {
        return digStartStopSentInTick;
    }

    public boolean isReleasingSent() {
        return releasingSentInTick;
    }

    public boolean isInventoryActionSent() {
        return inventoryActionSentInTick;
    }

    public boolean isSlotPending() {
        return pendingSlot != -1;
    }

    public int getPendingSlot() {
        return pendingSlot;
    }

    public int getLastSentSlot() {
        return lastSentSlot;
    }

    public boolean isPostAttackSwitchBlocked() {
        return postAttackSwitchBlocked;
    }

    public boolean canSwapSlotNow() {
        return inTick
                && !actionSentInTick
                && !movementSentInTick
                && !tickEnded
                && !postAttackSwitchBlocked
                && MinecraftRenderState.getScreen() == null;
    }

    public boolean isInsideAction(ActionType type) {
        return activeDepth > 0 && activeType == type;
    }

    public boolean isInsideApprovedAction(ActionType type, Module owner) {
        if (activeDepth <= 0 || activeType != type) {
            return false;
        }
        return owner == null || activeOwner == null || activeOwner == owner;
    }

    public boolean isAutomatedActionActive() {
        return activeDepth > 0 && activeAutomated;
    }

    public boolean hasAttackedThisTick() {
        syncTick();
        return attackSentInTick;
    }

    public boolean hasUsedItemThisTick() {
        syncTick();
        return blockActionSentInTick || useItemMainSentInTick || useItemOffSentInTick;
    }

    public boolean hasModifiedInventoryThisTick() {
        syncTick();
        return inventoryActionSentInTick || swappingSentInTick;
    }

    public void onTickStart() {
        if (CLIENT.world == null) {
            reset();
            return;
        }
        inTick = true;
        movementSentInTick = false;
        tickEnded = false;
        if (CLIENT.player != null) {
            trackedTick = CLIENT.player.age;
        }
        clearTickState();
        clearPendingSynthetic();
        flushPendingSlot();
    }

    public void onTickEnd() {
        inTick = false;
        movementSentInTick = true;
        tickEnded = true;
        clearTickState();
        clearPendingSynthetic();
    }

    public void reset() {
        trackedTick = -1;
        inTick = false;
        movementSentInTick = false;
        tickEnded = false;
        clearTickState();
        pendingSlot = -1;
        lastSentSlot = -1;
        flushingPendingSlot = false;
        postAttackSwitchBlocked = false;
        lastSetbackTick = -100;
        clearPendingSynthetic();
        activeDepth = 0;
        activeType = null;
        activeOwner = null;
        activeAutomated = false;
        attackObservedInFrame = false;
        offhandPacketSentInFrame = false;
        observedUseResult = null;
        lastResult = null;
    }

    // -------------------------------------------------------------------------
    // Stage 1 + 2: Pre-Execution Validation (<Action allowed?>)
    // -------------------------------------------------------------------------

    private static final int OK = 0;
    private static final int DENY_WORLD = 1;
    private static final int DENY_PLAYER = 2;
    private static final int DENY_SCREEN = 3;
    private static final int DENY_TARGET = 4;
    private static final int DENY_ACTIVE_CONFLICT = 5;
    private static final int DENY_TRANSFER_ACTIVE = 6;
    private static final int DENY_TICK_CONFLICT = 7;
    private static final int DENY_TICK_WINDOW = 8;
    private static final int DENY_SLOT_PENDING = 9;
    private static final int DENY_SETBACK = 10;
    private static final int DENY_USING_ITEM = 11;
    private static final int DENY_BREAKING_BLOCK = 12;
    private static final int DENY_INVENTORY_TICK = 13;
    private static final int DENY_SLOT_CONFLICT = 14;
    private static final int DENY_HOTBAR_OWNER = 15;
    private static final int DENY_INVALID_ARG = 16;
    private static final int DENY_COOLDOWN = 17;
    private static final int DENY_DUPLICATE = 18;
    private static final int DENY_POST_ATTACK_SWITCH = 19;
    private static final int DENY_PROTECTED_SLOT = 20;
    private static final int DENY_MIDTICK = 21;
    private static final int DENY_EMPTY_SLOT = 22;
    private static final int REJECT_NOT_APPLIED = 30;

    public boolean canAttack(Module owner, Entity target, boolean automated) {
        return checkAttackCode(owner, target, automated) == OK;
    }

    public int checkAttackCode(Module owner, Entity target, boolean automated) {
        syncTick();
        if (!hasValidPlayerAndWorld()) {
            return DENY_WORLD;
        }
        if (!CLIENT.player.isAlive() || CLIENT.player.isSpectator()) {
            return DENY_PLAYER;
        }
        if (MinecraftRenderState.getScreen() != null) {
            return DENY_SCREEN;
        }
        if (target != null) {
            if (target == CLIENT.player || !target.isAlive() || target.isRemoved()
                    || target.isSpectator() || !target.isAttackable()) {
                return DENY_TARGET;
            }
        }
        if (activeDepth > 0 && activeType != ActionType.ATTACK) {
            return DENY_ACTIVE_CONFLICT;
        }
        if (InventoryActionScheduler.isTransferring()) {
            return DENY_TRANSFER_ACTIVE;
        }
        if (isAttackPacketConflict(target != null || isHoldingPiercingWeapon())) {
            return DENY_TICK_CONFLICT;
        }
        if (!automated) {
            return OK;
        }
        if (!inTick || movementSentInTick || tickEnded) {
            return DENY_TICK_WINDOW;
        }
        if (isSlotPending()) {
            return DENY_SLOT_PENDING;
        }
        if (CLIENT.player.age <= lastSetbackTick) {
            return DENY_SETBACK;
        }
        if (CLIENT.player.isUsingItem() && !(owner instanceof AutoTotem)) {
            return DENY_USING_ITEM;
        }
        if (CLIENT.interactionManager.isBreakingBlock()) {
            return DENY_BREAKING_BLOCK;
        }
        if (inventoryActionSentInTick || swappingSentInTick) {
            return DENY_INVENTORY_TICK;
        }
        if (slotChangedThisTick && slotOwnerThisTick != null && owner != null && slotOwnerThisTick != owner) {
            return DENY_SLOT_CONFLICT;
        }
        Module hotbarOwner = DeferredActionQueue.topOwner();
        if (hotbarOwner != null && owner != null && hotbarOwner != owner) {
            return DENY_HOTBAR_OWNER;
        }
        return OK;
    }

    public boolean canUseItem(Module owner, Hand hand, BlockHitResult blockHit, boolean automated) {
        return checkItemUseCode(owner, hand, blockHit, automated) == OK;
    }

    public int checkItemUseCode(Module owner, Hand hand, BlockHitResult blockHit, boolean automated) {
        syncTick();
        if (!hasValidPlayerAndWorld()) {
            return DENY_WORLD;
        }
        if (!CLIENT.player.isAlive() || CLIENT.player.isSpectator()) {
            return DENY_PLAYER;
        }
        if (MinecraftRenderState.getScreen() != null) {
            return DENY_SCREEN;
        }
        if (blockHit != null) {
            if (blockHit.getType() != HitResult.Type.BLOCK || blockHit.getBlockPos() == null) {
                return DENY_INVALID_ARG;
            }
        } else if (hand != null && automated) {
            ItemStack stack = CLIENT.player.getStackInHand(hand);
            if (stack.isEmpty()) {
                return DENY_EMPTY_SLOT;
            }
            if (CLIENT.player.getItemCooldownManager().isCoolingDown(stack)) {
                return DENY_COOLDOWN;
            }
        }
        if (activeDepth > 0 && activeType != ActionType.ITEM_USE) {
            return DENY_ACTIVE_CONFLICT;
        }
        if (InventoryActionScheduler.isTransferring()) {
            return DENY_TRANSFER_ACTIVE;
        }
        if (attackSentInTick && !entityInteractSentInTick) {
            return DENY_TICK_CONFLICT;
        }
        if (diggingSentInTick || releasingSentInTick) {
            return DENY_TICK_CONFLICT;
        }
        if (blockHit != null) {
            if (entityInteractSentInTick || inventoryActionSentInTick || useItemMainSentInTick || useItemOffSentInTick) {
                return DENY_TICK_CONFLICT;
            }
            Hand resolvedHand = hand == null ? Hand.MAIN_HAND : hand;
            if (isDuplicateBlockPlace(resolvedHand, blockHit)) {
                return DENY_DUPLICATE;
            }
        } else if (hand != null) {
            if ((hand == Hand.MAIN_HAND && useItemMainSentInTick) || (hand == Hand.OFF_HAND && useItemOffSentInTick)) {
                return DENY_DUPLICATE;
            }
        }
        if (!automated) {
            return OK;
        }
        if (!inTick || movementSentInTick || tickEnded) {
            return DENY_TICK_WINDOW;
        }
        if (isSlotPending()) {
            return DENY_SLOT_PENDING;
        }
        if (CLIENT.player.age <= lastSetbackTick) {
            return DENY_SETBACK;
        }
        if (CLIENT.interactionManager.isBreakingBlock()) {
            return DENY_BREAKING_BLOCK;
        }
        if (CLIENT.player.isUsingItem() && hand != null && CLIENT.player.getActiveHand() != hand) {
            return DENY_USING_ITEM;
        }
        if (inventoryActionSentInTick || swappingSentInTick) {
            return DENY_INVENTORY_TICK;
        }
        if (attackSentInTick && !sameOwner(owner, attackOwnerThisTick)) {
            return DENY_TICK_CONFLICT;
        }
        if ((blockActionSentInTick || useItemMainSentInTick || useItemOffSentInTick)
                && !sameOwner(owner, itemUseOwnerThisTick)) {
            return DENY_TICK_CONFLICT;
        }
        if (slotChangedThisTick && slotOwnerThisTick != null && owner != null && slotOwnerThisTick != owner) {
            return DENY_SLOT_CONFLICT;
        }
        Module hotbarOwner = DeferredActionQueue.topOwner();
        if (hotbarOwner != null && owner != null && hotbarOwner != owner) {
            return DENY_HOTBAR_OWNER;
        }
        return OK;
    }

    public boolean canSelectSlot(Module owner, int slot, boolean automated, boolean allowDeferredOwnerBypass) {
        return checkSlotSelectCode(owner, slot, automated, allowDeferredOwnerBypass) == OK;
    }

    public int checkSlotSelectCode(Module owner, int slot, boolean automated, boolean allowDeferredOwnerBypass) {
        syncTick();
        if (CLIENT.player == null || CLIENT.world == null) {
            return DENY_WORLD;
        }
        if (slot < 0 || slot > 8) {
            return DENY_INVALID_ARG;
        }
        int currentSlot = CLIENT.player.getInventory().getSelectedSlot();
        if (currentSlot == slot && !isSlotPending()) {
            return OK;
        }
        if (activeDepth > 0 && activeType != ActionType.INVENTORY && !sameOwner(owner, activeOwner)) {
            return DENY_ACTIVE_CONFLICT;
        }
        if (!automated) {
            return OK;
        }
        if (postAttackSwitchBlocked) {
            return DENY_POST_ATTACK_SWITCH;
        }
        if (InventoryActionScheduler.isTransferring()) {
            return DENY_TRANSFER_ACTIVE;
        }
        if (inventoryActionSentInTick && !sameOwner(owner, inventoryClickOwnerThisTick)) {
            return DENY_INVENTORY_TICK;
        }
        if (attackSentInTick && !sameOwner(owner, attackOwnerThisTick)) {
            return DENY_TICK_CONFLICT;
        }
        if ((blockActionSentInTick || useItemMainSentInTick || useItemOffSentInTick)
                && !sameOwner(owner, itemUseOwnerThisTick)) {
            return DENY_TICK_CONFLICT;
        }
        if (!allowDeferredOwnerBypass) {
            Module hotbarOwner = DeferredActionQueue.topOwner();
            if (hotbarOwner != null && hotbarOwner != owner) {
                return DENY_HOTBAR_OWNER;
            }
            if (slotChangedThisTick && slotOwnerThisTick != null && slotOwnerThisTick != owner) {
                return DENY_SLOT_CONFLICT;
            }
        }
        return OK;
    }

    public boolean canClickSlot(Module owner, int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, boolean automated) {
        return checkSlotClickCode(owner, syncId, slotId, button, actionType, player, automated) == OK;
    }

    public int checkSlotClickCode(Module owner, int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, boolean automated) {
        syncTick();
        if (!hasValidPlayerAndWorld() || player != CLIENT.player || actionType == null) {
            return DENY_WORLD;
        }
        if (!CLIENT.player.isAlive() || CLIENT.player.isSpectator()) {
            return DENY_PLAYER;
        }
        ScreenHandler handler = CLIENT.player.currentScreenHandler;
        if (handler == null || handler.syncId != syncId) {
            return DENY_INVALID_ARG;
        }
        if (slotId != -999 && (slotId < 0 || slotId >= handler.slots.size())) {
            return DENY_INVALID_ARG;
        }
        if (actionType == SlotActionType.SWAP && !((button >= 0 && button <= 8) || button == 40)) {
            return DENY_INVALID_ARG;
        }
        if ((actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE || actionType == SlotActionType.THROW)
                && button != 0 && button != 1) {
            return DENY_INVALID_ARG;
        }
        if (isBlockedByPreventOffhand(slotId, button, actionType)) {
            return DENY_PROTECTED_SLOT;
        }
        if (activeDepth > 0 && activeType != ActionType.INVENTORY) {
            return DENY_ACTIVE_CONFLICT;
        }
        if (inTick && movementSentInTick) {
            return DENY_MIDTICK;
        }
        if (inTick && (attackSentInTick || blockActionSentInTick || entityInteractSentInTick
                || diggingSentInTick || droppingSentInTick)) {
            return DENY_TICK_CONFLICT;
        }
        if (!automated) {
            return OK;
        }
        if (CLIENT.player.age <= lastSetbackTick) {
            return DENY_SETBACK;
        }
        if (CLIENT.player.isUsingItem() && !(owner instanceof AutoTotem)) {
            return DENY_USING_ITEM;
        }
        if (useItemMainSentInTick || useItemOffSentInTick) {
            return DENY_TICK_CONFLICT;
        }
        if (inventoryActionSentInTick && !sameOwner(owner, inventoryClickOwnerThisTick)) {
            return DENY_INVENTORY_TICK;
        }
        if (swappingSentInTick && !sameOwner(owner, offhandSwapOwnerThisTick)) {
            return DENY_INVENTORY_TICK;
        }
        if (slotId >= 0) {
            ItemStack clicked = handler.slots.get(slotId).getStack();
            if (actionType == SlotActionType.QUICK_MOVE && clicked.isEmpty()) {
                return DENY_EMPTY_SLOT;
            }
            if (actionType == SlotActionType.SWAP && clicked.isEmpty()) {
                ItemStack swapTarget = getSwapTargetStack(button);
                if (swapTarget.isEmpty()) {
                    return DENY_EMPTY_SLOT;
                }
            }
        }
        return OK;
    }

    public boolean canSwapOffhand(Module owner, boolean automated) {
        return checkOffhandSwapCode(owner, automated) == OK;
    }

    public int checkOffhandSwapCode(Module owner, boolean automated) {
        syncTick();
        if (!hasValidPlayerAndWorld() || CLIENT.getNetworkHandler() == null) {
            return DENY_WORLD;
        }
        if (!CLIENT.player.isAlive() || CLIENT.player.isSpectator()) {
            return DENY_PLAYER;
        }
        if (MinecraftRenderState.getScreen() != null) {
            return DENY_SCREEN;
        }
        if (activeDepth > 0 && activeType != ActionType.INVENTORY) {
            return DENY_ACTIVE_CONFLICT;
        }
        if (droppingSentInTick || inventoryActionSentInTick || attackSentInTick || releasingSentInTick
                || entityInteractSentInTick || blockPlaceMainSentInTick || blockPlaceOffSentInTick
                || useItemMainSentInTick || useItemOffSentInTick || diggingSentInTick || isSlotPending()) {
            return DENY_TICK_CONFLICT;
        }
        if (!automated) {
            return OK;
        }
        if (!inTick || movementSentInTick || tickEnded) {
            return DENY_TICK_WINDOW;
        }
        if (CLIENT.player.age <= lastSetbackTick) {
            return DENY_SETBACK;
        }
        if (CLIENT.player.isUsingItem() && !(owner instanceof AutoTotem)) {
            return DENY_USING_ITEM;
        }
        if (InventoryActionScheduler.isTransferring()) {
            return DENY_TRANSFER_ACTIVE;
        }
        if (swappingSentInTick && !sameOwner(owner, offhandSwapOwnerThisTick)) {
            return DENY_INVENTORY_TICK;
        }
        return OK;
    }

    // -------------------------------------------------------------------------
    // Stage 3 + 4: Execute via Minecraft -> Verify Action Result
    // -------------------------------------------------------------------------

    public ActionResultInfo executeEntityAttack(Module owner, Entity target, boolean swingHand) {
        int deny = checkAttackCode(owner, target, true);
        if (deny != OK) {
            return finish(ActionResultInfo.skipped(ActionType.ATTACK, deny));
        }
        if (target == null) {
            return finish(ActionResultInfo.skipped(ActionType.ATTACK, DENY_TARGET));
        }

        float preCooldown = CLIENT.player.getAttackCooldownProgress(0.0F);
        boolean preSwinging = CLIENT.player.handSwinging;
        int preHurtTime = target instanceof LivingEntity living ? living.hurtTime : -1;
        boolean preRemoved = target.isRemoved();

        beginFrame(ActionType.ATTACK, owner, true);
        try {
            CLIENT.interactionManager.attackEntity(CLIENT.player, target);
            if (swingHand && !CLIENT.player.isSpectator() && attackObservedInFrame) {
                CLIENT.player.swingHand(Hand.MAIN_HAND);
            }
        } finally {
            endFrame();
        }

        boolean succeeded = attackObservedInFrame
                || CLIENT.player.getAttackCooldownProgress(0.0F) < preCooldown
                || (!preSwinging && CLIENT.player.handSwinging)
                || (target.isRemoved() && !preRemoved)
                || (target instanceof LivingEntity living && living.hurtTime > preHurtTime);

        if (succeeded) {
            recordAttackSuccess(owner);
            InputSimulator simulator = InputSimulator.get();
            if (simulator != null) {
                simulator.simulateAttackTap(owner);
            }
            return finish(ActionResultInfo.succeeded(ActionType.ATTACK, ActionResult.SUCCESS));
        }
        return finish(ActionResultInfo.rejected(ActionType.ATTACK, ActionResult.PASS, REJECT_NOT_APPLIED));
    }

    public ActionResultInfo executeBlockUse(Module owner, Hand hand, BlockHitResult hit, boolean swingHand) {
        Hand resolvedHand = hand == null ? Hand.MAIN_HAND : hand;
        int deny = checkItemUseCode(owner, resolvedHand, hit, true);
        if (deny != OK) {
            return finish(ActionResultInfo.skipped(ActionType.ITEM_USE, deny));
        }
        if (hit == null) {
            return finish(ActionResultInfo.skipped(ActionType.ITEM_USE, DENY_INVALID_ARG));
        }

        ItemStack preStack = CLIENT.player.getStackInHand(resolvedHand).copy();
        BlockPos hitPos = hit.getBlockPos();
        BlockPos adjacentPos = hitPos.offset(hit.getSide());
        BlockState preHitState = CLIENT.world.getBlockState(hitPos);
        BlockState preAdjacentState = CLIENT.world.getBlockState(adjacentPos);
        boolean preUsing = CLIENT.player.isUsingItem();

        ActionResult rawResult;
        beginFrame(ActionType.ITEM_USE, owner, true);
        try {
            rawResult = CLIENT.interactionManager.interactBlock(CLIENT.player, resolvedHand, hit);
            if (rawResult.isAccepted() && swingHand && rawResult instanceof ActionResult.Success success) {
                ActionResult.SwingSource source = success.swingSource();
                if (source == ActionResult.SwingSource.CLIENT || source == ActionResult.SwingSource.SERVER) {
                    CLIENT.player.swingHand(resolvedHand);
                }
            }
        } finally {
            endFrame();
        }

        boolean stateChanged = !ItemStack.areEqual(preStack, CLIENT.player.getStackInHand(resolvedHand))
                || (!preUsing && CLIENT.player.isUsingItem())
                || !CLIENT.world.getBlockState(hitPos).equals(preHitState)
                || !CLIENT.world.getBlockState(adjacentPos).equals(preAdjacentState);

        if ((rawResult != null && rawResult.isAccepted()) || stateChanged) {
            recordItemUseSuccess(owner);
            InputSimulator simulator = InputSimulator.get();
            if (simulator != null) {
                simulator.simulateUseTap(owner);
            }
            return finish(ActionResultInfo.succeeded(ActionType.ITEM_USE, rawResult));
        }
        return finish(ActionResultInfo.rejected(ActionType.ITEM_USE, rawResult, REJECT_NOT_APPLIED));
    }

    public ActionResultInfo executeItemUse(Module owner, Hand hand) {
        Hand resolvedHand = hand == null ? Hand.MAIN_HAND : hand;
        int deny = checkItemUseCode(owner, resolvedHand, null, true);
        if (deny != OK) {
            return finish(ActionResultInfo.skipped(ActionType.ITEM_USE, deny));
        }

        ItemStack preStack = CLIENT.player.getStackInHand(resolvedHand).copy();
        boolean preUsing = CLIENT.player.isUsingItem();
        boolean preCooldown = CLIENT.player.getItemCooldownManager().isCoolingDown(preStack);

        ActionResult rawResult;
        beginFrame(ActionType.ITEM_USE, owner, true);
        try {
            rawResult = CLIENT.interactionManager.interactItem(CLIENT.player, resolvedHand);
        } finally {
            endFrame();
        }

        boolean stateChanged = !ItemStack.areEqual(preStack, CLIENT.player.getStackInHand(resolvedHand))
                || (!preUsing && CLIENT.player.isUsingItem())
                || (!preCooldown && !preStack.isEmpty() && CLIENT.player.getItemCooldownManager().isCoolingDown(preStack));

        if ((rawResult != null && rawResult.isAccepted()) || stateChanged) {
            recordItemUseSuccess(owner);
            InputSimulator simulator = InputSimulator.get();
            if (simulator != null) {
                simulator.simulateUseTap(owner);
            }
            return finish(ActionResultInfo.succeeded(ActionType.ITEM_USE, rawResult));
        }
        return finish(ActionResultInfo.rejected(ActionType.ITEM_USE, rawResult, REJECT_NOT_APPLIED));
    }

    public ActionResultInfo executeDoItemUse(Module owner) {
        HitResult crosshair = CLIENT.crosshairTarget;
        BlockHitResult blockHit = crosshair instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK ? bhr : null;
        int deny = checkItemUseCode(owner, null, blockHit, true);
        if (deny != OK) {
            return finish(ActionResultInfo.skipped(ActionType.ITEM_USE, deny));
        }

        ItemStack preMain = CLIENT.player.getMainHandStack().copy();
        ItemStack preOff = CLIENT.player.getOffHandStack().copy();
        boolean preUsing = CLIENT.player.isUsingItem();
        boolean preSwinging = CLIENT.player.handSwinging;
        boolean preMainCd = !preMain.isEmpty() && CLIENT.player.getItemCooldownManager().isCoolingDown(preMain);
        boolean preOffCd = !preOff.isEmpty() && CLIENT.player.getItemCooldownManager().isCoolingDown(preOff);
        BlockPos hitPos = blockHit == null ? null : blockHit.getBlockPos();
        BlockPos adjacentPos = blockHit == null ? null : hitPos.offset(blockHit.getSide());
        BlockState preHitState = hitPos == null ? null : CLIENT.world.getBlockState(hitPos);
        BlockState preAdjState = adjacentPos == null ? null : CLIENT.world.getBlockState(adjacentPos);

        beginFrame(ActionType.ITEM_USE, owner, true);
        try {
            ((MinecraftClientAccessor) CLIENT).invokeDoItemUse();
        } finally {
            endFrame();
        }

        ActionResult observed = observedUseResult;
        boolean stateChanged = !ItemStack.areEqual(preMain, CLIENT.player.getMainHandStack())
                || !ItemStack.areEqual(preOff, CLIENT.player.getOffHandStack())
                || (!preUsing && CLIENT.player.isUsingItem())
                || (!preSwinging && CLIENT.player.handSwinging)
                || (!preMainCd && !preMain.isEmpty() && CLIENT.player.getItemCooldownManager().isCoolingDown(preMain))
                || (!preOffCd && !preOff.isEmpty() && CLIENT.player.getItemCooldownManager().isCoolingDown(preOff))
                || (hitPos != null && !CLIENT.world.getBlockState(hitPos).equals(preHitState))
                || (adjacentPos != null && !CLIENT.world.getBlockState(adjacentPos).equals(preAdjState));

        if ((observed != null && observed.isAccepted()) || stateChanged) {
            recordItemUseSuccess(owner);
            InputSimulator simulator = InputSimulator.get();
            if (simulator != null) {
                simulator.simulateUseTap(owner);
            }
            return finish(ActionResultInfo.succeeded(ActionType.ITEM_USE, observed == null ? ActionResult.SUCCESS : observed));
        }
        return finish(ActionResultInfo.rejected(ActionType.ITEM_USE, observed, REJECT_NOT_APPLIED));
    }

    public ActionResultInfo executeSlotSelect(Module owner, int slot) {
        return executeSlotSelect(owner, slot, false);
    }

    public ActionResultInfo executeSlotSelect(Module owner, int slot, boolean allowDeferredOwnerBypass) {
        int deny = checkSlotSelectCode(owner, slot, true, allowDeferredOwnerBypass);
        if (deny != OK) {
            return finish(ActionResultInfo.skipped(ActionType.INVENTORY, deny));
        }
        int previousSlot = CLIENT.player.getInventory().getSelectedSlot();
        if (previousSlot == slot) {
            return finish(ActionResultInfo.succeeded(ActionType.INVENTORY, ActionResult.SUCCESS));
        }

        beginFrame(ActionType.INVENTORY, owner, true);
        try {
            CLIENT.player.getInventory().setSelectedSlot(slot);
        } finally {
            endFrame();
        }

        if (CLIENT.player.getInventory().getSelectedSlot() == slot) {
            recordSlotChangeSuccess(owner);
            InputSimulator simulator = InputSimulator.get();
            if (simulator != null) {
                simulator.simulateHotbarSelect(owner, previousSlot, slot);
            }
            return finish(ActionResultInfo.succeeded(ActionType.INVENTORY, ActionResult.SUCCESS));
        }
        return finish(ActionResultInfo.rejected(ActionType.INVENTORY, ActionResult.PASS, REJECT_NOT_APPLIED));
    }

    public ActionResultInfo executeSlotClick(Module owner, int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
        PlayerEntity targetPlayer = player == null ? CLIENT.player : player;
        int deny = checkSlotClickCode(owner, syncId, slotId, button, actionType, targetPlayer, true);
        if (deny != OK) {
            return finish(ActionResultInfo.skipped(ActionType.INVENTORY, deny));
        }

        ScreenHandler handler = targetPlayer.currentScreenHandler;
        ItemStack preCursor = handler.getCursorStack().copy();
        ItemStack preSlot = slotId >= 0 ? handler.slots.get(slotId).getStack().copy() : ItemStack.EMPTY;
        ItemStack preSwap = actionType == SlotActionType.SWAP ? getSwapTargetStack(button).copy() : ItemStack.EMPTY;
        int preRevision = handler.getRevision();

        beginFrame(ActionType.INVENTORY, owner, true);
        try {
            if (owner instanceof AutoTotem && targetPlayer.isUsingItem()) {
                CLIENT.interactionManager.stopUsingItem(targetPlayer);
            }
            CLIENT.interactionManager.clickSlot(syncId, slotId, button, actionType, targetPlayer);
        } finally {
            endFrame();
        }

        boolean changed = handler.getRevision() != preRevision
                || !ItemStack.areEqual(preCursor, handler.getCursorStack())
                || (slotId >= 0 && slotId < handler.slots.size() && !ItemStack.areEqual(preSlot, handler.slots.get(slotId).getStack()))
                || (actionType == SlotActionType.SWAP && !ItemStack.areEqual(preSwap, getSwapTargetStack(button)));

        if (changed) {
            recordInventoryClickSuccess(owner);
            InputSimulator simulator = InputSimulator.get();
            if (simulator != null) {
                simulator.simulateSlotClick(owner, slotId, button, actionType);
            }
            return finish(ActionResultInfo.succeeded(ActionType.INVENTORY, ActionResult.SUCCESS));
        }
        return finish(ActionResultInfo.rejected(ActionType.INVENTORY, ActionResult.PASS, REJECT_NOT_APPLIED));
    }

    public ActionResultInfo executeOffhandSwap(Module owner) {
        int deny = checkOffhandSwapCode(owner, true);
        if (deny != OK) {
            return finish(ActionResultInfo.skipped(ActionType.INVENTORY, deny));
        }

        ItemStack preMain = CLIENT.player.getMainHandStack().copy();
        ItemStack preOff = CLIENT.player.getOffHandStack().copy();
        beginFrame(ActionType.INVENTORY, owner, true);
        try {
            CLIENT.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        } finally {
            endFrame();
        }

        if (offhandPacketSentInFrame && (!preMain.isEmpty() || !preOff.isEmpty()
                || !ItemStack.areEqual(preMain, CLIENT.player.getMainHandStack()))) {
            recordOffhandSwapSuccess(owner);
            InputSimulator simulator = InputSimulator.get();
            if (simulator != null) {
                simulator.simulateOffhandSwap(owner);
            }
            return finish(ActionResultInfo.succeeded(ActionType.INVENTORY, ActionResult.SUCCESS));
        }
        return finish(ActionResultInfo.rejected(ActionType.INVENTORY, ActionResult.PASS, REJECT_NOT_APPLIED));
    }

    // -------------------------------------------------------------------------
    // Keybinding-driven Module Request Helpers
    // -------------------------------------------------------------------------

    public boolean requestAttackClick(Module owner, AttackKeyEvent event) {
        if (event == null || event.cancelled) {
            return false;
        }
        if (syntheticAttackClaimOwner != null && owner != null && syntheticAttackClaimOwner != owner) {
            return false;
        }
        Entity target = CLIENT.crosshairTarget != null && CLIENT.crosshairTarget.getType() == HitResult.Type.ENTITY
                ? ((EntityHitResult) CLIENT.crosshairTarget).getEntity() : null;
        if (!canAttack(owner, target, true)) {
            return false;
        }
        syntheticAttackClaimOwner = owner;
        syntheticAttackPending = true;
        if (!event.hasClicks()) {
            event.addClick();
        }
        return true;
    }

    public boolean requestUseClick(Module owner, UseKeyEvent event) {
        if (event == null || event.cancelled) {
            return false;
        }
        if (syntheticUseClaimOwner != null && owner != null && syntheticUseClaimOwner != owner) {
            return false;
        }
        HitResult crosshair = CLIENT.crosshairTarget;
        BlockHitResult blockHit = crosshair instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK ? bhr : null;
        if (!canUseItem(owner, null, blockHit, true)) {
            return false;
        }
        syntheticUseClaimOwner = owner;
        syntheticUsePending = true;
        if (!event.hasClicks()) {
            event.addClick();
        }
        return true;
    }

    public boolean validateSyntheticKeyAction(KeybindingEvent event) {
        if (event == null || event.cancelled || event.getClickCountDelta() <= 0) {
            return true;
        }
        syncTick();
        if (event instanceof AttackKeyEvent) {
            Entity target = CLIENT.crosshairTarget != null && CLIENT.crosshairTarget.getType() == HitResult.Type.ENTITY
                    ? ((EntityHitResult) CLIENT.crosshairTarget).getEntity() : null;
            if (!canAttack(syntheticAttackClaimOwner, target, true)) {
                syntheticAttackPending = false;
                return false;
            }
            if (event.getClickCountDelta() > 1) {
                event.setClickCount(event.getClickCount() - event.getClickCountDelta() + 1);
            }
            syntheticAttackPending = true;
            return true;
        }
        if (event instanceof UseKeyEvent) {
            HitResult crosshair = CLIENT.crosshairTarget;
            BlockHitResult blockHit = crosshair instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK ? bhr : null;
            if (!canUseItem(syntheticUseClaimOwner, null, blockHit, true)) {
                syntheticUsePending = false;
                return false;
            }
            syntheticUsePending = true;
            return true;
        }
        if (event instanceof SwapHandsKeyEvent) {
            if (!canSwapOffhand(null, true)) {
                syntheticOffhandSwapPending = false;
                return false;
            }
            syntheticOffhandSwapPending = true;
            return true;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Mixin Entry / Verification Hooks
    // -------------------------------------------------------------------------

    public boolean beforeDoAttack(Entity target) {
        if (isInsideApprovedAction(ActionType.ATTACK, null)) {
            return true;
        }
        boolean automated = syntheticAttackPending;
        Module owner = automated ? syntheticAttackClaimOwner : null;
        int deny = checkAttackCode(owner, target, automated);
        if (deny != OK) {
            syntheticAttackPending = false;
            finish(ActionResultInfo.skipped(ActionType.ATTACK, deny));
            return false;
        }
        return true;
    }

    public void afterDoAttack(boolean attacked) {
        boolean automated = syntheticAttackPending || isAutomatedActionActive();
        Module owner = syntheticAttackPending ? syntheticAttackClaimOwner : activeOwner;
        syntheticAttackPending = false;
        if (attacked) {
            recordAttackSuccess(owner);
            if (!isInsideAction(ActionType.ATTACK)) {
                finish(ActionResultInfo.succeeded(ActionType.ATTACK, ActionResult.SUCCESS));
            }
        } else if (automated && !isInsideAction(ActionType.ATTACK)) {
            finish(ActionResultInfo.rejected(ActionType.ATTACK, ActionResult.PASS, REJECT_NOT_APPLIED));
        }
    }

    public boolean beforeBlockBreaking(boolean breaking) {
        if (!breaking) {
            return true;
        }
        syncTick();
        if (attackSentInTick || entityInteractSentInTick || isBlockPlaceSent()
                || isUseItemSent() || releasingSentInTick || digStartStopSentInTick) {
            finish(ActionResultInfo.skipped(ActionType.ATTACK, DENY_TICK_CONFLICT));
            return false;
        }
        return true;
    }

    public boolean beforeEntityAttack(PlayerEntity player, Entity target) {
        if (player != CLIENT.player) {
            return true;
        }
        if (isInsideApprovedAction(ActionType.ATTACK, null)) {
            return true;
        }
        boolean automated = syntheticAttackPending;
        Module owner = automated ? syntheticAttackClaimOwner : null;
        int deny = checkAttackCode(owner, target, automated);
        if (deny != OK) {
            finish(ActionResultInfo.skipped(ActionType.ATTACK, deny));
            return false;
        }
        return true;
    }

    public void afterEntityAttack(PlayerEntity player) {
        if (player != CLIENT.player) {
            return;
        }
        if (activeDepth > 0 && activeType == ActionType.ATTACK) {
            attackObservedInFrame = true;
        }
        Module owner = activeDepth > 0 ? activeOwner : (syntheticAttackPending ? syntheticAttackClaimOwner : null);
        recordAttackSuccess(owner);
    }

    public boolean beforeDoItemUse(HitResult crosshair) {
        if (isInsideApprovedAction(ActionType.ITEM_USE, null)) {
            return true;
        }
        boolean automated = syntheticUsePending;
        Module owner = automated ? syntheticUseClaimOwner : null;
        BlockHitResult blockHit = crosshair instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK ? bhr : null;
        int deny = checkItemUseCode(owner, null, blockHit, automated);
        if (deny != OK) {
            syntheticUsePending = false;
            finish(ActionResultInfo.skipped(ActionType.ITEM_USE, deny));
            return false;
        }
        observedUseResult = null;
        return true;
    }

    public void afterDoItemUse() {
        boolean automated = syntheticUsePending;
        Module owner = automated ? syntheticUseClaimOwner : activeOwner;
        syntheticUsePending = false;
        if (!isInsideAction(ActionType.ITEM_USE)) {
            ActionResult observed = observedUseResult;
            if (observed != null && observed.isAccepted()) {
                recordItemUseSuccess(owner);
                finish(ActionResultInfo.succeeded(ActionType.ITEM_USE, observed));
            } else if (automated) {
                finish(ActionResultInfo.rejected(ActionType.ITEM_USE, observed, REJECT_NOT_APPLIED));
            }
        }
    }

    public boolean beforeInteractBlock(PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player != CLIENT.player) {
            return true;
        }
        if (isInsideApprovedAction(ActionType.ITEM_USE, null)) {
            return true;
        }
        boolean automated = syntheticUsePending;
        Module owner = automated ? syntheticUseClaimOwner : null;
        int deny = checkItemUseCode(owner, hand, hit, automated);
        if (deny != OK) {
            finish(ActionResultInfo.skipped(ActionType.ITEM_USE, deny));
            return false;
        }
        return true;
    }

    public void afterInteractBlock(PlayerEntity player, ActionResult result) {
        if (player != CLIENT.player) {
            return;
        }
        if (result != null && result.isAccepted()) {
            observedUseResult = result;
            if (!isInsideAction(ActionType.ITEM_USE)) {
                recordItemUseSuccess(syntheticUsePending ? syntheticUseClaimOwner : null);
            }
        } else if (observedUseResult == null) {
            observedUseResult = result;
        }
    }

    public boolean beforeInteractItem(PlayerEntity player, Hand hand) {
        if (player != CLIENT.player) {
            return true;
        }
        if (isInsideApprovedAction(ActionType.ITEM_USE, null)) {
            return true;
        }
        boolean automated = syntheticUsePending;
        Module owner = automated ? syntheticUseClaimOwner : null;
        int deny = checkItemUseCode(owner, hand, null, automated);
        if (deny != OK) {
            finish(ActionResultInfo.skipped(ActionType.ITEM_USE, deny));
            return false;
        }
        return true;
    }

    public void afterInteractItem(PlayerEntity player, ActionResult result) {
        if (player != CLIENT.player) {
            return;
        }
        if (result != null && result.isAccepted()) {
            observedUseResult = result;
            if (!isInsideAction(ActionType.ITEM_USE)) {
                recordItemUseSuccess(syntheticUsePending ? syntheticUseClaimOwner : null);
            }
        } else if (observedUseResult == null) {
            observedUseResult = result;
        }
    }

    public boolean beforeClickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
        if (player != CLIENT.player) {
            return true;
        }
        if (isInsideApprovedAction(ActionType.INVENTORY, null)) {
            return true;
        }
        boolean automated = InventoryActionScheduler.isTransferring();
        int deny = checkSlotClickCode(null, syncId, slotId, button, actionType, player, automated);
        if (deny != OK) {
            finish(ActionResultInfo.skipped(ActionType.INVENTORY, deny));
            return false;
        }
        return true;
    }

    public void afterClickSlot(PlayerEntity player) {
        if (player != CLIENT.player || isInsideAction(ActionType.INVENTORY)) {
            return;
        }
        recordInventoryClickSuccess(null);
    }

    public void afterSelectedSlotChanged(int previousSlot, int selectedSlot) {
        if (previousSlot == selectedSlot || isInsideAction(ActionType.INVENTORY)) {
            return;
        }
        recordSlotChangeSuccess(null);
    }

    public boolean beforeOffhandSwapPacket() {
        if (isInsideApprovedAction(ActionType.INVENTORY, null)) {
            return true;
        }
        boolean automated = syntheticOffhandSwapPending;
        int deny = checkOffhandSwapCode(null, automated);
        if (deny != OK) {
            syntheticOffhandSwapPending = false;
            finish(ActionResultInfo.skipped(ActionType.INVENTORY, deny));
            return false;
        }
        syntheticOffhandSwapPending = false;
        recordOffhandSwapSuccess(null);
        return true;
    }

    // -------------------------------------------------------------------------
    // Wire-Level Packet Order & Deferred Slot Safety Net
    // -------------------------------------------------------------------------

    /**
     * Final protocol-order gate executed right before a packet is sent on the wire.
     * Returns {@code true} if the packet must be suppressed (or deferred for slot packets).
     */
    public boolean filterOutgoingPacket(Packet<?> packet) {
        if (packet == null) {
            return false;
        }

        if (packet instanceof ClientTickEndC2SPacket) {
            inTick = false;
            movementSentInTick = true;
            tickEnded = true;
            postAttackSwitchBlocked = false;
            clearTickState();
            return false;
        }

        if (packet instanceof PlayerMoveC2SPacket) {
            movementSentInTick = true;
            postAttackSwitchBlocked = false;
            return false;
        }

        if (packet instanceof TeleportConfirmC2SPacket) {
            postAttackSwitchBlocked = false;
            if (CLIENT.player != null) {
                lastSetbackTick = CLIENT.player.age;
            }
            return false;
        }

        if (isPostMovementSuppressedPacket(packet)) {
            if (movementSentInTick || tickEnded || !inTick) {
                suppressedActionInTick = true;
                return true;
            }
        }

        if (packet instanceof PlayerInteractEntityC2SPacket entityPacket) {
            final boolean[] isAttack = {false};
            entityPacket.handle(new PlayerInteractEntityC2SPacket.Handler() {
                @Override
                public void interact(Hand hand) { }

                @Override
                public void interactAt(Hand hand, Vec3d pos) { }

                @Override
                public void attack() {
                    isAttack[0] = true;
                }
            });

            if (isAttack[0]) {
                if (attackSentInTick || blockActionSentInTick || entityInteractSentInTick
                        || inventoryActionSentInTick || releasingSentInTick || diggingSentInTick
                        || useItemMainSentInTick || useItemOffSentInTick) {
                    suppressedActionInTick = true;
                    return true;
                }
                suppressedActionInTick = false;
                actionSentInTick = true;
                attackSentInTick = true;
                postAttackSwitchBlocked = true;
                pendingAttackSwing = true;
            } else {
                if (blockActionSentInTick || inventoryActionSentInTick || releasingSentInTick
                        || diggingSentInTick || useItemMainSentInTick || useItemOffSentInTick) {
                    suppressedActionInTick = true;
                    return true;
                }
                suppressedActionInTick = false;
                actionSentInTick = true;
                entityInteractSentInTick = true;
            }
            return false;
        }

        if (packet instanceof PlayerInteractBlockC2SPacket blockPacket) {
            if (attackSentInTick || entityInteractSentInTick || inventoryActionSentInTick
                    || releasingSentInTick || diggingSentInTick || useItemMainSentInTick || useItemOffSentInTick) {
                suppressedActionInTick = true;
                return true;
            }
            Hand hand = blockPacket.getHand();
            BlockHitResult hitNow = blockPacket.getBlockHitResult();
            if (isDuplicateBlockPlace(hand, hitNow)) {
                suppressedActionInTick = true;
                return true;
            }
            suppressedActionInTick = false;
            actionSentInTick = true;
            blockActionSentInTick = true;
            if (hand == Hand.MAIN_HAND) {
                blockPlaceMainSentInTick = true;
                lastPlaceMainHit = hitNow;
            } else {
                blockPlaceOffSentInTick = true;
                lastPlaceOffHit = hitNow;
            }
            return false;
        }

        if (packet instanceof PlayerInteractItemC2SPacket itemPacket) {
            boolean main = itemPacket.getHand() == Hand.MAIN_HAND;
            if ((attackSentInTick && !entityInteractSentInTick)
                    || inventoryActionSentInTick
                    || releasingSentInTick
                    || diggingSentInTick
                    || (main && useItemMainSentInTick)
                    || (!main && useItemOffSentInTick)) {
                suppressedActionInTick = true;
                return true;
            }
            suppressedActionInTick = false;
            actionSentInTick = true;
            if (main) {
                useItemMainSentInTick = true;
            } else {
                useItemOffSentInTick = true;
            }
            return false;
        }

        if (packet instanceof PlayerActionC2SPacket actionPacket) {
            PlayerActionC2SPacket.Action action = actionPacket.getAction();
            if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK
                    || action == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
                if (attackSentInTick || blockPlaceMainSentInTick || blockPlaceOffSentInTick
                        || digStartStopSentInTick || entityInteractSentInTick
                        || inventoryActionSentInTick || releasingSentInTick
                        || useItemMainSentInTick || useItemOffSentInTick) {
                    suppressedActionInTick = true;
                    return true;
                }
                suppressedActionInTick = false;
                actionSentInTick = true;
                blockActionSentInTick = true;
                diggingSentInTick = true;
                digStartStopSentInTick = true;
                pendingDigSwing = true;
            } else if (action == PlayerActionC2SPacket.Action.STAB) {
                if (attackSentInTick || blockActionSentInTick || entityInteractSentInTick
                        || inventoryActionSentInTick || releasingSentInTick || diggingSentInTick
                        || useItemMainSentInTick || useItemOffSentInTick
                        || isSlotPending() || !isHoldingPiercingWeapon()) {
                    suppressedActionInTick = true;
                    return true;
                }
                suppressedActionInTick = false;
                actionSentInTick = true;
                attackSentInTick = true;
                postAttackSwitchBlocked = true;
                pendingAttackSwing = true;
            } else if (action == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                actionSentInTick = true;
                releasingSentInTick = true;
            } else if (action == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK) {
                // Allow vanilla ABORT_DESTROY_BLOCK -> START_DESTROY_BLOCK transition in the same tick.
                actionSentInTick = true;
                diggingSentInTick = true;
            } else if (action == PlayerActionC2SPacket.Action.DROP_ITEM
                    || action == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
                actionSentInTick = true;
                droppingSentInTick = true;
            } else if (action == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
                if (droppingSentInTick || inventoryActionSentInTick || attackSentInTick
                        || releasingSentInTick || entityInteractSentInTick
                        || blockPlaceMainSentInTick || blockPlaceOffSentInTick
                        || useItemMainSentInTick || useItemOffSentInTick
                        || diggingSentInTick || isSlotPending()) {
                    suppressedActionInTick = true;
                    return true;
                }
                suppressedActionInTick = false;
                actionSentInTick = true;
                swappingSentInTick = true;
                if (activeDepth > 0 && activeType == ActionType.INVENTORY) {
                    offhandPacketSentInFrame = true;
                }
            } else {
                actionSentInTick = true;
            }
            return false;
        }

        if (packet instanceof HandSwingC2SPacket swingPacket) {
            if (movementSentInTick || tickEnded || !inTick) {
                return true;
            }
            boolean required = (pendingAttackSwing && swingPacket.getHand() == Hand.MAIN_HAND) || pendingDigSwing;
            if (!required && (suppressedActionInTick || swingSentInTick)) {
                return true;
            }
            if (swingPacket.getHand() == Hand.MAIN_HAND) {
                pendingAttackSwing = false;
            }
            pendingDigSwing = false;
            swingSentInTick = true;
            return false;
        }

        if (packet instanceof ClickSlotC2SPacket || packet instanceof CloseHandledScreenC2SPacket) {
            if (inTick && movementSentInTick) {
                return true;
            }
            if (inTick && (attackSentInTick || blockActionSentInTick || entityInteractSentInTick
                    || diggingSentInTick || droppingSentInTick)) {
                return true;
            }
            if (inTick) {
                actionSentInTick = true;
                inventoryActionSentInTick = true;
            }
            return false;
        }

        if (packet instanceof UpdateSelectedSlotC2SPacket slotPacket) {
            int slot = slotPacket.getSelectedSlot();
            if (lastSentSlot == slot) {
                pendingSlot = -1;
                return true;
            }
            boolean screenOpen = MinecraftRenderState.getScreen() != null;
            if (!flushingPendingSlot && (screenOpen || actionSentInTick || movementSentInTick || tickEnded || !inTick)) {
                pendingSlot = slot;
                return true;
            }
            pendingSlot = -1;
            lastSentSlot = slot;
        }

        return false;
    }

    public void onServerPacketReceived(Packet<?> packet) {
        if (packet instanceof BundleS2CPacket bundle) {
            for (Packet<?> inner : bundle.getPackets()) {
                onServerPacketReceived(inner);
            }
            return;
        }
        if (packet instanceof UpdateSelectedSlotS2CPacket slotPacket) {
            int slot = slotPacket.slot();
            CLIENT.execute(() -> {
                lastSentSlot = slot;
                pendingSlot = -1;
            });
        } else if (packet instanceof GameJoinS2CPacket) {
            CLIENT.execute(this::reset);
        } else if (packet instanceof PlayerRespawnS2CPacket) {
            CLIENT.execute(() -> {
                pendingSlot = -1;
                clearTickState();
                postAttackSwitchBlocked = false;
            });
        } else if (packet instanceof PlayerPositionLookS2CPacket) {
            CLIENT.execute(() -> {
                if (CLIENT.player != null) {
                    lastSetbackTick = CLIENT.player.age;
                }
            });
        }
    }

    public void flushPendingSlot() {
        if (MinecraftRenderState.getScreen() != null || CLIENT.player == null || CLIENT.interactionManager == null) {
            return;
        }
        int slot = pendingSlot;
        if (slot < 0 || slot > 8) {
            pendingSlot = -1;
            return;
        }
        pendingSlot = -1;
        if (lastSentSlot == slot) {
            return;
        }
        flushingPendingSlot = true;
        beginFrame(ActionType.INVENTORY, null, true);
        try {
            if (CLIENT.player.getInventory().getSelectedSlot() != slot) {
                CLIENT.player.getInventory().setSelectedSlot(slot);
            }
            ((ClientPlayerInteractionManagerAccessor) CLIENT.interactionManager).invokeSyncSelectedSlot();
            if (lastSentSlot != slot && CLIENT.getNetworkHandler() != null) {
                CLIENT.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
        } finally {
            endFrame();
            flushingPendingSlot = false;
        }
    }

    // -------------------------------------------------------------------------
    // Event Lifecycle Subscribers
    // -------------------------------------------------------------------------

    @Subscribe(priority = EventPriority.HIGHEST)
    private void onHandleInputPre(HandleInputEvent.Pre event) {
        syncTick();
        clearPendingSynthetic();
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void onHandleInputPost(HandleInputEvent.Post event) {
        clearPendingSynthetic();
    }

    @Subscribe
    private void onAttackPost(EntityAttackEvent.Post event) {
        if (CLIENT.player != null && event.getPlayer() == CLIENT.player) {
            postAttackSwitchBlocked = true;
        }
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void onPacketSent(PacketSendEvent event) {
        if (event.cancelled) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerActionC2SPacket action
                && action.getAction() == PlayerActionC2SPacket.Action.STAB) {
            postAttackSwitchBlocked = true;
        }
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        reset();
    }

    // -------------------------------------------------------------------------
    // Internal Helpers
    // -------------------------------------------------------------------------

    private static boolean isActionPacket(Packet<?> packet) {
        return packet instanceof PlayerInteractEntityC2SPacket
                || packet instanceof PlayerInteractBlockC2SPacket
                || packet instanceof PlayerInteractItemC2SPacket
                || packet instanceof PlayerActionC2SPacket;
    }

    private static boolean isPostMovementSuppressedPacket(Packet<?> packet) {
        if (!isActionPacket(packet)) {
            return false;
        }
        if (packet instanceof PlayerActionC2SPacket actionPacket) {
            PlayerActionC2SPacket.Action action = actionPacket.getAction();
            if (action == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
                    || action == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK) {
                return false;
            }
        }
        return true;
    }

    private boolean isAttackPacketConflict(boolean entityOrPiercing) {
        return attackSentInTick
                || isBlockPlaceSent()
                || digStartStopSentInTick
                || releasingSentInTick
                || isUseItemSent()
                || (entityOrPiercing && diggingSentInTick);
    }

    private boolean isDuplicateBlockPlace(Hand hand, BlockHitResult hitNow) {
        boolean main = hand == Hand.MAIN_HAND;
        boolean alreadySent = main ? blockPlaceMainSentInTick : blockPlaceOffSentInTick;
        if (!alreadySent) {
            return false;
        }
        BlockHitResult hitPrev = main ? lastPlaceMainHit : lastPlaceOffHit;
        boolean identicalRepeat = hitPrev != null && hitNow != null
                && hitPrev.getBlockPos().equals(hitNow.getBlockPos())
                && hitPrev.getSide() == hitNow.getSide()
                && hitPrev.getPos().equals(hitNow.getPos());
        return !identicalRepeat;
    }

    private boolean isHoldingPiercingWeapon() {
        if (CLIENT.player == null) {
            return false;
        }
        if (CLIENT.player.getMainHandStack().contains(DataComponentTypes.PIERCING_WEAPON)) {
            return true;
        }
        if (lastSentSlot >= 0 && lastSentSlot < 9) {
            return CLIENT.player.getInventory().getStack(lastSentSlot).contains(DataComponentTypes.PIERCING_WEAPON);
        }
        return false;
    }

    private void beginFrame(ActionType type, Module owner, boolean automated) {
        if (activeDepth == 0) {
            activeType = type;
            activeOwner = owner;
            activeAutomated = automated;
            attackObservedInFrame = false;
            offhandPacketSentInFrame = false;
            observedUseResult = null;
        }
        activeDepth++;
    }

    private void endFrame() {
        activeDepth = Math.max(0, activeDepth - 1);
        if (activeDepth == 0) {
            activeType = null;
            activeOwner = null;
            activeAutomated = false;
        }
    }

    private void syncTick() {
        if (CLIENT.player == null) {
            if (trackedTick != -1) {
                trackedTick = -1;
                clearTickState();
            }
            return;
        }
        int age = CLIENT.player.age;
        if (age != trackedTick) {
            trackedTick = age;
            clearTickState();
        }
    }

    private void clearTickState() {
        actionSentInTick = false;
        attackSentInTick = false;
        attackOwnerThisTick = null;
        blockActionSentInTick = false;
        blockPlaceMainSentInTick = false;
        blockPlaceOffSentInTick = false;
        lastPlaceMainHit = null;
        lastPlaceOffHit = null;
        entityInteractSentInTick = false;
        useItemMainSentInTick = false;
        useItemOffSentInTick = false;
        itemUseOwnerThisTick = null;
        inventoryActionSentInTick = false;
        inventoryClickOwnerThisTick = null;
        releasingSentInTick = false;
        diggingSentInTick = false;
        digStartStopSentInTick = false;
        droppingSentInTick = false;
        swappingSentInTick = false;
        offhandSwapOwnerThisTick = null;
        slotChangedThisTick = false;
        slotOwnerThisTick = null;
        pendingAttackSwing = false;
        pendingDigSwing = false;
        swingSentInTick = false;
        suppressedActionInTick = false;
    }

    private void clearPendingSynthetic() {
        syntheticAttackClaimOwner = null;
        syntheticAttackPending = false;
        syntheticUseClaimOwner = null;
        syntheticUsePending = false;
        syntheticOffhandSwapPending = false;
    }

    private void recordAttackSuccess(Module owner) {
        syncTick();
        actionSentInTick = true;
        attackSentInTick = true;
        if (owner != null) {
            attackOwnerThisTick = owner;
        }
        postAttackSwitchBlocked = true;
    }

    private void recordItemUseSuccess(Module owner) {
        syncTick();
        actionSentInTick = true;
        if (owner != null) {
            itemUseOwnerThisTick = owner;
        }
    }

    private void recordSlotChangeSuccess(Module owner) {
        syncTick();
        slotChangedThisTick = true;
        if (owner != null) {
            slotOwnerThisTick = owner;
        }
    }

    private void recordInventoryClickSuccess(Module owner) {
        syncTick();
        actionSentInTick = true;
        inventoryActionSentInTick = true;
        if (owner != null) {
            inventoryClickOwnerThisTick = owner;
        }
    }

    private void recordOffhandSwapSuccess(Module owner) {
        syncTick();
        actionSentInTick = true;
        swappingSentInTick = true;
        if (owner != null) {
            offhandSwapOwnerThisTick = owner;
        }
    }

    private ActionResultInfo finish(ActionResultInfo result) {
        lastResult = result;
        return result;
    }

    private boolean hasValidPlayerAndWorld() {
        return CLIENT.player != null && CLIENT.world != null && CLIENT.interactionManager != null;
    }

    private static boolean sameOwner(Module requester, Module holder) {
        return requester != null && holder != null && requester == holder;
    }

    private boolean isBlockedByPreventOffhand(int slotId, int button, SlotActionType actionType) {
        if (ClumpsClient.ModuleManager == null || InventoryActionScheduler.isTransferring()) {
            return false;
        }
        Prevent prevent = (Prevent) ClumpsClient.ModuleManager.findFeature(Prevent.class);
        if (prevent == null || !prevent.isEnabled() || !Prevent.untotem.getValue() || !Prevent.inInventory.getValue()) {
            return false;
        }
        Item offhand = CLIENT.player.getOffHandStack().getItem();
        if (offhand != Items.TOTEM_OF_UNDYING) {
            return false;
        }
        SlotAction slotAction = SlotPackets.fromEnum(actionType);
        return slotId == 45 || (slotAction == SlotAction.SWAP && button == 40);
    }

    private ItemStack getSwapTargetStack(int button) {
        if (CLIENT.player == null) {
            return ItemStack.EMPTY;
        }
        if (button == 40) {
            return CLIENT.player.getOffHandStack();
        }
        if (button >= 0 && button <= 8) {
            return CLIENT.player.getInventory().getStack(button);
        }
        return ItemStack.EMPTY;
    }
}
