package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.render.FireOverlayEvent;
import com.blamejared.clumps.client.event.render.FloatingItemOverlayEvent;
import com.blamejared.clumps.client.event.render.NauseaOverlayEvent;
import com.blamejared.clumps.client.event.render.PortalOverlayEvent;
import com.blamejared.clumps.client.event.render.SpyglassOverlayEvent;
import com.blamejared.clumps.client.event.render.VignetteOverlayEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.BooleanSetting;
import static com.blamejared.clumps.client.modules.ModuleLiterals.literal;

public class NoRender extends Module {
    public static BooleanSetting noFireOverlay;
    public static BooleanSetting noNausea;
    public static BooleanSetting noVignette;
    public static BooleanSetting noFloatingItem;
    public static BooleanSetting noSpyglassOverlay;
    public static BooleanSetting noPortalOverlay;
    public static BooleanSetting noCrystalBounce;
    private boolean renderSuppressionActive;

    public NoRender() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("No Render"), com.blamejared.clumps.client.init.CatalogText.literal("Disables certain rendering"), com.blamejared.clumps.client.modules.ModuleCategory.RENDER, false));
    }

    @Override
    public void postInitialize() {
        renderSuppressionActive = false;
        super.postInitialize();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        renderSuppressionActive = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        renderSuppressionActive = false;
    }

    @Subscribe
    public final void onFireOverlay(FireOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noFireOverlay.getValue()) event.cancel();
    }

    @Subscribe
    public final void onNauseaOverlay(NauseaOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noNausea.getValue()) event.cancel();
    }

    @Subscribe
    public final void onVignetteOverlay(VignetteOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noVignette.getValue()) event.cancel();
    }

    @Subscribe
    public final void onFloatingItemOverlay(FloatingItemOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noFloatingItem.getValue()) event.cancel();
    }

    @Subscribe
    public final void onSpyglassOverlay(SpyglassOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noSpyglassOverlay.getValue()) event.cancel();
    }

    @Subscribe
    public final void onPortalOverlay(PortalOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noPortalOverlay.getValue()) event.cancel();
    }

}
