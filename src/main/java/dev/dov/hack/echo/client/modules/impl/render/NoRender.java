package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.render.FireOverlayEvent;
import dev.dov.hack.echo.client.event.render.FloatingItemOverlayEvent;
import dev.dov.hack.echo.client.event.render.NauseaOverlayEvent;
import dev.dov.hack.echo.client.event.render.PortalOverlayEvent;
import dev.dov.hack.echo.client.event.render.SpyglassOverlayEvent;
import dev.dov.hack.echo.client.event.render.VignetteOverlayEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import static dev.dov.hack.echo.client.modules.ModuleLiterals.literal;

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
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("No Render"), dev.dov.hack.echo.client.init.CatalogText.literal("Disables certain rendering"), dev.dov.hack.echo.client.modules.ModuleCategory.RENDER, false));
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
