package dev.dov.hack.echo.client.gui.widgets;

import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.ui.ColorTheme;
import dev.dov.hack.echo.client.ui.HintRenderer;
import dev.dov.hack.echo.client.gui.animations.Easing;
import dev.dov.hack.echo.client.gui.animations.Tween;
import dev.dov.hack.echo.client.utils.strings.TextBuffers;
import java.awt.Color;

public class ChoiceWidget implements SettingWidget {
    private final ChoiceSetting setting;
    private float x;
    private float y;
    private boolean expanded = false;
    private final Tween expansion;

    
    public ChoiceWidget(ChoiceSetting setting) {
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
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, height - 4.0f, 0.0f, ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 14);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ColorTheme.CONTENT_PADDING, y + ColorTheme.ROW_HEIGHT / 2.0f - 4.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
        CharSequence selected = setting.getValue();
        float selectedWidth = FontAsset.INTER_SEMI_BOLD.measureText(selected, 6.0f);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, selected, rowX + rowWidth - ColorTheme.CONTENT_PADDING - selectedWidth, y + ColorTheme.ROW_HEIGHT / 2.0f - 4.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity));

        if (progress > ColorTheme.VISIBLE_ANIMATION_THRESHOLD) {
            float optionY = y + ColorTheme.ROW_HEIGHT;
            int alpha = (int) (progress * 255.0f);
            for (CharSequence option : setting.getOptions()) {
                Color color = setting.isSelected(option) ? ColorTheme.SELECTED_OPTION_TEXT : ColorTheme.SECONDARY_TEXT;
                int argb = ColorTheme.multiplyAlpha(ColorTheme.withAlpha(color, alpha), opacity);
                renderer.drawText(FontAsset.INTER_SEMI_BOLD, option, rowX + ColorTheme.CONTENT_PADDING + 4.0f, optionY + (ColorTheme.ROW_HEIGHT - 2.0f) / 2.0f - 4.0f, 6.0f, argb);
                optionY += ColorTheme.ROW_HEIGHT - 2.0f;
            }
        }
    }

    
    @Override
    public float height() {
        float progress = (float) expansion.value();
        float height = ColorTheme.ROW_HEIGHT;
        if (progress > ColorTheme.VISIBLE_ANIMATION_THRESHOLD) {
            height += (ColorTheme.ROW_HEIGHT - 2.0f) * setting.getOptions().size() * progress;
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
        if (ColorTheme.contains(rowX, y, rowWidth, ColorTheme.ROW_HEIGHT, (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            return true;
        }
        float progress = (float) expansion.value();
        if (expanded && progress > ColorTheme.INPUT_ANIMATION_THRESHOLD) {
            float optionY = y + ColorTheme.ROW_HEIGHT;
            for (CharSequence option : setting.getOptions()) {
                if (ColorTheme.contains(rowX + ColorTheme.CONTENT_PADDING, optionY, rowWidth - ColorTheme.CONTENT_PADDING * 2.0f, ColorTheme.ROW_HEIGHT - 2.0f, (float) mouseX, (float) mouseY)) {
                    setting.setValue(option);
                    return true;
                }
                optionY += ColorTheme.ROW_HEIGHT - 2.0f;
            }
        }
        return false;
    }

    
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    
    @Override
    public ChoiceSetting setting() {
        return setting;
    }
}
