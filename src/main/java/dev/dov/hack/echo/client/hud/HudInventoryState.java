package dev.dov.hack.echo.client.hud;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class HudInventoryState {
    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private final ItemStack[] hotbar = new ItemStack[9];
    private final ItemStack[] armor = new ItemStack[4];
    private ItemStack offhand = ItemStack.EMPTY;
    private int selectedSlot;
    private float attackCooldown = 1.0f;
    private PlayerEntity player;

    
    public HudInventoryState() {
        reset();
    }

    
    public void update(PlayerEntity player) {
        this.player = player;
        if (player == null) {
            reset();
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            hotbar[slot] = player.getInventory().getStack(slot);
        }
        selectedSlot = Math.max(0, Math.min(8, player.getInventory().getSelectedSlot()));
        for (int slot = 0; slot < 4; slot++) {
            armor[slot] = player.getEquippedStack(ARMOR_ORDER[slot]);
        }
        offhand = player.getOffHandStack();
    }

    
    public void reset() {
        for (int slot = 0; slot < 9; slot++) hotbar[slot] = ItemStack.EMPTY;
        for (int slot = 0; slot < 4; slot++) armor[slot] = ItemStack.EMPTY;
        offhand = ItemStack.EMPTY;
        selectedSlot = 0;
        attackCooldown = 1.0f;
        player = null;
    }

    
    public ItemStack hotbar(int slot) { return hotbar[slot]; }
    
    public ItemStack armor(int slot) { return armor[slot]; }
    
    public ItemStack offhand() { return offhand; }
    
    public int selectedSlot() { return selectedSlot; }
    
    public PlayerEntity player() { return player; }
    
    public float attackCooldown() { return attackCooldown; }
    
    public void setAttackCooldown(float value) { attackCooldown = value; }
}
