package com.blamejared.clumps.client.gui.widgets;

import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.keyboard.KeyModifier;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.gui.ScreenManager;
import com.blamejared.clumps.client.settings.MultiChoiceSetting;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.utils.strings.Concat;
import com.blamejared.clumps.client.ui.ColorTheme;
import com.blamejared.clumps.client.ui.HintRenderer;
import com.blamejared.clumps.client.gui.animations.Easing;
import com.blamejared.clumps.client.gui.animations.Tween;
import com.blamejared.clumps.client.utils.strings.TextBuffers;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BooleanGroupWidget implements SettingWidget {
    private final MultiChoiceSetting setting;
    private float x;
    private float y;
    private boolean expanded = false;
    private final Tween expansion;
    private static final int VISIBLE_ROWS = 8;
    private int scrollOffset = 0;
    private String search = "";
    private int caret = 0;
    private final Tween caretPulse;

    public BooleanGroupWidget(MultiChoiceSetting setting) {
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 200.0);
        caretPulse = new Tween(Easing.EASE_IN_OUT_QUART, 400.0);
        this.setting = setting;
        expansion.start(0.0, 0.0);
    }

    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x; this.y = y;
        expansion.setTarget(expanded ? 1.0f : 0.0f);
        float progress = (float) expansion.value();
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        float height = height();
        boolean hovered = ColorTheme.contains(rowX, y, rowWidth, ColorTheme.ROW_HEIGHT, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                HintRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }
        renderer.drawRoundedRect(rowX, y, rowWidth, height, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha((hovered ? ColorTheme.ROW_HOVER : ColorTheme.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2, 1.5f, height - 4, 0, ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity));
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, TextBuffers.abbreviate(setting.getLabel(), 14), rowX + ColorTheme.CONTENT_PADDING, y + ColorTheme.ROW_HEIGHT / 2 - 4, 6, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
        int selected = 0;
        for (Boolean value : setting.getOptions().values()) if (Boolean.TRUE.equals(value)) selected++;
        CharSequence count = selected == 0 ? CatalogText.literal("None")
                : Concat.sequences(Concat.integer(selected), CatalogText.literal(" sel."));
        float countWidth = FontAsset.INTER_SEMI_BOLD.measureText(count, 6);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, count, rowX + rowWidth - ColorTheme.CONTENT_PADDING - countWidth, y + ColorTheme.ROW_HEIGHT / 2 - 4, 6, ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity));
        if (!(progress > ColorTheme.VISIBLE_ANIMATION_THRESHOLD)) return;
        int alpha = (int) (progress * 255.0f);
        float optionY = y + ColorTheme.ROW_HEIGHT;
        if (setting.isSelectionMode()) {
            float searchY = optionY;
            renderer.drawRoundedRect(rowX + ColorTheme.CONTENT_PADDING, searchY, rowWidth - ColorTheme.CONTENT_PADDING * 2, ColorTheme.ROW_HEIGHT, ColorTheme.SLOT_RADIUS, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(ColorTheme.SLOT_BACKGROUND, alpha), opacity));
            if (FontAsset.INTER_SEMI_BOLD != null) {
                CharSequence text = search.isEmpty() ? CatalogText.literal("Search...") : search;
                Color color = search.isEmpty() ? ColorTheme.SECONDARY_TEXT : ColorTheme.SELECTED_OPTION_TEXT;
                renderer.drawText(FontAsset.INTER_SEMI_BOLD, text, rowX + ColorTheme.CONTENT_PADDING + 4, searchY + ColorTheme.ROW_HEIGHT / 2 - 3, 6, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(color, alpha), opacity));
                if (ScreenManager.focusedSetting == setting) {
                    if (caretPulse.isFinished()) caretPulse.start(caretPulse.targetValue(), 1 - caretPulse.targetValue());
                    int position = Math.min(caret, search.length());
                    float caretX = rowX + ColorTheme.CONTENT_PADDING + 4
                            + FontAsset.INTER_SEMI_BOLD.measureText(search.substring(0, position), 6);
                    int caretAlpha = (int) (caretPulse.value() * alpha);
                    renderer.drawRoundedRect(caretX, searchY + 2, 1, ColorTheme.ROW_HEIGHT - 4, 0, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(ColorTheme.SLIDER_FILL, caretAlpha), opacity));
                }
            }
            optionY += ColorTheme.ROW_HEIGHT + ColorTheme.SEARCH_GAP;
        }
        List<Map.Entry<CharSequence, Boolean>> entries = filteredEntries();
        int size = entries.size();
        int first = size > VISIBLE_ROWS ? scrollOffset : 0;
        int end = size > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, size) : size;
        for (int index = first; index < end; index++) {
            Map.Entry<CharSequence, Boolean> entry = entries.get(index);
            Color color = Boolean.TRUE.equals(entry.getValue()) ? ColorTheme.SELECTED_OPTION_TEXT : ColorTheme.SECONDARY_TEXT;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, entry.getKey(), rowX + ColorTheme.CONTENT_PADDING + 4, optionY + (ColorTheme.ROW_HEIGHT - 2) / 2 - 4, 6, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(color, alpha), opacity));
            optionY += ColorTheme.ROW_HEIGHT - 2;
        }
    }

    
    private List<Map.Entry<CharSequence, Boolean>> filteredEntries() {
        List<Map.Entry<CharSequence, Boolean>> entries = new ArrayList<>(setting.getOptions().entrySet());
        if (!setting.isSelectionMode() || search.isEmpty()) return entries;
        String query = search.toLowerCase();
        List<Map.Entry<CharSequence, Boolean>> filtered = new ArrayList<>();
        for (Map.Entry<CharSequence, Boolean> entry : entries) {
            if (Concat.containsLowercase(entry.getKey(), query)) filtered.add(entry);
        }
        return filtered;
    }

    private float searchHeight() {
        return setting.isSelectionMode() ? ColorTheme.ROW_HEIGHT + ColorTheme.SEARCH_GAP : 0;
    }

    
    @Override
    public float height() {
        float progress = (float) expansion.value();
        float height = ColorTheme.ROW_HEIGHT;
        if (progress > ColorTheme.VISIBLE_ANIMATION_THRESHOLD) {
            float rows = Math.min(setting.getOptions().size(), VISIBLE_ROWS) * (ColorTheme.ROW_HEIGHT - 2);
            height += (searchHeight() + rows) * progress;
        }
        return height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        float rowX = x + ColorTheme.ROW_INDENT;
        float width = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4;
        if (ColorTheme.contains(rowX, y, width, ColorTheme.ROW_HEIGHT, (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            if (expanded) { scrollOffset = 0; ScreenManager.focusedSetting = null; }
            return true;
        }
        float progress = (float) expansion.value();
        if (!expanded || !(progress > ColorTheme.INPUT_ANIMATION_THRESHOLD)) return false;
        if (setting.isSelectionMode() && ColorTheme.contains(rowX + ColorTheme.CONTENT_PADDING, y + ColorTheme.ROW_HEIGHT, width - ColorTheme.CONTENT_PADDING * 2, ColorTheme.ROW_HEIGHT, (float) mouseX, (float) mouseY)) {
            ScreenManager.focusedSetting = setting;
            caretPulse.start(0, 1);
            float relativeX = (float) mouseX - rowX - ColorTheme.CONTENT_PADDING - 4;
            caret = 0;
            for (int index = 0; index <= search.length(); index++) {
                if (relativeX <= FontAsset.INTER_SEMI_BOLD.measureText(search.substring(0, index), 6)) break;
                caret = index;
            }
            caret = Math.min(caret, search.length());
            return true;
        }
        List<Map.Entry<CharSequence, Boolean>> entries = filteredEntries();
        int size = entries.size();
        float visibleHeight = Math.min(size * (ColorTheme.ROW_HEIGHT - 2), VISIBLE_ROWS * (ColorTheme.ROW_HEIGHT - 2));
        float top = y + ColorTheme.ROW_HEIGHT + searchHeight();
        int first = size > VISIBLE_ROWS ? scrollOffset : 0;
        int end = size > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, size) : size;
        float optionY = top;
        if (mouseY >= top && mouseY <= top + visibleHeight) {
            for (int index = first; index < end; index++) {
                Map.Entry<CharSequence, Boolean> entry = entries.get(index);
                if (ColorTheme.contains(rowX + ColorTheme.CONTENT_PADDING, optionY, width - ColorTheme.CONTENT_PADDING * 2, ColorTheme.ROW_HEIGHT - 2, (float) mouseX, (float) mouseY)) {
                    setting.toggle(entry.getKey());
                    return true;
                }
                optionY += ColorTheme.ROW_HEIGHT - 2;
            }
        }
        if (setting.isSelectionMode()) ScreenManager.focusedSetting = null;
        return false;
    }

    @Override public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!setting.isSelectionMode() || ScreenManager.focusedSetting != setting) return false;
        if (keyCode == InputKey.BACKSPACE.code()) {
            if (KeyModifier.CONTROL.isSet(modifiers)) {
                int previous = previousWordStart(search, caret);
                search = search.substring(0, previous) + search.substring(caret);
                caret = previous; scrollOffset = 0;
            } else if (caret > 0) {
                search = search.substring(0, caret - 1) + search.substring(caret);
                caret--; scrollOffset = 0;
            }
        } else if (keyCode == InputKey.DELETE.code()) {
            if (caret < search.length()) { search = search.substring(0, caret) + search.substring(caret + 1); scrollOffset = 0; }
        } else if (keyCode == InputKey.LEFT.code()) caret = Math.max(0, caret - 1);
        else if (keyCode == InputKey.RIGHT.code()) caret = Math.min(search.length(), caret + 1);
        else if (keyCode == InputKey.HOME.code()) caret = 0;
        else if (keyCode == InputKey.END.code()) caret = search.length();
        else if (keyCode == InputKey.ESCAPE.code()) { ScreenManager.focusedSetting = null; return true; }
        else return false;
        caretPulse.start(0, 1);
        return true;
    }

    @Override
    public void charTyped(char character, int modifiers) {
        if (!setting.isSelectionMode() || ScreenManager.focusedSetting != setting || character < 32 || character == 127) return;
        int position = Math.min(caret, search.length());
        search = search.substring(0, position) + character + search.substring(position);
        caret = position + 1; scrollOffset = 0; caretPulse.start(0, 1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        float progress = (float) expansion.value();
        if (!expanded || !(progress > ColorTheme.INPUT_ANIMATION_THRESHOLD)) return false;
        float rowX = x + ColorTheme.ROW_INDENT;
        float width = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4;
        int size = filteredEntries().size();
        if (size <= VISIBLE_ROWS || !ColorTheme.contains(rowX, y, width, height(), (float) mouseX, (float) mouseY)) return false;
        float top = y + ColorTheme.ROW_HEIGHT + searchHeight();
        float visibleHeight = Math.min(size * (ColorTheme.ROW_HEIGHT - 2), VISIBLE_ROWS * (ColorTheme.ROW_HEIGHT - 2));
        if (mouseY < top || mouseY > top + visibleHeight) return false;
        if (amount > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else if (amount < 0) scrollOffset = Math.min(size - VISIBLE_ROWS, scrollOffset + 1);
        return true;
    }

    private static int previousWordStart(String text, int caret) {
        int index = caret;
        while (index > 0 && text.charAt(index - 1) == ' ') index--;
        while (index > 0 && text.charAt(index - 1) != ' ') index--;
        return index;
    }

    @Override public MultiChoiceSetting setting() { return setting; }
}
