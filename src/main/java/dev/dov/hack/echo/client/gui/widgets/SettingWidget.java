package dev.dov.hack.echo.client.gui.widgets;

import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.settings.Setting;

public interface SettingWidget {
    
    void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity);

    
    float height();

    
    boolean mouseClicked(double mouseX, double mouseY, int button);

    
    void mouseReleased(double mouseX, double mouseY, int button);

    
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    
    default void charTyped(char character, int modifiers) {
    }

    
    default boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return false;
    }

    
    Setting setting();
}
