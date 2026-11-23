package com.blamejared.clumps.client.runtime.api;

import com.blamejared.clumps.client.utils.items.PlayerInventoryView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class PlayerView extends EntityView {
    private final PlayerEntity player;

    
    public PlayerView(PlayerEntity player) {
        super(player);
        this.player = player;
    }

    
    public Vector3View getVelocity() { return new Vector3View(player.getVelocity()); }

    
    public boolean isOnGround() { return player.isOnGround(); }

    
    public float getHealth() { return player.getHealth(); }

    
    public boolean isSprinting() { return player.isSprinting(); }

    
    public boolean isUsingItem() { return player.isUsingItem(); }

    
    public boolean isTouchingWater() { return player.isTouchingWater(); }

    
    public boolean isDead() { return player.isDead(); }

    
    public boolean isGliding() { return player.isGliding(); }

    
    public boolean isCreative() { return player.isCreative(); }

    
    public boolean isSneaking() { return player.isSneaking(); }

    
    public float getAttackCooldownProgress() { return player.getAttackCooldownProgress(0.5F); }

    
    public double getBlockInteractionRange() { return player.getBlockInteractionRange(); }

    
    public PlayerInventoryView getInventory() { return new PlayerInventoryView(player.getInventory()); }

    
    public void swingHand(String hand) { player.swingHand(parseHand(hand)); }

    
    public void setRotation(float yaw, float pitch) {
        player.setYaw(yaw);
        player.setPitch(pitch);
    }

    
    public void setSprinting(boolean sprinting) { player.setSprinting(sprinting); }

    
    public void setSelectedSlot(int slot) {
        var coordinator = com.blamejared.clumps.client.handlers.action.ActionCoordinator.get();
        if (coordinator != null) {
            coordinator.executeSlotSelect(null, slot);
            return;
        }
        player.getInventory().setSelectedSlot(slot);
    }

    
    public void setVelocity(double x, double y, double z) { player.setVelocity(new Vec3d(x, y, z)); }

    
    public void clearActiveItem() { player.clearActiveItem(); }

    
    public void interactMainHand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null) return;
        var coordinator = com.blamejared.clumps.client.handlers.action.ActionCoordinator.get();
        if (coordinator != null) {
            coordinator.executeItemUse(null, Hand.MAIN_HAND);
            return;
        }
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
    }

    
    public void attack(EntityView target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null || target == null) return;
        var coordinator = com.blamejared.clumps.client.handlers.action.ActionCoordinator.get();
        if (coordinator != null) {
            coordinator.executeEntityAttack(null, target.entity, false);
            return;
        }
        client.interactionManager.attackEntity(player, target.entity);
    }

    private Hand parseHand(String hand) {
        return "off".equalsIgnoreCase(hand) ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }
}
