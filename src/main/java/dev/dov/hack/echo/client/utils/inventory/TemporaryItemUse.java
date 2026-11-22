package dev.dov.hack.echo.client.utils.inventory;

import static dev.dov.hack.echo.client.MinecraftAccess.CLIENT;
import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.event.input.HotbarKeyEvent;
import dev.dov.hack.echo.client.event.input.SwapHandsKeyEvent;
import dev.dov.hack.echo.client.event.interaction.AttackAttemptEvent;
import dev.dov.hack.echo.client.event.interaction.HandInteractEvent;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.event.network.PacketSendEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;

public final class TemporaryItemUse {
    private static final int SWAP_DELAY_TICKS = 2;
    private static final int TIMEOUT_TICKS = 20;
    private final Module owner;
    private final Predicate<Item> itemPredicate;
    private final Consumer<Item> transferCallback;
    private TemporaryItemUseState state = TemporaryItemUseState.IDLE;
    private long nextActionTick = -1L;
    private int targetSlot = -1;
    private int previousSlot = -1;
    private Item requestedItem;
    private boolean swapKeyActive;

    public TemporaryItemUse(Module owner, Predicate<Item> itemPredicate, Consumer<Item> transferCallback) {
        this.owner = owner;
        this.itemPredicate = itemPredicate;
        this.transferCallback = transferCallback;
    }

    public boolean isBusy() { return state != TemporaryItemUseState.IDLE; }
    public boolean isSwapKeyActive() { return swapKeyActive; }

    public void beginSwap(int slot, Item item) {
        requestedItem = item;
        targetSlot = slot;
        previousSlot = selectedSlot();
        enterState(slot == previousSlot ? TemporaryItemUseState.SWAP : TemporaryItemUseState.SELECT, 0);
    }

    public void cancelAndRestore() {
        if (isBusy() && CLIENT.player != null && targetSlot != previousSlot && selectedSlot() == targetSlot) {
            if (ownsTopSelection()) {
                DeferredActionQueue.select(owner, previousSlot, -1, false);
            } else if (!DeferredActionQueue.contains(owner)) {
                InventoryItems.selectHotbarSlot(previousSlot);
            }
        }
        resetState();
    }

    public void reset() { resetState(); }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (!isBusy() || CLIENT.player == null) { return; }
        boolean timedOut = (long) CLIENT.player.age - nextActionTick > TIMEOUT_TICKS;
        boolean changedSlot = state != TemporaryItemUseState.SELECT && selectedSlot() != targetSlot;
        if (timedOut || changedSlot) { cancelAndRestore(); }
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (!canHandleInput()) { return; }
        if (state == TemporaryItemUseState.SELECT && event.slot == targetSlot) {
            selectTarget(event);
        } else if (state == TemporaryItemUseState.RESELECT && event.slot == previousSlot) {
            restoreSelection(event);
        }
    }

    @Subscribe
    private void onSwapHandsKey(SwapHandsKeyEvent event) {
        if (state != TemporaryItemUseState.SWAP || !canHandleInput()) { return; }
        if (!canSwapItem()) {
            scheduleReselect();
            return;
        }
        swapKeyActive = true;
        event.setClickCount(1);
    }

    @Subscribe
    private void onPacketSend(PacketSendEvent event) {
        if (!swapKeyActive || event.cancelled) { return; }
        if (!(event.getPacket() instanceof PlayerActionC2SPacket packet)
                || packet.getAction() != PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) { return; }
        swapKeyActive = false;
        swapLocalHands();
        transferCallback.accept(requestedItem);
        scheduleReselect();
    }

    @Subscribe
    private void onInputComplete(HandleInputEvent.Post event) {
        if (!swapKeyActive) { return; }
        swapKeyActive = false;
        scheduleReselect();
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onAttackAttempt(AttackAttemptEvent event) {
        if (isBusy()) { event.cancel(); }
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void onHandInteract(HandInteractEvent.Pre event) {
        if (isBusy()) { event.cancel(); }
    }

    private void selectTarget(HotbarKeyEvent event) {
        if (!DeferredActionQueue.select(owner, targetSlot, -1, false)) { return; }
        event.addClick();
        enterState(TemporaryItemUseState.SWAP, SWAP_DELAY_TICKS);
    }

    private void restoreSelection(HotbarKeyEvent event) {
        boolean buriedOwner = DeferredActionQueue.contains(owner) && !ownsTopSelection();
        if (selectedSlot() == targetSlot && !buriedOwner) {
            if (ownsTopSelection() && !DeferredActionQueue.select(owner, previousSlot, -1, false)) { return; }
            event.addClick();
        }
        resetState();
    }

    private boolean canSwapItem() {
        return MinecraftRenderState.getScreen() == null && selectedSlot() == targetSlot
                && CLIENT.player.getMainHandStack().isOf(requestedItem) && itemPredicate.test(requestedItem);
    }

    private void scheduleReselect() {
        if (targetSlot == previousSlot || selectedSlot() != targetSlot) {
            resetState();
            return;
        }
        enterState(TemporaryItemUseState.RESELECT, 1);
    }

    private void swapLocalHands() {
        ItemStack offhand = CLIENT.player.getStackInHand(Hand.OFF_HAND);
        CLIENT.player.setStackInHand(Hand.OFF_HAND, CLIENT.player.getStackInHand(Hand.MAIN_HAND));
        CLIENT.player.setStackInHand(Hand.MAIN_HAND, offhand);
    }

    private boolean canHandleInput() {
        return isBusy() && CLIENT.player != null && (long) CLIENT.player.age - nextActionTick >= 0L;
    }

    private boolean ownsTopSelection() { return DeferredActionQueue.selectedSlot(owner) != -1; }
    private int selectedSlot() { return CLIENT.player.getInventory().getSelectedSlot(); }

    private void enterState(TemporaryItemUseState next, int delayTicks) {
        state = next;
        nextActionTick = (long) (CLIENT.player.age + delayTicks);
    }

    private void resetState() {
        DeferredActionQueue.drop(owner);
        state = TemporaryItemUseState.IDLE;
        nextActionTick = -1L;
        targetSlot = -1;
        previousSlot = -1;
        requestedItem = null;
        swapKeyActive = false;
    }
}
