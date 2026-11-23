package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.input.SelectedSlotEvent;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Unique private int clumps$previousSlot = -1;

    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void onSetSelectedSlotHead(int selectedSlot, CallbackInfo callback) {
        if (!ClumpsClient.eventsDisabled) {
            PlayerInventory inventory = (PlayerInventory) (Object) this;
            int previousSlot = inventory.getSelectedSlot();
            clumps$previousSlot = previousSlot;
            if (previousSlot != selectedSlot) {
                SelectedSlotEvent event = new SelectedSlotEvent(previousSlot, selectedSlot);
                event.post();
                if (event.cancelled) {
                    clumps$previousSlot = -1;
                    callback.cancel();
                }
            }
        }
    }

    @Inject(method = "setSelectedSlot", at = @At("RETURN"))
    private void onSetSelectedSlotReturn(int selectedSlot, CallbackInfo callback) {
        int previousSlot = clumps$previousSlot;
        clumps$previousSlot = -1;
        if (ClumpsClient.eventsDisabled || previousSlot == -1 || previousSlot == selectedSlot) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && (Object) this == client.player.getInventory()) {
            ActionCoordinator coordinator = ActionCoordinator.get();
            if (coordinator != null && client.player.getInventory().getSelectedSlot() == selectedSlot) {
                coordinator.afterSelectedSlotChanged(previousSlot, selectedSlot);
            }
        }
    }
}
