package com.blamejared.clumps.client.gui.widgets;

import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.gui.ScreenManager;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.ui.ColorTheme;
import com.blamejared.clumps.client.ui.HintRenderer;
import com.blamejared.clumps.client.gui.animations.Easing;
import com.blamejared.clumps.client.gui.animations.Tween;
import com.blamejared.clumps.client.utils.strings.TextBuffers;

public class FloatSliderWidget implements SettingWidget {
    private static final String EMPTY_EDIT_PLACEHOLDER = new String(new char[]{'.', '.', '.'}).intern();
    private final FloatSetting setting;
    private float x;
    private float y;
    private boolean dragging = false;
    private final Tween fill;
    private float lastDragValue = Float.MIN_VALUE;
    private boolean editing = false;
    private String editText = "";
    private int caret = 0;
    private final Tween caretPulse;
    private float valueTextX;
    private float valueTextWidth;

    
    public FloatSliderWidget(FloatSetting setting) {
        fill = new Tween(Easing.EASE_OUT_CUBIC, 150.0);
        caretPulse = new Tween(Easing.EASE_IN_OUT_QUART, 400.0);
        this.setting = setting;
        float initial = sliderFraction();
        fill.start(initial, initial);
    }

    
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        if (editing && ScreenManager.focusedSetting != setting) commitTextEdit();
        if (dragging) updateFromPointer(mouseX);
        float fraction = sliderFraction();
        fill.setTarget(fraction);
        float progress = (float) fill.value();
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        boolean hovered = ColorTheme.contains(rowX, y, rowWidth, ColorTheme.SLIDER_ROW_HEIGHT, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                HintRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }
        renderer.drawRoundedRect(rowX, y, rowWidth, ColorTheme.SLIDER_ROW_HEIGHT, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha((hovered ? ColorTheme.ROW_HOVER : ColorTheme.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, ColorTheme.SLIDER_ROW_HEIGHT - 4.0f, 0.0f, ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 16);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ColorTheme.CONTENT_PADDING, y + 3.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
        CharSequence valueText = editing ? (editText.isEmpty() ? EMPTY_EDIT_PLACEHOLDER : editText)
                : TextBuffers.fixed(setting.getValue(), 2);
        CharSequence unit = setting.getUnit();
        float unitWidth = unit == null || unit.isEmpty() ? 0.0f : FontAsset.INTER_SEMI_BOLD.measureText(unit, 5.0f);
        valueTextWidth = FontAsset.INTER_SEMI_BOLD.measureText(valueText, 6.0f);
        valueTextX = rowX + rowWidth - ColorTheme.CONTENT_PADDING - valueTextWidth - unitWidth;
        int valueColor = editing ? ColorTheme.SELECTED_OPTION_TEXT.getRGB()
                : ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, valueText, valueTextX, y + 3.0f, 6.0f, editing ? ColorTheme.multiplyAlpha(valueColor, opacity) : valueColor);
        if (unitWidth > 0.0f) {
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, unit, valueTextX + valueTextWidth, y + 4.0f, 5.0f, ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity * 0.6f));
        }
        if (editing && !editText.isEmpty()) {
            if (caretPulse.isFinished()) caretPulse.start(caretPulse.targetValue(), 1.0 - caretPulse.targetValue());
            int visibleCaret = Math.min(caret, editText.length());
            float caretX = valueTextX + FontAsset.INTER_SEMI_BOLD.measureText(editText.substring(0, visibleCaret), 6.0f);
            int caretAlpha = (int) (caretPulse.value() * 255.0);
            float caretHeight = 6.0f;
            float caretY = y + 3.0f + (FontAsset.INTER_SEMI_BOLD.lineHeight(6.0f) - caretHeight) * 0.5f;
            renderer.drawRoundedRect(caretX, caretY, 1.0f, caretHeight, 0.0f, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(ColorTheme.SLIDER_FILL, caretAlpha).getRGB(), opacity));
        }
        float trackY = y + ColorTheme.ROW_HEIGHT;
        float trackX = rowX + ColorTheme.CONTENT_PADDING;
        float trackRight = rowX + rowWidth - ColorTheme.CONTENT_PADDING;
        float trackWidth = trackRight - trackX;
        renderer.drawRoundedRect(trackX, trackY, trackWidth, 3.0f, 1.5f, ColorTheme.multiplyAlpha(ColorTheme.SLOT_BACKGROUND.getRGB(), opacity));
        renderer.drawRoundedRect(trackX, trackY, trackWidth * progress, 3.0f, 1.5f, ColorTheme.multiplyAlpha(ColorTheme.SLIDER_FILL.getRGB(), opacity));
        renderer.drawRoundedRect(trackX + trackWidth * progress - 4.0f, trackY - 2.0f, 8.0f, 7.0f, 4.0f, ColorTheme.multiplyAlpha(ColorTheme.TOGGLE_KNOB.getRGB(), opacity));
    }

    
    @Override public float height() { return ColorTheme.SLIDER_ROW_HEIGHT; }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        float trackY = y + ColorTheme.ROW_HEIGHT;
        float trackX = rowX + ColorTheme.CONTENT_PADDING;
        float trackRight = rowX + rowWidth - ColorTheme.CONTENT_PADDING;
        if (ColorTheme.contains(valueTextX - 3.0f, y + 1.0f, valueTextWidth + 6.0f, 10.0f, (float) mouseX, (float) mouseY)) {
            if (!editing) {
                editing = true;
                ScreenManager.focusedSetting = setting;
                editText = "";
                caret = 0;
                caretPulse.start(0.0, 1.0);
            }
            return true;
        }
        if (editing) commitTextEdit();
        if (ColorTheme.contains(trackX - 6.0f, trackY - 5.0f, trackRight - trackX + 12.0f, 12.0f, (float) mouseX, (float) mouseY)) {
            dragging = true;
            lastDragValue = setting.getValue();
            updateFromPointer(mouseX);
            return true;
        }
        return false;
    }

    
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) setting.notifyChanged();
        dragging = false;
    }

    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editing || ScreenManager.focusedSetting != setting) return false;
        if (keyCode == InputKey.BACKSPACE.code()) {
            if (caret > 0) { editText = editText.substring(0, caret - 1) + editText.substring(caret); caret--; }
        } else if (keyCode == InputKey.DELETE.code()) {
            if (caret < editText.length()) editText = editText.substring(0, caret) + editText.substring(caret + 1);
        } else if (keyCode == InputKey.LEFT.code()) {
            caret = Math.max(0, caret - 1);
        } else if (keyCode == InputKey.RIGHT.code()) {
            caret = Math.min(editText.length(), caret + 1);
        } else if (keyCode == InputKey.HOME.code()) {
            caret = 0;
        } else if (keyCode == InputKey.END.code()) {
            caret = editText.length();
        } else if (keyCode == InputKey.ENTER.code() || keyCode == InputKey.KEYPAD_ENTER.code()) {
            commitTextEdit(); return true;
        } else if (keyCode == InputKey.ESCAPE.code()) {
            cancelTextEdit(); return true;
        } else {
            return false;
        }
        caretPulse.start(0.0, 1.0);
        return true;
    }

    
    @Override
    public void charTyped(char character, int modifiers) {
        if (!editing || ScreenManager.focusedSetting != setting) return;
        boolean accepted = character >= '0' && character <= '9'
                || character == '.' && editText.indexOf('.') < 0
                || character == '-' && caret == 0 && editText.indexOf('-') < 0;
        if (!accepted) return;
        editText = editText.substring(0, caret) + character + editText.substring(caret);
        caret++;
        caretPulse.start(0.0, 1.0);
    }

    private void commitTextEdit() {
        try {
            setting.setValue(Float.parseFloat(editText.trim()));
        } catch (Exception ignored) {
        }
        cancelTextEdit();
    }

    private void cancelTextEdit() {
        editing = false;
        if (ScreenManager.focusedSetting == setting) ScreenManager.focusedSetting = null;
    }

    private void updateFromPointer(double mouseX) {
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        float trackX = rowX + ColorTheme.CONTENT_PADDING;
        float trackWidth = rowWidth - ColorTheme.CONTENT_PADDING * 2.0f;
        float fraction = Math.clamp((float) (mouseX - trackX) / trackWidth, 0.0f, 1.0f);
        float value = setting.getMinimum() + (setting.getMaximum() - setting.getMinimum()) * fraction;
        setting.setValueSilently(value);
        float range = setting.getMaximum() - setting.getMinimum();
        float threshold = range * 0.03f;
        if (lastDragValue == Float.MIN_VALUE || Math.abs(value - lastDragValue) >= threshold) lastDragValue = value;
    }

    private float sliderFraction() {
        return Math.clamp((setting.getValue() - setting.getMinimum())
                / (setting.getMaximum() - setting.getMinimum()), 0.0f, 1.0f);
    }

    
    @Override public FloatSetting setting() { return setting; }
}
