package com.blamejared.clumps.client.gui.widgets;

import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.ui.ColorTheme;
import com.blamejared.clumps.client.ui.HintRenderer;
import com.blamejared.clumps.client.gui.animations.Easing;
import com.blamejared.clumps.client.gui.animations.Tween;
import com.blamejared.clumps.client.utils.strings.TextBuffers;
import java.awt.Color;

public class BooleanToggleWidget implements SettingWidget {
    private BooleanSetting setting;
    private float x;
    private float y;
    private Tween toggle = new Tween(Easing.EASE_OUT_CUBIC, 150.0);

    
    public BooleanToggleWidget(BooleanSetting setting) {
        this.setting = setting;
        float initial = setting.getValue() ? 1.0f : 0.0f;
        toggle.start(initial, initial);
    }

    
    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        toggle.setTarget(setting.getValue() ? 1.0f : 0.0f);
        float progress = (float) toggle.value();
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        boolean hovered = ColorTheme.contains(rowX, y, rowWidth, ColorTheme.ROW_HEIGHT, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                HintRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }

        renderer.drawRoundedRect(rowX, y, rowWidth, ColorTheme.ROW_HEIGHT, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha((hovered ? ColorTheme.ROW_HOVER : ColorTheme.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, ColorTheme.ROW_HEIGHT - 4.0f, 0.0f, ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 18);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ColorTheme.CONTENT_PADDING, y + ColorTheme.ROW_HEIGHT / 2.0f - 4.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));

        float trackX = rowX + rowWidth - ColorTheme.TOGGLE_WIDTH - ColorTheme.CONTENT_PADDING;
        float trackY = y + (ColorTheme.ROW_HEIGHT - ColorTheme.TOGGLE_HEIGHT) / 2.0f;
        Color track = ColorTheme.blend(ColorTheme.TOGGLE_OFF, ColorTheme.TOGGLE_ON, progress);
        renderer.drawRoundedRect(trackX, trackY, ColorTheme.TOGGLE_WIDTH, ColorTheme.TOGGLE_HEIGHT, ColorTheme.TOGGLE_HEIGHT / 2.3f, ColorTheme.multiplyAlpha(track.getRGB(), opacity));
        float knobX = trackX + 1.5f
                + (ColorTheme.TOGGLE_WIDTH - ColorTheme.TOGGLE_KNOB_SIZE - 3.0f) * progress;
        float knobY = trackY + 1.5f;
        renderer.drawRoundedRect(knobX + 0.5f, knobY + 0.5f, ColorTheme.TOGGLE_KNOB_SIZE, ColorTheme.TOGGLE_KNOB_SIZE, ColorTheme.TOGGLE_KNOB_SIZE / 2.0f, ColorTheme.multiplyAlpha(ColorTheme.KNOB_SHADOW.getRGB(), opacity));
        renderer.drawRoundedRect(knobX, knobY, ColorTheme.TOGGLE_KNOB_SIZE, ColorTheme.TOGGLE_KNOB_SIZE, ColorTheme.TOGGLE_KNOB_SIZE / 2.0f, ColorTheme.multiplyAlpha(ColorTheme.TOGGLE_KNOB.getRGB(), opacity));
    }

    
    @Override
    public float height() {
        return ColorTheme.ROW_HEIGHT;
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        float hitX = x + ColorTheme.PANEL_WIDTH - ColorTheme.TOGGLE_WIDTH
                - ColorTheme.CONTENT_PADDING - 2.0f;
        float hitY = y + (ColorTheme.ROW_HEIGHT - ColorTheme.TOGGLE_HEIGHT) / 2.0f;
        if (!ColorTheme.contains(hitX, hitY, ColorTheme.TOGGLE_WIDTH, ColorTheme.TOGGLE_HEIGHT, (float) mouseX, (float) mouseY)) {
            return false;
        }
        setting.setValue(!setting.getValue());
        return true;
    }

    
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    
    @Override
    public BooleanSetting setting() {
        return setting;
    }
}
