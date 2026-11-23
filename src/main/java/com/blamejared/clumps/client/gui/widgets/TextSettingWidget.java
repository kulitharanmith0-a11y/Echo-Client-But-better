package com.blamejared.clumps.client.gui.widgets;

import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.keyboard.KeyModifier;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.gui.ScreenManager;
import com.blamejared.clumps.client.settings.TextSetting;
import com.blamejared.clumps.client.ui.ColorTheme;
import com.blamejared.clumps.client.ui.HintRenderer;
import com.blamejared.clumps.client.utils.strings.TextBuffers;
import com.blamejared.clumps.client.gui.animations.Easing;
import com.blamejared.clumps.client.gui.animations.Tween;
import java.util.ArrayList;
import java.util.List;

public class TextSettingWidget implements SettingWidget {
    private static final String REMOVE_ENTRY_GLYPH = "\ue1b2";
    private static final String EXPANDED_GLYPH = "\ue06d";
    private static final String COLLAPSED_GLYPH = "\ue070";
    private final TextSetting setting;
    private float x;
    private float y;
    private int caret = 0;
    private int firstVisibleCharacter = 0;
    private final Tween caretPulse;
    private boolean expanded = false;
    private final Tween expansion;
    private int firstVisibleEntry = 0;
    private final int visibleRows = 5;

    public TextSettingWidget(TextSetting setting) {
        caretPulse = new Tween(Easing.EASE_IN_OUT_QUART, 400);
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 200);
        this.setting = setting;
        caret = setting.getText().length();
        expansion.start(0, 0);
    }

    
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        float target = expanded ? 1.0f : 0.0f;
        expansion.setTarget(target);
        float progress = (float) expansion.value();
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        float extraHeight = listHeight() * progress;
        float totalHeight = baseHeight() + extraHeight;
        boolean hovered = ColorTheme.contains(rowX, y, rowWidth, totalHeight, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                HintRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }
        renderer.drawRoundedRect(rowX, y, rowWidth, totalHeight, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha((hovered ? ColorTheme.ROW_HOVER : ColorTheme.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, totalHeight - 4.0f, 0.0f, ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 16);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ColorTheme.CONTENT_PADDING, y + 3.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));

        float inputY = y + ColorTheme.ROW_HEIGHT - 2.0f;
        float inputX = rowX + ColorTheme.CONTENT_PADDING;
        float inputWidth = rowWidth - ColorTheme.CONTENT_PADDING * 2.0f;
        renderer.drawRoundedRect(inputX, inputY, inputWidth, 12.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.SLOT_BACKGROUND.getRGB(), opacity));
        String text = setting.getText();
        int textColor = (text.isEmpty() ? ColorTheme.SECONDARY_TEXT : ColorTheme.SELECTED_OPTION_TEXT).getRGB();
        float textWidth = inputWidth - 8.0f;
        int visibleCaret = Math.min(caret, text.length());
        firstVisibleCharacter = Math.min(firstVisibleCharacter, visibleCaret);
        while (FontAsset.INTER_SEMI_BOLD.measureText(text.substring(firstVisibleCharacter, visibleCaret), 6.0f) > textWidth) {
            firstVisibleCharacter++;
        }
        while (firstVisibleCharacter > 0
                && FontAsset.INTER_SEMI_BOLD.measureText(text.substring(firstVisibleCharacter - 1), 6.0f) <= textWidth) {
            firstVisibleCharacter--;
        }
        String visibleText = text.isEmpty() ? setting.getPlaceholder() : text.substring(firstVisibleCharacter);
        while (!visibleText.isEmpty() && FontAsset.INTER_SEMI_BOLD.measureText(visibleText, 6.0f) > textWidth) {
            visibleText = visibleText.substring(0, visibleText.length() - 1);
        }
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, visibleText, inputX + 4.0f, inputY + 6.0f - 4.0f, 6.0f, ColorTheme.multiplyAlpha(textColor, opacity));
        if (ScreenManager.focusedSetting == setting) {
            if (caretPulse.isFinished()) caretPulse.start(caretPulse.targetValue(), 1.0 - caretPulse.targetValue());
            float caretX = inputX + 4.0f
                    + FontAsset.INTER_SEMI_BOLD.measureText(text.substring(firstVisibleCharacter, visibleCaret), 6.0f);
            int alpha = (int) (caretPulse.value() * 255.0);
            renderer.drawRoundedRect(caretX, inputY + 2.0f, 1.0f, 8.0f, 0.0f, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(ColorTheme.SLIDER_FILL, alpha).getRGB(), opacity));
        }

        if (setting.isListMode() && progress > ColorTheme.INPUT_ANIMATION_THRESHOLD && !setting.getEntries().isEmpty()) {
            List<String> entries = new ArrayList<>(setting.getEntries());
            int first = Math.max(0, firstVisibleEntry);
            int end = Math.min(entries.size(), first + 5);
            float entryY = y + baseHeight() + ColorTheme.CONTENT_PADDING;
            float entryHeight = 14.0f;
            for (int index = first; index < end; index++) {
                String entry = entries.get(index);
                boolean rowHovered = ColorTheme.contains(rowX + ColorTheme.CONTENT_PADDING, entryY, rowWidth - ColorTheme.CONTENT_PADDING * 2.0f, entryHeight, mouseX, mouseY);
                if (rowHovered) {
                    renderer.drawRoundedRect(rowX + ColorTheme.CONTENT_PADDING, entryY, rowWidth - ColorTheme.CONTENT_PADDING * 2.0f, entryHeight, ColorTheme.SLOT_RADIUS, ColorTheme.multiplyAlpha(ColorTheme.ROW_HOVER.getRGB(), opacity));
                }
                float removeWidth = 14.0f;
                float removeX = rowX + rowWidth - ColorTheme.CONTENT_PADDING - removeWidth - 2.0f;
                float entryX = rowX + ColorTheme.CONTENT_PADDING + 2.0f;
                float entryRight = removeX - 4.0f;
                float availableWidth = Math.max(0.0f, entryRight - entryX);
                String visibleEntry = fitVisibleText(entry, availableWidth, 6.0f);
                renderer.drawText(FontAsset.INTER_SEMI_BOLD, visibleEntry, entryX, entryY + (entryHeight - 6.0f) / 2.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity));
                if (FontAsset.LUCIDE != null) {
                    boolean removeHovered = ColorTheme.contains(removeX, entryY, removeWidth, entryHeight, mouseX, mouseY);
                    if (removeHovered) {
                        renderer.drawRoundedRect(removeX, entryY + 1.0f, removeWidth, entryHeight - 2.0f, ColorTheme.SLOT_RADIUS, ColorTheme.multiplyAlpha(ColorTheme.SLOT_BACKGROUND.getRGB(), opacity));
                    }
                    renderer.drawText(FontAsset.LUCIDE, REMOVE_ENTRY_GLYPH, removeX + (removeWidth - FontAsset.LUCIDE.measureText(REMOVE_ENTRY_GLYPH, 10.0f)) / 2.0f, entryY + (entryHeight - 10.0f) / 2.0f, 10.0f, ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity));
                }
                entryY += entryHeight + ColorTheme.SEARCH_GAP;
            }
        }
        if (setting.isListMode() && !setting.getEntries().isEmpty() && FontAsset.LUCIDE != null) {
            float toggleX = rowX + rowWidth - ColorTheme.CONTENT_PADDING - 14.0f;
            float toggleY = y + 2.0f;
            String glyph = expanded ? EXPANDED_GLYPH : COLLAPSED_GLYPH;
            renderer.drawText(FontAsset.LUCIDE, glyph, toggleX, toggleY, 10.0f, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
        }
    }

    private float baseHeight() {
        return ColorTheme.ROW_HEIGHT + 10 + (expanded ? 0 : 5);
    }

    private float listHeight() {
        if (!setting.isListMode() || setting.getEntries().isEmpty()) return 0;
        int count = Math.min(5, setting.getEntries().size());
        return count * 14.0f + Math.max(0, count - 1) * ColorTheme.SEARCH_GAP + ColorTheme.CONTENT_PADDING * 2;
    }

    
    private String fitVisibleText(String text, float width, float size) {
        if (width <= 0 || text == null || text.isEmpty()) return "";
        if (FontAsset.INTER_SEMI_BOLD == null) return text.length() > 10 ? text.substring(0, 10) + "\u2026" : text;
        if (FontAsset.INTER_SEMI_BOLD.measureText(text, size) <= width) return text;
        String ellipsis = "\u2026";
        if (FontAsset.INTER_SEMI_BOLD.measureText(ellipsis, size) > width) return "";
        for (int end = text.length(); end > 0; end--) {
            String candidate = text.substring(0, end) + ellipsis;
            if (FontAsset.INTER_SEMI_BOLD.measureText(candidate, size) <= width) return candidate;
        }
        return ellipsis;
    }

    public float height() {
        float progress = (float) expansion.value();
        float extra = listHeight() * progress;
        return baseHeight() + extra;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4;
        float inputY = y + ColorTheme.ROW_HEIGHT - 2;
        float inputX = rowX + ColorTheme.CONTENT_PADDING;
        float inputWidth = rowWidth - ColorTheme.CONTENT_PADDING * 2;
        if (ColorTheme.contains(inputX, inputY, inputWidth, 12, (float) mouseX, (float) mouseY)) {
            ScreenManager.focusedSetting = setting;
            caretPulse.start(0, 1);
            String text = setting.getText();
            float relativeX = (float) mouseX - inputX - 4;
            caret = firstVisibleCharacter;
            for (int index = firstVisibleCharacter; index <= text.length(); index++) {
                float width = FontAsset.INTER_SEMI_BOLD.measureText(text.substring(firstVisibleCharacter, index), 6);
                if (relativeX <= width) break;
                caret = index;
            }
            caret = Math.min(caret, text.length());
            return true;
        }
        if (setting.isListMode() && !setting.getEntries().isEmpty()) {
            float toggleX = rowX + rowWidth - ColorTheme.CONTENT_PADDING - 14;
            if (ColorTheme.contains(toggleX, y, 10, 16, (float) mouseX, (float) mouseY)) {
                expanded = !expanded;
                return true;
            }
        }
        float progress = (float) expansion.value();
        if (expanded && progress > ColorTheme.INPUT_ANIMATION_THRESHOLD && !setting.getEntries().isEmpty()) {
            List<String> entries = new ArrayList<>(setting.getEntries());
            int first = Math.max(0, firstVisibleEntry);
            int end = Math.min(entries.size(), first + 5);
            float entryY = y + ColorTheme.SLIDER_ROW_HEIGHT + ColorTheme.CONTENT_PADDING;
            for (int index = first; index < end; index++) {
                String entry = entries.get(index);
                float entryHeight = 14;
                float removeWidth = 14;
                float removeX = rowX + rowWidth - ColorTheme.CONTENT_PADDING - removeWidth - 2;
                if (ColorTheme.contains(removeX, entryY, removeWidth, entryHeight, (float) mouseX, (float) mouseY)) {
                    setting.removeEntry(entry);
                    return true;
                }
                entryY += entryHeight + ColorTheme.SEARCH_GAP;
            }
        }
        ScreenManager.focusedSetting = null;
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ScreenManager.focusedSetting != setting) return false;
        String text = setting.getText();
        if (keyCode == InputKey.BACKSPACE.code()) {
            if (KeyModifier.CONTROL.isSet(modifiers)) {
                int previous = previousWordStart(text, caret);
                setting.setText(text.substring(0, previous) + text.substring(caret));
                caret = previous;
            } else if (caret > 0) {
                int previous = caret - 1;
                setting.setText(text.substring(0, previous) + text.substring(caret));
                caret = previous;
            }
        } else if (keyCode == InputKey.DELETE.code()) {
            if (caret < text.length()) setting.setText(text.substring(0, caret) + text.substring(caret + 1));
        } else if (keyCode == InputKey.LEFT.code()) caret = Math.max(0, caret - 1);
        else if (keyCode == InputKey.RIGHT.code()) caret = Math.min(text.length(), caret + 1);
        else if (keyCode == InputKey.HOME.code()) caret = 0;
        else if (keyCode == InputKey.END.code()) caret = text.length();
        else if (keyCode == InputKey.ENTER.code()) {
            if (setting.isListMode() && !text.trim().isEmpty()) {
                setting.addEntry(text);
                setting.setText("");
                caret = 0;
            } else {
                ScreenManager.focusedSetting = null;
            }
            return true;
        } else if (keyCode == InputKey.ESCAPE.code()) {
            ScreenManager.focusedSetting = null;
            return true;
        } else return false;
        caretPulse.start(0, 1);
        return true;
    }

    
    public void charTyped(char character, int modifiers) {
        if (ScreenManager.focusedSetting != setting || character < ' ' || character == 127) return;
        String text = setting.getText();
        if (text.length() >= setting.getMaximumLength()) return;
        int position = Math.min(caret, text.length());
        String updated = text.substring(0, position) + character + text.substring(position);
        setting.setText(updated);
        caret = position + 1;
        caretPulse.start(0, 1);
    }

    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!expanded || setting.getEntries().size() <= 5) return false;
        float top = y + baseHeight();
        float listHeight = listHeight();
        if (mouseY < top || mouseY > top + listHeight) return false;
        firstVisibleEntry = Math.clamp((long) (firstVisibleEntry - (int) amount), 0, setting.getEntries().size() - 5);
        return true;
    }

    
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    private static int previousWordStart(String text, int caret) {
        int index = caret;
        while (index > 0 && text.charAt(index - 1) == ' ') index--;
        while (index > 0 && text.charAt(index - 1) != ' ') index--;
        return index;
    }

    public TextSetting setting() { return setting; }
}
