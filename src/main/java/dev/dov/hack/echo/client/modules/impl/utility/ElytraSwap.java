package dev.dov.hack.echo.client.modules.impl.utility;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HotbarKeyEvent;
import dev.dov.hack.echo.client.event.input.UseKeyEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.KeybindSetting;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Items;

public class ElytraSwap extends Module {
    private static BooleanSetting switchBack;
    private static KeybindSetting activateKey;
    private boolean swapActive;
    private boolean startedWithElytra;
    private boolean activateWasPressed;
    private int pendingHotbarSlot;
    private long pendingSlotPlayerAge;

    public ElytraSwap() {
        super(new dev.dov.hack.echo.client.modules.ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Elytra Swap"), dev.dov.hack.echo.client.init.CatalogText.literal("Quick elytra swap"), dev.dov.hack.echo.client.modules.ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        swapActive = false;
        startedWithElytra = false;
        activateWasPressed = false;
        pendingHotbarSlot = -1;
        pendingSlotPlayerAge = -1L;
        super.postInitialize();
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (isOutsideWorld() || client.player.isCreative()) {
            return;
        }
        try {
            Class.forName(Screen.class.getName(), true, Screen.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        if (MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        boolean pressed = activateKey.getKey().isPressed();
        if (pressed && !activateWasPressed && !swapActive) {
            beginSwap();
        }
        activateWasPressed = pressed;
        if (!swapActive) {
            return;
        }
        if (InventoryItems.wearsElytra() != startedWithElytra) {
            DeferredActionQueue.finish(this, switchBack.getValue());
            resetSwapState();
            return;
        }
        if (holdsDesiredItem()) {
            pendingHotbarSlot = -1;
            event.addClick();
            return;
        }
        if (pendingHotbarSlot != -1) {
            return;
        }
        int slot = findDesiredHotbarSlot();
        if (slot == -1) {
            DeferredActionQueue.finish(this, false);
            resetSwapState();
            return;
        }
        pendingHotbarSlot = slot;
        pendingSlotPlayerAge = client.player.age;
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (pendingHotbarSlot == -1 || event.slot != pendingHotbarSlot) {
            return;
        }
        if (client.player == null || (long) client.player.age - pendingSlotPlayerAge > 2L) {
            pendingHotbarSlot = -1;
            pendingSlotPlayerAge = -1L;
            return;
        }
        pendingHotbarSlot = -1;
        if (client.player.getInventory().getSelectedSlot() != event.slot) {
            if (!DeferredActionQueue.select(this, event.slot, -1, false)) {
                DeferredActionQueue.finish(this, false);
                resetSwapState();
                return;
            }
        }
        event.addClick();
    }

    private boolean holdsDesiredItem() {
        if (startedWithElytra) {
            return InventoryItems.holdsChestplateInMainHand();
        }
        return client.player.getMainHandStack().getItem() == Items.ELYTRA;
    }

    private int findDesiredHotbarSlot() {
        if (startedWithElytra) {
            return InventoryItems.findChestplateInHotbar();
        }
        return InventoryItems.findStackInHotbar(ElytraSwapItems::isElytra);
    }

    private void beginSwap() {
        swapActive = true;
        startedWithElytra = InventoryItems.wearsElytra();
    }

    private void resetSwapState() {
        swapActive = false;
        startedWithElytra = false;
        pendingHotbarSlot = -1;
        pendingSlotPlayerAge = -1L;
    }

    @Override
    public void onEnable() {
        resetSwapState();
        activateWasPressed = false;
        DeferredActionQueue.finish(this, false);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetSwapState();
        activateWasPressed = false;
        DeferredActionQueue.finish(this, false);
        super.onDisable();
    }

    
    public static void appendBootstrapSettings(ElytraSwap Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, ElytraSwap.switchBack);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, ElytraSwap.activateKey);
    }

    
    public static void assignBootstrapSettings(ElytraSwap Module, dev.dov.hack.echo.client.settings.BooleanSetting preparedSwitchBack, dev.dov.hack.echo.client.settings.KeybindSetting preparedActivateKey) {
        ElytraSwap.switchBack = preparedSwitchBack;
        ElytraSwap.activateKey = preparedActivateKey;
    }
}
