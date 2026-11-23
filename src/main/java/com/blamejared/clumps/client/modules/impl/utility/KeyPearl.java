package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.SelectedSlotEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.mixin.accessor.KeyBindingAccessor;
import com.blamejared.clumps.client.mixin.accessor.MinecraftClientAccessor;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.utils.CooldownTimer;
import com.blamejared.clumps.client.utils.Rotation;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.CrosshairHits;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class KeyPearl extends Module {
    private static BooleanSetting swapBack;
    private static FloatRangeSetting swapDelay;
    private static FloatRangeSetting throwDelay;
    private static KeybindSetting activateKey;
    private static BooleanSetting syncToClient;
    int pearlSlot;
    int slotBeforeThrow;
    int lastPreviousSlot;
    private CooldownTimer phaseTimer;
    private boolean throwInProgress;
    private boolean pearlThrown;
    private boolean activateWasPressed;
    private int sampledThrowDelay;
    private int sampledSwapDelay;

    public KeyPearl() {
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Key Pearl"), com.blamejared.clumps.client.init.CatalogText.literal("Quick pearl throw"), com.blamejared.clumps.client.modules.ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        pearlSlot = -1;
        slotBeforeThrow = -1;
        lastPreviousSlot = -1;
        phaseTimer = new CooldownTimer();
        throwInProgress = false;
        pearlThrown = false;
        activateWasPressed = false;
        sampledThrowDelay = 0;
        sampledSwapDelay = 0;
        swapDelay.setVisibilityPredicate(ignored -> swapBack.getValue());
        swapDelay.tooltip("Timing");
        throwDelay.tooltip("Timing");
    }

    @Subscribe
    public void onSelectedSlot(SelectedSlotEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (!throwInProgress) {
            lastPreviousSlot = event.getPreviousSlot();
        }
    }

    @Subscribe
    public void beforeInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) {
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
        if (pressed && !activateWasPressed) {
            slotBeforeThrow = client.player.getInventory().getSelectedSlot();
            pearlSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() instanceof EnderPearlItem);
            throwInProgress = false;
            pearlThrown = false;
            sampledThrowDelay = sampleDelay(throwDelay);
            phaseTimer.reset();
            if (pearlSlot != -1) {
                InputKey key = activateKey.getKey();
                if (!key.isMouse() && client.options != null) {
                    for (int slot = 0; slot < 9; slot++) {
                        KeyBindingAccessor binding = (KeyBindingAccessor) client.options.hotbarKeys[slot];
                        InputUtil.Key boundKey = binding.getBoundKey();
                        if (boundKey != null && key.matches(boundKey.getCode())) {
                            binding.setTimesPressed(0);
                            break;
                        }
                    }
                }
                if (!client.player.getItemCooldownManager().isCoolingDown(createPearlIcon())) {
                    throwInProgress = true;
                    if (swapBack.getValue()) {
                        if (!DeferredActionQueue.ensureSelected(this, pearlSlot, -1, true, null)) {
                            throwInProgress = false;
                            return;
                        }
                    } else {
                        InventoryItems.selectHotbarSlot(pearlSlot);
                    }
                }
            }
        }
        if (throwInProgress && !pearlThrown && phaseTimer.hasElapsedTicks(sampledThrowDelay)) {
            Runnable throwAction = KeyPearl::throwSelectedPearl;
            if (syncToClient.getValue()) {
                Rotation rotation = rotations().snapshot().playerRotation();
                rotations().override(this, RotationPriority.CRITICAL, rotation).ifPresentOrElse(scope -> {
                    try (scope) {
                        throwAction.run();
                    }
                }, throwAction::run);
            } else {
                throwAction.run();
            }
            pearlThrown = true;
            sampledSwapDelay = sampleDelay(swapDelay);
            phaseTimer.reset();
        }
        if (pearlThrown && swapBack.getValue() && phaseTimer.hasElapsedTicks(sampledSwapDelay)) {
            DeferredActionQueue.finish(this);
            throwInProgress = false;
            pearlThrown = false;
        }
        activateWasPressed = pressed;
    }

    private static int sampleDelay(FloatRangeSetting setting) {
        return Math.max(0, Math.round(setting.sampleGaussian()));
    }

    private ItemStack createPearlIcon() {
        return new ItemStack(Items.ENDER_PEARL);
    }

    private static void throwSelectedPearl() {
        CrosshairHits.withItemUseHit(KeyPearl::useSelectedItem);
    }

    private static void useSelectedItem() {
        try {
            Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        var coordinator = com.blamejared.clumps.client.handlers.action.ActionCoordinator.get();
        if (coordinator != null) {
            coordinator.executeDoItemUse(null);
            return;
        }
        ((MinecraftClientAccessor) (Object) client).invokeDoItemUse();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this);
        super.onDisable();
    }

    
    public static void appendBootstrapSettings(KeyPearl Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, KeyPearl.swapBack);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, KeyPearl.swapDelay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, KeyPearl.throwDelay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, KeyPearl.activateKey);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, KeyPearl.syncToClient);
    }

    
    public static void assignBootstrapSettings(KeyPearl Module, com.blamejared.clumps.client.settings.BooleanSetting preparedSyncToClient, com.blamejared.clumps.client.settings.FloatRangeSetting preparedThrowDelay, com.blamejared.clumps.client.settings.KeybindSetting preparedActivateKey, com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBack, com.blamejared.clumps.client.settings.FloatRangeSetting preparedSwapDelay) {
        KeyPearl.syncToClient = preparedSyncToClient;
        KeyPearl.throwDelay = preparedThrowDelay;
        KeyPearl.activateKey = preparedActivateKey;
        KeyPearl.swapBack = preparedSwapBack;
        KeyPearl.swapDelay = preparedSwapDelay;
    }
}
