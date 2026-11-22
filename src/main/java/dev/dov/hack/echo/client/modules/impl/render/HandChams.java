package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.render.player.EntityMaterialSettings;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.gfx.HandChamsRenderer;

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
        super(new dev.dov.hack.echo.client.modules.ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Hand Chams"), dev.dov.hack.echo.client.init.CatalogText.literal("Gradient outline shader on your hand"), dev.dov.hack.echo.client.modules.ModuleCategory.RENDER));
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
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HandChams.color1);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HandChams.color2);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HandChams.lineWidth);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HandChams.outlineOpacity);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HandChams.fillOpacity);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HandChams.glowRadius);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HandChams.glowIntensity);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HandChams.speed);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, HandChams.gradientScale);
    }

    
    public static void assignBootstrapSettings(HandChams Module, dev.dov.hack.echo.client.settings.FloatSetting preparedLineWidth, dev.dov.hack.echo.client.settings.FloatSetting preparedGlowRadius, dev.dov.hack.echo.client.settings.FloatSetting preparedOutlineOpacity, dev.dov.hack.echo.client.settings.FloatSetting preparedFillOpacity, dev.dov.hack.echo.client.settings.ColorSetting preparedColor2, dev.dov.hack.echo.client.settings.ColorSetting preparedColor1, dev.dov.hack.echo.client.settings.FloatSetting preparedGradientScale, dev.dov.hack.echo.client.settings.FloatSetting preparedGlowIntensity, dev.dov.hack.echo.client.settings.FloatSetting preparedSpeed) {
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
