package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.modules.impl.movement.DiveBombSequence;
import com.blamejared.clumps.client.modules.impl.movement.ThrowInventoryOperations;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.rotation.RotationManager;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.mixin.accessor.MinecraftClientAccessor;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.KeybindSetting;
import com.blamejared.clumps.client.utils.CooldownTimer;
import com.blamejared.clumps.client.utils.WeakRefSet;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public class DiveBomb extends Module {
    private static BooleanSetting autoJump;
    private static BooleanSetting silentAim;
    private static BooleanSetting switchback;
    private static IntSetting tickDelay;
    private static KeybindSetting diveBomb;
    private static KeybindSetting instantDiveBomb;

    private boolean throwActive;
    private boolean previousNormalPress;
    private boolean previousInstantPress;
    private CooldownTimer throwTimer;
    private boolean rotationRequested;
    private int throwStage;
    private DiveBombSequence instantSequence;

    public DiveBomb() {
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Dive Bomb"), com.blamejared.clumps.client.init.CatalogText.literal("Tp to the sky with a wind charge and enderpearl"), com.blamejared.clumps.client.modules.ModuleCategory.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        throwActive = false;
        previousNormalPress = false;
        previousInstantPress = false;
        throwTimer = new CooldownTimer();
        rotationRequested = false;
        throwStage = 0;
        instantSequence = null;
        tickDelay.tooltip("Timing");
    }

    @Override
    public void onDisable() {
        WeakRefSet.remove(this);
        releaseRotationIfRequested();
        throwActive = false;
        previousNormalPress = false;
        previousInstantPress = false;
        instantSequence = null;
        DeferredActionQueue.finish(this, false);
        throwTimer.reset();
        throwStage = 0;
        super.onDisable();
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (autoJump.getValue() && throwActive && instantSequence == null && client.player.isOnGround()) {
            event.jump = true;
        }
        if (throwActive) {
            event.forward = false;
            event.backward = false;
            event.left = false;
            event.right = false;
        }
        handleInstantPress();
        if (instantSequence != null) {
            if (instantSequence.tick()) {
                finishInstantSequence();
            }
            return;
        }
        if (!throwActive && (!hasRequiredHotbarItems() || hasItemCooldown())) {
            return;
        }
        boolean pressed = diveBomb.getKey().isPressed();
        if (pressed && !previousNormalPress) {
            if (rotations().snapshot().target() != null && !rotations().canRequest(this, RotationPriority.HIGH)) {
                if (com.blamejared.clumps.client.diagnostics.DevDiagnostics.ENABLED) {
                    com.blamejared.clumps.client.diagnostics.DevDiagnostics.recordDeny("Dive Bomb", "ROT", "high_prio_busy");
                }
                return;
            }
            throwActive = true;
            throwStage = 0;
            if (com.blamejared.clumps.client.diagnostics.DevDiagnostics.ENABLED) {
                com.blamejared.clumps.client.diagnostics.DevDiagnostics.log("Dive Bomb", "SEQ_START", "delay=" + tickDelay.getValue());
            }
        }
        previousNormalPress = pressed;
        if (!throwActive || !rotations().isAiming()) {
            return;
        }
        float pitch = silentAim.getValue() ? rotations().effectiveRotation().pitch() : client.player.getPitch();
        if (pitch >= -89.5F) {
            return;
        }
        if (throwStage == 0) {
            if (hasItemCooldown()) {
                return;
            }
            int pearlSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
            if (pearlSlot == -1 || !DeferredActionQueue.ensureSelected(this, pearlSlot, -1, false)) {
                finishNormalSequence();
                return;
            }
            useItem();
            throwTimer.reset();
            throwStage = 1;
        } else if (throwStage == 1) {
            if (throwTimer.hasElapsedTicks(tickDelay.getValue())) {
                throwStage = 2;
            }
        } else if (throwStage == 2) {
            int chargeSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
            if (chargeSlot == -1 || !DeferredActionQueue.ensureSelected(this, chargeSlot, -1, false)) {
                finishNormalSequence();
                return;
            }
            useItem();
            throwTimer.reset();
            throwStage = 3;
        } else if (throwStage == 3 && throwTimer.hasElapsedTicks(1)) {
            finishNormalSequence();
        }
    }

    private boolean hasRequiredHotbarItems() {
        return InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE) != -1
                && InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL) != -1;
    }

    private boolean hasItemCooldown() {
        return client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))
                || client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE));
    }

    private void handleInstantPress() {
        boolean pressed = instantDiveBomb.getKey().isPressed();
        if (pressed && !previousInstantPress) {
            startInstantSequence();
        }
        previousInstantPress = pressed;
    }

    private void startInstantSequence() {
        if (throwActive || instantSequence != null || hasItemCooldown()) {
            return;
        }
        if (client.interactionManager == null || client.player.isSpectator()) {
            return;
        }
        int originalSlot = client.player.getInventory().getSelectedSlot();
        boolean moveCharge = !client.player.getOffHandStack().isOf(Items.WIND_CHARGE);
        int chargeSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        int pearlSlot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.ENDER_PEARL);
        if (moveCharge && chargeSlot == -1) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(Items.ENDER_PEARL) && pearlSlot == -1) {
            return;
        }
        if (rotations().snapshot().target() != null && !rotations().canRequest(this, RotationPriority.HIGH)) {
            return;
        }
        throwActive = true;
        throwStage = 0;
        WeakRefSet.add(this);
        instantSequence = new DiveBombSequence(client, originalSlot, chargeSlot, pearlSlot, moveCharge, () -> ThrowInventoryOperations.swapHands(client), this::useItem, () -> silentAim.getValue() ? rotations().effectiveRotation().pitch() : client.player.getPitch());
    }

    private void finishInstantSequence() {
        WeakRefSet.remove(this);
        instantSequence = null;
        throwActive = false;
        throwStage = 0;
        releaseRotationIfRequested();
    }

    private void finishNormalSequence() {
        throwActive = false;
        throwStage = 0;
        if (!switchback.getValue()) {
            DeferredActionQueue.finish(this, false);
        }
        releaseRotationIfRequested();
    }

    private void releaseRotationIfRequested() {
        if (!rotationRequested) {
            return;
        }
        if (silentAim.getValue() && rotations().beginReturn(this)) {
            return;
        }
        rotationRequested = false;
    }

    @Subscribe
    public void onMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (!throwActive && rotationRequested) {
            if (throwTimer.hasElapsedTicks(1)) {
                releaseRotationIfRequested();
            }
            return;
        }
        if (!throwActive) {
            return;
        }
        RotationManager manager = rotations();
        float yaw;
        if (manager.isAiming() && manager.isSilentRotationActive()) {
            yaw = manager.effectiveRotation().yaw();
        } else {
            yaw = silentAim.getValue() ? manager.effectiveRotation().yaw() : client.player.getYaw();
        }
        Vec3d aimPoint = client.player.getEyePos().add(Vec3d.fromPolar(-89.9F, yaw).multiply(10.0));
        manager.aim(this, RotationPriority.HIGH, aimPoint, RotationSettings.uniform(
                silentAim.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, AimTransition.EASED, 450.0F));
        rotationRequested = true;
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (!switchback.getValue()) {
            return;
        }
        if (!throwActive && DeferredActionQueue.contains(this) && throwTimer.hasElapsedTicks(1)) {
            DeferredActionQueue.finish(this);
        }
    }

    private void useItem() {
        actions().executeDoItemUse(this);
    }

    
    public static void appendBootstrapSettings(DiveBomb Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, DiveBomb.autoJump);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, DiveBomb.silentAim);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, DiveBomb.switchback);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, DiveBomb.tickDelay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, DiveBomb.diveBomb);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, DiveBomb.instantDiveBomb);
    }

    
    public static void assignBootstrapSettings(DiveBomb Module, com.blamejared.clumps.client.settings.IntSetting preparedTickDelay, com.blamejared.clumps.client.settings.BooleanSetting preparedAutoJump, com.blamejared.clumps.client.settings.KeybindSetting preparedInstantDiveBomb, com.blamejared.clumps.client.settings.BooleanSetting preparedSilentAim, com.blamejared.clumps.client.settings.KeybindSetting preparedDiveBomb, com.blamejared.clumps.client.settings.BooleanSetting preparedSwitchback) {
        DiveBomb.tickDelay = preparedTickDelay;
        DiveBomb.autoJump = preparedAutoJump;
        DiveBomb.instantDiveBomb = preparedInstantDiveBomb;
        DiveBomb.silentAim = preparedSilentAim;
        DiveBomb.diveBomb = preparedDiveBomb;
        DiveBomb.switchback = preparedSwitchback;
    }

    
    private static int sharedControlState;

    static {
        if (readSharedControlState() != 0) {
            writeSharedControlState(6);
        }
    }

    
    public static int readSharedControlState() {
        return sharedControlState;
    }

    
    public static void writeSharedControlState(int value) {
        sharedControlState = value;
    }

    
    public static int getSharedControlFallbackValue() {
        int state = readSharedControlState();
        return state == 0 ? 73 : 0;
    }
}
