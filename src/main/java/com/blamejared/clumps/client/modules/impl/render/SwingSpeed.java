package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.IntSetting;

public class SwingSpeed extends Module {
    public static IntSetting swingSpeed;

    public SwingSpeed() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Swing Speed"), com.blamejared.clumps.client.init.CatalogText.literal("Increases the speed of your swing animation"), com.blamejared.clumps.client.modules.ModuleCategory.RENDER, false));
    }

    @Override
    public void postInitialize() {
        swingSpeed.tooltip("Timing");
    }

}
