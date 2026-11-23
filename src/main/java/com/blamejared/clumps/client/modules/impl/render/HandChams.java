package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.render.player.EntityMaterialSettings;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.gfx.HandChamsRenderer;

public class HandChams extends Module implements EntityMaterialSettings {
    private static ColorSetting color1;
    private static ColorSetting color2;
    private static FloatSetting lineWidth;
    private static FloatSetting outlineOpacity;
    private static FloatSetting fillOpacity;
    private static FloatSetting glowRadius;
    private static FloatSetting glowIntensity;
    private static FloatSetting speed;
    private static FloatSetting gradientScale;
    private float animationPhase;
    private long previousAnimationNanos;

    public HandChams() {
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Hand Chams"), com.blamejared.clumps.client.init.CatalogText.literal("Gradient outline shader on your hand"), com.blamejared.clumps.client.modules.ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        color1.tooltip("Appearance");
        color2.tooltip("Appearance");
        lineWidth.tooltip("Appearance");
        outlineOpacity.tooltip("Appearance");
        fillOpacity.tooltip("Appearance");
        glowRadius.tooltip("Appearance");
        glowIntensity.tooltip("Appearance");
        speed.tooltip("Timing");
        gradientScale.tooltip("Appearance");
    }

    @Override
    public ColorSetting getColor1Setting() {
        return color1;
    }

    @Override
    public ColorSetting getColor2Setting() {
        return color2;
    }

    @Override
    public FloatSetting getLineWidthSetting() {
        return lineWidth;
    }

    @Override
    public FloatSetting getOutlineOpacitySetting() {
        return outlineOpacity;
    }

    @Override
    public FloatSetting getFillOpacitySetting() {
        return fillOpacity;
    }

    @Override
    public FloatSetting getGlowRadiusSetting() {
        return glowRadius;
    }

    @Override
    public FloatSetting getGlowIntensitySetting() {
        return glowIntensity;
    }

    @Override
    public FloatSetting getGradientScaleSetting() {
        return gradientScale;
    }

    @Override
    public float getAnimationPhase() {
        boolean paused = Freecam.isPerspectiveRefreshBypassed();
        long now = System.nanoTime();
        if (paused) {
            return animationPhase;
        }
        if (previousAnimationNanos != 0L) {
            float elapsedSeconds = (float) (now - previousAnimationNanos) / 1.0E9f;
            animationPhase = (animationPhase + elapsedSeconds * speed.getValue()) % 1.0f;
        }
        previousAnimationNanos = now;
        return animationPhase;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        previousAnimationNanos = 0L;
        HandChamsRenderer.reset();
    }

    
    public static void appendBootstrapSettings(HandChams Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, HandChams.color1);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, HandChams.color2);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, HandChams.lineWidth);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, HandChams.outlineOpacity);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, HandChams.fillOpacity);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, HandChams.glowRadius);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, HandChams.glowIntensity);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, HandChams.speed);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, HandChams.gradientScale);
    }

    
    public static void assignBootstrapSettings(HandChams Module, com.blamejared.clumps.client.settings.FloatSetting preparedLineWidth, com.blamejared.clumps.client.settings.FloatSetting preparedGlowRadius, com.blamejared.clumps.client.settings.FloatSetting preparedOutlineOpacity, com.blamejared.clumps.client.settings.FloatSetting preparedFillOpacity, com.blamejared.clumps.client.settings.ColorSetting preparedColor2, com.blamejared.clumps.client.settings.ColorSetting preparedColor1, com.blamejared.clumps.client.settings.FloatSetting preparedGradientScale, com.blamejared.clumps.client.settings.FloatSetting preparedGlowIntensity, com.blamejared.clumps.client.settings.FloatSetting preparedSpeed) {
        HandChams.lineWidth = preparedLineWidth;
        HandChams.glowRadius = preparedGlowRadius;
        HandChams.outlineOpacity = preparedOutlineOpacity;
        HandChams.fillOpacity = preparedFillOpacity;
        HandChams.color2 = preparedColor2;
        HandChams.color1 = preparedColor1;
        HandChams.gradientScale = preparedGradientScale;
        HandChams.glowIntensity = preparedGlowIntensity;
        HandChams.speed = preparedSpeed;
    }
}
