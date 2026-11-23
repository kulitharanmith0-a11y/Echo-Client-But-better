package com.blamejared.clumps.client.gui.widgets;

import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.ui.ColorTheme;
import com.blamejared.clumps.client.ui.HintRenderer;
import com.blamejared.clumps.client.gui.animations.Easing;
import com.blamejared.clumps.client.gui.animations.Tween;
import com.blamejared.clumps.client.utils.strings.TextBuffers;
import java.awt.Color;

public class ColorPickerWidget implements SettingWidget {
    private final ColorSetting setting;
    private float x;
    private float y;
    private boolean expanded = false;
    private final Tween expansion;
    private boolean draggingSaturationBrightness = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    
    public ColorPickerWidget(ColorSetting setting) {
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 200.0);
        this.setting = setting;
        expansion.start(0.0, 0.0);
    }

    
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        expansion.setTarget(expanded ? 1.0f : 0.0f);
        float progress = (float) expansion.value();
        if (draggingSaturationBrightness) {
            updateSaturationBrightness(mouseX, mouseY);
        } else if (draggingHue) {
            updateHue(mouseX);
        } else if (draggingAlpha) {
            updateAlpha(mouseX);
        }

        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        boolean hovered = ColorTheme.contains(rowX, y, rowWidth, height(), mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                HintRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }
        renderer.drawRoundedRect(rowX, y, rowWidth, height(), ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha((hovered ? ColorTheme.ROW_HOVER : ColorTheme.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, height() - 4.0f, 0.0f, ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 14);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ColorTheme.CONTENT_PADDING, y + ColorTheme.ROW_HEIGHT / 2.0f - 4.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));

        float previewX = rowX + rowWidth - ColorTheme.TOGGLE_WIDTH - ColorTheme.CONTENT_PADDING;
        float previewY = y + (ColorTheme.ROW_HEIGHT - ColorTheme.TOGGLE_HEIGHT) / 2.0f;
        renderer.drawRoundedRect(previewX, previewY, ColorTheme.TOGGLE_WIDTH, ColorTheme.TOGGLE_HEIGHT, ColorTheme.TOGGLE_HEIGHT / 2.3f, ColorTheme.multiplyAlpha(setting.getArgb(), opacity));
        if (progress > 0.01f) {
            float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
            float pickerY = y + ColorTheme.ROW_HEIGHT;
            float pickerHeight = 60.0f * progress;
            float pickerX = rowX + ColorTheme.CONTENT_PADDING;
            float pickerWidth = rowWidth - ColorTheme.CONTENT_PADDING * 2.0f;
            renderer.drawSaturationBrightness(pickerX, pickerY, pickerWidth, pickerHeight, hsb[0], opacity);
            float saturationX = pickerX + pickerWidth * hsb[1];
            float brightnessY = pickerY + pickerHeight * (1.0f - hsb[2]);
            renderer.drawRoundedRect(saturationX - 2.0f, brightnessY - 2.0f, 4.0f, 4.0f, 2.0f, ColorTheme.multiplyAlpha(Color.WHITE.getRGB(), opacity * progress));

            float hueY = pickerY + pickerHeight + 5.0f * progress;
            renderer.drawHueStrip(pickerX, hueY, pickerWidth, 5.0f * progress, opacity);
            renderer.drawRoundedRect(pickerX + pickerWidth * hsb[0] - 2.0f, hueY - 2.0f * progress, 4.0f, 9.0f * progress, 2.0f * progress, ColorTheme.multiplyAlpha(Color.WHITE.getRGB(), opacity * progress));

            if (setting.isAlphaMode()) {
                float alphaY = hueY + 10.0f * progress;
                float red = setting.getRed() / 255.0f;
                float green = setting.getGreen() / 255.0f;
                float blue = setting.getBlue() / 255.0f;
                renderer.drawAlphaStrip(pickerX, alphaY, pickerWidth, 5.0f * progress, red, green, blue, opacity);
                renderer.drawRoundedRect(pickerX + pickerWidth * setting.getAlphaFraction() - 2.0f, alphaY - 2.0f * progress, 4.0f, 9.0f * progress, 2.0f * progress, ColorTheme.multiplyAlpha(Color.WHITE.getRGB(), opacity * progress));
            }
        }
    }

    
    @Override
    public float height() {
        float progress = (float) expansion.value();
        float height = ColorTheme.ROW_HEIGHT;
        if (progress > ColorTheme.VISIBLE_ANIMATION_THRESHOLD) {
            float pickerHeight = setting.isAlphaMode()
                    ? ColorTheme.COLOR_PICKER_HEIGHT : ColorTheme.COLOR_PICKER_HEIGHT - 10.0f;
            height += pickerHeight * progress;
        }
        return height;
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        float previewX = rowX + rowWidth - ColorTheme.TOGGLE_WIDTH - ColorTheme.CONTENT_PADDING;
        float previewY = y + (ColorTheme.ROW_HEIGHT - ColorTheme.TOGGLE_HEIGHT) / 2.0f;
        if (ColorTheme.contains(previewX, previewY, ColorTheme.TOGGLE_WIDTH, ColorTheme.TOGGLE_HEIGHT, (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            return true;
        }
        float progress = (float) expansion.value();
        if (expanded && progress > ColorTheme.INPUT_ANIMATION_THRESHOLD) {
            float pickerY = y + ColorTheme.ROW_HEIGHT;
            float pickerX = rowX + ColorTheme.CONTENT_PADDING;
            float pickerWidth = rowWidth - ColorTheme.CONTENT_PADDING * 2.0f;
            float hueY = pickerY + 65.0f;
            float alphaY = hueY + 10.0f;
            if (ColorTheme.contains(pickerX, pickerY, pickerWidth, 60.0f, (float) mouseX, (float) mouseY)) {
                stopDragging();
                draggingSaturationBrightness = true;
                updateSaturationBrightness(mouseX, mouseY);
                return true;
            }
            if (ColorTheme.contains(pickerX, hueY, pickerWidth, 5.0f, (float) mouseX, (float) mouseY)) {
                stopDragging();
                draggingHue = true;
                updateHue(mouseX);
                return true;
            }
            if (setting.isAlphaMode()
                    && ColorTheme.contains(pickerX, alphaY, pickerWidth, 5.0f, (float) mouseX, (float) mouseY)) {
                stopDragging();
                draggingAlpha = true;
                updateAlpha(mouseX);
                return true;
            }
        }
        return false;
    }

    
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSaturationBrightness || draggingHue || draggingAlpha) {
            setting.notifyChanged();
        }
        stopDragging();
    }

    private void stopDragging() {
        draggingSaturationBrightness = false;
        draggingHue = false;
        draggingAlpha = false;
    }

    private void updateSaturationBrightness(double mouseX, double mouseY) {
        float rowX = x + ColorTheme.ROW_INDENT;
        float pickerX = rowX + ColorTheme.CONTENT_PADDING;
        float pickerWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f
                - ColorTheme.CONTENT_PADDING * 2.0f;
        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        float pickerY = y + ColorTheme.ROW_HEIGHT;
        float saturation = Math.clamp((float) (mouseX - pickerX) / pickerWidth, 0.0f, 1.0f);
        float brightness = Math.clamp(1.0f - (float) (mouseY - pickerY) / 60.0f, 0.0f, 1.0f);
        Color color = Color.getHSBColor(hsb[0], saturation, brightness);
        setting.setRedSilently(color.getRed());
        setting.setGreenSilently(color.getGreen());
        setting.setBlueSilently(color.getBlue());
    }

    private void updateHue(double mouseX) {
        float rowX = x + ColorTheme.ROW_INDENT;
        float pickerX = rowX + ColorTheme.CONTENT_PADDING;
        float pickerWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f
                - ColorTheme.CONTENT_PADDING * 2.0f;
        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        float hue = Math.clamp((float) (mouseX - pickerX) / pickerWidth, 0.0f, 1.0f);
        Color color = Color.getHSBColor(hue, hsb[1], hsb[2]);
        setting.setRedSilently(color.getRed());
        setting.setGreenSilently(color.getGreen());
        setting.setBlueSilently(color.getBlue());
    }

    private void updateAlpha(double mouseX) {
        float rowX = x + ColorTheme.ROW_INDENT;
        float pickerX = rowX + ColorTheme.CONTENT_PADDING;
        float pickerWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f
                - ColorTheme.CONTENT_PADDING * 2.0f;
        setting.setAlphaSilently((int) (Math.clamp((float) (mouseX - pickerX) / pickerWidth, 0.0f, 1.0f) * 255.0f));
    }

    
    @Override
    public ColorSetting setting() {
        return setting;
    }
}
