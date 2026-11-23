package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.mixin.accessor.HandledScreenAccessor;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.utils.CooldownTimer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class PotRefill extends Module {
    private static final CharSequence HOVER = CatalogText.literal("Hover");
    private static final CharSequence SHIFT_LEFT_CLICK = CatalogText.literal("ShiftLMB");
    private static final CharSequence AUTO = CatalogText.literal("Auto");
    private static ChoiceSetting mode;
    private static BooleanSetting refillBuffPotions;
    private static FloatRangeSetting delay;
    private CooldownTimer refillTimer;

    public PotRefill() {
        super(new ModuleInfo(CatalogText.literal("Pot Refill"), CatalogText.literal("Automatically refills potions"), ModuleCategory.UTILITY));
    }

    public static void appendBootstrapSettings(PotRefill Module) {
        CatalogSettings.append(Module, mode);
        CatalogSettings.append(Module, refillBuffPotions);
        CatalogSettings.append(Module, delay);
    }

    public static CharSequence bootstrapModeOption3() { return HOVER; }
    public static CharSequence bootstrapModeOption() { return SHIFT_LEFT_CLICK; }
    public static CharSequence bootstrapModeOption2() { return AUTO; }

    public static void assignBootstrapSettings(PotRefill Module, ChoiceSetting preparedMode, BooleanSetting preparedRefillBuffPotions, FloatRangeSetting preparedDelay) {
        mode = preparedMode;
        refillBuffPotions = preparedRefillBuffPotions;
        delay = preparedDelay;
    }

    @Override
    public void postInitialize() {
        refillTimer = new CooldownTimer();
        refillBuffPotions.tooltip("Appearance");
        delay.tooltip("Timing");
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld()) return;
        if (!(MinecraftClient.getInstance().currentScreen instanceof InventoryScreen inventory)) return;
        if (mode.isSelected(HOVER)) {
            refillHovered(inventory);
        } else if (mode.isSelected(SHIFT_LEFT_CLICK)) {
            refillShiftClicked(inventory);
        } else if (mode.isSelected(AUTO)) {
            refillAutomatically(inventory);
        }
    }

    private void refillHovered(InventoryScreen inventory) {
        Slot slot = ((HandledScreenAccessor) inventory).getFocusedSlot();
        if (slot == null) return;
        ItemStack stack = slot.getStack();
        if (!shouldRefill(stack)) return;
        if (isHotbarIndex(slot.getIndex())) return;
        int destination = findEmptyHotbarSlot();
        if (destination == -1) return;
        if (!refillTimer.hasElapsedMillis((double) delay.sampleGaussian())) return;
        refillTimer.reset();
        swapIntoHotbar(this, inventory.getScreenHandler().syncId, slot.getIndex(), destination, client.player);
    }

    private void refillShiftClicked(InventoryScreen inventory) {
        Slot slot = ((HandledScreenAccessor) inventory).getFocusedSlot();
        if (slot == null) return;
        if (!InputKey.LEFT_SHIFT.isPressed()) return;
        if (!InputKey.MOUSE_LEFT.isPressed()) return;
        ItemStack stack = slot.getStack();
        if (!shouldRefill(stack)) return;
        if (isHotbarIndex(slot.getIndex())) return;
        int destination = findEmptyHotbarSlot();
        if (destination == -1) return;
        if (!refillTimer.hasElapsedMillis((double) delay.sampleGaussian())) return;
        refillTimer.reset();
        swapIntoHotbar(this, inventory.getScreenHandler().syncId, slot.getIndex(), destination, client.player);
    }

    private void refillAutomatically(InventoryScreen inventory) {
        if (!refillTimer.hasElapsedMillis((double) delay.sampleGaussian())) return;
        refillTimer.reset();
        for (int slot = 9; slot < 45; ++slot) {
            ItemStack stack = inventory.getScreenHandler().getSlot(slot).getStack();
            if (!shouldRefill(stack)) continue;
            int destination = findEmptyHotbarSlot();
            if (destination == -1) continue;
            swapIntoHotbar(this, inventory.getScreenHandler().syncId, slot, destination, client.player);
            break;
        }
    }

    private boolean isSplashPotion(ItemStack stack) {
        return stack.getItem() == Items.SPLASH_POTION;
    }

    private boolean shouldRefill(ItemStack stack) {
        if (!isSplashPotion(stack) || client.player == null) return false;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        for (StatusEffectInstance instance : contents.getEffects()) {
            RegistryEntry<StatusEffect> entry = instance.getEffectType();
            StatusEffect effect = entry.value();
            if (isInstant(effect) && effect.isBeneficial()) return true;
            if (!refillBuffPotions.getValue()) continue;
            if (effect.isBeneficial() && !isInstant(effect)
                    && !client.player.hasStatusEffect(entry) && !hotbarHasEffect(entry)) return true;
        }
        return false;
    }

    private boolean hotbarHasEffect(RegistryEntry<StatusEffect> effect) {
        for (int slot = 0; slot < 9; ++slot) {
            if (containsBuffEffect(client.player.getInventory().getStack(slot), effect)) return true;
        }
        return false;
    }

    private boolean containsBuffEffect(ItemStack stack, RegistryEntry<StatusEffect> wanted) {
        if (!isSplashPotion(stack)) return false;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        for (StatusEffectInstance instance : contents.getEffects()) {
            RegistryEntry<StatusEffect> entry = instance.getEffectType();
            StatusEffect effect = entry.value();
            if (effect.isBeneficial() && !isInstant(effect) && entry.equals(wanted)) return true;
        }
        return false;
    }

    private boolean isHotbarIndex(int slot) { return slot >= 0 && slot <= 8; }

    private int findEmptyHotbarSlot() {
        for (int slot = 0; slot < 9; ++slot) {
            if (client.player.getInventory().getStack(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private boolean isInstant(StatusEffect effect) { return effect.isInstant(); }

    
    private static void swapIntoHotbar(Module owner, int syncId, int source, int destination, PlayerEntity player) {
        if (player == null || client.interactionManager == null) return;
        var simulator = com.blamejared.clumps.client.handlers.input.InputSimulator.get();
        if (simulator != null && simulator.scheduleCursorClick(owner, syncId, source, destination, SlotActionType.SWAP, null)) {
            return;
        }
        var coordinator = com.blamejared.clumps.client.handlers.action.ActionCoordinator.get();
        if (coordinator != null) {
            coordinator.executeSlotClick(owner, syncId, source, destination, SlotActionType.SWAP, player);
            return;
        }
        client.interactionManager.clickSlot(syncId, source, destination, SlotActionType.SWAP, player);
    }
}
