package dev.dov.hack.echo.client.modules.impl.world;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.utils.StopwatchTimer;
import dev.dov.hack.echo.client.utils.inventory.SlotAction;
import dev.dov.hack.echo.client.utils.inventory.SlotPackets;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ChestStealer extends Module {
    public static IntSetting initialDelay;
    public static IntSetting takeDelay;
    public static IntSetting closeDelay;
    public static BooleanSetting onlyImportant;
    private dev.dov.hack.echo.client.utils.StopwatchTimer openDelayTimer;
    private dev.dov.hack.echo.client.utils.StopwatchTimer transferDelayTimer;
    private dev.dov.hack.echo.client.utils.StopwatchTimer closeDelayTimer;
    private boolean hasTransferredItems;

    public ChestStealer() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Chest Stealer"), dev.dov.hack.echo.client.init.CatalogText.literal("Automatically loots chests"), dev.dov.hack.echo.client.modules.ModuleCategory.WORLD));
    }

    @Override
    public void postInitialize() {
        openDelayTimer = new StopwatchTimer();
        transferDelayTimer = new StopwatchTimer();
        closeDelayTimer = new StopwatchTimer();
        hasTransferredItems = false;
        initialDelay.tooltip("Timing");
        takeDelay.tooltip("Timing");
        closeDelay.tooltip("Timing");
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (client.player == null || client.world == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        ScreenHandler currentHandler = client.player.currentScreenHandler;
        if (!(currentHandler instanceof GenericContainerScreenHandler container)) {
            openDelayTimer.reset();
            transferDelayTimer.reset();
            closeDelayTimer.reset();
            hasTransferredItems = false;
            return;
        }
        if (!openDelayTimer.hasElapsedMillis(initialDelay.getValue())) {
            transferDelayTimer.reset();
            return;
        }
        if (hasNoEligibleItems(container)) {
            if (hasTransferredItems && closeDelayTimer.hasElapsedMillis(closeDelay.getValue())) {
                client.player.closeHandledScreen();
            }
            return;
        }

        int containerSize = container.getInventory().size();
        for (int slotIndex = 0; slotIndex < containerSize; slotIndex++) {
            Slot slot = container.getSlot(slotIndex);
            if (!slot.hasStack()) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (!isEligibleItem(stack)) {
                continue;
            }
            if (!transferDelayTimer.hasElapsedMillis(takeDelay.getValue())) {
                continue;
            }
            var simulator = dev.dov.hack.echo.client.handlers.input.InputSimulator.get();
            if (simulator != null && simulator.scheduleCursorClick(
                    this,
                    client.player.currentScreenHandler.syncId,
                    slotIndex,
                    0,
                    net.minecraft.screen.slot.SlotActionType.QUICK_MOVE,
                    () -> {
                        hasTransferredItems = true;
                        transferDelayTimer.reset();
                        closeDelayTimer.reset();
                    })) {
                break;
            }
            if (SlotPackets.clickSlot(this, client.player.currentScreenHandler.syncId, slotIndex, 0, SlotAction.QUICK_MOVE, client.player)) {
                hasTransferredItems = true;
                transferDelayTimer.reset();
                closeDelayTimer.reset();
            }
        }
    }

    public boolean hasNoEligibleItems(GenericContainerScreenHandler container) {
        int containerSize = container.getInventory().size();
        for (int slotIndex = 0; slotIndex < containerSize; slotIndex++) {
            Slot slot = container.getSlot(slotIndex);
            if (slot.hasStack() && isEligibleItem(slot.getStack())) {
                return false;
            }
        }
        return true;
    }

    private boolean isEligibleItem(ItemStack stack) {
        if (!onlyImportant.getValue()) {
            return true;
        }
        return InventoryItems.isAxe(stack)
                || stack.contains(DataComponentTypes.FOOD)
                || stack.getItem() instanceof BlockItem
                || InventoryItems.isLegacySword(stack.getItem())
                || stack.contains(DataComponentTypes.WEAPON)
                || stack.contains(DataComponentTypes.TOOL)
                || stack.contains(DataComponentTypes.EQUIPPABLE)
                || stack.getItem() instanceof EnderPearlItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof ArrowItem
                || stack.getItem() instanceof FishingRodItem
                || stack.getItem() instanceof PotionItem;
    }

    @Override
    public void onDisable() {
        openDelayTimer.reset();
        transferDelayTimer.reset();
        closeDelayTimer.reset();
        hasTransferredItems = false;
        super.onDisable();
    }

}
