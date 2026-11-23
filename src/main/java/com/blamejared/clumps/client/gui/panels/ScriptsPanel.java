package com.blamejared.clumps.client.gui.panels;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.gui.CategoryPanelScreen;
import com.blamejared.clumps.client.runtime.ScriptFeature;
import com.blamejared.clumps.client.ui.ColorTheme;
import com.blamejared.clumps.client.utils.strings.Concat;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

public class ScriptsPanel {
    private static final float ROW_HEIGHT = 44.0F;
    private static final float BUTTON_WIDTH = 54.0F;
    private static final float BUTTON_HEIGHT = 18.0F;
    private static final float SEARCH_WIDTH = 130.0F;
    private static final float SEARCH_HEIGHT = 14.0F;
    private static final Color LOADED_OVERLAY = new Color(120, 230, 165, 20);

    private final CategoryPanelScreen screen;
    private float x;
    private float y;
    private float scroll;
    private float maxScroll;
    private final List<ScriptActionButton> buttons;
    private boolean positioned;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean searchFocused;
    private String search;
    private static final String RELOAD_LABEL = "Reload";
    private static final String NOT_LOADED = "not loaded";
    private static final String LOAD_LABEL = "Load";
    private static final String NO_MATCHES = "No matches";
    private static final String DROP_HINT = "Drop .js files into scripts folder";
    private static final String UNLOAD_LABEL = "Unload";
    private static final String NO_SCRIPTS = "No scripts found";
    private static final String SEARCH_HINT = "Search...";
    private static final String TITLE = "Scripts";
    private static final String ELLIPSIS = "...";

    
    public ScriptsPanel(CategoryPanelScreen screen) {
        buttons = new ArrayList<>();
        search = "";
        this.screen = screen;
    }

    
    public void render(UiRenderer renderer, float viewportWidth, int mouseX, int mouseY, float opacity, TextureView backdrop, float topOffset) {
        initializePosition(viewportWidth, topOffset);
        renderBackground(renderer, backdrop, opacity);
        renderHeader(renderer, mouseX, mouseY, opacity);
        renderScripts(renderer, mouseX, mouseY, opacity);
        renderScrollbar(renderer, opacity);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!ColorTheme.contains(x, y, 344.0F, 232.0F, (float) mouseX, (float) mouseY)) {
            return false;
        }
        if (maxScroll <= 0.0F || amount == 0.0) return true;
        scroll = Math.clamp(scroll - (float) amount * scrollSpeed(), 0.0F, maxScroll);
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        if (ColorTheme.contains(searchX(), searchY(), SEARCH_WIDTH, SEARCH_HEIGHT, (float) mouseX, (float) mouseY)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        if (ColorTheme.contains(x, y, 344.0F, 24.0F, (float) mouseX, (float) mouseY)) {
            dragging = true;
            dragOffsetX = x - (float) mouseX;
            dragOffsetY = y - (float) mouseY;
            return true;
        }
        for (ScriptActionButton action : buttons) {
            if (ColorTheme.contains(action.x, action.y, action.width, action.height, (float) mouseX, (float) mouseY)) {
                return performAction(action);
            }
        }
        return ColorTheme.contains(x, y, 344.0F, 232.0F, (float) mouseX, (float) mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, int viewportWidth, int viewportHeight) {
        if (!InputKey.MOUSE_LEFT.matches(button) || !dragging) return false;
        x = PanelGeometry.clampPosition((float) mouseX + dragOffsetX, 344.0F, viewportWidth);
        y = PanelGeometry.clampPosition((float) mouseY + dragOffsetY, 232.0F, viewportHeight);
        return true;
    }

    public boolean mouseReleased(int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        boolean wasDragging = dragging;
        dragging = false;
        return wasDragging;
    }

    public boolean charTyped(CharInput input) {
        if (!searchFocused) return false;
        char character = (char) input.codepoint();
        if (Character.isISOControl(character)) return false;
        search = search + character;
        return true;
    }

    public boolean keyPressed(KeyInput input) {
        if (!searchFocused) return false;
        if (input.key() == InputKey.ESCAPE.code()) {
            searchFocused = false;
            return true;
        }
        if (input.key() == InputKey.BACKSPACE.code()) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            return true;
        }
        return false;
    }

    private void renderBackground(UiRenderer renderer, TextureView backdrop, float opacity) {
        if (backdrop != null) renderer.drawBackdrop(backdrop, x, y, 344.0F, 232.0F, 10.0F, opacity);
        renderer.drawRoundedRect(x, y, 344.0F, 232.0F, 10.0F, ColorTheme.multiplyAlpha(ColorTheme.PANEL_FOREGROUND.getRGB(), opacity));
    }

    private void renderHeader(UiRenderer renderer, int mouseX, int mouseY, float opacity) {
        if (FontAsset.INTER_SEMI_BOLD != null) {
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, TITLE, x + 10.0F, y + 7.0F, 10.0F, ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity));
        }
        float fieldX = searchX();
        float fieldY = searchY();
        boolean hovered = ColorTheme.contains(fieldX, fieldY, SEARCH_WIDTH, SEARCH_HEIGHT, mouseX, mouseY);
        int background = ColorTheme.multiplyAlpha(
                (searchFocused || hovered ? ColorTheme.ROW_HOVER : ColorTheme.ROW_BACKGROUND).getRGB(), opacity);
        renderer.drawRoundedRect(fieldX, fieldY, SEARCH_WIDTH, SEARCH_HEIGHT, 6.0F, background);
        if (FontAsset.INTER_SEMI_BOLD == null) return;
        String label = search.isEmpty() ? SEARCH_HINT : search;
        int foreground = ColorTheme.multiplyAlpha(
                search.isEmpty() ? ColorTheme.SECONDARY_TEXT : ColorTheme.SELECTED_OPTION_TEXT, opacity);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, truncate(label, 118.0F, 6.0F), fieldX + 6.0F, fieldY + 7.0F - 3.0F, 6.0F, foreground);
        if (searchFocused && caretVisible()) {
            float caretX = Math.min(fieldX + 6.0F + FontAsset.INTER_SEMI_BOLD.measureText(search, 6.0F), fieldX + SEARCH_WIDTH - 4.0F);
            renderer.drawRoundedRect(caretX, fieldY + 3.0F, 1.0F, 8.0F, 0.0F, ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity));
        }
    }

    private void renderScripts(UiRenderer renderer, int mouseX, int mouseY, float opacity) {
        buttons.clear();
        List<String> filenames = filteredFilenames();
        if (filenames.isEmpty()) {
            renderEmptyState(renderer, opacity);
            return;
        }
        float listX = x + 10.0F;
        float listY = y + 24.0F + 10.0F;
        float listWidth = 314.0F;
        float listHeight = 188.0F;
        maxScroll = Math.max(0.0F, PanelGeometry.scriptListHeight(filenames.size()) - listHeight);
        scroll = Math.clamp(scroll, 0.0F, maxScroll);
        renderer.pushScissor(listX, listY, listWidth, listHeight);
        for (int index = 0; index < filenames.size(); index++) {
            float rowY = listY + index * 50.0F - scroll;
            if (rowY + ROW_HEIGHT < listY || rowY > listY + listHeight) continue;
            String filename = filenames.get(index);
            ScriptFeature loaded = ClumpsClient.scriptManager.loadedScripts().get(filename);
            boolean hovered = ColorTheme.contains(listX, rowY, listWidth, ROW_HEIGHT, mouseX, mouseY);
            renderRow(renderer, listX, rowY, listWidth, opacity, hovered, filename, loaded, mouseX, mouseY);
        }
        renderer.popScissor();
    }

    private void renderRow(UiRenderer renderer, float rowX, float rowY, float width, float opacity, boolean hovered, String filename, ScriptFeature Module, int mouseX, int mouseY) {
        boolean loaded = Module != null;
        int background = ColorTheme.multiplyAlpha(
                (hovered ? ColorTheme.ROW_HOVER : ColorTheme.ROW_BACKGROUND).getRGB(), opacity);
        renderer.drawRoundedRect(rowX, rowY, width, ROW_HEIGHT, 8.0F, background);
        if (loaded) renderer.drawRoundedRect(rowX, rowY, width, ROW_HEIGHT, 8.0F, ColorTheme.multiplyAlpha(LOADED_OVERLAY.getRGB(), opacity));
        float iconSize = 22.0F;
        float iconX = rowX + 8.0F;
        float iconY = rowY + (ROW_HEIGHT - iconSize) / 2.0F;
        renderer.drawRoundedRect(iconX, iconY, iconSize, iconSize, 5.0F, ColorTheme.multiplyAlpha(ColorTheme.ROW_HOVER.getRGB(), opacity));
        if (FontAsset.LUCIDE != null) {
            float fontSize = 8.0F;
            String glyph = "\ue0c0";
            float glyphWidth = FontAsset.LUCIDE.measureText(glyph, fontSize);
            renderer.drawText(FontAsset.LUCIDE, glyph, iconX + (iconSize - glyphWidth) / 2.0F, iconY + iconSize / 2.0F - fontSize / 2.0F, fontSize, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
        }
        float buttonY = rowY + 13.0F;
        float reloadX = rowX + width - 8.0F - BUTTON_WIDTH;
        float unloadX = reloadX - 6.0F - BUTTON_WIDTH;
        float textX = iconX + iconSize + 6.0F;
        float textWidth = unloadX - textX - 6.0F;
        if (FontAsset.INTER_SEMI_BOLD != null) {
            CharSequence name = loaded ? Module.getName() : filenameStem(filename);
            CharSequence description = loaded ? Module.getDescription() : NOT_LOADED;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, truncate(name, textWidth, 8.0F), textX, rowY + 10.0F, 8.0F, ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity));
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, truncate(description, textWidth, 6.0F), textX, rowY + 25.0F, 6.0F, ColorTheme.multiplyAlpha(loaded ? ColorTheme.PRIMARY_TEXT : ColorTheme.SECONDARY_TEXT, opacity));
        }
        boolean unloadHovered = loaded && ColorTheme.contains(unloadX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY);
        boolean reloadHovered = ColorTheme.contains(reloadX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY);
        renderButton(renderer, unloadX, buttonY, opacity, UNLOAD_LABEL, unloadHovered, loaded);
        renderButton(renderer, reloadX, buttonY, opacity, loaded ? RELOAD_LABEL : LOAD_LABEL, reloadHovered, true);
        if (loaded) buttons.add(new ScriptActionButton(filename, ScriptAction.UNLOAD, unloadX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT));
        buttons.add(new ScriptActionButton(filename, ScriptAction.RELOAD, reloadX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT));
    }

    private void renderButton(UiRenderer renderer, float buttonX, float buttonY, float opacity, String label, boolean hovered, boolean enabled) {
        int background = !enabled
                ? ColorTheme.multiplyAlpha(ColorTheme.ROW_BACKGROUND.getRGB(), opacity * 0.6F)
                : ColorTheme.multiplyAlpha((hovered ? ColorTheme.ROW_HOVER : ColorTheme.REGISTRY_SURFACE).getRGB(), opacity);
        renderer.drawRoundedRect(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 4.0F, background);
        if (FontAsset.INTER_SEMI_BOLD == null) return;
        float textWidth = FontAsset.INTER_SEMI_BOLD.measureText(label, 7.0F);
        int foreground = ColorTheme.multiplyAlpha(enabled ? ColorTheme.SELECTED_OPTION_TEXT : ColorTheme.SECONDARY_TEXT, opacity);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, buttonX + (BUTTON_WIDTH - textWidth) / 2.0F, buttonY + 9.0F - 3.5F, 7.0F, foreground);
    }

    private void renderScrollbar(UiRenderer renderer, float opacity) {
        if (maxScroll <= 0.0F) return;
        float trackX = x + 344.0F - 10.0F - 4.0F;
        float trackY = y + 24.0F + 10.0F;
        float trackHeight = 188.0F;
        renderer.drawRoundedRect(trackX, trackY, 4.0F, trackHeight, 2.0F, ColorTheme.multiplyAlpha(ColorTheme.ROW_BACKGROUND.getRGB(), opacity));
        float fraction = trackHeight / (trackHeight + maxScroll);
        float thumbHeight = Math.max(18.0F, trackHeight * fraction);
        float thumbOffset = (trackHeight - thumbHeight) * (scroll / maxScroll);
        renderer.drawRoundedRect(trackX, trackY + thumbOffset, 4.0F, thumbHeight, 2.0F, ColorTheme.multiplyAlpha(ColorTheme.SLIDER_FILL.getRGB(), opacity));
    }

    private void renderEmptyState(UiRenderer renderer, float opacity) {
        maxScroll = 0.0F;
        scroll = 0.0F;
        if (FontAsset.INTER_SEMI_BOLD == null) return;
        float textX = x + 10.0F;
        float textY = y + 24.0F + 10.0F + 10.0F;
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, search.isBlank() ? NO_SCRIPTS : NO_MATCHES, textX, textY, 8.0F, ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity));
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, DROP_HINT, textX, textY + 12.0F, 6.0F, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
    }

    
    private boolean performAction(ScriptActionButton action) {
        if (ClumpsClient.scriptManager == null) return true;
        switch (action.action) {
            case UNLOAD -> ClumpsClient.scriptManager.unload(action.scriptName);
            case RELOAD -> ClumpsClient.scriptManager.reload(action.scriptName);
        }
        return true;
    }

    private void initializePosition(float viewportWidth, float topOffset) {
        if (positioned) return;
        x = (viewportWidth - 344.0F) / 2.0F;
        y = topOffset + 8.0F;
        positioned = true;
    }

    private float searchX() { return x + 344.0F - 10.0F - SEARCH_WIDTH; }

    private float searchY() { return y + 5.0F - 1.0F; }

    private float scrollSpeed() {
        var clickGui = screen.ClickGui;
        return clickGui == null ? ColorTheme.SCROLL_SPEED : clickGui.getScrollSpeed();
    }

    private boolean caretVisible() { return System.currentTimeMillis() / 500L % 2L == 0L; }

    private List<String> filteredFilenames() {
        if (ClumpsClient.scriptManager == null) return List.of();
        List<String> filenames = ClumpsClient.scriptManager.listFilenames();
        Collections.sort(filenames);
        if (search.isBlank()) return filenames;
        String query = search.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String filename : filenames) {
            if (matchesSearch(filename, query)) result.add(filename);
        }
        return result;
    }

    private boolean matchesSearch(String filename, String query) {
        if (filename.toLowerCase().contains(query)) return true;
        ScriptFeature Module = ClumpsClient.scriptManager.loadedScripts().get(filename);
        if (Module == null) return false;
        return Concat.containsLowercase(Module.getName(), query)
                || Concat.containsLowercase(Module.getDescription(), query);
    }

    private String filenameStem(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private CharSequence truncate(CharSequence text, float width, float size) {
        if (FontAsset.INTER_SEMI_BOLD == null) return text;
        if (FontAsset.INTER_SEMI_BOLD.measureText(text, size) <= width) return text;
        for (int end = text.length() - 1; end > 0; end--) {
            CharSequence shortened = Concat.sequences(text.subSequence(0, end), ELLIPSIS);
            if (FontAsset.INTER_SEMI_BOLD.measureText(shortened, size) <= width) return shortened;
        }
        return ELLIPSIS;
    }
}
