package dev.dov.hack.echo.client.render.player;

import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;

public interface EntityMaterialSettings {
    ColorSetting getColor1Setting();

    ColorSetting getColor2Setting();

    FloatSetting getLineWidthSetting();

    FloatSetting getOutlineOpacitySetting();

    FloatSetting getFillOpacitySetting();

    FloatSetting getGlowRadiusSetting();

    FloatSetting getGlowIntensitySetting();

    FloatSetting getGradientScaleSetting();

    
    float getAnimationPhase();

    
    default boolean usesTeamColors() {
        return false;
    }
}
