package dev.dov.hack.echo.client.modules.impl.utility;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.handlers.action.ActionCoordinator;
import dev.dov.hack.echo.client.handlers.input.InputSimulator;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.mixin.accessor.HandledScreenAccessor;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatRangeSetting;
import dev.dov.hack.echo.client.settings.HotbarSlotsSetting;
import dev.dov.hack.echo.client.utils.CooldownTimer;
import dev.dov.hack.echo.client.utils.MathRng;
import dev.dov.hack.echo.client.utils.inventory.InventoryActionScheduler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class CartRefill extends Module {
    private static final CharSequence MODE_AUTO_OPEN = CatalogText.literal("Auto Open");
    private static final CharSequence MODE_IN_INVENTORY = CatalogText.literal("In Inventory");
    private static final CharSequence MODE_SILENT = CatalogText.literal("Silent");

    private static final CharSequence CART_TNT = CatalogText.literal("TNT Minecart");
    private static final CharSequence CART_ANY = CatalogText.literal("Any Minecart");

    private static final CharSequence TRIGGER_NO_CART = CatalogText.literal("No Cart In Hotbar");
    private static final CharSequence TRIGGER_FILL_SLOTS = CatalogText.literal("Fill Empty Slots");

    private static final CharSequence PICK_CLOSEST = CatalogText.literal("Closest");
    private static final CharSequence PICK_RANDOM = CatalogText.literal("Random");
    private static final CharSequence PICK_FIRST = CatalogText.literal("First");

    private static final CharSequence SECTION_INVENTORY = CatalogText.literal("Inventory");
    private static final CharSequence SECTION_TIMING = CatalogText.literal("Timing");
    private static final CharSequence SECTION_SAFETY = CatalogText.literal("Safety");

    private static final int MAIN_INV_START = 9;
    private static final int MAIN_INV_END = 36;

    public static ChoiceSetting mode;
    public static ChoiceSetting cartType;
    public static ChoiceSetting trigger;
    public static HotbarSlotsSetting hotbarSlots;
    public static ChoiceSetting pickOrder;
    public static FloatRangeSetting delay;
    public static BooleanSetting autoClose;
    public static FloatRangeSetting closeDelay;
    public static BooleanSetting onlyEmptySlot;
    public static BooleanSetting pauseOnUse;

    private final CooldownTimer actionTimer = new CooldownTimer();
    private final CooldownTimer closeTimer = new CooldownTimer();
    private long nextActionDelayMs;
    private long nextCloseDelayMs;
    private boolean openedInventory;
    private boolean pendingClose;

    public CartRefill() {
        super(new ModuleInfo(
                CatalogText.literal("Cart Refill"),
                CatalogText.literal("Refills minecarts from your inventory into selected hotbar slots"),
                ModuleCategory.UTILITY));
    }

    public static CharSequence bootstrapModeAutoOpen() { return MODE_AUTO_OPEN; }
    public static CharSequence bootstrapModeInInventory() { return MODE_IN_INVENTORY; }
    public static CharSequence bootstrapModeSilent() { return MODE_SILENT; }
    public static CharSequence bootstrapCartTnt() { return CART_TNT; }
    public static CharSequence bootstrapCartAny() { return CART_ANY; }
    public static CharSequence bootstrapTriggerNoCart() { return TRIGGER_NO_CART; }
    public static CharSequence bootstrapTriggerFillSlots() { return TRIGGER_FILL_SLOTS; }
    public static CharSequence bootstrapPickClosest() { return PICK_CLOSEST; }
    public static CharSequence bootstrapPickRandom() { return PICK_RANDOM; }
    public static CharSequence bootstrapPickFirst() { return PICK_FIRST; }

    @Override
    public void postInitialize() {
        autoClose.setVisibilityPredicate(ignored -> mode.isSelected(MODE_AUTO_OPEN));
        closeDelay.setVisibilityPredicate(ignored -> mode.isSelected(MODE_AUTO_OPEN) && autoClose.getValue());

        mode.description(CatalogText.literal("Auto Open opens your inventory to refill; In Inventory only refills while your inventory is open; Silent swaps without opening"));
        cartType.description(CatalogText.literal("Choose whether to refill only TNT Minecarts or any minecart type"));
        trigger.description(CatalogText.literal("No Cart In Hotbar waits until zero carts remain in your hotbar; Fill Empty Slots refills every selected slot"));
        hotbarSlots.description(CatalogText.literal("Target hotbar slots to refill carts into (leave all unchecked to allow any empty hotbar slot)"));
        pickOrder.description(CatalogText.literal("Closest picks the inventory cart nearest to your target hotbar column; Random picks a random cart; First scans top-left to bottom-right"));
        delay.description(CatalogText.literal("Randomized delay in milliseconds before moving a cart"));
        autoClose.description(CatalogText.literal("Automatically close the inventory after refilling in Auto Open mode"));
        closeDelay.description(CatalogText.literal("Delay in milliseconds before closing the inventory"));
        onlyEmptySlot.description(CatalogText.literal("Skip selected hotbar slots that currently hold a different item"));
        pauseOnUse.description(CatalogText.literal("Pause auto-refill while eating, drinking, blocking, or using an item"));

        cartType.tooltip(SECTION_INVENTORY);
        trigger.tooltip(SECTION_INVENTORY);
        hotbarSlots.tooltip(SECTION_INVENTORY);
        pickOrder.tooltip(SECTION_INVENTORY);
        delay.tooltip(SECTION_TIMING);
        autoClose.tooltip(SECTION_TIMING);
        closeDelay.tooltip(SECTION_TIMING);
        onlyEmptySlot.tooltip(SECTION_SAFETY);
        pauseOnUse.tooltip(SECTION_SAFETY);

        resetState();
    }

    @Override
    public CharSequence getSuffix() {
        return pickOrder != null ? pickOrder.getValue() : PICK_CLOSEST;
    }

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (openedInventory && client.currentScreen instanceof InventoryScreen) {
            client.setScreen(null);
        }
        resetState();
        super.onDisable();
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld() || client.interactionManager == null || !client.player.isAlive()) {
            resetState();
            return;
        }
        if (InventoryActionScheduler.isTransferring()) {
            return;
        }
        if (pauseOnUse.getValue() && client.player.isUsingItem()) {
            return;
        }

        if (pendingClose) {
            if (!(client.currentScreen instanceof InventoryScreen)) {
                openedInventory = false;
                pendingClose = false;
            } else if (closeTimer.hasElapsedMillis((double) nextCloseDelayMs)) {
                client.setScreen(null);
                openedInventory = false;
                pendingClose = false;
            }
            return;
        }

        if (openedInventory && !(client.currentScreen instanceof InventoryScreen)) {
            openedInventory = false;
        }

        int targetHotbarSlot = findDestinationHotbarSlot();
        if (targetHotbarSlot == -1) {
            if (openedInventory && autoClose.getValue() && client.currentScreen instanceof InventoryScreen) {
                armAutoClose();
            }
            return;
        }

        if (mode.isSelected(MODE_IN_INVENTORY)) {
            if (!(client.currentScreen instanceof InventoryScreen inventory)) {
                return;
            }
            int sourceSlot = findSourceCartSlot(targetHotbarSlot, inventory);
            if (sourceSlot == -1) {
                return;
            }
            if (!actionTimer.hasElapsedMillis((double) nextActionDelayMs)) {
                return;
            }
            if (executeSwap(inventory.getScreenHandler().syncId, sourceSlot, targetHotbarSlot)) {
                sampleNextActionDelay();
            }
            return;
        }

        if (mode.isSelected(MODE_AUTO_OPEN)) {
            if (client.currentScreen == null) {
                if (findSourceCartSlot(targetHotbarSlot, null) == -1) {
                    return;
                }
                client.setScreen(new InventoryScreen(client.player));
                openedInventory = true;
                sampleNextActionDelay();
                return;
            }
            if (!(client.currentScreen instanceof InventoryScreen inventory)) {
                return;
            }
            int sourceSlot = findSourceCartSlot(targetHotbarSlot, inventory);
            if (sourceSlot == -1) {
                if (openedInventory && autoClose.getValue()) {
                    armAutoClose();
                }
                return;
            }
            if (!actionTimer.hasElapsedMillis((double) nextActionDelayMs)) {
                return;
            }
            if (executeSwap(inventory.getScreenHandler().syncId, sourceSlot, targetHotbarSlot)) {
                sampleNextActionDelay();
                if (openedInventory && autoClose.getValue() && findDestinationHotbarSlot() == -1) {
                    armAutoClose();
                }
            }
            return;
        }

        // Silent mode: swap directly through playerScreenHandler when no screen blocks interaction
        if (MinecraftRenderState.getScreen() != null && !(client.currentScreen instanceof InventoryScreen)) {
            return;
        }
        int sourceSlot = findSourceCartSlot(targetHotbarSlot,
                client.currentScreen instanceof InventoryScreen inv ? inv : null);
        if (sourceSlot == -1) {
            return;
        }
        if (!actionTimer.hasElapsedMillis((double) nextActionDelayMs)) {
            return;
        }
        int syncId = client.player.playerScreenHandler.syncId;
        if (executeSwap(syncId, sourceSlot, targetHotbarSlot)) {
            sampleNextActionDelay();
        }
    }

    private int findDestinationHotbarSlot() {
        if (trigger.isSelected(TRIGGER_NO_CART) && hotbarHasAnyCart()) {
            return -1;
        }
        boolean anySelected = hasAnySelectedHotbarSlot();
        int currentSelected = client.player.getInventory().getSelectedSlot();

        // First pass: prefer an empty allowed slot (prioritizing current slot if eligible)
        if (isHotbarSlotEligible(currentSelected, anySelected)
                && client.player.getInventory().getStack(currentSelected).isEmpty()) {
            return currentSelected;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (!isHotbarSlotEligible(slot, anySelected)) {
                continue;
            }
            if (client.player.getInventory().getStack(slot).isEmpty()) {
                return slot;
            }
        }
        if (onlyEmptySlot.getValue()) {
            return -1;
        }
        // Second pass (when Only Empty Slot is disabled): replace a non-cart item in selected slots
        for (int slot = 0; slot < 9; slot++) {
            if (!isHotbarSlotEligible(slot, anySelected)) {
                continue;
            }
            if (!isTargetCart(client.player.getInventory().getStack(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private int findSourceCartSlot(int targetHotbarSlot, InventoryScreen screen) {
        List<Integer> candidates = new ArrayList<>();
        for (int slot = MAIN_INV_START; slot < MAIN_INV_END; slot++) {
            ItemStack stack = client.player.playerScreenHandler.getSlot(slot).getStack();
            if (isTargetCart(stack)) {
                candidates.add(slot);
            }
        }
        if (candidates.isEmpty()) {
            return -1;
        }
        if (pickOrder.isSelected(PICK_RANDOM)) {
            return candidates.get(MathRng.randomInt(0, candidates.size()));
        }
        if (pickOrder.isSelected(PICK_FIRST)) {
            return candidates.get(0);
        }

        // Closest: pick inventory slot closest in 2D grid distance to focused slot (if hovered) or target hotbar column
        int refCol = targetHotbarSlot;
        int refRow = 3; // Hotbar sits directly below inventory row 2
        if (screen != null) {
            Slot focused = ((HandledScreenAccessor) screen).getFocusedSlot();
            if (focused != null && focused.id >= MAIN_INV_START && focused.id < MAIN_INV_END) {
                refRow = (focused.id - MAIN_INV_START) / 9;
                refCol = (focused.id - MAIN_INV_START) % 9;
            }
        }
        int bestSlot = candidates.get(0);
        int bestDistSq = Integer.MAX_VALUE;
        for (int slot : candidates) {
            int row = (slot - MAIN_INV_START) / 9;
            int col = (slot - MAIN_INV_START) % 9;
            int dx = col - refCol;
            int dy = row - refRow;
            int distSq = dx * dx + dy * dy;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private boolean executeSwap(int syncId, int sourceSlot, int hotbarSlot) {
        if (client.player == null || client.interactionManager == null) {
            return false;
        }
        InputSimulator simulator = InputSimulator.get();
        if (simulator != null && simulator.scheduleCursorClick(this, syncId, sourceSlot, hotbarSlot, SlotActionType.SWAP, null)) {
            return true;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null) {
            return coordinator.executeSlotClick(this, syncId, sourceSlot, hotbarSlot, SlotActionType.SWAP, client.player).succeeded();
        }
        client.interactionManager.clickSlot(syncId, sourceSlot, hotbarSlot, SlotActionType.SWAP, client.player);
        return true;
    }

    private boolean hotbarHasAnyCart() {
        for (int slot = 0; slot < 9; slot++) {
            if (isTargetCart(client.player.getInventory().getStack(slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnySelectedHotbarSlot() {
        for (int slot = 0; slot < 9; slot++) {
            if (hotbarSlots.isSelected(slot)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHotbarSlotEligible(int slot, boolean anySelected) {
        if (slot < 0 || slot > 8) {
            return false;
        }
        return !anySelected || hotbarSlots.isSelected(slot);
    }

    private boolean isTargetCart(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item == Items.TNT_MINECART) {
            return true;
        }
        if (cartType.isSelected(CART_ANY)) {
            return item == Items.MINECART
                    || item == Items.CHEST_MINECART
                    || item == Items.HOPPER_MINECART
                    || item == Items.FURNACE_MINECART;
        }
        return false;
    }

    private void armAutoClose() {
        pendingClose = true;
        nextCloseDelayMs = Math.max(0L, Math.round(closeDelay.sampleGaussian()));
        closeTimer.reset();
    }

    private void sampleNextActionDelay() {
        nextActionDelayMs = Math.max(0L, Math.round(delay.sampleGaussian()));
        actionTimer.reset();
    }

    private void resetState() {
        openedInventory = false;
        pendingClose = false;
        nextActionDelayMs = delay != null ? Math.max(0L, Math.round(delay.sampleGaussian())) : 60L;
        nextCloseDelayMs = closeDelay != null ? Math.max(0L, Math.round(closeDelay.sampleGaussian())) : 50L;
        actionTimer.reset();
        closeTimer.reset();
    }

    public static void appendBootstrapSettings(CartRefill module) {
        CatalogSettings.append(module, mode);
        CatalogSettings.append(module, cartType);
        CatalogSettings.append(module, trigger);
        CatalogSettings.append(module, hotbarSlots);
        CatalogSettings.append(module, pickOrder);
        CatalogSettings.append(module, delay);
        CatalogSettings.append(module, autoClose);
        CatalogSettings.append(module, closeDelay);
        CatalogSettings.append(module, onlyEmptySlot);
        CatalogSettings.append(module, pauseOnUse);
    }

    public static void assignBootstrapSettings(
            CartRefill module,
            ChoiceSetting preparedMode,
            ChoiceSetting preparedCartType,
            ChoiceSetting preparedTrigger,
            HotbarSlotsSetting preparedHotbarSlots,
            ChoiceSetting preparedPickOrder,
            FloatRangeSetting preparedDelay,
            BooleanSetting preparedAutoClose,
            FloatRangeSetting preparedCloseDelay,
            BooleanSetting preparedOnlyEmptySlot,
            BooleanSetting preparedPauseOnUse) {
        mode = preparedMode;
        cartType = preparedCartType;
        trigger = preparedTrigger;
        hotbarSlots = preparedHotbarSlots;
        pickOrder = preparedPickOrder;
        delay = preparedDelay;
        autoClose = preparedAutoClose;
        closeDelay = preparedCloseDelay;
        onlyEmptySlot = preparedOnlyEmptySlot;
        pauseOnUse = preparedPauseOnUse;
    }
}
