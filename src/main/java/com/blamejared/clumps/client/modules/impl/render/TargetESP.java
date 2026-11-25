package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.render.player.TargetMotionTrail;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.ColorSetting;

public class TargetESP extends Module {
    public static TargetESP instance;
    private static final CharSequence modeOption = CatalogText.literal("Circle");
    private static final CharSequence chainsModeOption = CatalogText.literal("Chains");
    public static ChoiceSetting mode;
    private static ColorSetting color;
    private TargetMotionTrail trail;

    public TargetESP() {
        super(new com.blamejared.clumps.client.modules.ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Target ESP"), com.blamejared.clumps.client.init.CatalogText.literal("Highlights current target"), com.blamejared.clumps.client.modules.ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        trail = new TargetMotionTrail(color, 48, 0.0f);
        instance = this;
        mode.onChange(trail::clearHistory);
        color.tooltip("Appearance");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        TargetMotionTrail currentTrail = trail;
        currentTrail.clearHistory();
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        boolean checkMode = Freecam.isPerspectiveRefreshEnabled();
        boolean render = isOutsideWorld();
        if (checkMode) {
            if (render) {
                return;
            }
            render = mode.isSelected(modeOption);
        }
        if (render) {
            trail.render(event);
        }
    }

    
    public static void appendBootstrapSettings(TargetESP Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TargetESP.mode);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, TargetESP.color);
    }

    
    public static java.lang.CharSequence bootstrapModeOption() {
        return modeOption;
    }

    
    public static void assignBootstrapSettings(TargetESP Module, com.blamejared.clumps.client.settings.ColorSetting preparedColor, com.blamejared.clumps.client.settings.ChoiceSetting preparedMode) {
        TargetESP.color = preparedColor;
        TargetESP.mode = preparedMode;
    }
}
