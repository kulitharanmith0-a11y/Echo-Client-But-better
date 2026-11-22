package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.event.input.MouseMovementEvent;
import dev.dov.hack.echo.client.event.input.PlayerInputEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.handlers.rotation.AimTransition;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.handlers.rotation.RotationPriority;
import dev.dov.hack.echo.client.handlers.rotation.RotationSettings;
import dev.dov.hack.echo.client.handlers.rotation.RotationVisibility;
import dev.dov.hack.echo.client.keyboard.KeybindingCache;
import dev.dov.hack.echo.client.mixin.accessor.MinecraftClientAccessor;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.KeybindSetting;
import dev.dov.hack.echo.client.utils.CooldownTimer;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public class KeyCharge extends Module {
    private static BooleanSetting autoJump;
    private static BooleanSetting autoAim;
    private static BooleanSetting silentAim;
    private static BooleanSetting switchback;
    private static BooleanSetting silentSwap;
    private static KeybindSetting activateKey;
    private boolean activationPending;
    private boolean previousActivationPressed;
    private CooldownTimer chargeTimer;
    private boolean chargeRotationActive;

    public KeyCharge() {
        super(new ModuleInfo(CatalogText.literal("Key Charge"), CatalogText.literal("Charge attacks with key"), ModuleCategory.MOVEMENT));
    }

    public static void appendBootstrapSettings(KeyCharge Module) {
        CatalogSettings.append(Module, autoJump);
        CatalogSettings.append(Module, autoAim);
        CatalogSettings.append(Module, silentAim);
        CatalogSettings.append(Module, switchback);
        CatalogSettings.append(Module, silentSwap);
        CatalogSettings.append(Module, activateKey);
    }

    public static void assignBootstrapSettings(KeyCharge Module, BooleanSetting preparedSwitchback, KeybindSetting preparedActivateKey, BooleanSetting preparedSilentSwap, BooleanSetting preparedAutoJump, BooleanSetting preparedSilentAim, BooleanSetting preparedAutoAim) {
        switchback = preparedSwitchback;
        activateKey = preparedActivateKey;
        silentSwap = preparedSilentSwap;
        autoJump = preparedAutoJump;
        silentAim = preparedSilentAim;
        autoAim = preparedAutoAim;
    }

    @Override
    public void postInitialize() {
        activationPending = false;
        previousActivationPressed = false;
        chargeTimer = new CooldownTimer();
        chargeRotationActive = false;
        silentAim.setVisibilityPredicate(ignored -> autoAim.getValue());
        silentSwap.setVisibilityPredicate(ignored -> switchback.getValue());
    }

    @Override
    public void onDisable() {
        if (chargeRotationActive && rotations().isOwnedBy(this)) rotations().cancel(this);
        chargeRotationActive = false;
        activationPending = false;
        previousActivationPressed = false;
        DeferredActionQueue.finish(this, false);
        chargeTimer.reset();
        super.onDisable();
    }

    private boolean hasWindCharge() {
        return InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE) != -1;
    }

    private void finishChargeRotation() {
        if (!chargeRotationActive) return;
        if (silentAim.getValue() && rotations().beginReturn(this)) return;
        chargeRotationActive = false;
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (!hasWindCharge()) return;
        if (isOutsideWorld()) return;
        if (client.currentScreen != null) return;
        event.jump = KeybindingCache.isPressed(client.options.jumpKey);
        boolean activationPressed = activateKey.getKey().isPressed();
        if (activationPressed && !previousActivationPressed) {
            if (rotations().snapshot().target() != null && !rotations().canRequest(this, RotationPriority.CRITICAL)) return;
            activationPending = true;
        }
        previousActivationPressed = activationPressed;
        if (!activationPending) return;
        if (client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE))) return;
        if (autoAim.getValue()) {
            if (!rotations().isAiming()) return;
            float pitch = silentAim.getValue() ? rotations().effectiveRotation().pitch() : client.player.getPitch();
            if (pitch < 88.0F) return;
        }
        int slot = InventoryItems.findStackInHotbar(stack -> stack.getItem() == Items.WIND_CHARGE);
        if (slot == -1) return;
        if (!DeferredActionQueue.ensureSelected(this, slot, -1, false)) return;
        if (!actions().executeDoItemUse(this).succeeded()) return;
        if (silentSwap.getValue()) {
            DeferredActionQueue.finish(this, switchback.getValue());
        } else if (!switchback.getValue()) {
            DeferredActionQueue.finish(this, false);
        }
        chargeTimer.reset();
        if (autoJump.getValue() && client.player.isOnGround()
                && (autoAim.getValue() || !(client.player.getPitch() < 80.0F))) {
            event.jump = true;
        }
        activationPending = false;
    }

    @Subscribe
    public void onMouseMovement(MouseMovementEvent.Post event) {
        if (isOutsideWorld()) return;
        if (client.currentScreen != null) return;
        if (!hasWindCharge() && chargeRotationActive) {
            if (!chargeTimer.hasElapsedTicks(1)) return;
            finishChargeRotation();
            return;
        }
        if (!hasWindCharge()) return;
        if (!autoAim.getValue()) return;
        if (activationPending) {
            float yaw;
            if (rotations().isAiming() && rotations().isSilentRotationActive()) {
                yaw = rotations().effectiveRotation().yaw();
            } else {
                yaw = silentAim.getValue() ? rotations().effectiveRotation().yaw() : client.player.getYaw();
            }
            Vec3d aimPoint = client.player.getEyePos().add(Vec3d.fromPolar(89.9F, yaw));
            rotations().aim(this, RotationPriority.CRITICAL, aimPoint, RotationSettings.uniform(silentAim.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, AimTransition.EASED, 450.0F));
            chargeRotationActive = true;
        } else if (chargeRotationActive) {
            finishChargeRotation();
        }
    }

    @Subscribe
    public void beforeInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) return;
        if (client.currentScreen != null) return;
        if (!switchback.getValue() || silentSwap.getValue()) return;
        if (!DeferredActionQueue.contains(this)) return;
        if (chargeTimer.hasElapsedTicks(1)) DeferredActionQueue.finish(this);
    }

    public static boolean hasPendingSlotChange() {
        return DeferredActionQueue.contains(KeyCharge.class);
    }
}
