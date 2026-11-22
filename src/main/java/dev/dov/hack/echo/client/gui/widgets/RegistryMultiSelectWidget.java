package dev.dov.hack.echo.client.gui.widgets;

import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.keyboard.KeyModifier;
import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.gui.ScreenManager;
import dev.dov.hack.echo.client.settings.RegistryListSetting;
import dev.dov.hack.echo.client.settings.list.RegistryEntryGroup;
import dev.dov.hack.echo.client.settings.list.SelectionState;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.utils.strings.Concat;
import dev.dov.hack.echo.client.ui.ColorTheme;
import dev.dov.hack.echo.client.ui.HintRenderer;
import dev.dov.hack.echo.client.gui.animations.Easing;
import dev.dov.hack.echo.client.gui.animations.Tween;
import dev.dov.hack.echo.client.utils.strings.TextBuffers;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RegistryMultiSelectWidget<T> implements SettingWidget {
    private final RegistryListSetting<T> setting;
    private final List<RegistryEntryGroup<T>> groups;
    private final Function<T, String> entryLabel;
    private float x;
    private float y;
    private boolean expanded = false;
    private final Tween expansion;
    private String search = "";
    private int caret = 0;
    private final Tween caretPulse;
    private boolean showGroups = true;
    private static final int VISIBLE_ROWS = 8;
    private int scrollOffset = 0;
    private static final String SELECTION_SUFFIX = new String(new char[]{' ', 's', 'e', 'l', '.'}).intern();

    public RegistryMultiSelectWidget(RegistryListSetting<T> setting) {
        expansion = new Tween(Easing.EASE_OUT_CUBIC, 200);
        caretPulse = new Tween(Easing.EASE_IN_OUT_QUART, 400);
        this.setting = setting;
        groups = setting.getGroups();
        entryLabel = setting.getEntryLabel();
        showGroups = !groups.isEmpty();
        expansion.start(0, 0);
    }

    private float popupHeight() {
        if (!expanded) return 0;
        float rows = VISIBLE_ROWS * (ColorTheme.ROW_HEIGHT - 2);
        if (groups.isEmpty()) return ColorTheme.ROW_HEIGHT + ColorTheme.SEARCH_GAP + rows + ColorTheme.CONTENT_PADDING;
        float tabHeight = ColorTheme.ROW_HEIGHT * 0.7f;
        return ColorTheme.ROW_HEIGHT + ColorTheme.SEARCH_GAP + tabHeight + ColorTheme.SEARCH_GAP + rows + ColorTheme.CONTENT_PADDING;
    }

    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x; this.y = y;
        expansion.setTarget(expanded ? 1.0f : 0.0f);
        float progress = (float) expansion.value();
        float rowX = x + ColorTheme.ROW_INDENT;
        float width = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4;
        float height = height();
        boolean hovered = ColorTheme.contains(rowX, y, width, ColorTheme.ROW_HEIGHT, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) HintRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
        }
        renderer.drawRoundedRect(rowX, y, width, height, ColorTheme.ROW_RADIUS, ColorTheme.multiplyAlpha((hovered ? ColorTheme.REGISTRY_SURFACE : ColorTheme.REGISTRY_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2, 1.5f, height - 4, 0, ColorTheme.multiplyAlpha(ColorTheme.ROW_ACCENT.getRGB(), opacity));
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, TextBuffers.abbreviate(setting.getLabel(), 14), rowX + ColorTheme.CONTENT_PADDING, y + ColorTheme.ROW_HEIGHT / 2 - 4, 6, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
        int count = setting.getSelectedCount();
        CharSequence countText = count == 0 ? CatalogText.literal("None") : Concat.strings(String.valueOf(count), SELECTION_SUFFIX);
        float countWidth = FontAsset.INTER_SEMI_BOLD.measureText(countText, 6);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, countText, rowX + width - ColorTheme.CONTENT_PADDING - countWidth, y + ColorTheme.ROW_HEIGHT / 2 - 4, 6, ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity));
        if (!(progress > ColorTheme.VISIBLE_ANIMATION_THRESHOLD)) return;
        int alpha = (int) (progress * 255);
        float rowY = y + ColorTheme.ROW_HEIGHT;
        renderer.drawRoundedRect(rowX + ColorTheme.CONTENT_PADDING, rowY, width - ColorTheme.CONTENT_PADDING * 2, ColorTheme.ROW_HEIGHT, ColorTheme.SLOT_RADIUS, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(ColorTheme.SLOT_BACKGROUND, alpha), opacity));
        if (FontAsset.INTER_SEMI_BOLD != null) {
            CharSequence text = search.isEmpty() ? CatalogText.literal("Search...") : search;
            Color color = search.isEmpty() ? ColorTheme.SECONDARY_TEXT : ColorTheme.SELECTED_OPTION_TEXT;
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, text, rowX + ColorTheme.CONTENT_PADDING + 4, rowY + ColorTheme.ROW_HEIGHT / 2 - 3, 6, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(color, alpha), opacity));
            if (ScreenManager.focusedSetting == setting) {
                if (caretPulse.isFinished()) caretPulse.start(caretPulse.targetValue(), 1 - caretPulse.targetValue());
                int position = Math.min(caret, search.length());
                float caretX = rowX + ColorTheme.CONTENT_PADDING + 4 + FontAsset.INTER_SEMI_BOLD.measureText(search.substring(0, position), 6);
                int caretAlpha = (int) (caretPulse.value() * alpha);
                renderer.drawRoundedRect(caretX, rowY + 2, 1, ColorTheme.ROW_HEIGHT - 4, 0, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(ColorTheme.SLIDER_FILL, caretAlpha), opacity));
            }
        }
        rowY += ColorTheme.ROW_HEIGHT + ColorTheme.SEARCH_GAP;
        if (!groups.isEmpty()) {
            float tabHeight = ColorTheme.ROW_HEIGHT * 0.7f;
            float tabWidth = (width - ColorTheme.CONTENT_PADDING * 2 - ColorTheme.SEARCH_GAP) / 2;
            int groupColor = ColorTheme.multiplyAlpha(ColorTheme.withAlpha(showGroups ? ColorTheme.ROW_ACCENT : ColorTheme.REGISTRY_SURFACE, alpha), opacity);
            int entryColor = ColorTheme.multiplyAlpha(ColorTheme.withAlpha(!showGroups ? ColorTheme.ROW_ACCENT : ColorTheme.REGISTRY_SURFACE, alpha), opacity);
            renderer.drawRoundedRect(rowX + ColorTheme.CONTENT_PADDING, rowY, tabWidth, tabHeight, ColorTheme.SLOT_RADIUS, groupColor);
            renderer.drawRoundedRect(rowX + ColorTheme.CONTENT_PADDING + tabWidth + ColorTheme.SEARCH_GAP, rowY, tabWidth, tabHeight, ColorTheme.SLOT_RADIUS, entryColor);
            if (FontAsset.INTER_SEMI_BOLD != null) {
                CharSequence groupsTab = CatalogText.literal("Groups");
                CharSequence entriesTab = CatalogText.literal("Entries");
                renderer.drawText(FontAsset.INTER_SEMI_BOLD, groupsTab, rowX + ColorTheme.CONTENT_PADDING + tabWidth / 2 - FontAsset.INTER_SEMI_BOLD.measureText(groupsTab, 5) / 2, rowY + tabHeight / 2 - 2, 5, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(showGroups ? ColorTheme.TAB_ACTIVE_TEXT : ColorTheme.SELECTED_OPTION_TEXT, alpha), opacity));
                renderer.drawText(FontAsset.INTER_SEMI_BOLD, entriesTab,
                        rowX + ColorTheme.CONTENT_PADDING + tabWidth + ColorTheme.SEARCH_GAP + tabWidth / 2
                                - FontAsset.INTER_SEMI_BOLD.measureText(entriesTab, 5) / 2, rowY + tabHeight / 2 - 2, 5, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(!showGroups ? ColorTheme.TAB_ACTIVE_TEXT : ColorTheme.SELECTED_OPTION_TEXT, alpha), opacity));
            }
            rowY += tabHeight + ColorTheme.SEARCH_GAP;
        }
        if (showGroups) {
            List<RegistryEntryGroup<T>> visible = filteredGroups(groups);
            int first = visible.size() > VISIBLE_ROWS ? scrollOffset : 0;
            int end = visible.size() > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, visible.size()) : visible.size();
            for (int index = first; index < end; index++) {
                RegistryEntryGroup<T> group = visible.get(index);
                SelectionState state = setting.getGroupState(group);
                if (FontAsset.INTER_SEMI_BOLD != null) {
                    CharSequence marker = state == SelectionState.ALL ? CatalogText.literal("[x]")
                            : state == SelectionState.PARTIAL ? CatalogText.literal("[~]") : CatalogText.literal("[ ]");
                    Color color = state == SelectionState.ALL ? ColorTheme.ROW_ACCENT : ColorTheme.SECONDARY_TEXT;
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, marker, rowX + ColorTheme.CONTENT_PADDING + 4, rowY + (ColorTheme.ROW_HEIGHT - 2) / 2 - 3, 6, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(color, alpha), opacity));
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, TextBuffers.abbreviate(group.label(), 18), rowX + ColorTheme.CONTENT_PADDING + 20, rowY + (ColorTheme.ROW_HEIGHT - 2) / 2 - 3, 6, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(ColorTheme.SELECTED_OPTION_TEXT, alpha), opacity));
                }
                rowY += ColorTheme.ROW_HEIGHT - 2;
            }
        } else {
            List<T> visible = filteredEntries();
            int first = visible.size() > VISIBLE_ROWS ? scrollOffset : 0;
            int end = visible.size() > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, visible.size()) : visible.size();
            for (int index = first; index < end; index++) {
                T entry = visible.get(index);
                boolean selected = setting.isEntrySelected(entry);
                if (FontAsset.INTER_SEMI_BOLD != null) {
                    CharSequence marker = selected ? CatalogText.literal("[x]") : CatalogText.literal("[ ]");
                    Color color = selected ? ColorTheme.ROW_ACCENT : ColorTheme.SECONDARY_TEXT;
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, marker, rowX + ColorTheme.CONTENT_PADDING + 4, rowY + (ColorTheme.ROW_HEIGHT - 2) / 2 - 3, 6, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(color, alpha), opacity));
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, TextBuffers.abbreviateString(entryLabel.apply(entry), 18), rowX + ColorTheme.CONTENT_PADDING + 20, rowY + (ColorTheme.ROW_HEIGHT - 2) / 2 - 3, 6, ColorTheme.multiplyAlpha(ColorTheme.withAlpha(ColorTheme.SELECTED_OPTION_TEXT, alpha), opacity));
                }
                rowY += ColorTheme.ROW_HEIGHT - 2;
            }
        }
    }

    private List<RegistryEntryGroup<T>> filteredGroups(List<RegistryEntryGroup<T>> source) {
        List<RegistryEntryGroup<T>> result = new ArrayList<>(source);
        result.sort(Comparator.<RegistryEntryGroup<T>, Integer>comparing(group -> {
            SelectionState state = setting.getGroupState(group);
            return state == SelectionState.ALL ? 0 : state == SelectionState.PARTIAL ? 1 : 2;
        }).thenComparing((first, second) -> Concat.compareLowercase(first.label(), second.label())));
        if (search.isEmpty()) return result;
        String query = search.toLowerCase();
        return result.stream().filter(group -> Concat.containsLowercase(group.label(), query)).collect(Collectors.toList());
    }

    private List<T> filteredEntries() {
        List<T> result = new ArrayList<>();
        Predicate<T> filter = setting.getEntryFilter();
        for (T entry : setting.getRegistry()) if (filter.test(entry)) result.add(entry);
        result.sort(Comparator.<T, Boolean>comparing(entry -> !setting.isEntrySelected(entry))
                .thenComparing(entry -> entryLabel.apply(entry).toLowerCase()));
        if (search.isEmpty()) return result;
        String query = search.toLowerCase();
        return result.stream().filter(entry -> entryLabel.apply(entry).toLowerCase().contains(query)).collect(Collectors.toList());
    }

    @Override public float height() {
        float progress = (float) expansion.value();
        float height = ColorTheme.ROW_HEIGHT;
        if (progress > ColorTheme.VISIBLE_ANIMATION_THRESHOLD) height += popupHeight() * progress;
        return height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!InputKey.MOUSE_LEFT.matches(button)) return false;
        float rowX = x + ColorTheme.ROW_INDENT;
        float width = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4;
        if (ColorTheme.contains(rowX, y, width, ColorTheme.ROW_HEIGHT, (float) mouseX, (float) mouseY)) {
            expanded = !expanded;
            if (expanded) { scrollOffset = 0; ScreenManager.focusedSetting = null; }
            return true;
        }
        float progress = (float) expansion.value();
        if (!expanded || !(progress > ColorTheme.INPUT_ANIMATION_THRESHOLD)) return false;
        float searchY = y + ColorTheme.ROW_HEIGHT;
        float tabHeight = ColorTheme.ROW_HEIGHT * 0.7f;
        float listY = groups.isEmpty() ? searchY + ColorTheme.ROW_HEIGHT + ColorTheme.SEARCH_GAP
                : searchY + ColorTheme.ROW_HEIGHT + ColorTheme.SEARCH_GAP + tabHeight + ColorTheme.SEARCH_GAP;
        if (ColorTheme.contains(rowX + ColorTheme.CONTENT_PADDING, searchY, width - ColorTheme.CONTENT_PADDING * 2, ColorTheme.ROW_HEIGHT, (float) mouseX, (float) mouseY)) {
            ScreenManager.focusedSetting = setting; caretPulse.start(0, 1);
            float relativeX = (float) mouseX - rowX - ColorTheme.CONTENT_PADDING - 4;
            caret = 0;
            for (int index = 0; index <= search.length(); index++) {
                if (relativeX <= FontAsset.INTER_SEMI_BOLD.measureText(search.substring(0, index), 6)) break;
                caret = index;
            }
            caret = Math.min(caret, search.length());
            return true;
        }
        if (!groups.isEmpty()) {
            float tabY = searchY + ColorTheme.ROW_HEIGHT + ColorTheme.SEARCH_GAP;
            float tabWidth = (width - ColorTheme.CONTENT_PADDING * 2 - ColorTheme.SEARCH_GAP) / 2;
            if (ColorTheme.contains(rowX + ColorTheme.CONTENT_PADDING, tabY, tabWidth, tabHeight, (float) mouseX, (float) mouseY)) {
                showGroups = true; scrollOffset = 0; return true;
            }
            if (ColorTheme.contains(rowX + ColorTheme.CONTENT_PADDING + tabWidth + ColorTheme.SEARCH_GAP, tabY, tabWidth, tabHeight, (float) mouseX, (float) mouseY)) {
                showGroups = false; scrollOffset = 0; return true;
            }
        }
        if (mouseY >= listY) {
            if (showGroups) {
                List<RegistryEntryGroup<T>> visible = filteredGroups(groups);
                int first = visible.size() > VISIBLE_ROWS ? scrollOffset : 0;
                int end = visible.size() > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, visible.size()) : visible.size();
                float rowY = listY;
                for (int index = first; index < end; index++) {
                    if (ColorTheme.contains(rowX + ColorTheme.CONTENT_PADDING, rowY, width - ColorTheme.CONTENT_PADDING * 2, ColorTheme.ROW_HEIGHT - 2, (float) mouseX, (float) mouseY)) {
                        RegistryEntryGroup<T> group = visible.get(index);
                        SelectionState state = setting.getGroupState(group);
                        setting.setGroupSelected(group, state != SelectionState.ALL);
                        return true;
                    }
                    rowY += ColorTheme.ROW_HEIGHT - 2;
                }
            } else {
                List<T> visible = filteredEntries();
                int first = visible.size() > VISIBLE_ROWS ? scrollOffset : 0;
                int end = visible.size() > VISIBLE_ROWS ? Math.min(scrollOffset + VISIBLE_ROWS, visible.size()) : visible.size();
                float rowY = listY;
                for (int index = first; index < end; index++) {
                    if (ColorTheme.contains(rowX + ColorTheme.CONTENT_PADDING, rowY, width - ColorTheme.CONTENT_PADDING * 2, ColorTheme.ROW_HEIGHT - 2, (float) mouseX, (float) mouseY)) {
                        T entry = visible.get(index);
                        setting.setEntrySelected(entry, !setting.isEntrySelected(entry));
                        return true;
                    }
                    rowY += ColorTheme.ROW_HEIGHT - 2;
                }
            }
        }
        ScreenManager.focusedSetting = null;
        return false;
    }

    @Override public void mouseReleased(double mouseX, double mouseY, int button) {  }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ScreenManager.focusedSetting != setting) return false;
        if (keyCode == InputKey.BACKSPACE.code()) {
            if (KeyModifier.CONTROL.isSet(modifiers)) {
                int previous = previousWordStart(search, caret);
                search = search.substring(0, previous) + search.substring(caret); caret = previous; scrollOffset = 0;
            } else if (caret > 0) { search = search.substring(0, caret - 1) + search.substring(caret); caret--; scrollOffset = 0; }
        } else if (keyCode == InputKey.DELETE.code()) {
            if (caret < search.length()) { search = search.substring(0, caret) + search.substring(caret + 1); scrollOffset = 0; }
        } else if (keyCode == InputKey.LEFT.code()) caret = Math.max(0, caret - 1);
        else if (keyCode == InputKey.RIGHT.code()) caret = Math.min(search.length(), caret + 1);
        else if (keyCode == InputKey.HOME.code()) caret = 0;
        else if (keyCode == InputKey.END.code()) caret = search.length();
        else if (keyCode == InputKey.ESCAPE.code()) { ScreenManager.focusedSetting = null; return true; }
        else return false;
        caretPulse.start(0, 1); return true;
    }

    @Override public void charTyped(char character, int modifiers) {
        if (ScreenManager.focusedSetting != setting || character < 32 || character == 127) return;
        int position = Math.min(caret, search.length());
        search = search.substring(0, position) + character + search.substring(position);
        caret = position + 1; scrollOffset = 0; caretPulse.start(0, 1);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        float progress = (float) expansion.value();
        if (!expanded || !(progress > ColorTheme.INPUT_ANIMATION_THRESHOLD)) return false;
        float rowX = x + ColorTheme.ROW_INDENT;
        float width = ColorTheme.PANEL_WIDTH - ColorTheme.ROW_INDENT - 4;
        float top = y + ColorTheme.ROW_HEIGHT + ColorTheme.SEARCH_GAP + ColorTheme.ROW_HEIGHT
                + ColorTheme.SEARCH_GAP + ColorTheme.ROW_HEIGHT * 0.7f + ColorTheme.SEARCH_GAP;
        if (!ColorTheme.contains(rowX, top, width, VISIBLE_ROWS * (ColorTheme.ROW_HEIGHT - 2), (float) mouseX, (float) mouseY)) return false;
        int count = showGroups ? filteredGroups(groups).size() : filteredEntries().size();
        if (count <= VISIBLE_ROWS) return false;
        scrollOffset = amount > 0 ? Math.max(0, scrollOffset - 1) : Math.min(count - VISIBLE_ROWS, scrollOffset + 1);
        return true;
    }

    private static int previousWordStart(String text, int caret) {
        int index = caret;
        while (index > 0 && text.charAt(index - 1) == ' ') index--;
        while (index > 0 && text.charAt(index - 1) != ' ') index--;
        return index;
    }

    @Override public RegistryListSetting<T> setting() { return setting; }
}
