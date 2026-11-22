package dev.dov.hack.echo.client.handlers;

import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.event.interaction.EntityAttackEvent;
import dev.dov.hack.echo.client.event.network.PacketReceiveEvent;
import dev.dov.hack.echo.client.event.network.PacketSendEvent;
import dev.dov.hack.echo.client.handlers.action.ActionCoordinator;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.impl.utility.AutoTotem;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;

public final class DeferredActionQueue extends ClientHandler {
    private static final Deque<DeferredFeatureAction> actions = new ArrayDeque<>();
    private static boolean switchingBlocked;

    public static boolean select(Module owner, int slot) {
        return select(owner, slot, 0, true, null);
    }

    public static boolean select(Module owner, int slot, int delayTicks) {
        return select(owner, slot, delayTicks, true, null);
    }

    public static boolean select(Module owner, int slot, int delayTicks, boolean restoreAll) {
        return select(owner, slot, delayTicks, restoreAll, null);
    }

    public static boolean select(Module owner, int slot, int delayTicks, Runnable callback) {
        return select(owner, slot, delayTicks, true, callback);
    }

    
    public static boolean select(Module owner, int slot, int delayTicks, boolean restoreAll, Runnable callback) {
        if (owner == null || CLIENT.player == null || slot < 0 || slot > 8) return false;
        if (switchingBlocked && slot != selectedSlot()) return false;
        DeferredFeatureAction top = actions.peek();
        boolean ownsTop = top != null && top.owner == owner;
        boolean allowOverride = ownsTop || owner instanceof AutoTotem;
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null && !coordinator.canSelectSlot(owner, slot, true, allowOverride)) {
            return false;
        }
        if (ownsTop) {
            if (!setHotbarSlot(owner, slot, true)) return false;
            top.selectedSlot = slot;
            top.restoreAll = restoreAll;
            top.expiresAtTick = delayTicks < 0 ? Long.MAX_VALUE : (long) (CLIENT.player.age + delayTicks);
            top.phase = delayTicks <= 0 ? ActionPhase.POST : ActionPhase.EARLY;
            top.callback = callback;
            return true;
        }
        if (contains(owner)) return false;
        if (top != null && !allowOverride) return false;
        int current = selectedSlot();
        if (current == slot) return false;
        DeferredFeatureAction oldest = actions.peekLast();
        int baseline = oldest == null ? current : oldest.baselineSlot;
        long deadline = delayTicks < 0 ? Long.MAX_VALUE : (long) (CLIENT.player.age + delayTicks);
        ActionPhase phase = delayTicks <= 0 ? ActionPhase.POST : ActionPhase.EARLY;
        if (!setHotbarSlot(owner, slot, true)) return false;
        actions.push(new DeferredFeatureAction(owner, baseline, slot, restoreAll, deadline, phase, callback));
        return true;
    }

    public static boolean ensureSelected(Module owner, int slot) {
        return ensureSelected(owner, slot, 0, true, null);
    }

    public static boolean ensureSelected(Module owner, int slot, int delayTicks) {
        return ensureSelected(owner, slot, delayTicks, true, null);
    }

    public static boolean ensureSelected(Module owner, int slot, int delayTicks, boolean restoreAll) {
        return ensureSelected(owner, slot, delayTicks, restoreAll, null);
    }

    public static boolean ensureSelected(Module owner, int slot, int delayTicks, Runnable callback) {
        return ensureSelected(owner, slot, delayTicks, true, callback);
    }

    
    public static boolean ensureSelected(Module owner, int slot, int delayTicks, boolean restoreAll, Runnable callback) {
        if (owner == null || CLIENT.player == null || slot < 0 || slot > 8) return false;
        if (selectedSlot() == slot && !contains(owner)) return true;
        return select(owner, slot, delayTicks, restoreAll, callback);
    }

    public static boolean contains(Module owner) {
        if (owner == null) return false;
        for (DeferredFeatureAction action : actions) if (action.owner == owner) return true;
        return false;
    }

    public static boolean contains(Class<?> featureType) {
        if (featureType == null) return false;
        for (DeferredFeatureAction action : actions) if (featureType.isInstance(action.owner)) return true;
        return false;
    }

    public static boolean hasPendingActions() { return !actions.isEmpty(); }

    public static Module topOwner() {
        DeferredFeatureAction top = actions.peek();
        return top == null ? null : top.owner;
    }

    
    public static boolean updateSelectedSlot(Module owner, int slot) {
        if (owner == null) return false;
        DeferredFeatureAction top = actions.peek();
        if (top == null || top.owner != owner) return false;
        top.selectedSlot = slot;
        return true;
    }

    public static int selectedSlot(Module owner) {
        if (owner == null) return -1;
        DeferredFeatureAction top = actions.peek();
        return top == null || top.owner != owner ? -1 : top.selectedSlot;
    }

    public static int selectedSlot() {
        if (CLIENT.player == null) return -1;
        DeferredFeatureAction top = actions.peek();
        return top == null ? CLIENT.player.getInventory().getSelectedSlot() : top.selectedSlot;
    }

    public static void finish(Module owner) { finish(owner, true); }

    
    public static void finish(Module owner, boolean restoreAll) {
        if (owner == null || actions.isEmpty() || !contains(owner)) return;
        if (restoreAll) {
            restoreAndDrain(actions.peekLast());
            return;
        }
        while (!actions.isEmpty()) {
            DeferredFeatureAction action = actions.pop();
            finishAction(action, false, true);
            if (action.owner == owner) break;
        }
    }

    
    public static void drop(Module owner) {
        if (owner != null) actions.removeIf(action -> action.owner == owner);
    }

    
    public static void clear() { actions.clear(); }

    @Subscribe(priority = EventPriority.LOWEST)
    private void beforeInput(HandleInputEvent.Pre event) { executePhase(ActionPhase.EARLY); }

    @Subscribe(priority = EventPriority.LOWEST)
    private void afterInput(HandleInputEvent.Post event) { executePhase(ActionPhase.POST); }

    @Subscribe
    private void afterAttack(EntityAttackEvent.Post event) { switchingBlocked = true; }

    @Subscribe
    private void onPacketSent(PacketSendEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerActionC2SPacket action && action.getAction() == PlayerActionC2SPacket.Action.STAB) {
            switchingBlocked = true;
        } else if (event.getPacket() instanceof PlayerMoveC2SPacket || event.getPacket() instanceof ClientTickEndC2SPacket) {
            switchingBlocked = false;
        }
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        if (event.getPacket() instanceof PlayerRespawnS2CPacket) {
            clear();
        } else if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            CLIENT.execute(() -> { if (!hasFiniteDeadline()) clear(); });
        }
    }

    private static boolean hasFiniteDeadline() {
        for (DeferredFeatureAction action : actions) if (action.expiresAtTick != Long.MAX_VALUE) return true;
        return false;
    }

    private static void executePhase(ActionPhase phase) {
        if (CLIENT.player == null) { clear(); return; }
        ActionCoordinator coordinator = ActionCoordinator.get();
        while (!actions.isEmpty()) {
            DeferredFeatureAction action = actions.peek();
            if (!action.isDue(CLIENT.player.age, phase)) break;
            if (action.restoreAll && CLIENT.player.getInventory().getSelectedSlot() != action.baselineSlot) {
                if (switchingBlocked || CLIENT.player.isUsingItem()
                        || (coordinator != null && (!coordinator.canSwapSlotNow() || coordinator.isSlotPending()))) {
                    break;
                }
            }
            actions.pop();
            if (action.restoreAll) {
                restoreAndDrain(action);
                runCallback(action);
                return;
            }
            finishAction(action, false, false);
        }
    }

    private static void restoreAndDrain(DeferredFeatureAction fallback) {
        DeferredFeatureAction baseline = actions.peekLast();
        if (baseline == null) baseline = fallback;
        if (baseline != null && CLIENT.player != null) setHotbarSlot(baseline.owner, baseline.baselineSlot, true);
        while (!actions.isEmpty()) runCallback(actions.pop());
    }

    private static void finishAction(DeferredFeatureAction action, boolean restoreSlot, boolean forceRestore) {
        if (action == null) return;
        if (restoreSlot && CLIENT.player != null && (forceRestore || action.restoreAll)) setHotbarSlot(action.owner, action.baselineSlot, true);
        runCallback(action);
    }

    private static void runCallback(DeferredFeatureAction action) {
        if (action.callback == null) return;
        try {
            action.callback.run();
        } catch (Throwable ignored) {
        }
    }

    private static boolean setHotbarSlot(Module owner, int slot, boolean bypassDeferredOwnership) {
        if (slot < 0 || slot > 8 || CLIENT.player == null) return false;
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null) {
            return coordinator.executeSlotSelect(owner, slot, bypassDeferredOwnership).succeeded();
        }
        if (CLIENT.player.getInventory().getSelectedSlot() != slot) CLIENT.player.getInventory().setSelectedSlot(slot);
        return CLIENT.player.getInventory().getSelectedSlot() == slot;
    }
}
