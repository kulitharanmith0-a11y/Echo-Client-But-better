package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.utils.items.InventoryItems;
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
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Elytra Swap"), com.blamejared.clumps.client.init.CatalogText.literal("Quick elytra swap"), com.blamejared.clumps.client.modules.ModuleCategory.UTILITY));
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
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, ElytraSwap.switchBack);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, ElytraSwap.activateKey);
    }

    
    public static void assignBootstrapSettings(ElytraSwap Module, com.blamejared.clumps.client.settings.BooleanSetting preparedSwitchBack, com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey) {
        ElytraSwap.switchBack = preparedSwitchBack;
        ElytraSwap.activateKey = preparedActivateKey;
    }
}
