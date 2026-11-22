package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

public class LightESP extends Module {
    private static IntSetting maxY;
    private static IntSetting minLight;
    private static ColorSetting color;
    private static BooleanSetting outline;
    private static BooleanSetting tracers;
    private static String colorTooltip;
    private static String renderTooltip;

    public LightESP() {
        super(new ModuleInfo(CatalogText.literal("Light ESP"), CatalogText.literal("Scans for out of place light sources"), ModuleCategory.RENDER, false));
    }

    @Override
    public void postInitialize() {
        maxY.onChange(this::rebuildLightSources);
        minLight.onChange(this::rebuildLightSources);
        color.onChange(this::rebuildLightSources);
        outline.onChange(() -> EchoClient.ChunkScanManager.setOutline(this, outline.getValue()));
        tracers.onChange(() -> EchoClient.ChunkScanManager.setTracers(this, tracers.getValue()));
        if (colorTooltip == null) colorTooltip = "Appearance";
        color.tooltip(colorTooltip);
        if (renderTooltip == null) renderTooltip = "Appearance";
        outline.tooltip(renderTooltip);
        tracers.tooltip(renderTooltip);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        rebuildLightSources();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        EchoClient.ChunkScanManager.unregister(this);
    }

    private void rebuildLightSources() {
        if (isOutsideWorld() || !isEnabled()) return;
        Object2IntOpenHashMap<Block> colors = new Object2IntOpenHashMap<>();
        int minimum = minLight.getValue();
        for (Block block : Registries.BLOCK) {
            int luminance = block.getDefaultState().getLuminance();
            if (luminance >= minimum) colors.put(block, packedColorForLightLevel(luminance));
        }
        EchoClient.ChunkScanManager.register(this, colors, outline.getValue(), tracers.getValue());
    }

    private float[] colorForLightLevel(int lightLevel) {
        return LightSourceColors.rgba(lightLevel, color);
    }

    private int packedColorForLightLevel(int lightLevel) {
        float[] rgba = colorForLightLevel(lightLevel);
        return (int) (rgba[3] * 255.0f) << 24
                | (int) (rgba[0] * 255.0f) << 16
                | (int) (rgba[1] * 255.0f) << 8
                | (int) (rgba[2] * 255.0f);
    }

    
    public static void appendBootstrapSettings(LightESP Module) {
        CatalogSettings.append(Module, maxY);
        CatalogSettings.append(Module, minLight);
        CatalogSettings.append(Module, color);
        CatalogSettings.append(Module, outline);
        CatalogSettings.append(Module, tracers);
    }

    
    public static void assignBootstrapSettings(LightESP Module, IntSetting preparedMaxY, IntSetting preparedMinLight, BooleanSetting preparedTracers, BooleanSetting preparedOutline, ColorSetting preparedColor) {
        maxY = preparedMaxY;
        minLight = preparedMinLight;
        tracers = preparedTracers;
        outline = preparedOutline;
        color = preparedColor;
    }
}
