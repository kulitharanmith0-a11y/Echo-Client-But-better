package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.modules.impl.movement.DiveBomb;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.DoubleSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class DiveBombSequence {
    private final MinecraftClient client;
    private final int originalSlot;
    private final int windChargeSlot;
    private final boolean movedWindChargeToOffhand;
    private final Runnable swapHands;
    private final Runnable useItem;
    private final DoubleSupplier effectivePitch;
    private final Deque<DiveBombAction> preparation;
    private final Deque<DiveBombAction> restoration;
    private DiveBombPhase phase;
    private DiveBombAction pendingAction;
    private int waitingTicks;

    
    public DiveBombSequence(MinecraftClient client, int originalSlot, int windChargeSlot, int pearlSlot, boolean moveWindChargeToOffhand, Runnable swapHands, Runnable useItem, DoubleSupplier effectivePitch) {
        this.client = client;
        int controlState = DiveBomb.readSharedControlState();
        preparation = new ArrayDeque<>();
        restoration = new ArrayDeque<>();
        phase = DiveBombPhase.PREP;
        pendingAction = null;
        waitingTicks = 0;
        this.originalSlot = originalSlot;
        this.windChargeSlot = windChargeSlot;
        this.movedWindChargeToOffhand = moveWindChargeToOffhand;
        this.swapHands = swapHands;
        this.useItem = useItem;
        this.effectivePitch = effectivePitch;
        if (controlState == 0) {
            if (moveWindChargeToOffhand) {
                preparation.add(selectSlot(windChargeSlot));
                preparation.add(swapOffhand(true));
            }
            if (!client.player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
                preparation.add(selectSlot(pearlSlot));
            }
        }
        if (Event.getEventCounter() != 0) {
            DiveBomb.writeSharedControlState(controlState + 1);
        }
    }

    
    public boolean tick() {
        return switch (phase) {
            case PREP -> processQueue(preparation, DiveBombPhase.AIM);
            case AIM -> waitForAim();
            case THROW -> throwItems();
            case RESTORE -> processQueue(restoration, null);
        };
    }

    private DiveBombAction selectSlot(int slot) {
        return new DiveBombAction(() -> {
            if (slot >= 0 && slot <= 8 && client.player.getInventory().getSelectedSlot() != slot) {
                ActionCoordinator coordinator = ActionCoordinator.get();
                if (coordinator != null) {
                    coordinator.executeSlotSelect(null, slot);
                } else {
                    client.player.getInventory().setSelectedSlot(slot);
                }
            }
        }, () -> client.player.getInventory().getSelectedSlot() == slot);
    }

    private DiveBombAction swapOffhand(boolean expectWindCharge) {
        return new DiveBombAction(swapHands, () -> client.player.getOffHandStack().isOf(Items.WIND_CHARGE) == expectWindCharge);
    }

    private boolean hasThrowItemsInHands() {
        return client.player.getMainHandStack().isOf(Items.ENDER_PEARL)
                && client.player.getOffHandStack().isOf(Items.WIND_CHARGE);
    }

    private boolean hasItemCooldown() {
        return client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))
                || client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE));
    }

    private boolean waitForAim() {
        if (!hasThrowItemsInHands() || hasItemCooldown()) {
            return abort();
        }
        if (effectivePitch.getAsDouble() >= -89.5F) {
            return false;
        }
        phase = DiveBombPhase.THROW;
        return throwItems();
    }

    private boolean throwItems() {
        if (hasThrowItemsInHands() && !hasItemCooldown()) {
            useItem.run();
            useItem.run();
            if (client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))
                    && !client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE))
                    && client.player.getOffHandStack().isOf(Items.WIND_CHARGE)) {
                ActionCoordinator coordinator = ActionCoordinator.get();
                if (coordinator != null) {
                    coordinator.executeItemUse(null, Hand.OFF_HAND);
                } else {
                    client.interactionManager.interactItem(client.player, Hand.OFF_HAND);
                }
            }
        }
        return beginRestoration();
    }

    private boolean beginRestoration() {
        restoration.clear();
        if (movedWindChargeToOffhand && client.player.getOffHandStack().isOf(Items.WIND_CHARGE)) {
            restoration.add(selectSlot(windChargeSlot));
            restoration.add(swapOffhand(false));
        }
        if (client.player.getInventory().getSelectedSlot() != originalSlot || !restoration.isEmpty()) {
            restoration.add(selectSlot(originalSlot));
        }
        if (restoration.isEmpty()) {
            return true;
        }
        phase = DiveBombPhase.RESTORE;
        pendingAction = null;
        return false;
    }

    private boolean processQueue(Deque<DiveBombAction> queue, DiveBombPhase nextPhase) {
        if (pendingAction == null) {
            if (queue.isEmpty()) {
                if (nextPhase == null) {
                    return true;
                }
                phase = nextPhase;
                return tick();
            }
            pendingAction = queue.poll();
            pendingAction.action().run();
            waitingTicks = 0;
        }
        if (pendingAction.ready().getAsBoolean()) {
            pendingAction = null;
            return false;
        }
        if (++waitingTicks > 10) {
            return abort();
        }
        return false;
    }

    private boolean abort() {
        return phase == DiveBombPhase.RESTORE || beginRestoration();
    }
}
