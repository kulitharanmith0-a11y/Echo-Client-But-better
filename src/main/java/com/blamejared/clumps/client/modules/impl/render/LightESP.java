package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.IntSetting;
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
        outline.onChange(() -> ClumpsClient.ChunkScanManager.setOutline(this, outline.getValue()));
        tracers.onChange(() -> ClumpsClient.ChunkScanManager.setTracers(this, tracers.getValue()));
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
        ClumpsClient.ChunkScanManager.unregister(this);
    }

    private void rebuildLightSources() {
        if (isOutsideWorld() || !isEnabled()) return;
        Object2IntOpenHashMap<Block> colors = new Object2IntOpenHashMap<>();
        int minimum = minLight.getValue();
        for (Block block : Registries.BLOCK) {
            int luminance = block.getDefaultState().getLuminance();
            if (luminance >= minimum) colors.put(block, packedColorForLightLevel(luminance));
        }
        ClumpsClient.ChunkScanManager.register(this, colors, outline.getValue(), tracers.getValue());
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
