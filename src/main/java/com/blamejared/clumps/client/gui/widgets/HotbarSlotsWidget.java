package com.blamejared.clumps.client.gui.widgets;

import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.settings.HotbarSlotsSetting;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.utils.strings.Concat;
import com.blamejared.clumps.client.ui.ColorTheme;
import com.blamejared.clumps.client.ui.HintRenderer;
import com.blamejared.clumps.client.gui.animations.Easing;
import com.blamejared.clumps.client.gui.animations.Tween;
import java.awt.Color;

public class HotbarSlotsWidget implements SettingWidget {
    private static final float SLOT_GAP = 1.5f;
    private static final float SLOT_ROW_PADDING = 1.5f;
    private final HotbarSlotsSetting setting;
    private float x;
    private float y;
    private int lastClickedSlot = -1;
    private final Tween clickHighlight;

    
    public HotbarSlotsWidget(HotbarSlotsSetting setting) {
        clickHighlight = new Tween(Easing.EASE_OUT_CUBIC, 150.0);
        this.setting = setting;
        clickHighlight.start(0.0, 0.0);
    }

    private float innerWidth(float rowWidth) {
        return rowWidth - 2.0f * ColorTheme.CONTENT_PADDING;
    }

    private float slotWidth(float rowWidth) {
        return (innerWidth(rowWidth) - 12.0f) / 9.0f;
    }

    
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        float height = height();
        float slotSize = slotWidth(rowWidth);
        float slotY = y + ColorTheme.ROW_HEIGHT + SLOT_ROW_PADDING;
        float firstSlotX = rowX + ColorTheme.CONTENT_PADDING;
        float highlight = (float) clickHighlight.value();
        boolean hovered = ColorTheme.contains(rowX, y, rowWidth, height, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                HintRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }
        renderer.drawRoundedRect(rowX, y, rowWidth, height, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha((hovered ? ColorTheme.ROW_HOVER : ColorTheme.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, height - 4.0f, 0.0f, ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity));
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, setting.getLabel(), rowX + ColorTheme.CONTENT_PADDING, y + ColorTheme.ROW_HEIGHT / 2.0f - 4.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));

        int selectedCount = 0;
        for (int slot = 0; slot < 9; slot++) {
            if (setting.isSelected(slot)) {
                selectedCount++;
            }
        }
        Concat countLabel = Concat.sequences(Concat.integer(selectedCount), CatalogText.literal("/9"));
        float countWidth = FontAsset.INTER_SEMI_BOLD.measureText(countLabel, 6.0f);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, countLabel, rowX + rowWidth - ColorTheme.CONTENT_PADDING - countWidth, y + ColorTheme.ROW_HEIGHT / 2.0f - 4.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.TOGGLE_ON.getRGB(), opacity));

        for (int slot = 0; slot < 9; slot++) {
            float slotX = firstSlotX + slot * (slotSize + SLOT_GAP);
            boolean slotHovered = ColorTheme.contains(slotX, slotY, slotSize, slotSize, mouseX, mouseY);
            Color color = setting.isSelected(slot) ? ColorTheme.TOGGLE_ON : ColorTheme.SLOT_BACKGROUND;
            if (slotHovered) {
                color = ColorTheme.blend(color, ColorTheme.ROW_HOVER, 0.3f);
            }
            if (slot == lastClickedSlot) {
                color = ColorTheme.blend(color, ColorTheme.TOGGLE_KNOB, highlight * 0.25f);
            }
            renderer.drawRoundedRect(slotX, slotY, slotSize, slotSize, ColorTheme.SLOT_RADIUS, ColorTheme.multiplyAlpha(color.getRGB(), opacity));
        }
    }

    
    @Override
    public float height() {
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        return ColorTheme.ROW_HEIGHT + SLOT_ROW_PADDING + slotWidth(rowWidth) + SLOT_ROW_PADDING;
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        float rowX = x + ColorTheme.ROW_INDENT;
        float slotSize = slotWidth(rowWidth);
        float slotY = y + ColorTheme.ROW_HEIGHT + SLOT_ROW_PADDING;
        float firstSlotX = rowX + ColorTheme.CONTENT_PADDING;
        for (int slot = 0; slot < 9; slot++) {
            float slotX = firstSlotX + slot * (slotSize + SLOT_GAP);
            if (ColorTheme.contains(slotX, slotY, slotSize, slotSize, (float) mouseX, (float) mouseY)) {
                setting.toggle(slot);
                lastClickedSlot = slot;
                clickHighlight.start(1.0, 0.0);
                return true;
            }
        }
        return false;
    }

    
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    
    @Override
    public HotbarSlotsSetting setting() {
        return setting;
    }
}
