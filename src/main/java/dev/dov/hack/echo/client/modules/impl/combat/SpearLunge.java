package dev.dov.hack.echo.client.modules.impl.combat;

import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.event.interaction.AttackAttemptEvent;
import dev.dov.hack.echo.client.event.interaction.HandSwingEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.HotbarSlotsSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.settings.KeybindSetting;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import dev.dov.hack.echo.client.utils.targeting.WeaponAttributes;
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
        super(new dev.dov.hack.echo.client.modules.ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Spear Lunge"), dev.dov.hack.echo.client.init.CatalogText.literal("Uses the spear's lunge ability to move quickly"), dev.dov.hack.echo.client.modules.ModuleCategory.COMBAT));
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
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.mode);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.silent);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.cooldown);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.cooldownPercent);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.itemWhitelist);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.hotbarWhitelist);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.activateKey);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.returnTo);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.returnSlots);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearLunge.returnItems);
    }

    
    public static void assignBootstrapSettings(SpearLunge Module, dev.dov.hack.echo.client.settings.ChoiceSetting preparedCooldown, dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey, dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode, dev.dov.hack.echo.client.settings.ItemListSetting preparedReturnItems, dev.dov.hack.echo.client.settings.IntSetting preparedCooldownPercent, dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist, dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist, dev.dov.hack.echo.client.settings.BooleanSetting preparedSilent, dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedReturnSlots, dev.dov.hack.echo.client.settings.ChoiceSetting preparedReturnTo) {
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
