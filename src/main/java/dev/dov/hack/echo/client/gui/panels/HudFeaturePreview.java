package dev.dov.hack.echo.client.gui.panels;

import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleManager;
import dev.dov.hack.echo.client.modules.HudModule;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.ui.ColorTheme;
import dev.dov.hack.echo.client.render.font.FontAsset;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class HudFeaturePreview {
    private static final Color IDLE_BACKGROUND = new Color(255, 255, 255, 10);
    private static final Color ACTIVE_BACKGROUND = new Color(0, 128, 255, 25);
    private static final Color IDLE_BORDER = new Color(255, 255, 255, 80);
    private static final Color ACTIVE_BORDER = new Color(0, 128, 255, 200);

    private HudModule draggingFeature;
    private float dragOffsetX;
    private float dragOffsetY;

    public HudFeaturePreview() {
        draggingFeature = null;
    }

    public void render(UiRenderer renderer, int viewportWidth, int viewportHeight, int mouseX, int mouseY, float opacity) {
        if (FontAsset.INTER_SEMI_BOLD == null) {
            return;
        }
        renderer.flush();
        List<HudModule> features = previewFeatures();
        for (HudModule Module : features) {
            float x = Module.getX();
            float y = Module.getY();
            float width = Module.getWidth();
            float height = Module.getHeight();
            boolean active = draggingFeature == Module || draggingFeature == null
                    && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            Color background = active ? ACTIVE_BACKGROUND : IDLE_BACKGROUND;
            Color border = active ? ACTIVE_BORDER : IDLE_BORDER;
            renderer.drawRoundedRect(x, y, width, height, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha(background.getRGB(), opacity));
            renderer.drawRoundedRect(x, y, width, 1.0f, 0.0f, ColorTheme.multiplyAlpha(border.getRGB(), opacity));
            renderer.drawRoundedRect(x, y + height - 1.0f, width, 1.0f, 0.0f, ColorTheme.multiplyAlpha(border.getRGB(), opacity));
            renderer.drawRoundedRect(x, y, 1.0f, height, 0.0f, ColorTheme.multiplyAlpha(border.getRGB(), opacity));
            renderer.drawRoundedRect(x + width - 1.0f, y, 1.0f, height, 0.0f, ColorTheme.multiplyAlpha(border.getRGB(), opacity));
            CharSequence label = Module.getName();
            float fontSize = 7.0f;
            float fontHeight = FontAsset.INTER_SEMI_BOLD.lineHeight(fontSize);
            int labelColor = ColorTheme.multiplyAlpha(
                    (active ? ACTIVE_BORDER : ColorTheme.SELECTED_OPTION_TEXT).getRGB(), opacity);
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, x, y - fontHeight - 2.0f, fontSize, labelColor);
        }
    }

    
    public boolean mouseClicked(double mouseX, double mouseY) {
        for (HudModule Module : previewFeatures()) {
            float x = Module.getX();
            float y = Module.getY();
            float width = Module.getWidth();
            float height = Module.getHeight();
            if (mouseX >= x && mouseX <= (double) (x + width)
                    && mouseY >= y && mouseY <= (double) (y + height)) {
                draggingFeature = Module;
                dragOffsetX = (float) mouseX - x;
                dragOffsetY = (float) mouseY - y;
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, int viewportWidth, int viewportHeight) {
        if (draggingFeature == null || !InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        float x = (float) mouseX - dragOffsetX;
        float y = (float) mouseY - dragOffsetY;
        draggingFeature.setPosition(
                Math.clamp(x, 0.0f, (float) viewportWidth - draggingFeature.getWidth()), Math.clamp(y, 0.0f, (float) viewportHeight - draggingFeature.getHeight()));
        return true;
    }

    public boolean mouseReleased(int button) {
        if (!InputKey.MOUSE_LEFT.matches(button) || draggingFeature == null) {
            return false;
        }
        draggingFeature = null;
        return true;
    }

    public void reset() {
        draggingFeature = null;
    }

    private List<HudModule> previewFeatures() {
        List<HudModule> result = new ArrayList<>();
        for (Module Module : ModuleManager.allFeatures()) {
            if (Module instanceof HudModule hudModule && Module.isEnabled()) {
                result.add(hudModule);
            }
        }
        return result;
    }
}
