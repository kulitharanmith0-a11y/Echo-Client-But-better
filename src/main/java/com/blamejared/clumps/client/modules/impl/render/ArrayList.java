package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.ModuleManager;
import com.blamejared.clumps.client.modules.hud.HudElement;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.hud.HudElementLayout;
import com.blamejared.clumps.client.hud.HudElementType;
import com.blamejared.clumps.client.hud.HudRenderer;
import com.blamejared.clumps.client.hud.KeybindRow;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.MultiChoiceSetting;
import com.blamejared.clumps.client.ui.UiId;
import com.blamejared.clumps.client.ui.UiSize;
import com.blamejared.clumps.client.utils.strings.Concat;
import java.util.List;

public class ArrayList extends HudElement {
    private static final CharSequence BLACKLIST = CatalogText.literal("Blacklist");
    private static final float ROW_SPACING = 39.5f;
    private static final CharSequence WHITELIST = CatalogText.literal("Whitelist");
    private BooleanSetting suffixEnabled;
    private BooleanSetting lowercase;
    private ChoiceSetting visibilityFilter;
    private final MultiChoiceSetting blacklistedFeatures;
    private final MultiChoiceSetting whitelistedFeatures;
    private static final String ANIMATION_PREFIX = "hud/overlay/array/";

    public ArrayList() {
        super(new ModuleInfo(CatalogText.literal("Array List"), CatalogText.literal("Enabled modules and their binds"), ModuleCategory.RENDER), HudElementType.ARRAY_LIST);
        blacklistedFeatures = new MultiChoiceSetting(CatalogText.literal("Blacklisted features")).selectionMode();
        whitelistedFeatures = new MultiChoiceSetting(CatalogText.literal("Whitelisted features")).selectionMode();
    }

    @Override
    public void postInitialize() {
        super.postInitialize();
        for (Module Module : ModuleManager.allFeatures()) {
            whitelistedFeatures.getMutableOptions().put(Module.getName(), false);
            blacklistedFeatures.getMutableOptions().put(Module.getName(), false);
        }
        whitelistedFeatures.setVisibilityPredicate(context -> visibilityFilter.isSelected(WHITELIST));
        blacklistedFeatures.setVisibilityPredicate(context -> visibilityFilter.isSelected(BLACKLIST));
    }

    public boolean isFeatureVisible(Module Module) {
        if (Module == this) {
            return false;
        }
        if (visibilityFilter.isSelected(WHITELIST)) {
            return whitelistedFeatures.isSelected(Module.getName());
        }
        return !blacklistedFeatures.isSelected(Module.getName());
    }

    public CharSequence normalizeLabel(Module Module, CharSequence label) {
        return lowercase.getValue() ? Concat.sequences(label).lowercaseView() : label;
    }

    public CharSequence getFeatureSuffix(Module Module) {
        if (!suffixEnabled.getValue()) {
            return "";
        }
        CharSequence suffix = Module.getSuffix();
        if (suffix == null || suffix.isEmpty()) {
            return "";
        }
        return lowercase.getValue() ? Concat.sequences(suffix).lowercaseView() : suffix;
    }

    @Override
    protected boolean shouldRender() {
        return !client.debugHudEntryList.isF3Enabled();
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        return HudRenderer.INSTANCE.measureElement(layout, HudElementType.ARRAY_LIST);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        boolean rightAligned = isRightOfScreenCenter();
        List<KeybindRow> rows = hud.getArrayListRows();
        List<String> order = hud.orderArrayListRows(layout);
        java.util.ArrayList<String> completed = new java.util.ArrayList<>();
        for (KeybindRow row : rows) {
            UiId identity = UiId.ROOT.child(ANIMATION_PREFIX + row.id());
            float opacity = hud.arrayRowOpacity(identity, row.enabled());
            if (opacity <= 0.002f) {
                if (!row.enabled()) {
                    completed.add(row.id());
                }
                continue;
            }
            int index = order.indexOf(row.id());
            if (index < 0) {
                continue;
            }
            float rowY = hud.arrayRowY(identity, HudElementType.ARRAY_LIST.defaultY() + index * ROW_SPACING);
            String binding = hud.formatArrayRowBinding(row);
            float bindingWidth = binding.isEmpty() ? 0.0f
                    : layout.textWidth(FontAsset.INTER, "[" + binding + "]", 12.0f);
            float width = hud.measureArrayListRow(layout, row);
            float x = HudElementType.ARRAY_LIST.defaultX()
                    + (rightAligned ? layout.getWidth() - width : 0.0f)
                    + hud.arrayRowHorizontalOffset(identity, row.enabled(), rightAligned);
            hud.drawPanel(layout, x, rowY, width, 35.0f, 10.0f, opacity, 28.0f);
            layout.drawIcon(row.icon(), x + 12.0f, rowY + 9.5f, 16.0f, 16.0f, -1, opacity);
            layout.drawText(FontAsset.INTER_SEMI_BOLD, row.name(), x + 36.0f, rowY + 10.0f, 13.0f, -1, opacity);
            if (!row.detail().isEmpty()) {
                float nameWidth = layout.textWidth(FontAsset.INTER_SEMI_BOLD, row.name(), 13.0f);
                layout.drawText(FontAsset.INTER_SEMI_BOLD, row.detail(), x + 36.0f + nameWidth + 5.0f, rowY + 10.0f, 13.0f, -1711276033, opacity);
            }
            if (!binding.isEmpty()) {
                float separatorX = x + width - 14.0f - bindingWidth - 9.0f;
                layout.drawRoundedRect(separatorX, rowY + 13.0f, 1.0f, 9.0f, 0.0f, 0x40ffffff, opacity);
                layout.drawText(FontAsset.INTER, "[" + binding + "]", separatorX + 9.0f, rowY + 10.5f, 12.0f, -1711276033, opacity);
            }
        }
        hud.removeCompletedArrayRows(completed);
    }

    
    public static void appendBootstrapSettings(ArrayList Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.suffixEnabled);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.lowercase);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.visibilityFilter);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.blacklistedFeatures);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.whitelistedFeatures);
    }

    
    public static void assignBootstrapSettings(ArrayList Module, com.blamejared.clumps.client.settings.ChoiceSetting preparedVisibilityFilter, com.blamejared.clumps.client.settings.BooleanSetting preparedLowercase, com.blamejared.clumps.client.settings.BooleanSetting preparedSuffixEnabled) {
        Module.visibilityFilter = preparedVisibilityFilter;
        Module.lowercase = preparedLowercase;
        Module.suffixEnabled = preparedSuffixEnabled;
    }
}
