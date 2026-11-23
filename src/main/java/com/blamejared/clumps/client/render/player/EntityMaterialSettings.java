package com.blamejared.clumps.client.render.player;

import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.FloatSetting;

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
