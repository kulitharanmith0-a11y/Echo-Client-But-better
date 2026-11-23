package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.input.SelectedSlotEvent;
import com.blamejared.clumps.client.event.lifecycle.ScreenChangeEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.utils.inventory.InventoryScreenSlots;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.ExplosionDamage;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttributes;

public class TotemHand extends Module {
    private static final double CRYSTAL_RANGE = 12.0;
    private static final int ANCHOR_RANGE = 10;
    private static BooleanSetting onPop;
    private static BooleanSetting predictive;
    private static BooleanSetting crystals;
    private static BooleanSetting anchors;
    private static BooleanSetting killable;
    private static IntSetting minDamage;
    private static BooleanSetting swapBackOnSafe;
    private static BooleanSetting onInventoryOpen;
    private static BooleanSetting swapBackOnClose;
    private static IntSetting inventoryChance;
    private int slotBeforeInventory;
    private int inventoryTotemSlot;
    private int predictedTotemSlot;
    private long predictionTick;
    private boolean protectingMainHand;

    public TotemHand() {
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Totem Hand"), com.blamejared.clumps.client.init.CatalogText.literal("Swaps a totem into your main hand on pop, on inventory open, or when explosion damage is predicted"), com.blamejared.clumps.client.modules.ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        slotBeforeInventory = -1;
        inventoryTotemSlot = -1;
        predictedTotemSlot = -1;
        predictionTick = -1L;
        crystals.setVisibilityPredicate(ignored -> predictive.getValue());
        anchors.setVisibilityPredicate(ignored -> predictive.getValue());
        killable.setVisibilityPredicate(ignored -> predictive.getValue());
        minDamage.setVisibilityPredicate(ignored -> predictive.getValue() && !killable.getValue());
        swapBackOnSafe.setVisibilityPredicate(ignored -> predictive.getValue());
        swapBackOnClose.setVisibilityPredicate(ignored -> onInventoryOpen.getValue());
        inventoryChance.setVisibilityPredicate(ignored -> onInventoryOpen.getValue());
        predictive.tooltip("Prediction");
        crystals.tooltip("Prediction");
        anchors.tooltip("Prediction");
        killable.tooltip("Prediction");
        minDamage.tooltip("Prediction");
        swapBackOnSafe.tooltip("Prediction");
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearInventoryRestore();
        clearPrediction();
    }

    private int findTotemSlot() {
        return InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.TOTEM_OF_UNDYING);
    }

    @Subscribe
    private void onPacket(PacketReceiveEvent event) {
        if (isOutsideWorld() || !onPop.getValue()) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket status)) return;
        if (status.getStatus() != 35) return;
        Entity entity = status.getEntity(client.world);
        if (!(entity instanceof PlayerEntity) || entity != client.player) return;
        int slot = findTotemSlot();
        if (slot != -1) InventoryItems.selectHotbarSlot(slot);
    }

    @Subscribe
    private void onScreenChange(ScreenChangeEvent event) {
        if (isOutsideWorld()) {
            clearInventoryRestore();
            return;
        }
        if (!InventoryScreenSlots.isPlayerInventoryScreen(event.getScreen())) {
            restoreAfterInventory();
            return;
        }
        clearInventoryRestore();
        if (!onInventoryOpen.getValue()) return;
        if (Math.random() * 100.0 >= inventoryChance.getValue()) return;
        int slot = findTotemSlot();
        if (slot == -1) return;
        int current = client.player.getInventory().getSelectedSlot();
        if (swapBackOnClose.getValue() && slot != current) {
            slotBeforeInventory = current;
            inventoryTotemSlot = slot;
        }
        InventoryItems.selectHotbarSlot(slot);
    }

    private void restoreAfterInventory() {
        int previous = slotBeforeInventory;
        int totem = inventoryTotemSlot;
        clearInventoryRestore();
        if (!swapBackOnClose.getValue()) return;
        if (previous < 0 || previous > 8) return;
        if (client.player.getInventory().getSelectedSlot() != totem) return;
        InventoryItems.selectHotbarSlot(previous);
    }

    private void clearInventoryRestore() {
        slotBeforeInventory = -1;
        inventoryTotemSlot = -1;
    }

    private void updatePrediction() {
        if (client.player.age == predictionTick) return;
        predictionTick = client.player.age;
        if (!predictive.getValue() || client.player.isCreative() || client.player.isSpectator()
                || !hasDangerousExplosion()) {
            clearPrediction();
            return;
        }
        boolean holdingTotem = client.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING;
        predictedTotemSlot = holdingTotem ? -1 : findTotemSlot();
        protectingMainHand = holdingTotem || predictedTotemSlot != -1;
    }

    @Subscribe
    private void onSelectedSlot(SelectedSlotEvent event) {
        if (isOutsideWorld() || !protectingMainHand) return;
        int slot = event.getSelectedSlot();
        if (slot < 0 || slot > 8) return;
        if (client.player.getInventory().getStack(slot).getItem() == Items.TOTEM_OF_UNDYING) return;
        event.cancel();
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (isOutsideWorld()) return;
        updatePrediction();
        if (predictedTotemSlot == -1 || event.slot != predictedTotemSlot) return;
        if (DeferredActionQueue.selectedSlot() == predictedTotemSlot) return;
        if (DeferredActionQueue.ensureSelected(this, predictedTotemSlot, -1, true, null)) {
            event.addClick();
        }
    }

    private boolean hasDangerousExplosion() {
        float threshold = killable.getValue()
                ? client.player.getHealth() + client.player.getAbsorptionAmount() : minDamage.getValue();
        if (crystals.getValue()) {
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof EndCrystalEntity)) continue;
                if (client.player.squaredDistanceTo(entity) > CRYSTAL_RANGE * CRYSTAL_RANGE) continue;
                if (ExplosionDamage.crystalDamage(client.player, entity.getEntityPos()) >= threshold) return true;
            }
        }
        if (anchors.getValue()) {
            BlockPos center = client.player.getBlockPos();
            for (BlockPos pos : BlockPos.iterate(center.add(-ANCHOR_RANGE, -ANCHOR_RANGE, -ANCHOR_RANGE), center.add(ANCHOR_RANGE, ANCHOR_RANGE, ANCHOR_RANGE))) {
                if (!client.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) continue;
                if (client.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) == 0) continue;
                if (client.world.getEnvironmentAttributes().getAttributeValue(
                        EnvironmentAttributes.RESPAWN_ANCHOR_WORKS_GAMEPLAY, pos)) continue;
                if (ExplosionDamage.anchorDamage(client.player, pos, null) >= threshold) return true;
            }
        }
        return false;
    }

    public boolean isProtectingMainHand() {
        return isEnabled() && protectingMainHand;
    }

    public boolean isExplosionThreatPresent() {
        if (!isEnabled() || isOutsideWorld() || !predictive.getValue()) return false;
        if (client.player.isCreative() || client.player.isSpectator()) return false;
        return hasDangerousExplosion();
    }

    private void clearPrediction() {
        predictedTotemSlot = -1;
        protectingMainHand = false;
        if (DeferredActionQueue.contains(this)) {
            DeferredActionQueue.finish(this, swapBackOnSafe.getValue());
        }
    }

    
    public static void appendBootstrapSettings(TotemHand Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.onPop);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.predictive);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.crystals);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.anchors);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.killable);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.minDamage);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.swapBackOnSafe);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.onInventoryOpen);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.swapBackOnClose);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TotemHand.inventoryChance);
    }

    
    public static void assignBootstrapSettings(TotemHand Module, com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBackOnClose, com.blamejared.clumps.client.settings.BooleanSetting preparedAnchors, com.blamejared.clumps.client.settings.IntSetting preparedInventoryChance, com.blamejared.clumps.client.settings.BooleanSetting preparedPredictive, com.blamejared.clumps.client.settings.BooleanSetting preparedKillable, com.blamejared.clumps.client.settings.IntSetting preparedMinDamage, com.blamejared.clumps.client.settings.BooleanSetting preparedCrystals, com.blamejared.clumps.client.settings.BooleanSetting preparedOnPop, com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBackOnSafe, com.blamejared.clumps.client.settings.BooleanSetting preparedOnInventoryOpen) {
        TotemHand.swapBackOnClose = preparedSwapBackOnClose;
        TotemHand.anchors = preparedAnchors;
        TotemHand.inventoryChance = preparedInventoryChance;
        TotemHand.predictive = preparedPredictive;
        TotemHand.killable = preparedKillable;
        TotemHand.minDamage = preparedMinDamage;
        TotemHand.crystals = preparedCrystals;
        TotemHand.onPop = preparedOnPop;
        TotemHand.swapBackOnSafe = preparedSwapBackOnSafe;
        TotemHand.onInventoryOpen = preparedOnInventoryOpen;
    }
}
