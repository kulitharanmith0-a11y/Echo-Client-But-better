package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.config.NameRegistry;
import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.utils.WeakRefSet;
import com.blamejared.clumps.client.utils.inventory.InventoryActionScheduler;
import com.blamejared.clumps.client.utils.inventory.TemporaryItemUse;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoTotem extends Module {
    private static final CharSequence HEALTH_GROUP = CatalogText.literal("Health Swap");
    private static final CharSequence restoreItemOption = CatalogText.literal("Previous");
    private static final CharSequence restoreItemOption2 = CatalogText.literal("Shield");
    private static final int OFFHAND_SLOT = 40;
    private static final Item TOTEM = Items.TOTEM_OF_UNDYING;
    private static BooleanSetting refillHotbar;
    private static IntSetting slotToRefillTotem;
    private static BooleanSetting healthSwap;
    private static IntSetting equipBelow;
    private static IntSetting restoreAbove;
    private static ChoiceSetting restoreItem;
    private InventoryActionScheduler inventoryActions;
    private TemporaryItemUse temporaryItemUse;
    private Item replacedOffhandItem;
    private Item cachedDesiredItem;
    private long desiredItemTick;

    public AutoTotem() {
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Auto Totem"), com.blamejared.clumps.client.init.CatalogText.literal("Automatically equips a totem in your offhand"), com.blamejared.clumps.client.modules.ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        inventoryActions = new InventoryActionScheduler(CatalogText.literal("Mode"), this::getTransferItem, this::getDestinationSlot, this::canRunInventoryAction, () -> true, true).withOwner(this).withSlotPredicate(this::isSourceSlotAllowed).onTransfer(this::onInventoryTransfer);
        temporaryItemUse = new TemporaryItemUse(this, this::isSwapStillNeeded, this::onHandSwap);
        desiredItemTick = -1L;
        slotToRefillTotem.setVisibilityPredicate(ignored -> refillHotbar.getValue());
        equipBelow.setVisibilityPredicate(ignored -> healthSwap.getValue());
        restoreAbove.setVisibilityPredicate(ignored -> healthSwap.getValue());
        restoreItem.setVisibilityPredicate(ignored -> healthSwap.getValue());
        healthSwap.description(CatalogText.literal("Hold a totem only while your health is low and put your offhand item back once you heal"));
        equipBelow.description(CatalogText.literal("Swap a totem into your offhand at or below this health. Absorption is not counted"));
        restoreAbove.description(CatalogText.literal("Put the restore item back once your health is above this. Between the two values the offhand is left alone"));
        restoreItem.description(CatalogText.literal("Previous puts back whatever the totem replaced, Shield always puts a shield back"));
        if (!settings.contains(inventoryActions.getSettings().get(0))) {
            settings.addAll(0, inventoryActions.getSettings());
        }
        refillHotbar.tooltip("Appearance");
        slotToRefillTotem.tooltip("Appearance");
        healthSwap.tooltip(HEALTH_GROUP).restrict();
        equipBelow.tooltip(HEALTH_GROUP);
        restoreAbove.tooltip(HEALTH_GROUP);
        restoreItem.tooltip(HEALTH_GROUP);
    }

    @Override
    public void onEnable() {
        resetState();
        inventoryActions.reset();
        EventBus.register(inventoryActions);
        EventBus.register(temporaryItemUse);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        EventBus.unregister(inventoryActions);
        EventBus.unregister(temporaryItemUse);
        inventoryActions.reset();
        temporaryItemUse.cancelAndRestore();
        resetState();
        super.onDisable();
    }

    
    public boolean shouldAllowTotemRemoval() {
        return temporaryItemUse.isSwapKeyActive();
    }

    @Subscribe
    private void onWorldContext(WorldTickContextEvent event) {
        resetState();
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld() || client.interactionManager == null) return;
        if (client.player.isDead()) {
            resetState();
            return;
        }
        if (!healthSwap.getValue() || temporaryItemUse.isBusy()) return;
        if (WeakRefSet.hasOwners() || inventoryActions.isActive()) return;
        Item desired = getDesiredOffhandItem();
        if (desired == null) return;
        int slot = findSwapSlot(desired);
        if (slot != -1) temporaryItemUse.beginSwap(slot, desired);
    }

    private boolean canRunInventoryAction() {
        return !temporaryItemUse.isBusy() && !WeakRefSet.hasOwners();
    }

    private int getDestinationSlot() {
        if (temporaryItemUse.isBusy()) return -1;
        Item desired = getDesiredOffhandItem();
        if (desired != null) return shouldUseInventory(desired) ? OFFHAND_SLOT : -1;
        if (!refillHotbar.getValue()) return -1;
        int slot = slotToRefillTotem.getValue() - 1;
        return client.player.getInventory().getStack(slot).isOf(TOTEM) ? -1 : slot;
    }

    private boolean shouldUseInventory(Item item) {
        return inventoryActions.isActive() || findSwapSlot(item) == -1;
    }

    private Item getTransferItem() {
        if (isOutsideWorld()) return TOTEM;
        Item desired = getDesiredOffhandItem();
        return desired != null ? desired : TOTEM;
    }

    private boolean isSourceSlotAllowed(int slot) {
        if (!healthSwap.getValue() || getTransferItem() != TOTEM) return true;
        return slot != selectedSlot();
    }

    private Item getDesiredOffhandItem() {
        if (desiredItemTick == client.player.age) return cachedDesiredItem;
        desiredItemTick = client.player.age;
        cachedDesiredItem = chooseOffhandItem();
        return cachedDesiredItem;
    }

    private void onHandSwap(Item item) {
        desiredItemTick = -1L;
        announceTransfer(item);
    }

    private void onInventoryTransfer(int slot, Item item) {
        if (slot == OFFHAND_SLOT && healthSwap.getValue()) announceTransfer(item);
    }

    private void announceTransfer(Item item) {
        if (item == TOTEM) {
            NameRegistry.postNotification(this, CatalogText.literal("Swapped a totem into your offhand"));
            return;
        }
        String name = new ItemStack(item).getName().getString();
        NameRegistry.postNotification(this, new StringBuilder("Put your ").append(name).append(" back"));
    }

    private Item chooseOffhandItem() {
        ItemStack offhand = client.player.getOffHandStack();
        if (!healthSwap.getValue()) return offhand.isOf(TOTEM) ? null : TOTEM;
        if (shouldEquipForHealth()) return chooseEmergencyTotem(offhand);
        if (shouldRestoreForHealth() && replacedOffhandItem != null && offhand.isOf(replacedOffhandItem)) {
            replacedOffhandItem = null;
        }
        Item restore = chooseRestoreItem(offhand);
        if (restore != null) return restore;
        return shouldFillOffhand(offhand) && hasUnselectedTotem() ? TOTEM : null;
    }

    private Item chooseEmergencyTotem(ItemStack offhand) {
        if (offhand.isOf(TOTEM) || !hasUnselectedTotem()) return null;
        if (!offhand.isEmpty()) replacedOffhandItem = offhand.getItem();
        return TOTEM;
    }

    private Item chooseRestoreItem(ItemStack offhand) {
        if (!shouldRestoreForHealth()) return null;
        if (!offhand.isEmpty() && !offhand.isOf(TOTEM)) return null;
        Item item = restoreItem.isSelected(restoreItemOption2) ? Items.SHIELD : replacedOffhandItem;
        if (item == null || !containsItem(item)) return null;
        return isTotemHandThreatPresent() ? null : item;
    }

    private boolean shouldFillOffhand(ItemStack offhand) {
        if (offhand.isOf(TOTEM)) return false;
        return shouldEquipForHealth() || offhand.isEmpty() && chooseRestoreItem(offhand) == null;
    }

    private boolean isSwapStillNeeded(Item desired) {
        if (!healthSwap.getValue() || WeakRefSet.hasOwners()) return false;
        ItemStack offhand = client.player.getOffHandStack();
        if (desired == TOTEM) return shouldFillOffhand(offhand);
        return chooseRestoreItem(offhand) == desired;
    }

    private int findSwapSlot(Item item) {
        if (!healthSwap.getValue() || client.currentScreen != null) return -1;
        if (isUsingMainHand() || isTotemHandProtecting()) return -1;
        if (item != TOTEM && client.player.getMainHandStack().isOf(item)) return selectedSlot();
        for (int slot = 0; slot < 9; ++slot) {
            if (slot == selectedSlot()) continue;
            if (client.player.getInventory().getStack(slot).isOf(item)) return slot;
        }
        return -1;
    }

    private boolean shouldEquipForHealth() {
        return client.player.getHealth() <= equipBelow.getValue();
    }

    private boolean shouldRestoreForHealth() {
        return client.player.getHealth() > Math.max(restoreAbove.getValue(), equipBelow.getValue());
    }

    private boolean hasUnselectedTotem() {
        for (int slot = 0; slot < 36; ++slot) {
            if (slot == selectedSlot()) continue;
            if (client.player.getInventory().getStack(slot).isOf(TOTEM)) return true;
        }
        return false;
    }

    private boolean containsItem(Item item) {
        for (int slot = 0; slot < 36; ++slot) {
            if (client.player.getInventory().getStack(slot).isOf(item)) return true;
        }
        return false;
    }

    private boolean isUsingMainHand() {
        return client.player.isUsingItem() && client.player.getActiveHand() == Hand.MAIN_HAND;
    }

    private boolean isTotemHandProtecting() {
        TotemHand Module = (TotemHand) ClumpsClient.ModuleManager.findFeature(TotemHand.class);
        return Module != null && Module.isProtectingMainHand();
    }

    private boolean isTotemHandThreatPresent() {
        TotemHand Module = (TotemHand) ClumpsClient.ModuleManager.findFeature(TotemHand.class);
        return Module != null && Module.isExplosionThreatPresent();
    }

    private int selectedSlot() {
        return client.player.getInventory().getSelectedSlot();
    }

    private void resetState() {
        temporaryItemUse.reset();
        replacedOffhandItem = null;
        cachedDesiredItem = null;
        desiredItemTick = -1L;
    }

    
    public static void appendBootstrapSettings(AutoTotem Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, AutoTotem.refillHotbar);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, AutoTotem.slotToRefillTotem);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, AutoTotem.healthSwap);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, AutoTotem.equipBelow);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, AutoTotem.restoreAbove);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, AutoTotem.restoreItem);
    }

    
    public static java.lang.CharSequence bootstrapRestoreItemOption() {
        return restoreItemOption;
    }

    
    public static java.lang.CharSequence bootstrapRestoreItemOption2() {
        return restoreItemOption2;
    }

    
    public static void assignBootstrapSettings(AutoTotem Module, com.blamejared.clumps.client.settings.BooleanSetting preparedHealthSwap, com.blamejared.clumps.client.settings.ChoiceSetting preparedRestoreItem, com.blamejared.clumps.client.settings.IntSetting preparedSlotToRefillTotem, com.blamejared.clumps.client.settings.IntSetting preparedRestoreAbove, com.blamejared.clumps.client.settings.IntSetting preparedEquipBelow, com.blamejared.clumps.client.settings.BooleanSetting preparedRefillHotbar) {
        AutoTotem.healthSwap = preparedHealthSwap;
        AutoTotem.restoreItem = preparedRestoreItem;
        AutoTotem.slotToRefillTotem = preparedSlotToRefillTotem;
        AutoTotem.restoreAbove = preparedRestoreAbove;
        AutoTotem.equipBelow = preparedEquipBelow;
        AutoTotem.refillHotbar = preparedRefillHotbar;
    }
}
