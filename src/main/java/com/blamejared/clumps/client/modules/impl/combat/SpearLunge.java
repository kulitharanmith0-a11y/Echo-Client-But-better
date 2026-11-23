package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.interaction.AttackAttemptEvent;
import com.blamejared.clumps.client.event.interaction.HandSwingEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.HotbarSlotsSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.WeaponAttributes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;

public class SpearLunge extends Module {
    private static final CharSequence VANILLA = CatalogText.literal("Vanilla");
    private static final CharSequence ATTACK = CatalogText.literal("Attack");
    private static final CharSequence KEYPRESS = CatalogText.literal("Keypress");
    private static final CharSequence SLOT = CatalogText.literal("Slot");
    private static final CharSequence ITEM = CatalogText.literal("Item");
    private static final CharSequence PAPER = CatalogText.literal("Paper");
    private static ChoiceSetting mode;
    private static BooleanSetting silent;
    private static ChoiceSetting cooldown;
    private static IntSetting cooldownPercent;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static KeybindSetting activateKey;
    private static ChoiceSetting returnTo;
    private static HotbarSlotsSetting returnSlots;
    private static ItemListSetting returnItems;
    private boolean activationKeyWasPressed;
    private static String cooldownTooltip;
    private static String cooldownPercentTooltip;
    private static String itemWhitelistTooltip;
    private static String hotbarWhitelistTooltip;
    private static String returnToTooltip;
    private static String returnFiltersTooltip;

    public SpearLunge() {
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Spear Lunge"), com.blamejared.clumps.client.init.CatalogText.literal("Uses the spear's lunge ability to move quickly"), com.blamejared.clumps.client.modules.ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        activationKeyWasPressed = false;
        cooldownPercent.setVisibilityPredicate(ignored -> cooldown.isSelected(VANILLA));
        itemWhitelist.setVisibilityPredicate(ignored -> mode.isSelected(ATTACK));
        hotbarWhitelist.setVisibilityPredicate(ignored -> mode.isSelected(ATTACK));
        activateKey.setVisibilityPredicate(ignored -> mode.isSelected(KEYPRESS));
        returnSlots.setVisibilityPredicate(ignored -> returnTo.isSelected(SLOT));
        returnItems.setVisibilityPredicate(ignored -> returnTo.isSelected(ITEM));
        if (cooldownTooltip == null) cooldownTooltip = "Timing";
        cooldown.tooltip(cooldownTooltip);
        if (cooldownPercentTooltip == null) cooldownPercentTooltip = "Timing";
        cooldownPercent.tooltip(cooldownPercentTooltip);
        if (itemWhitelistTooltip == null) itemWhitelistTooltip = "Conditions";
        itemWhitelist.tooltip(itemWhitelistTooltip);
        if (hotbarWhitelistTooltip == null) hotbarWhitelistTooltip = "Conditions";
        hotbarWhitelist.tooltip(hotbarWhitelistTooltip);
        if (returnToTooltip == null) returnToTooltip = "Return";
        returnTo.tooltip(returnToTooltip);
        if (returnFiltersTooltip == null) returnFiltersTooltip = "Return";
        returnSlots.tooltip(returnFiltersTooltip);
        returnItems.tooltip(returnFiltersTooltip);
    }

    private int findLungeSpearSlot() {
        return InventoryItems.findItemWithEnchantmentInHotbar(
                stack -> InventoryItems.isSpear(stack.getItem()), Enchantments.LUNGE, false);
    }

    private boolean holdsLungeSpear() {
        if (client.player == null) return false;
        ItemStack stack = client.player.getMainHandStack();
        return !stack.isEmpty() && InventoryItems.isSpear(stack.getItem())
                && InventoryItems.hasEnchantment(stack, Enchantments.LUNGE);
    }

    @Subscribe
    public final void onAttackAttempt(AttackAttemptEvent event) {
        if (!mode.isSelected(ATTACK)) return;
        if (isOutsideWorld() || !isEnabled()) return;
        if (MinecraftRenderState.getScreen() != null || client.isPaused()) return;
        if (holdsLungeSpear()) return;
        if (!itemWhitelist.acceptsHeldItem() || !hotbarWhitelist.permitsCurrentSlot()) return;
        if (!tryActivate()) return;
        event.cancel();
        WeaponAttributes.performPiercingAttack();
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (!mode.isSelected(KEYPRESS)) return;
        if (isOutsideWorld() || !isEnabled()) return;
        if (MinecraftRenderState.getScreen() != null || client.isPaused()) return;
        boolean pressed = activateKey.getKey().isPressed();
        if (pressed && !activationKeyWasPressed) {
            if (holdsLungeSpear()) {
                activationKeyWasPressed = true;
                return;
            }
            if (tryActivate()) WeaponAttributes.performPiercingAttack();
        }
        activationKeyWasPressed = pressed;
    }

    private boolean tryActivate() {
        if (DeferredActionQueue.hasPendingActions()) return false;
        int spearSlot = findLungeSpearSlot();
        if (spearSlot == -1) return false;
        if (cooldown.isSelected(VANILLA)) {
            if (client.player.getAttackCooldownProgress(0.0f) < (float) cooldownPercent.getValue() / 100.0f) return false;
        } else if (cooldown.isSelected(PAPER)) {
            ItemStack spear = client.player.getInventory().getStack(spearSlot);
            if (WeaponAttributes.remainingCooldownSeconds(client.player, spear, 0.0f) > 0.0) return false;
        }
        if (client.player.getInventory().getSelectedSlot() == spearSlot) return false;
        return DeferredActionQueue.select(this, spearSlot, silent.getValue() ? 0 : 1, true, this::applyReturnSlot);
    }

    @Subscribe
    public void onHandSwing(HandSwingEvent event) {
        if (event.getPlayer() == client.player) WeaponAttributes.recordSwingTick(client.player);
    }

    private void applyReturnSlot() {
        int slot;
        if (returnTo.isSelected(SLOT)) {
            slot = returnSlots.chooseSelectedSlot();
        } else if (returnTo.isSelected(ITEM)) {
            slot = InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
        } else {
            slot = -1;
        }
        if (slot != -1) InventoryItems.selectHotbarSlot(slot);
    }

    public ItemStack getPaperCooldownSpear() {
        if (!isEnabled() || !cooldown.isSelected(PAPER) || isOutsideWorld()) return ItemStack.EMPTY;
        int slot = findLungeSpearSlot();
        return slot == -1 ? ItemStack.EMPTY : client.player.getInventory().getStack(slot);
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this);
        activationKeyWasPressed = false;
    }

    
    public static void appendBootstrapSettings(SpearLunge Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.mode);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.silent);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.cooldown);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.cooldownPercent);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.itemWhitelist);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.hotbarWhitelist);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.activateKey);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.returnTo);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.returnSlots);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SpearLunge.returnItems);
    }

    
    public static void assignBootstrapSettings(SpearLunge Module, com.blamejared.clumps.client.settings.ChoiceSetting preparedCooldown, com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey, com.blamejared.clumps.client.settings.ChoiceSetting preparedMode, com.blamejared.clumps.client.settings.ItemListSetting preparedReturnItems, com.blamejared.clumps.client.settings.IntSetting preparedCooldownPercent, com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedHotbarWhitelist, com.blamejared.clumps.client.settings.ItemListSetting preparedItemWhitelist, com.blamejared.clumps.client.settings.BooleanSetting preparedSilent, com.blamejared.clumps.client.settings.HotbarSlotsSetting preparedReturnSlots, com.blamejared.clumps.client.settings.ChoiceSetting preparedReturnTo) {
        SpearLunge.cooldown = preparedCooldown;
        SpearLunge.activateKey = preparedActivateKey;
        SpearLunge.mode = preparedMode;
        SpearLunge.returnItems = preparedReturnItems;
        SpearLunge.cooldownPercent = preparedCooldownPercent;
        SpearLunge.hotbarWhitelist = preparedHotbarWhitelist;
        SpearLunge.itemWhitelist = preparedItemWhitelist;
        SpearLunge.silent = preparedSilent;
        SpearLunge.returnSlots = preparedReturnSlots;
        SpearLunge.returnTo = preparedReturnTo;
    }
}
