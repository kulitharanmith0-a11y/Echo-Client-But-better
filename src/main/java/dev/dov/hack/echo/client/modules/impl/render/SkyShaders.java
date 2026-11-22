package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;

import static dev.dov.hack.echo.client.modules.ModuleLiterals.literal;

public class SkyShaders extends Module {
    private static final CharSequence AURORA_LABEL = literal("Aurora");
    private static final CharSequence TWILIGHT_LABEL = literal("Twilight");
    public static final int NEBULA = 0;
    public static final int AURORA = 1;
    public static final int TWILIGHT = 2;
    private static ChoiceSetting mode;
    private static BooleanSetting customColors;
    private static ColorSetting primaryColor;
    private static ColorSetting secondaryColor;
    private static FloatSetting speed;
    private static FloatSetting brightness;
    private static FloatSetting stars;
    private static BooleanSetting sunAndMoon;
    private static BooleanSetting endDimension;
    private static String customColorsTooltip;
    private static String colorTooltip;
    private static String speedTooltip;
    private static final float[][] PRIMARY_PALETTES = {
            {0.27f, 0.75f, 0.59f}, {0.35f, 1.0f, 0.62f}, {0.24f, 0.2f, 0.58f}
    };
    private static final float[][] SECONDARY_PALETTES = {
            {0.14f, 0.22f, 0.43f}, {0.55f, 0.3f, 1.0f}, {0.62f, 0.48f, 0.76f}
    };

    public SkyShaders() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Sky Shaders"), dev.dov.hack.echo.client.init.CatalogText.literal("Replaces the sky with a procedural shader"), dev.dov.hack.echo.client.modules.ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        primaryColor.setVisibilityPredicate(ignored -> customColors.getValue());
        secondaryColor.setVisibilityPredicate(ignored -> customColors.getValue());
        if (customColorsTooltip == null) customColorsTooltip = "Appearance";
        customColors.tooltip(customColorsTooltip);
        if (colorTooltip == null) colorTooltip = "Appearance";
        primaryColor.tooltip(colorTooltip);
        secondaryColor.tooltip(colorTooltip);
        if (speedTooltip == null) speedTooltip = "Timing";
        speed.tooltip(speedTooltip);
    }

    public static int getShaderMode() {
        if (mode.isSelected(AURORA_LABEL)) return AURORA;
        if (mode.isSelected(TWILIGHT_LABEL)) return TWILIGHT;
        return NEBULA;
    }

    public static float getPrimaryChannel(int channel) {
        if (customColors.getValue()) return getColorChannel(primaryColor, channel);
        return PRIMARY_PALETTES[getShaderMode()][channel];
    }

    public static float getSecondaryChannel(int channel) {
        if (customColors.getValue()) return getColorChannel(secondaryColor, channel);
        return SECONDARY_PALETTES[getShaderMode()][channel];
    }

    private static float getColorChannel(ColorSetting color, int channel) {
        int value = switch (channel) {
            case 0 -> color.getRed();
            case 1 -> color.getGreen();
            default -> color.getBlue();
        };
        return value / 255.0f;
    }

    public static float getSpeed() { return speed.getValue(); }
    public static float getBrightness() { return brightness.getValue(); }
    public static float getStars() { return stars.getValue(); }
    public static boolean shouldRenderSunAndMoon() { return sunAndMoon.getValue(); }
    public static boolean shouldRenderInEnd() { return endDimension.getValue(); }

    
    public static void appendBootstrapSettings(SkyShaders Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SkyShaders.mode);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SkyShaders.customColors);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SkyShaders.primaryColor);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SkyShaders.secondaryColor);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SkyShaders.speed);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SkyShaders.brightness);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SkyShaders.stars);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SkyShaders.sunAndMoon);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SkyShaders.endDimension);
    }

    
    public static void assignBootstrapSettings(SkyShaders Module, dev.dov.hack.echo.client.settings.FloatSetting preparedStars, dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode, dev.dov.hack.echo.client.settings.ColorSetting preparedSecondaryColor, dev.dov.hack.echo.client.settings.BooleanSetting preparedEndDimension, dev.dov.hack.echo.client.settings.FloatSetting preparedBrightness, dev.dov.hack.echo.client.settings.BooleanSetting preparedCustomColors, dev.dov.hack.echo.client.settings.BooleanSetting preparedSunAndMoon, dev.dov.hack.echo.client.settings.FloatSetting preparedSpeed, dev.dov.hack.echo.client.settings.ColorSetting preparedPrimaryColor) {
        SkyShaders.stars = preparedStars;
        SkyShaders.mode = preparedMode;
        SkyShaders.secondaryColor = preparedSecondaryColor;
        SkyShaders.endDimension = preparedEndDimension;
        SkyShaders.brightness = preparedBrightness;
        SkyShaders.customColors = preparedCustomColors;
        SkyShaders.sunAndMoon = preparedSunAndMoon;
        SkyShaders.speed = preparedSpeed;
        SkyShaders.primaryColor = preparedPrimaryColor;
    }
}
