package com.blamejared.clumps.client.keyboard;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.event.EventPhase;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.DropKeyEvent;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.input.KeybindingEvent;
import com.blamejared.clumps.client.event.input.PickItemKeyEvent;
import com.blamejared.clumps.client.event.input.SwapHandsKeyEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.mixin.accessor.KeyBindingAccessor;
import java.util.function.IntFunction;
import net.minecraft.client.option.KeyBinding;

public final class KeybindingEvents {
    private static boolean attackHandled;
    private static boolean useHandled;
    private static boolean pickBlockHandled;
    private static boolean swapHandsHandled;
    private static boolean dropHandled;
    private static int nextHotbarSlot;

    private KeybindingEvents() {
    }

    
    public static void beginInputCycle() {
        attackHandled = false;
        useHandled = false;
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        pickBlockHandled = false;
        swapHandsHandled = false;
        dropHandled = false;
        nextHotbarSlot = 0;
        if (Event.getEventCounter() != 0) {
            EventPhase.setDispatchState(!dispatchStateClear);
        }
    }

    public static void onAttack() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        boolean handled = attackHandled;
        if (!dispatchStateClear) {
            if (handled) return;
            handled = true;
        }
        attackHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.attackKey, AttackKeyEvent::new);
    }

    public static void onUse() {
        boolean dispatchState = EventPhase.getDispatchState();
        boolean handled = useHandled;
        if (dispatchState) {
            if (handled) return;
            handled = true;
        }
        useHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.useKey, UseKeyEvent::new);
    }

    public static void onPickBlock() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        boolean handled = pickBlockHandled;
        if (!dispatchStateClear) {
            if (handled) return;
            handled = true;
        }
        pickBlockHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.pickItemKey, PickItemKeyEvent::new);
    }

    public static void onSwapHands() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        boolean handled = swapHandsHandled;
        if (!dispatchStateClear) {
            if (handled) return;
            handled = true;
        }
        swapHandsHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.swapHandsKey, SwapHandsKeyEvent::new);
    }

    public static void onDrop() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        boolean handled = dropHandled;
        if (!dispatchStateClear) {
            if (handled) return;
            handled = true;
        }
        dropHandled = handled;
        hasAction(MinecraftAccess.CLIENT.options.dropKey, DropKeyEvent::new);
    }

    
    public static void onHotbarSlot() {
        boolean dispatchStateClear = EventPhase.isDispatchStateClear();
        int slot = nextHotbarSlot++;
        if (!dispatchStateClear && slot < 0) return;
        if (slot >= MinecraftAccess.CLIENT.options.hotbarKeys.length) return;
        hasAction(MinecraftAccess.CLIENT.options.hotbarKeys[slot], clickCount -> new HotbarKeyEvent(slot, clickCount));
    }

    
    private static void hasAction(KeyBinding binding, IntFunction<KeybindingEvent> factory) {
        boolean dispatchState = EventPhase.getDispatchState();
        if (ClumpsClient.eventsDisabled || binding == null || MinecraftAccess.CLIENT.player == null) return;
        KeyBindingAccessor accessor = (KeyBindingAccessor) binding;
        KeybindingEvent event = factory.apply(accessor.getTimesPressed());
        int clickCount;
        if (dispatchState) {
            int initialClicks = accessor.getTimesPressed();
            event.post();
            if (!event.cancelled && event.getClickCountDelta() > 0) {
                ActionCoordinator coordinator = ActionCoordinator.get();
                if (coordinator != null && !coordinator.validateSyntheticKeyAction(event)) {
                    event.setClickCount(initialClicks);
                }
            }
            InputState.applyExtraClicks(event);
            clickCount = event.cancelled ? 0 : event.getClickCount();
        } else {
            clickCount = event.getClickCount();
        }
        if (clickCount != accessor.getTimesPressed()) {
            accessor.setTimesPressed(clickCount);
        }
    }
}
