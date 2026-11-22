package dev.dov.hack.echo.client.mixin;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.input.SelectedSlotEvent;
import dev.dov.hack.echo.client.handlers.action.ActionCoordinator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Unique private int echo$previousSlot = -1;

    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void onSetSelectedSlotHead(int selectedSlot, CallbackInfo callback) {
        if (!EchoClient.eventsDisabled) {
            PlayerInventory inventory = (PlayerInventory) (Object) this;
            int previousSlot = inventory.getSelectedSlot();
            echo$previousSlot = previousSlot;
            if (previousSlot != selectedSlot) {
                SelectedSlotEvent event = new SelectedSlotEvent(previousSlot, selectedSlot);
                event.post();
                if (event.cancelled) {
                    echo$previousSlot = -1;
                    callback.cancel();
                }
            }
        }
    }

    @Inject(method = "setSelectedSlot", at = @At("RETURN"))
    private void onSetSelectedSlotReturn(int selectedSlot, CallbackInfo callback) {
        int previousSlot = echo$previousSlot;
        echo$previousSlot = -1;
        if (EchoClient.eventsDisabled || previousSlot == -1 || previousSlot == selectedSlot) {
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
