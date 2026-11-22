package dev.dov.hack.echo.client.gui.panels;

import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.gui.CategoryPanelScreen;
import dev.dov.hack.echo.client.gui.widgets.*;
import dev.dov.hack.echo.client.settings.*;
import dev.dov.hack.echo.client.ui.ColorTheme;
import dev.dov.hack.echo.client.ui.HintRenderer;
import dev.dov.hack.echo.client.gui.animations.Easing;
import dev.dov.hack.echo.client.gui.animations.Tween;
import dev.dov.hack.echo.client.render.font.FontAsset;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.Click;

public class FeatureCard {
    private float height;
    private float width;
    private float x;
    private float y;
    private final Module feature;
    private final CategoryPanelScreen screen;
    private final List<SettingWidget> widgets;
    private final Tween expansion;
    public boolean expanded;
    private static final float VISIBILITY_ICON_SIZE = 9.0f;
    private static final float ICON_GAP = 4.0f;
    private float keyLeft;
    private float keyRight;
    private static final String CAPTURE_LABEL = "...";

    public FeatureCard(CategoryPanelScreen screen, Module feature) {
        height = ColorTheme.FEATURE_ROW_HEIGHT;
        widgets = new ObjectArrayList<>();
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 300.0);
        expanded = false;
        this.feature = feature;
        this.screen = screen;
        for (Setting setting : feature.settings) {
            widgets.add(switch (Objects.requireNonNull(setting)) {
                case BooleanSetting value -> new BooleanToggleWidget(value);
                case IntSetting value -> new IntegerSliderWidget(value);
                case FloatSetting value -> new FloatSliderWidget(value);
                case ChoiceSetting value -> new ChoiceWidget(value);
                case KeybindSetting value -> new KeybindWidget(value);
                case ColorSetting value -> new ColorPickerWidget(value);
                case MultiChoiceSetting value -> new BooleanGroupWidget(value);
                case FloatRangeSetting value -> new RangeSliderWidget(value);
                case TextSetting value -> new TextSettingWidget(value);
                case RegistryListSetting<?> value -> new RegistryMultiSelectWidget<>(value);
                case HotbarSlotsSetting value -> new HotbarSlotsWidget(value);
                default -> null;
            });
        }
        expansion.start(ColorTheme.FEATURE_ROW_HEIGHT, ColorTheme.FEATURE_ROW_HEIGHT);
    }

    private float preferredHeight() {
        if (!expanded) {
            return ColorTheme.FEATURE_ROW_HEIGHT;
        }
        float result = ColorTheme.FEATURE_ROW_HEIGHT;
        for (SettingWidget widget : widgets) {
            if (widget == null || !widget.setting().isVisible()) {
                continue;
            }
            result += widget.height() + ColorTheme.SEARCH_GAP;
        }
        return result;
    }

    public void updateExpansion() {
        float target = preferredHeight();
        if (target != expansion.targetValue()) {
            expansion.setTarget(target);
        }
    }

    public void layout(float x, float y) {
        this.x = x;
        this.y = y;
        width = ColorTheme.PANEL_WIDTH;
        updateExpansion();
        height = (float) expansion.value();
    }

    public void render(UiRenderer renderer, int mouseX, int mouseY, float opacity, boolean layoutLocked) {
        boolean hovered = ColorTheme.contains(x, y, width, ColorTheme.FEATURE_ROW_HEIGHT, mouseX, mouseY);
        if (hovered && !layoutLocked) {
            CharSequence description = feature.getDescription();
            if (description != null && description.length() > 0) {
                HintRenderer.offer(feature, feature.getName(), description, mouseX, mouseY);
            }
        }
        if (layoutLocked) {
            renderVisibilityRow(renderer, opacity, hovered);
            return;
        }
        if (feature.isEnabled()) {
            renderer.drawRoundedRect(x + 2.0f, y, width - 4.0f, ColorTheme.FEATURE_ROW_HEIGHT, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity));
        }
        if (hovered) {
            renderer.drawRoundedRect(x + 2.0f, y, width - 4.0f, ColorTheme.FEATURE_ROW_HEIGHT, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha(ColorTheme.FEATURE_CARD_BACKGROUND.getRGB(), opacity));
        }
        int labelColor = ColorTheme.multiplyAlpha(
                ColorTheme.toArgb(feature.isEnabled() ? ColorTheme.TAB_ACTIVE_TEXT : ColorTheme.PRIMARY_TEXT), opacity);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, feature.getName(), x + 2.0f + ColorTheme.CONTENT_PADDING, y + ColorTheme.FEATURE_ROW_HEIGHT / 2.0f - 5.0f, 8.0f, labelColor);

        boolean capturing = screen.bindingCard == this;
        boolean unbound = !capturing && feature.getKey() == InputKey.NONE;
        String keyLabel = "[" + (capturing ? CAPTURE_LABEL : unbound ? " " : feature.getKey().displayLabel()) + "]";
        float keyWidth = FontAsset.INTER_SEMI_BOLD.measureText(keyLabel, 7.0f);
        float keyX = x + width - keyWidth - 10.0f;
        keyLeft = keyX - 1.0f;
        keyRight = keyX + keyWidth + 1.0f;
        boolean keyHovered = !capturing && ColorTheme.contains(keyLeft, y, keyWidth + 2.0f, ColorTheme.FEATURE_ROW_HEIGHT, mouseX, mouseY);
        if (!unbound || keyHovered) {
            int keyColor = capturing || keyHovered
                    ? ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity) : labelColor;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, keyLabel, keyX, y + ColorTheme.FEATURE_ROW_HEIGHT / 2.0f - 4.0f, 7.0f, keyColor);
            if (!capturing && !unbound && FontAsset.LUCIDE != null) {
                String glyph = feature.isHoldMode() ? "\ue1e8" : "\ue18b";
                renderer.drawText(FontAsset.LUCIDE, glyph, keyX - FontAsset.LUCIDE.measureText(glyph, 8.0f) - ICON_GAP, y + ColorTheme.FEATURE_ROW_HEIGHT / 2.0f - 4.0f, 8.0f, labelColor);
            }
        }
        if (!feature.settings.isEmpty()) {
            String glyph = expanded ? "\u25bc" : "\u25b6";
            float glyphWidth = FontAsset.INTER_SEMI_BOLD.measureText(glyph, 8.0f);
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, glyph, x + width - glyphWidth - ICON_GAP, y + ColorTheme.FEATURE_ROW_HEIGHT / 2.0f - 5.0f, 8.0f, labelColor);
        }
        if (height > ColorTheme.FEATURE_ROW_HEIGHT) {
            float widgetY = y + ColorTheme.FEATURE_ROW_HEIGHT + ColorTheme.SEARCH_GAP;
            renderer.pushScissor(x, widgetY, ColorTheme.PANEL_WIDTH, height - ColorTheme.FEATURE_ROW_HEIGHT);
            for (SettingWidget widget : widgets) {
                if (widget == null || !widget.setting().isVisible()) {
                    continue;
                }
                widget.render(renderer, x, widgetY, mouseX, mouseY, opacity);
                widgetY += widget.height() + ColorTheme.SEARCH_GAP;
            }
            renderer.popScissor();
        }
        x -= 2.0f;
        width -= 4.0f;
    }

    private void renderVisibilityRow(UiRenderer renderer, float opacity, boolean hovered) {
        if (hovered) {
            renderer.drawRoundedRect(x + 2.0f, y, width - 4.0f, ColorTheme.FEATURE_ROW_HEIGHT, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha(ColorTheme.FEATURE_CARD_BACKGROUND.getRGB(), opacity));
        }
        boolean visible = feature.isVisible();
        int labelColor = visible ? ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity)
                : ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity);
        float labelX = x + 2.0f + ColorTheme.CONTENT_PADDING;
        if (FontAsset.LUCIDE != null) {
            String glyph = visible ? "\ue11c" : "\ue13d";
            float glyphWidth = FontAsset.LUCIDE.measureText(glyph, VISIBILITY_ICON_SIZE);
            float glyphY = y + ColorTheme.FEATURE_ROW_HEIGHT / 2.0f - 4.5f;
            renderer.drawText(FontAsset.LUCIDE, glyph, labelX, glyphY, VISIBILITY_ICON_SIZE, labelColor);
            labelX += glyphWidth + ICON_GAP;
        }
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, feature.getName(), labelX, y + ColorTheme.FEATURE_ROW_HEIGHT / 2.0f - 5.0f, 8.0f, labelColor);
    }

    public boolean isKeyHovered(float mouseX) {
        return mouseX >= keyLeft && mouseX <= keyRight;
    }

    public boolean mouseClicked(Click click) {
        if (!expanded) {
            return false;
        }
        float widgetY = y + ColorTheme.FEATURE_ROW_HEIGHT + ColorTheme.SEARCH_GAP;
        for (SettingWidget widget : widgets) {
            if (widget == null || !widget.setting().isVisible()) {
                continue;
            }
            float widgetHeight = widget.height();
            if (click.y() >= widgetY && click.y() <= widgetY + widgetHeight
                    && widget.mouseClicked(click.x(), click.y(), click.button())) {
                return true;
            }
            widgetY += widgetHeight + ColorTheme.SEARCH_GAP;
        }
        return false;
    }

    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!expanded) {
            return false;
        }
        for (SettingWidget widget : widgets) {
            if (widget != null) {
                widget.mouseReleased(mouseX, mouseY, button);
            }
        }
        return false;
    }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!expanded) {
            return false;
        }
        for (SettingWidget widget : widgets) {
            if (widget != null && widget.setting().isVisible() && widget.keyPressed(key, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char character, int modifiers) {
        if (!expanded) {
            return false;
        }
        for (SettingWidget widget : widgets) {
            if (widget != null && widget.setting().isVisible()) {
                widget.charTyped(character, modifiers);
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (!expanded) {
            return false;
        }
        float widgetY = y + ColorTheme.FEATURE_ROW_HEIGHT + ColorTheme.SEARCH_GAP;
        for (SettingWidget widget : widgets) {
            if (widget == null || !widget.setting().isVisible()) {
                continue;
            }
            float widgetHeight = widget.height();
            if (mouseY >= widgetY && mouseY <= widgetY + widgetHeight
                    && mouseX >= x && mouseX <= x + ColorTheme.PANEL_WIDTH
                    && widget.mouseScrolled(mouseX, mouseY, vertical)) {
                return true;
            }
            widgetY += widgetHeight + ColorTheme.SEARCH_GAP;
        }
        return false;
    }

    public float getHeight() { return height; }
    public float getWidth() { return width; }
    public float getX() { return x; }
    public float getY() { return y; }
    public Module getFeature() { return feature; }
}
