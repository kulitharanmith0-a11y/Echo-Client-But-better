package com.blamejared.clumps.client.modules.internals;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import org.lwjgl.system.Platform;

public class ClickGui extends Module {
    public static FloatSetting scrollSpeed;
    public static BooleanSetting streamProof;
    public static final boolean SUPPORTS_STREAM_PROOF;
    private static final String SCROLL_SPEED_TOOLTIP;
    private static final String APPEARANCE_TOOLTIP = "Appearance";
    private static final CharSequence THEME_OBSIDIAN = CatalogText.literal("Obsidian");
    private static final CharSequence THEME_EMERALD = CatalogText.literal("Emerald");
    private static final CharSequence THEME_VIOLET = CatalogText.literal("Midnight Violet");
    private static final CharSequence THEME_OCEAN = CatalogText.literal("Ocean Blue");
    private static final CharSequence THEME_CRIMSON = CatalogText.literal("Crimson");
    private static final CharSequence THEME_CUSTOM = CatalogText.literal("Custom");

    public static final ChoiceSetting theme = new ChoiceSetting(
            CatalogText.literal("Theme"),
            THEME_OBSIDIAN,
            THEME_OBSIDIAN, THEME_EMERALD, THEME_VIOLET, THEME_OCEAN, THEME_CRIMSON, THEME_CUSTOM);
    public static final ColorSetting accentColor = new ColorSetting(
            CatalogText.literal("Accent Color"), 52, 211, 153, 255);
    public static final ColorSetting panelColor = new ColorSetting(
            CatalogText.literal("Panel Color"), 20, 22, 28, 232);
    public static final FloatSetting panelOpacity = new FloatSetting(
            CatalogText.literal("Panel Opacity"), 1.0F, 0.50F, 1.0F, 0.05F);
    public static final BooleanSetting backgroundBlur = new BooleanSetting(
            CatalogText.literal("Background Blur"), true);
    public static final FloatSetting backgroundDim = new FloatSetting(
            CatalogText.literal("Background Dim"), 0.55F, 0.0F, 0.85F, 0.05F);
    public static final FloatSetting uiScale = new FloatSetting(
            CatalogText.literal("UI Scale"), 1.0F, 0.75F, 1.25F, 0.05F, CatalogText.literal("x"));

    private static boolean syncingStreamProof;

    static {
        SCROLL_SPEED_TOOLTIP = "Timing";
        SUPPORTS_STREAM_PROOF =
                Platform.get() == Platform.WINDOWS || Platform.get() == Platform.MACOSX;
    }

    public ClickGui() {
        super(new ModuleInfo(CatalogText.literal("Click GUI"), CatalogText.literal("Click-based GUI"), ModuleCategory.INTERNALS, 344));
    }

    @Override
    public void postInitialize() {
        streamProof.onChange(ClickGui::applyStreamProofSetting);
        streamProof.setVisibilityPredicate(ClickGui::isStreamProofSettingVisible);
        scrollSpeed.tooltip(SCROLL_SPEED_TOOLTIP);

        theme.tooltip(APPEARANCE_TOOLTIP);
        accentColor.tooltip(APPEARANCE_TOOLTIP);
        panelColor.tooltip(APPEARANCE_TOOLTIP);
        panelOpacity.tooltip(APPEARANCE_TOOLTIP);
        backgroundBlur.tooltip(APPEARANCE_TOOLTIP);
        backgroundDim.tooltip(APPEARANCE_TOOLTIP);
        uiScale.tooltip(APPEARANCE_TOOLTIP);

        accentColor.setVisibilityPredicate(ignored -> theme.isSelected(THEME_CUSTOM));
        panelColor.setVisibilityPredicate(ignored -> theme.isSelected(THEME_CUSTOM));

        theme.description(CatalogText.literal("Color preset for panels, accents, toggles, and borders"));
        accentColor.description(CatalogText.literal("Custom highlight color for active toggles, sliders, and selection indicators"));
        panelColor.description(CatalogText.literal("Custom surface tint for ClickGUI panels"));
        panelOpacity.description(CatalogText.literal("Controls panel background opacity"));
        backgroundBlur.description(CatalogText.literal("Blurs the game world behind the ClickGUI"));
        backgroundDim.description(CatalogText.literal("Darkness of the screen backdrop behind the ClickGUI"));
        uiScale.description(CatalogText.literal("Scales the ClickGUI up or down"));
    }

    public static void appendAppearanceSettings(ClickGui module) {
        CatalogSettings.append(module, theme);
        CatalogSettings.append(module, accentColor);
        CatalogSettings.append(module, panelColor);
        CatalogSettings.append(module, panelOpacity);
        CatalogSettings.append(module, backgroundBlur);
        CatalogSettings.append(module, backgroundDim);
        CatalogSettings.append(module, uiScale);
    }

    public static boolean isDefaultObsidianTheme() {
        return theme.isSelected(THEME_OBSIDIAN);
    }

    public static int accentArgb() {
        if (theme.isSelected(THEME_EMERALD)) return 0xFF34D399;
        if (theme.isSelected(THEME_VIOLET)) return 0xFFA78BFA;
        if (theme.isSelected(THEME_OCEAN)) return 0xFF38BDF8;
        if (theme.isSelected(THEME_CRIMSON)) return 0xFFFB7185;
        if (theme.isSelected(THEME_CUSTOM)) return (accentColor.getArgb() & 0x00FFFFFF) | 0xFF000000;
        return 0xFFE5E7EB;
    }

    public static int panelSurfaceRgb() {
        if (theme.isSelected(THEME_EMERALD)) return 0x0F1916;
        if (theme.isSelected(THEME_VIOLET)) return 0x151221;
        if (theme.isSelected(THEME_OCEAN)) return 0x0E1622;
        if (theme.isSelected(THEME_CRIMSON)) return 0x1D1115;
        if (theme.isSelected(THEME_CUSTOM)) return panelColor.getArgb() & 0x00FFFFFF;
        return 0x0C0C0C;
    }

    public static int tintPanelFill(int defaultFill) {
        if (defaultFill == 0) {
            return 0;
        }
        float opacityMul = panelOpacityFactor();
        int baseAlpha = (defaultFill >>> 24) & 0xFF;
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * opacityMul)));
        if (isDefaultObsidianTheme()) {
            return (scaledAlpha << 24) | (defaultFill & 0x00FFFFFF);
        }
        int baseR = (defaultFill >>> 16) & 0xFF;
        int baseG = (defaultFill >>> 8) & 0xFF;
        int baseB = defaultFill & 0xFF;
        // Only retint dark neutral dashboard surfaces; leave bright semantic badges/text untouched.
        int maxChannel = Math.max(baseR, Math.max(baseG, baseB));
        int minChannel = Math.min(baseR, Math.min(baseG, baseB));
        if (maxChannel <= 46 && (maxChannel - minChannel) <= 18) {
            int surf = panelSurfaceRgb();
            int sr = (surf >>> 16) & 0xFF;
            int sg = (surf >>> 8) & 0xFF;
            int sb = surf & 0xFF;
            float lift = maxChannel / 46.0F;
            int r = Math.min(255, Math.round(sr + lift * 14.0F));
            int g = Math.min(255, Math.round(sg + lift * 14.0F));
            int b = Math.min(255, Math.round(sb + lift * 14.0F));
            return (scaledAlpha << 24) | (r << 16) | (g << 8) | b;
        }
        return (scaledAlpha << 24) | (defaultFill & 0x00FFFFFF);
    }

    public static int tintPanelBorder(int defaultBorder) {
        if (defaultBorder == 0 || isDefaultObsidianTheme()) {
            return defaultBorder;
        }
        int baseAlpha = (defaultBorder >>> 24) & 0xFF;
        int r = (defaultBorder >>> 16) & 0xFF;
        int g = (defaultBorder >>> 8) & 0xFF;
        int b = defaultBorder & 0xFF;
        int maxChannel = Math.max(r, Math.max(g, b));
        int minChannel = Math.min(r, Math.min(g, b));
        if (maxChannel <= 72 && (maxChannel - minChannel) <= 20) {
            int accent = accentArgb();
            int ar = (accent >>> 16) & 0xFF;
            int ag = (accent >>> 8) & 0xFF;
            int ab = accent & 0xFF;
            int nr = Math.round(r * 0.68F + ar * 0.32F);
            int ng = Math.round(g * 0.68F + ag * 0.32F);
            int nb = Math.round(b * 0.68F + ab * 0.32F);
            return (baseAlpha << 24) | (nr << 16) | (ng << 8) | nb;
        }
        return defaultBorder;
    }

    public static float panelOpacityFactor() {
        return Math.max(0.50F, Math.min(1.0F, panelOpacity.getValue()));
    }

    public static boolean backgroundBlurEnabled() {
        return backgroundBlur.getValue();
    }

    public static int backgroundDimArgb() {
        int alpha = Math.max(0, Math.min(230, Math.round(backgroundDim.getValue() * 255.0F)));
        return alpha << 24;
    }

    public static float uiScaleFactor() {
        return Math.max(0.75F, Math.min(1.25F, uiScale.getValue()));
    }

    @Override
    public void toggle() {
        if (isEnabled()) {
            if (ClumpsClient.screenManager != null) {
                ClumpsClient.screenManager.close();
            }
            return;
        }
        setEnabled(true);
    }

    @Override
    public void onEnable() {
        try {
            try {
                initializeScreenManagerType();
                if (ClumpsClient.screenManager != null) {
                    ClumpsClient.screenManager.open();
                }
            } catch (Throwable ignored) {
            }
        } finally {
            setEnabled(false);
        }
    }

    @Override
    public boolean isEnabled() {
        initializeScreenManagerType();
        return ClumpsClient.screenManager != null && ClumpsClient.screenManager.isOpen();
    }

    @Override
    public boolean recordsStateChanges() {
        return false;
    }

    @Override
    public boolean persistsEnabledState() {
        return false;
    }

    public float getScrollSpeed() {
        return scrollSpeed.getValue();
    }

    private static boolean isStreamProofSettingVisible(Object context) {
        return SUPPORTS_STREAM_PROOF;
    }

    private static void applyStreamProofSetting() {
        if (syncingStreamProof) {
            return;
        }
        ClumpsClient.setExternalRendererEnabled(streamProof.getValue() && SUPPORTS_STREAM_PROOF);
    }

    public static void syncStreamProofFailure() {
        if (streamProof == null || !streamProof.getValue()) {
            return;
        }
        syncingStreamProof = true;
        try {
            streamProof.setValue(false);
        } finally {
            syncingStreamProof = false;
        }
    }

    private static void initializeScreenManagerType() {
    }
}
