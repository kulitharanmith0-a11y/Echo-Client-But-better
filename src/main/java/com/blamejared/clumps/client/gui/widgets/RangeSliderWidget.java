package com.blamejared.clumps.client.gui.widgets;

import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.gui.ScreenManager;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.ui.ColorTheme;
import com.blamejared.clumps.client.ui.HintRenderer;
import com.blamejared.clumps.client.gui.animations.Easing;
import com.blamejared.clumps.client.gui.animations.Tween;
import com.blamejared.clumps.client.utils.strings.TextBuffers;

public class RangeSliderWidget implements SettingWidget {
    private static final float VALUE_GAP = 6.0f;
    private static String lowerEmptyPlaceholder;
    private static String upperEmptyPlaceholder;
    private final FloatRangeSetting setting;
    private float x;
    private float y;
    private boolean draggingLower = false;
    private boolean draggingUpper = false;
    private final Tween lowerFill;
    private final Tween upperFill;
    private float lastDragValue = Float.MIN_VALUE;
    private boolean draggingTie = false;
    private float tieStartValue = Float.NaN;
    private boolean editing = false;
    private boolean editingUpper = false;
    private String editText = "";
    private int caret = 0;
    private final Tween caretPulse;
    private float lowerTextX;
    private float lowerTextWidth;
    private float upperTextX;
    private float upperTextWidth;

    
    public RangeSliderWidget(FloatRangeSetting setting) {
        lowerFill = new Tween(Easing.EASE_OUT_CUBIC, 150.0);
        upperFill = new Tween(Easing.EASE_OUT_CUBIC, 150.0);
        caretPulse = new Tween(Easing.EASE_IN_OUT_QUART, 400.0);
        this.setting = setting;
        float lower = setting.getLowerFraction();
        float upper = setting.getUpperFraction();
        lowerFill.start(lower, lower);
        upperFill.start(upper, upper);
    }

    
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        if (editing && ScreenManager.focusedSetting != setting) commitTextEdit(false);
        if (draggingLower || draggingUpper || draggingTie) updateFromPointer(mouseX);
        lowerFill.setTarget(setting.getLowerFraction());
        upperFill.setTarget(setting.getUpperFraction());
        float lowerProgress = (float) lowerFill.value();
        float upperProgress = (float) upperFill.value();
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
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, TextBuffers.abbreviate(setting.getLabel(), 12), rowX + ColorTheme.CONTENT_PADDING, y + 3.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
        boolean lowerEditing = editing && !editingUpper;
        boolean upperEditing = editing && editingUpper;
        CharSequence lowerText = lowerEditing ? (editText.isEmpty() ? lowerEmptyPlaceholder() : editText)
                : TextBuffers.fixed(setting.getLower(), 1);
        CharSequence upperText = upperEditing ? (editText.isEmpty() ? upperEmptyPlaceholder() : editText)
                : TextBuffers.fixed(setting.getUpper(), 1);
        String separator = "-";
        CharSequence unit = setting.getUnit();
        float unitWidth = unit == null || unit.isEmpty() ? 0.0f : FontAsset.INTER_SEMI_BOLD.measureText(unit, 5.0f);
        lowerTextWidth = FontAsset.INTER_SEMI_BOLD.measureText(lowerText, 6.0f);
        upperTextWidth = FontAsset.INTER_SEMI_BOLD.measureText(upperText, 6.0f);
        float separatorWidth = FontAsset.INTER_SEMI_BOLD.measureText(separator, 6.0f);
        upperTextX = rowX + rowWidth - ColorTheme.CONTENT_PADDING - upperTextWidth - unitWidth;
        float separatorX = upperTextX - VALUE_GAP - separatorWidth;
        lowerTextX = separatorX - VALUE_GAP - lowerTextWidth;
        int lowerColor = lowerEditing ? ColorTheme.SELECTED_OPTION_TEXT.getRGB()
                : ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, lowerText, lowerTextX, y + 3.0f, 6.0f, lowerEditing ? ColorTheme.multiplyAlpha(lowerColor, opacity) : lowerColor);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, separator, separatorX, y + 3.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity));
        int upperColor = upperEditing ? ColorTheme.SELECTED_OPTION_TEXT.getRGB()
                : ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, upperText, upperTextX, y + 3.0f, 6.0f, upperEditing ? ColorTheme.multiplyAlpha(upperColor, opacity) : upperColor);
        if (unitWidth > 0.0f) {
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, unit, upperTextX + upperTextWidth, y + 4.0f, 5.0f, ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity * 0.6f));
        }
        if (editing && !editText.isEmpty()) {
            if (caretPulse.isFinished()) caretPulse.start(caretPulse.targetValue(), 1.0 - caretPulse.targetValue());
            float textX = editingUpper ? upperTextX : lowerTextX;
            int visibleCaret = Math.min(caret, editText.length());
            float caretX = textX + FontAsset.INTER_SEMI_BOLD.measureText(editText.substring(0, visibleCaret), 6.0f);
            int alpha = (int) (caretPulse.value() * 255.0);
            float caretY = y + 3.0f + (FontAsset.INTER_SEMI_BOLD.lineHeight(6.0f) - 6.0f) * 0.5f;
            renderer.drawRoundedRect(caretX, caretY, 1.0f, 6.0f, 0.0f, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(ColorTheme.SLIDER_FILL, alpha).getRGB(), opacity));
        }
        float trackY = y + ColorTheme.ROW_HEIGHT;
        float trackX = rowX + ColorTheme.CONTENT_PADDING;
        float trackRight = rowX + rowWidth - ColorTheme.CONTENT_PADDING;
        float trackWidth = trackRight - trackX;
        renderer.drawRoundedRect(trackX, trackY, trackWidth, 3.0f, 1.5f, ColorTheme.multiplyAlpha(ColorTheme.SLOT_BACKGROUND.getRGB(), opacity));
        float lowerX = trackX + trackWidth * lowerProgress;
        float upperX = trackX + trackWidth * upperProgress;
        renderer.drawRoundedRect(lowerX, trackY, upperX - lowerX, 3.0f, 1.5f, ColorTheme.multiplyAlpha(ColorTheme.SLIDER_FILL.getRGB(), opacity));
        renderer.drawRoundedRect(lowerX - 4.0f, trackY - 2.0f, 8.0f, 7.0f, 4.0f, ColorTheme.multiplyAlpha(ColorTheme.TOGGLE_KNOB.getRGB(), opacity));
        renderer.drawRoundedRect(upperX - 4.0f, trackY - 2.0f, 8.0f, 7.0f, 4.0f, ColorTheme.multiplyAlpha(ColorTheme.TOGGLE_KNOB.getRGB(), opacity));
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
        if (ColorTheme.contains(lowerTextX - 3.0f, y + 1.0f, lowerTextWidth + 6.0f, 10.0f, (float) mouseX, (float) mouseY)) { beginTextEdit(false); return true; }
        if (ColorTheme.contains(upperTextX - 3.0f, y + 1.0f, upperTextWidth + 6.0f, 10.0f, (float) mouseX, (float) mouseY)) { beginTextEdit(true); return true; }
        if (editing) commitTextEdit(false);
        if (!ColorTheme.contains(trackX - 6.0f, trackY - 5.0f, trackRight - trackX + 12.0f, 12.0f, (float) mouseX, (float) mouseY)) return false;
        float fraction = Math.clamp((float) (mouseX - trackX) / (trackRight - trackX), 0.0f, 1.0f);
        float lowerDistance = Math.abs(fraction - setting.getLowerFraction());
        float upperDistance = Math.abs(fraction - setting.getUpperFraction());
        if (lowerDistance < upperDistance) {
            draggingLower = true;
            lastDragValue = setting.getLower();
        } else if (upperDistance < lowerDistance) {
            draggingUpper = true;
            lastDragValue = setting.getUpper();
        } else {
            draggingTie = true;
            tieStartValue = setting.getLower();
            lastDragValue = tieStartValue;
        }
        updateFromPointer(mouseX);
        return true;
    }

    
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingLower = false; draggingUpper = false; draggingTie = false; tieStartValue = Float.NaN;
    }

    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editing || ScreenManager.focusedSetting != setting) return false;
        if (keyCode == InputKey.BACKSPACE.code()) {
            if (caret > 0) { editText = editText.substring(0, caret - 1) + editText.substring(caret); caret--; }
        } else if (keyCode == InputKey.DELETE.code()) {
            if (caret < editText.length()) editText = editText.substring(0, caret) + editText.substring(caret + 1);
        } else if (keyCode == InputKey.LEFT.code()) caret = Math.max(0, caret - 1);
        else if (keyCode == InputKey.RIGHT.code()) caret = Math.min(editText.length(), caret + 1);
        else if (keyCode == InputKey.HOME.code()) caret = 0;
        else if (keyCode == InputKey.END.code()) caret = editText.length();
        else if (keyCode == InputKey.ENTER.code() || keyCode == InputKey.KEYPAD_ENTER.code()) {
            commitTextEdit(setting.isOutOfRangeAllowed()); return true;
        } else if (keyCode == InputKey.ESCAPE.code()) { cancelTextEdit(); return true; }
        else return false;
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
        caret++; caretPulse.start(0.0, 1.0);
    }

    private void beginTextEdit(boolean upper) {
        if (editing && editingUpper == upper) return;
        if (editing) commitTextEdit(setting.isOutOfRangeAllowed());
        editing = true; editingUpper = upper; ScreenManager.focusedSetting = setting;
        editText = ""; caret = 0; caretPulse.start(0.0, 1.0);
    }

    private static String lowerEmptyPlaceholder() {
        if (lowerEmptyPlaceholder == null) {
            lowerEmptyPlaceholder = new String(new char[]{'.', '.', '.'});
        }
        return lowerEmptyPlaceholder;
    }

    private static String upperEmptyPlaceholder() {
        if (upperEmptyPlaceholder == null) {
            upperEmptyPlaceholder = new String(new char[]{'.', '.', '.'});
        }
        return upperEmptyPlaceholder;
    }

    private void commitTextEdit(boolean allowOutOfRange) {
        try {
            float value = Float.parseFloat(editText.trim());
            value = Math.round(value / setting.getStep()) * setting.getStep();
            if (editingUpper) {
                if (!allowOutOfRange) value = Math.clamp(value, setting.getLower(), setting.getMaximum());
                setting.setUpper(value);
            } else {
                if (!allowOutOfRange) value = Math.clamp(value, setting.getMinimum(), setting.getUpper());
                setting.setLower(value);
            }
            setting.notifyChanged();
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
        float range = setting.getMaximum() - setting.getMinimum();
        float value = setting.getMinimum() + range * fraction;
        value = Math.round(value / setting.getStep()) * setting.getStep();
        if (draggingTie) {
            if (value <= setting.getUpper()) setting.setLower(value);
            else if (value >= setting.getLower()) setting.setUpper(value);
            else return;
        }
        if (draggingLower && value <= setting.getUpper()) setting.setLower(value);
        if (draggingUpper && value >= setting.getLower()) setting.setUpper(value);
        float threshold = range * 0.03f;
        if (lastDragValue == Float.MIN_VALUE || Math.abs(value - lastDragValue) >= threshold) lastDragValue = value;
    }

    
    @Override public FloatRangeSetting setting() { return setting; }
}
