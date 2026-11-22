package dev.dov.hack.echo.client.utils.items;

import dev.dov.hack.echo.client.runtime.api.ItemStackView;
import dev.dov.hack.echo.client.utils.inventory.SlotAction;
import dev.dov.hack.echo.client.utils.inventory.SlotPackets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;

public class PlayerInventoryView {
    private final PlayerInventory inventory;

    
    public PlayerInventoryView(PlayerInventory inventory) {
        this.inventory = inventory;
    }

    
    public ItemStackView getStack(int slot) {
        return new ItemStackView(inventory.getStack(slot));
    }

    
    public int size() {
        return inventory.size();
    }

    
    public int getSelectedSlot() {
        return inventory.getSelectedSlot();
    }

    
    public ItemStackView getSelectedStack() {
        return new ItemStackView(inventory.getStack(inventory.getSelectedSlot()));
    }

    
    public void swap(int inventorySlot, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null || client.player == null) {
            return;
        }
        PlayerScreenHandler screenHandler = client.player.playerScreenHandler;
        SlotPackets.clickSlot(
                screenHandler.syncId, toScreenSlot(inventorySlot), button, SlotAction.SWAP, client.player);
    }

    
    private int toScreenSlot(int slot) {
        if (slot <= 8) {
            return 36 + slot;
        }
        if (slot <= 35) {
            return slot;
        }
        if (slot <= 39) {
            return 44 - slot;
        }
        return 45;
    }
}
