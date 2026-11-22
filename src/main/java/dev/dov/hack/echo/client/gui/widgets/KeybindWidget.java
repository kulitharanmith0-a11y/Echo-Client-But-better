package dev.dov.hack.echo.client.gui.widgets;

import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.settings.KeybindSetting;
import dev.dov.hack.echo.client.settings.Setting;
import dev.dov.hack.echo.client.ui.ColorTheme;
import dev.dov.hack.echo.client.ui.HintRenderer;
import dev.dov.hack.echo.client.utils.strings.TextBuffers;
import dev.dov.hack.echo.client.render.font.FontAsset;

public class KeybindWidget implements SettingWidget {
    private static final String CAPTURE_RENDER_LABEL = "...";
    private static final String CAPTURE_HIT_TEST_LABEL = "...";

    private final KeybindSetting setting;
    private float x;
    private float y;
    private boolean capturing;

    public KeybindWidget(KeybindSetting setting) {
        capturing = false;
        this.setting = setting;
    }

    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
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
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 14);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ColorTheme.CONTENT_PADDING, y + ColorTheme.ROW_HEIGHT / 2.0f - 4.0f, 6.0f, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
        String keyLabel = capturing ? CAPTURE_RENDER_LABEL : setting.getKey().displayLabel();
        float keyWidth = FontAsset.INTER_SEMI_BOLD.measureText(keyLabel, 6.0f) + 8.0f;
        float keyX = rowX + rowWidth - ColorTheme.CONTENT_PADDING - keyWidth;
        float keyY = y + (ColorTheme.ROW_HEIGHT - 10.0f) / 2.0f;
        int background = capturing
                ? ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity)
                : ColorTheme.multiplyAlpha(ColorTheme.SLOT_BACKGROUND.getRGB(), opacity);
        int foreground = capturing
                ? ColorTheme.multiplyAlpha(ColorTheme.TAB_ACTIVE_TEXT, opacity)
                : ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity);
        renderer.drawRoundedRect(keyX, keyY, keyWidth, 10.0f, 5.0f, background);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, keyLabel, keyX + 4.0f, y + ColorTheme.ROW_HEIGHT / 2.0f - 4.0f, 6.0f, foreground);
    }

    @Override
    public float height() {
        return ColorTheme.ROW_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float rowX = x + ColorTheme.ROW_INDENT;
        float rowWidth = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4.0f;
        if (capturing) {
            setting.setKey(InputKey.fromMouseCode(button));
            setting.setListeningForKey(false);
            capturing = false;
            return true;
        }
        if (!InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        String keyLabel = capturing ? CAPTURE_HIT_TEST_LABEL : setting.getKey().displayLabel();
        float keyWidth = FontAsset.INTER_SEMI_BOLD.measureText(keyLabel, 6.0f) + 8.0f;
        float keyX = rowX + rowWidth - ColorTheme.CONTENT_PADDING - keyWidth;
        float keyY = y + (ColorTheme.ROW_HEIGHT - 10.0f) / 2.0f;
        if (ColorTheme.contains(keyX, keyY, keyWidth, 10.0f, (float) mouseX, (float) mouseY)) {
            capturing = !capturing;
            setting.setListeningForKey(capturing);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!capturing) {
            return false;
        }
        if (keyCode == InputKey.ESCAPE.code()) {
            setting.setKey(InputKey.NONE);
        } else {
            setting.setKey(InputKey.fromCode(keyCode));
        }
        setting.setListeningForKey(false);
        capturing = false;
        return true;
    }

    
    public KeybindSetting getSetting() {
        return setting;
    }

    
    @Override
    public Setting setting() {
        return getSetting();
    }
}
