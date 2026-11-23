package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.handlers.rotation.AimTransition;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.rotation.RotationPriority;
import com.blamejared.clumps.client.handlers.rotation.RotationSettings;
import com.blamejared.clumps.client.handlers.rotation.RotationVisibility;
import com.blamejared.clumps.client.mixin.accessor.MinecraftClientAccessor;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.utils.CooldownTimer;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.items.PotionItems;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

public class SmartPot extends Module {
    private static BooleanSetting angleCheck;
    private static FloatRangeSetting fov;
    private static FloatRangeSetting minHealth;
    private static BooleanSetting throwPotion;
    private static BooleanSetting autoAim;
    private static BooleanSetting silentAim;
    private static FloatRangeSetting throwDelay;
    private static BooleanSetting autoBuff;
    private static BooleanSetting swapBack;
    private static FloatRangeSetting swapBackDelay;
    private CooldownTimer throwTimer;
    private CooldownTimer restoreTimer;
    private int speedAttemptTick;
    private int strengthAttemptTick;
    private boolean aimingForPotion;
    private boolean ownsRotation;

    public SmartPot() {
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Smart Pot"), com.blamejared.clumps.client.init.CatalogText.literal("Intelligent potion usage"), com.blamejared.clumps.client.modules.ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        throwTimer = new CooldownTimer();
        restoreTimer = new CooldownTimer();
        speedAttemptTick = -1;
        strengthAttemptTick = -1;
        aimingForPotion = false;
        ownsRotation = false;
        fov.setVisibilityPredicate(ignored -> angleCheck.getValue());
        autoAim.setVisibilityPredicate(ignored -> throwPotion.getValue());
        silentAim.setVisibilityPredicate(ignored -> throwPotion.getValue() && autoAim.getValue());
        throwDelay.setVisibilityPredicate(ignored -> throwPotion.getValue());
        swapBackDelay.setVisibilityPredicate(ignored -> swapBack.getValue());
        throwDelay.tooltip("Timing");
        swapBackDelay.tooltip("Timing");
    }

    @Subscribe
    private void afterMouseMovement(MouseMovementEvent.Post event) {
        if (!autoAim.getValue() || !throwPotion.getValue() || !canAct()) {
            if (aimingForPotion) finishAim();
            return;
        }
        boolean needsAim = hasHealingPotion() && needsHealing();
        if (needsAim) {
            beginAim();
            aimDownward();
        } else if (aimingForPotion) {
            finishAim();
        }
    }

    @Subscribe
    private void beforeInput(HandleInputEvent.Pre event) {
        if (!canAct()) return;
        updatePotions();
    }

    private void updatePotions() {
        selectHealingPotion();
        selectBuffPotions();
        if (swapBack.getValue() && DeferredActionQueue.contains(this)
                && restoreTimer.hasElapsedMillis((double) swapBackDelay.sampleGaussian())) {
            restoreSlot();
        }
    }

    private void selectHealingPotion() {
        if (!canUseHealingPotion()) return;
        if (PotionItems.holdsHealingPotion()) {
            useHeldPotionIfEnabled();
        } else {
            int slot = PotionItems.findHealingPotion();
            if (slot != -1) selectPotion(slot);
        }
    }

    private void selectBuffPotions() {
        if (!autoBuff.getValue()) return;
        if (angleCheck.getValue() && client.player.getPitch() < 90.0F - fov.sampleUniform()) return;
        if (client.player.hasStatusEffect(StatusEffects.SPEED)
                && client.player.hasStatusEffect(StatusEffects.STRENGTH)) return;
        if (speedAttemptTick != -1 && client.player.age <= speedAttemptTick) speedAttemptTick = -1;
        if (strengthAttemptTick != -1 && client.player.age <= strengthAttemptTick) strengthAttemptTick = -1;
        if (!client.player.hasStatusEffect(StatusEffects.SPEED) && client.player.age > speedAttemptTick) {
            if (useMatchingHeldPotion(PotionItems.holdsSpeedPotion())) {
                speedAttemptTick = client.player.age;
            } else {
                int slot = PotionItems.findSpeedPotion();
                if (slot != -1 && selectPotion(slot)) speedAttemptTick = client.player.age;
            }
        }
        if (client.player.hasStatusEffect(StatusEffects.STRENGTH) || client.player.age <= strengthAttemptTick) return;
        if (useMatchingHeldPotion(PotionItems.holdsStrengthPotion())) {
            strengthAttemptTick = client.player.age;
        } else {
            int slot = PotionItems.findStrengthPotion();
            if (slot != -1 && selectPotion(slot)) strengthAttemptTick = client.player.age;
        }
    }

    private boolean canUseHealingPotion() {
        float minimumPitch = 90.0F - fov.sampleUniform();
        float pitch;
        if (autoAim.getValue() && silentAim.getValue()) {
            pitch = rotations().effectiveRotation().pitch();
        } else {
            pitch = client.player.getPitch();
        }
        boolean angleAllowed = !angleCheck.getValue() || pitch >= minimumPitch;
        return client.player.isOnGround() && needsHealing() && angleAllowed;
    }

    private boolean selectPotion(int slot) {
        if (swapBack.getValue()) {
            if (!DeferredActionQueue.ensureSelected(this, slot, -1, false, null)) return false;
        } else {
            InventoryItems.selectHotbarSlot(slot);
        }
        restoreTimer.reset();
        if (throwPotion.getValue()) {
            tryThrowPotion();
            return true;
        }
        return false;
    }

    private void tryThrowPotion() {
        if (!throwTimer.hasElapsedMillis((double) throwDelay.sampleGaussian())) return;
        if (!actions().executeDoItemUse(this).succeeded()) return;
        throwTimer.reset();
    }

    private void useHeldPotionIfEnabled() {
        if (throwPotion.getValue()) tryThrowPotion();
    }

    private boolean useMatchingHeldPotion(boolean matches) {
        if (!matches) return false;
        useHeldPotionIfEnabled();
        return true;
    }

    private boolean canAct() {
        return !isOutsideWorld() && client.currentScreen == null;
    }

    private boolean hasHealingPotion() {
        return PotionItems.holdsHealingPotion() || PotionItems.findHealingPotion() != -1;
    }

    private boolean needsHealing() {
        return client.player.getHealth() <= minHealth.sampleUniform();
    }

    private void beginAim() {
        if (!aimingForPotion) aimingForPotion = true;
    }

    private void aimDownward() {
        float minimumPitch = 90.0F - fov.sampleUniform();
        Vec3d point = client.player.getEyePos().add(Vec3d.fromPolar(Math.max(minimumPitch, 89.9F), client.player.getYaw()));
        RotationSettings settings = RotationSettings.uniform(
                silentAim.getValue() ? RotationVisibility.SILENT : RotationVisibility.VISIBLE, AimTransition.EASED, 450.0F);
        if (rotations().aim(this, RotationPriority.CRITICAL, point, settings)) ownsRotation = true;
    }

    private void finishAim() {
        if (!ownsRotation) {
            aimingForPotion = false;
            return;
        }
        if (silentAim.getValue() && rotations().beginReturn(this)) return;
        ownsRotation = false;
        aimingForPotion = false;
    }

    private void restoreSlot() {
        DeferredActionQueue.finish(this);
        restoreTimer.reset();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, false);
        speedAttemptTick = -1;
        strengthAttemptTick = -1;
        aimingForPotion = false;
        ownsRotation = false;
        rotations().cancel(this);
        throwTimer.reset();
        restoreTimer.reset();
        super.onDisable();
    }

    
    public static void appendBootstrapSettings(SmartPot Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.angleCheck);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.fov);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.minHealth);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.throwPotion);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.autoAim);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.silentAim);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.throwDelay);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.autoBuff);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.swapBack);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, SmartPot.swapBackDelay);
    }

    
    public static void assignBootstrapSettings(SmartPot Module, com.blamejared.clumps.client.settings.BooleanSetting preparedAutoBuff, com.blamejared.clumps.client.settings.FloatRangeSetting preparedThrowDelay, com.blamejared.clumps.client.settings.BooleanSetting preparedThrowPotion, com.blamejared.clumps.client.settings.FloatRangeSetting preparedSwapBackDelay, com.blamejared.clumps.client.settings.BooleanSetting preparedSwapBack, com.blamejared.clumps.client.settings.BooleanSetting preparedSilentAim, com.blamejared.clumps.client.settings.BooleanSetting preparedAngleCheck, com.blamejared.clumps.client.settings.FloatRangeSetting preparedFov, com.blamejared.clumps.client.settings.FloatRangeSetting preparedMinHealth, com.blamejared.clumps.client.settings.BooleanSetting preparedAutoAim) {
        SmartPot.autoBuff = preparedAutoBuff;
        SmartPot.throwDelay = preparedThrowDelay;
        SmartPot.throwPotion = preparedThrowPotion;
        SmartPot.swapBackDelay = preparedSwapBackDelay;
        SmartPot.swapBack = preparedSwapBack;
        SmartPot.silentAim = preparedSilentAim;
        SmartPot.angleCheck = preparedAngleCheck;
        SmartPot.fov = preparedFov;
        SmartPot.minHealth = preparedMinHealth;
        SmartPot.autoAim = preparedAutoAim;
    }
}
