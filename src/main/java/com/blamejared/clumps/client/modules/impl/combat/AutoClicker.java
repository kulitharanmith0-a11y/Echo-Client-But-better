package com.blamejared.clumps.client.modules.impl.combat;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.FrameStartEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.mixin.accessor.KeyBindingAccessor;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.HotbarSlotsSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.utils.CooldownTimer;
import com.blamejared.clumps.client.utils.MathRng;

public class AutoClicker extends Module {
    private static final CharSequence randomizationOption2 = CatalogText.literal("None");
    private static final CharSequence randomizationOption = CatalogText.literal("Normal");
    private static FloatRangeSetting cps;
    private static ChoiceSetting randomization;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static BooleanSetting requireClick;
    private static BooleanSetting breakBlocks;
    private CooldownTimer clickRateTimer;
    private CooldownTimer burstTimer;
    private int burstClickCount;
    private long nextClickMillis;
    private double intervalScale;
    private double currentClicksPerSecond;
    private double targetClicksPerSecond;
    private long nextRateChangeMillis;
    private double previousIntervalMillis;
    private int similarIntervalCount;

    public AutoClicker() {
        super(new ModuleInfo(CatalogText.literal("Auto Clicker"), CatalogText.literal("Clicks the attack button for you"), ModuleCategory.COMBAT, false));
    }

    
    public static void appendBootstrapSettings(AutoClicker Module) {
        CatalogSettings.append(Module, cps);
        CatalogSettings.append(Module, randomization);
        CatalogSettings.append(Module, itemWhitelist);
        CatalogSettings.append(Module, hotbarWhitelist);
        CatalogSettings.append(Module, requireClick);
        CatalogSettings.append(Module, breakBlocks);
    }

    public static CharSequence bootstrapRandomizationOption() { return randomizationOption; }
    public static CharSequence bootstrapRandomizationOption2() { return randomizationOption2; }

    public static void assignBootstrapSettings(AutoClicker Module, HotbarSlotsSetting preparedHotbarWhitelist, BooleanSetting preparedBreakBlocks, ItemListSetting preparedItemWhitelist, BooleanSetting preparedRequireClick, FloatRangeSetting preparedCps, ChoiceSetting preparedRandomization) {
        hotbarWhitelist = preparedHotbarWhitelist;
        breakBlocks = preparedBreakBlocks;
        itemWhitelist = preparedItemWhitelist;
        requireClick = preparedRequireClick;
        cps = preparedCps;
        randomization = preparedRandomization;
    }

    @Override
    public void postInitialize() {
        clickRateTimer = new CooldownTimer();
        burstTimer = new CooldownTimer();
        intervalScale = 0.85;
        cps.setAllowOutOfRange(true);
        requireClick.description(CatalogText.literal("Only click while the attack button is held"));
        breakBlocks.description(CatalogText.literal("Keep clicking while breaking a block"));
        randomization.description(CatalogText.literal("None clicks at a fixed rate in the middle of the CPS range"));
        cps.tooltip(CatalogText.literal("Timing"));
        randomization.tooltip(CatalogText.literal("Timing"));
        itemWhitelist.tooltip(CatalogText.literal("Conditions"));
        hotbarWhitelist.tooltip(CatalogText.literal("Conditions"));
    }

    
    @Override
    public void onEnable() {
        super.onEnable();
        java.util.Objects.requireNonNull(clickRateTimer, "object reference is null").reset();
        float initialRate = java.util.Objects.requireNonNull(cps, "object reference is null").sampleGaussian();
        targetClicksPerSecond = initialRate;
        currentClicksPerSecond = initialRate;
        nextRateChangeMillis = 0L;
        nextClickMillis = 0L;
        java.util.Objects.requireNonNull(burstTimer, "object reference is null").reset();
        burstClickCount = 0;
    }
    @Subscribe
    private void onFrameStart(FrameStartEvent event) {
        long now = System.nanoTime() / 1_000_000L;
        if (shouldSuppressClick()) {
            nextClickMillis = Math.max(nextClickMillis, now);
            return;
        }
        if (now < nextClickMillis) return;
        client.attackCooldown = 0;
        KeyBindingAccessor attack = (KeyBindingAccessor) client.options.attackKey;
        attack.setTimesPressed(attack.getTimesPressed() + 1);
        nextClickMillis = Math.max(nextClickMillis, now - 40L) + computeClickIntervalMillis();
    }

    private boolean shouldSuppressClick() {
        return isOutsideWorld() || MinecraftRenderState.getScreen() != null
                || client.player.isUsingItem() || client.interactionManager == null
                || (!breakBlocks.getValue() && client.interactionManager.isBreakingBlock())
                || (requireClick.getValue() && !client.options.attackKey.isPressed())
                || !itemWhitelist.acceptsHeldItem() || !hotbarWhitelist.permitsCurrentSlot();
    }

    private long computeClickIntervalMillis() {
        if (randomization.isSelected(randomizationOption2)) {
            return (long) (2000.0 / (cps.getLower() + cps.getUpper()));
        }
        if (clickRateTimer.hasElapsedMillis((double) nextRateChangeMillis)) {
            targetClicksPerSecond = cps.sampleGaussian();
            nextRateChangeMillis = (long) (2500.0 + MathRng.random.nextDouble() * 3500.0);
            clickRateTimer.reset();
        }
        currentClicksPerSecond += (targetClicksPerSecond - currentClicksPerSecond) * 0.05;
        currentClicksPerSecond = Math.max(cps.getLower(), Math.min(cps.getUpper(), currentClicksPerSecond));
        double nominalInterval = 1000.0 / currentClicksPerSecond;
        double scaledInterval = nominalInterval * intervalScale;
        double interval = scaledInterval + MathRng.random.nextGaussian() * scaledInterval * 0.2
                - Math.log(1.0 - MathRng.random.nextDouble()) * scaledInterval * 0.1;
        if (MathRng.random.nextDouble() < 0.04) {
            interval += 140.0 + MathRng.random.nextDouble() * 260.0;
        }
        interval = Math.max(scaledInterval * 0.55, interval);
        double similarityThreshold = scaledInterval * 0.3;
        if (Math.abs(interval - previousIntervalMillis) < similarityThreshold && ++similarIntervalCount >= 5) {
            interval += 60.0 + MathRng.random.nextDouble() * 100.0;
            similarIntervalCount = 0;
        } else if (Math.abs(interval - previousIntervalMillis) >= similarityThreshold) {
            similarIntervalCount = 0;
        }
        previousIntervalMillis = interval;
        intervalScale = Math.max(0.5, Math.min(1.1, intervalScale + (nominalInterval - interval) / nominalInterval * 0.03));
        return (long) interval;
    }
}
