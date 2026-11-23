package com.blamejared.clumps.client.gui.dashboard;

import com.blamejared.clumps.client.handlers.config.AutoConfigManager;
import com.blamejared.clumps.client.handlers.config.CustomKitRule;
import com.blamejared.clumps.client.handlers.config.GamemodeKit;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.ui.ImmediateUiContext;
import com.blamejared.clumps.client.ui.UiRect;
import com.blamejared.clumps.client.utils.chat.ChatHudMessages;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.text.Text;

final class AutoConfigDashboardView {
    private static final String ID_MASTER = "ac/m";
    private static final String ID_NOTIFY = "ac/n";
    private static final String ID_DELAY = "ac/d";
    private static final String ID_FALLBACK = "ac/f";
    private static final String ID_SERVER = "ac/s";
    private static final String ID_UNLOCK = "ac/u";
    private static final String ID_KIT_TOGGLE = "ac/kt/";
    private static final String ID_KIT_CFG = "ac/kc/";
    private static final String ID_CUSTOM_TOGGLE = "ac/ct/";
    private static final String ID_CUSTOM_MATCH = "ac/cm/";
    private static final String ID_CUSTOM_CFG = "ac/cc/";
    private static final String ID_CUSTOM_CAPTURE = "ac/cp/";
    private static final String ID_CUSTOM_ITEMS = "ac/ci/";
    private static final String ID_CUSTOM_CLEAR = "ac/cl/";
    private static final String ID_DROPDOWN_ROW = "ac/dd/";
    private static final String DD_FALLBACK = "fb";
    private static final String DD_SERVER = "srv";
    private static final String DD_KIT_PREFIX = "k:";
    private static final String DD_CUSTOM_PREFIX = "c:";

    private static final float CONTENT_X = 354.0f;
    private static final float CONTENT_W = 1512.0f;
    private static final float VIEWPORT_X = 330.0f;
    private static final float VIEWPORT_Y = 157.0f;
    private static final float VIEWPORT_W = 1560.0f;
    private static final float VIEWPORT_H = 885.0f;

    private static final float KIT_CARD_W = 493.0f;
    private static final float KIT_CARD_STEP_X = 509.0f;
    private static final float KIT_CARD_H = 108.0f;
    private static final float KIT_ROW_GAP = 16.0f;

    private static final float CUSTOM_CARD_GAP = 16.0f;
    private static final float CUSTOM_CARD_W = (CONTENT_W - CUSTOM_CARD_GAP) * 0.5f;
    private static final float CUSTOM_CARD_H = 132.0f;
    private static final float CUSTOM_ROW_GAP = 16.0f;

    // Exact native Dashboard color palette (matches ConfigCard / DashboardController)
    private static final int CARD_FILL = 150994943;
    private static final int CARD_BORDER = 268435455;
    private static final int CARD_BORDER_ACTIVE = 536870911;
    private static final int CARD_BORDER_HOVER = 872415231;
    private static final int CONTROL_FILL = 234881023;
    private static final int CONTROL_FILL_ACTIVE = 268435455;
    private static final int CONTROL_BORDER = -14408668;
    private static final int CONTROL_BORDER_ACTIVE = 1728053247;
    private static final int TOGGLE_TRACK_ON = 536870911;
    private static final int RULE_COLOR = -14671840;
    private static final int DROPDOWN_FILL = -234091508;
    private static final int DROPDOWN_BORDER = -14079703;
    private static final int TEXT_PRIMARY = -1;
    private static final int TEXT_BRIGHT = -637534209;
    private static final int TEXT_SOFT = -1275068417;
    private static final int TEXT_SECONDARY = -2130706433;
    private static final int TEXT_MUTED = 1728053247;

    private float scroll;
    private String openDropdownKey;
    private float dropdownX;
    private float dropdownY;
    private float dropdownW;

    void resetOverlays() {
        openDropdownKey = null;
    }

    void handleInput(ImmediateUiContext ui, float mouseX, float mouseY, IntConsumer openCustomItemPicker) {
        AutoConfigManager manager = AutoConfigManager.get();
        if (manager == null) {
            return;
        }
        UiRect viewport = new UiRect(VIEWPORT_X, VIEWPORT_Y, VIEWPORT_W, VIEWPORT_H);

        if (openDropdownKey != null) {
            if (handleDropdownInput(ui, manager, mouseX, mouseY)) {
                return;
            }
            if (ui.input().wasPressed(InputKey.MOUSE_LEFT.code())) {
                openDropdownKey = null;
            }
        }

        if (viewport.contains(mouseX, mouseY) && Math.abs(ui.input().verticalScroll()) > 0.0) {
            float maxScroll = maxContentScroll();
            scroll = Math.max(0.0f, Math.min(maxScroll, scroll - (float) ui.input().verticalScroll() * 60.0f));
        }
        scroll = Math.max(0.0f, Math.min(maxContentScroll(), scroll));

        ui.pushClip(viewport);
        float y = 165.0f - scroll;

        // Top controls bar
        if (ui.interact(ID_MASTER, new UiRect(CONTENT_X + 16.0f, y + 12.0f, 152.0f, 34.0f)).clicked()) {
            manager.setEnabled(!manager.isEnabled());
            openDropdownKey = null;
        }
        if (ui.interact(ID_NOTIFY, new UiRect(CONTENT_X + 180.0f, y + 12.0f, 118.0f, 34.0f)).clicked()) {
            manager.setNotifyOnSwitch(!manager.isNotifyOnSwitch());
        }
        if (ui.interact(ID_DELAY, new UiRect(CONTENT_X + 310.0f, y + 12.0f, 126.0f, 34.0f)).clicked()) {
            manager.cycleStabilityDelay();
        }
        UiRect fallbackRect = new UiRect(CONTENT_X + 448.0f, y + 12.0f, 224.0f, 34.0f);
        if (ui.interact(ID_FALLBACK, fallbackRect).clicked()) {
            toggleDropdown(DD_FALLBACK, fallbackRect.x(), fallbackRect.y() + fallbackRect.height() + 4.0f, fallbackRect.width());
        }
        String currentServer = manager.getCurrentServerKey();
        if (!currentServer.isEmpty()) {
            UiRect serverRect = new UiRect(CONTENT_X + 684.0f, y + 12.0f, 240.0f, 34.0f);
            if (ui.interact(ID_SERVER, serverRect).clicked()) {
                toggleDropdown(DD_SERVER, serverRect.x(), serverRect.y() + serverRect.height() + 4.0f, serverRect.width());
            }
        }
        if (manager.isKitLocked()) {
            if (ui.interact(ID_UNLOCK, new UiRect(CONTENT_X + CONTENT_W - 96.0f, y + 12.0f, 80.0f, 34.0f)).clicked()) {
                manager.triggerUnlockRescan();
            }
        }

        y += 74.0f;
        y += 32.0f; // Gamemodes section header

        GamemodeKit[] kits = GamemodeKit.values();
        for (int i = 0; i < kits.length; i++) {
            int col = i % 3;
            int row = i / 3;
            float cx = CONTENT_X + col * KIT_CARD_STEP_X;
            float cy = y + row * (KIT_CARD_H + KIT_ROW_GAP);
            if (cy + KIT_CARD_H < VIEWPORT_Y || cy > VIEWPORT_Y + VIEWPORT_H) {
                continue;
            }
            GamemodeKit kit = kits[i];
            if (ui.interact(ID_KIT_TOGGLE + kit.name(), new UiRect(cx + KIT_CARD_W - 54.0f, cy + 18.0f, 38.0f, 20.0f)).clicked()) {
                manager.setKitEnabled(kit, !manager.isKitEnabled(kit));
            }
            UiRect cfgRect = new UiRect(cx + 16.0f, cy + KIT_CARD_H - 42.0f, KIT_CARD_W - 32.0f, 28.0f);
            if (ui.interact(ID_KIT_CFG + kit.name(), cfgRect).clicked()) {
                toggleDropdown(DD_KIT_PREFIX + kit.name(), cfgRect.x(), cfgRect.y() + cfgRect.height() + 4.0f, cfgRect.width());
            }
        }

        int kitRows = (kits.length + 2) / 3;
        y += kitRows * (KIT_CARD_H + KIT_ROW_GAP) + 18.0f;
        y += 32.0f; // Custom Kits section header

        List<CustomKitRule> customs = manager.getCustomRules();
        for (int i = 0; i < customs.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            float cx = CONTENT_X + col * (CUSTOM_CARD_W + CUSTOM_CARD_GAP);
            float cy = y + row * (CUSTOM_CARD_H + CUSTOM_ROW_GAP);
            if (cy + CUSTOM_CARD_H < VIEWPORT_Y || cy > VIEWPORT_Y + VIEWPORT_H) {
                continue;
            }
            CustomKitRule rule = customs.get(i);
            if (ui.interact(ID_CUSTOM_TOGGLE + i, new UiRect(cx + CUSTOM_CARD_W - 54.0f, cy + 18.0f, 38.0f, 20.0f)).clicked()) {
                manager.setCustomRuleEnabled(i, !rule.isEnabled());
            }
            if (ui.interact(ID_CUSTOM_MATCH + i, new UiRect(cx + CUSTOM_CARD_W - 148.0f, cy + 15.0f, 82.0f, 26.0f)).clicked()) {
                manager.toggleCustomRuleMatchMode(i);
            }
            float rowY = cy + CUSTOM_CARD_H - 42.0f;
            UiRect cfgRect = new UiRect(cx + 16.0f, rowY, 260.0f, 28.0f);
            if (ui.interact(ID_CUSTOM_CFG + i, cfgRect).clicked()) {
                toggleDropdown(DD_CUSTOM_PREFIX + i, cfgRect.x(), cfgRect.y() + cfgRect.height() + 4.0f, cfgRect.width());
            }
            if (ui.interact(ID_CUSTOM_ITEMS + i, new UiRect(cx + 286.0f, rowY, 138.0f, 28.0f)).clicked()) {
                openDropdownKey = null;
                if (openCustomItemPicker != null) {
                    openCustomItemPicker.accept(i);
                }
            }
            if (ui.interact(ID_CUSTOM_CAPTURE + i, new UiRect(cx + 434.0f, rowY, 146.0f, 28.0f)).clicked()) {
                int captured = manager.captureHotbarToCustomRule(i);
                ChatHudMessages.addMessage(Text.literal("Captured " + captured + " hotbar item(s) to " + rule.getName()));
            }
            if (ui.interact(ID_CUSTOM_CLEAR + i, new UiRect(cx + 590.0f, rowY, 74.0f, 28.0f)).clicked()) {
                manager.clearCustomRuleItems(i);
            }
        }
        ui.popClip();
    }

    void render(UiRenderer renderer, DashboardPainter painter, DashboardTransform transform, float mouseX, float mouseY) {
        AutoConfigManager manager = AutoConfigManager.get();
        if (manager == null) {
            return;
        }
        DashboardWidgetRenderer widgets = new DashboardWidgetRenderer(painter);
        scroll = Math.max(0.0f, Math.min(maxContentScroll(), scroll));

        renderer.pushScissor(transform.renderX(VIEWPORT_X), transform.renderY(VIEWPORT_Y), transform.renderSize(VIEWPORT_W), transform.renderSize(VIEWPORT_H));
        float y = 165.0f - scroll;

        // Top controls bar (matches native Dashboard glass card)
        painter.panel(CONTENT_X, y, CONTENT_W, 58.0f, 10.0f, CARD_FILL, 1.0f, manager.isEnabled() ? CARD_BORDER_ACTIVE : CARD_BORDER);

        drawLabeledToggleButton(painter, CONTENT_X + 16.0f, y + 12.0f, 152.0f, 34.0f, "Auto Switch", manager.isEnabled(), mouseX, mouseY);
        drawLabeledToggleButton(painter, CONTENT_X + 180.0f, y + 12.0f, 118.0f, 34.0f, "Notify", manager.isNotifyOnSwitch(), mouseX, mouseY);
        drawNativeButton(painter, widgets, CONTENT_X + 310.0f, y + 12.0f, 126.0f, 34.0f,
                "Delay: " + manager.getStabilityDelayMs() + "ms", false, mouseX, mouseY);

        String fb = manager.getFallbackConfig().isBlank() ? "Fallback: None" : "Fallback: " + manager.getFallbackConfig();
        drawDropdownButton(painter, widgets, CONTENT_X + 448.0f, y + 12.0f, 224.0f, 34.0f,
                fb, !manager.getFallbackConfig().isBlank(), DD_FALLBACK.equals(openDropdownKey), mouseX, mouseY);

        String currentServer = manager.getCurrentServerKey();
        if (!currentServer.isEmpty()) {
            String srvCfg = manager.getServerOverride(currentServer);
            String srvLabel = srvCfg.isBlank() ? "Server: Auto" : "Server: " + srvCfg;
            drawDropdownButton(painter, widgets, CONTENT_X + 684.0f, y + 12.0f, 240.0f, 34.0f,
                    srvLabel, !srvCfg.isBlank(), DD_SERVER.equals(openDropdownKey), mouseX, mouseY);
        }

        // Status pill on the right (exact ConfigCard "Active" badge style)
        String status = manager.getStatusLabel();
        float statusW = painter.textWidth(FontAsset.INTER, status, 12.0f) + 28.0f;
        float statusRight = manager.isKitLocked() ? CONTENT_X + CONTENT_W - 108.0f : CONTENT_X + CONTENT_W - 16.0f;
        painter.roundedRect(statusRight - statusW, y + 18.0f, statusW, 22.0f, 4.0f, CONTROL_FILL_ACTIVE);
        painter.roundedRect(statusRight - statusW + 8.0f, y + 26.0f, 6.0f, 6.0f, 3.0f, manager.isEnabled() ? TEXT_BRIGHT : TEXT_MUTED);
        painter.text(FontAsset.INTER, status, statusRight - statusW + 20.0f, y + 22.0f, 12.0f, manager.isEnabled() ? TEXT_BRIGHT : TEXT_MUTED);
        if (manager.isKitLocked()) {
            drawNativeButton(painter, widgets, CONTENT_X + CONTENT_W - 96.0f, y + 12.0f, 80.0f, 34.0f, "Unlock", false, mouseX, mouseY);
        }

        y += 74.0f;
        painter.text(FontAsset.INTER_SEMI_BOLD, "Gamemodes", CONTENT_X + 2.0f, y + 2.0f, 13.0f, TEXT_BRIGHT);
        painter.text(FontAsset.INTER, "Locks onto the active kit until a different kit appears, you respawn, or you change worlds",
                CONTENT_X + 94.0f, y + 2.5f, 12.0f, TEXT_MUTED);
        painter.roundedRect(CONTENT_X, y + 24.0f, CONTENT_W, 1.0f, 0.0f, RULE_COLOR);
        y += 32.0f;

        GamemodeKit[] kits = GamemodeKit.values();
        for (int i = 0; i < kits.length; i++) {
            int col = i % 3;
            int row = i / 3;
            float cx = CONTENT_X + col * KIT_CARD_STEP_X;
            float cy = y + row * (KIT_CARD_H + KIT_ROW_GAP);
            if (cy + KIT_CARD_H < VIEWPORT_Y || cy > VIEWPORT_Y + VIEWPORT_H) {
                continue;
            }
            GamemodeKit kit = kits[i];
            boolean kitEnabled = manager.isKitEnabled(kit);
            String cfg = manager.getKitConfig(kit);
            boolean mapped = kitEnabled && !cfg.isBlank();
            boolean hovered = contains(cx, cy, KIT_CARD_W, KIT_CARD_H, mouseX, mouseY);
            int border = hovered ? CARD_BORDER_HOVER : mapped ? CARD_BORDER_ACTIVE : CARD_BORDER;
            painter.panel(cx, cy, KIT_CARD_W, KIT_CARD_H, 10.0f, CARD_FILL, 1.0f, border);

            // Native ConfigCard icon box + title
            painter.roundedRect(cx + 16.0f, cy + 16.0f, 24.0f, 24.0f, 6.0f, CONTROL_FILL_ACTIVE);
            painter.icon(DashboardIcon.COMBAT, cx + 21.0f, cy + 21.0f, 14.0f, 14.0f, 1.0f, mapped ? TEXT_PRIMARY : TEXT_SECONDARY);
            painter.text(FontAsset.INTER_SEMI_BOLD, kit.displayName(), cx + 48.0f, cy + 19.5f, 15.0f, TEXT_PRIMARY);

            drawToggleSwitch(painter, cx + KIT_CARD_W - 54.0f, cy + 18.0f, kitEnabled);

            painter.text(FontAsset.INTER, ellipsize(painter, kit.hint(), KIT_CARD_W - 32.0f, 12.0f),
                    cx + 16.0f, cy + 47.0f, 12.0f, TEXT_MUTED);

            String cfgLabel = cfg.isBlank() ? "Select config..." : cfg;
            drawDropdownButton(painter, widgets, cx + 16.0f, cy + KIT_CARD_H - 42.0f, KIT_CARD_W - 32.0f, 28.0f,
                    cfgLabel, !cfg.isBlank(), (DD_KIT_PREFIX + kit.name()).equals(openDropdownKey), mouseX, mouseY);
        }

        int kitRows = (kits.length + 2) / 3;
        y += kitRows * (KIT_CARD_H + KIT_ROW_GAP) + 18.0f;
        painter.text(FontAsset.INTER_SEMI_BOLD, "Custom Kits", CONTENT_X + 2.0f, y + 2.0f, 13.0f, TEXT_BRIGHT);
        painter.text(FontAsset.INTER, "Checked before built-in gamemodes — choose any items or capture your current hotbar",
                CONTENT_X + 94.0f, y + 2.5f, 12.0f, TEXT_MUTED);
        painter.roundedRect(CONTENT_X, y + 24.0f, CONTENT_W, 1.0f, 0.0f, RULE_COLOR);
        y += 32.0f;

        List<CustomKitRule> customs = manager.getCustomRules();
        for (int i = 0; i < customs.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            float cx = CONTENT_X + col * (CUSTOM_CARD_W + CUSTOM_CARD_GAP);
            float cy = y + row * (CUSTOM_CARD_H + CUSTOM_ROW_GAP);
            if (cy + CUSTOM_CARD_H < VIEWPORT_Y || cy > VIEWPORT_Y + VIEWPORT_H) {
                continue;
            }
            CustomKitRule rule = customs.get(i);
            boolean active = rule.isEnabled() && !rule.getConfigName().isBlank() && !rule.getItemIds().isEmpty();
            boolean hovered = contains(cx, cy, CUSTOM_CARD_W, CUSTOM_CARD_H, mouseX, mouseY);
            int border = hovered ? CARD_BORDER_HOVER : active ? CARD_BORDER_ACTIVE : CARD_BORDER;
            painter.panel(cx, cy, CUSTOM_CARD_W, CUSTOM_CARD_H, 10.0f, CARD_FILL, 1.0f, border);

            painter.roundedRect(cx + 16.0f, cy + 16.0f, 24.0f, 24.0f, 6.0f, CONTROL_FILL_ACTIVE);
            painter.icon(DashboardIcon.CONFIGS, cx + 21.0f, cy + 21.0f, 14.0f, 14.0f, 1.0f, active ? TEXT_PRIMARY : TEXT_SECONDARY);
            painter.text(FontAsset.INTER_SEMI_BOLD, rule.getName(), cx + 48.0f, cy + 19.5f, 15.0f, TEXT_PRIMARY);

            drawNativeButton(painter, widgets, cx + CUSTOM_CARD_W - 148.0f, cy + 15.0f, 82.0f, 26.0f,
                    rule.isMatchAll() ? "Match: All" : "Match: Any", false, mouseX, mouseY);
            drawToggleSwitch(painter, cx + CUSTOM_CARD_W - 54.0f, cy + 18.0f, rule.isEnabled());

            painter.text(FontAsset.INTER, ellipsize(painter, rule.itemsSummary(5), CUSTOM_CARD_W - 32.0f, 12.0f),
                    cx + 16.0f, cy + 52.0f, 12.0f, rule.getItemIds().isEmpty() ? TEXT_MUTED : TEXT_SOFT);

            float rowY = cy + CUSTOM_CARD_H - 42.0f;
            String cfgLabel = rule.getConfigName().isBlank() ? "Select config..." : rule.getConfigName();
            drawDropdownButton(painter, widgets, cx + 16.0f, rowY, 260.0f, 28.0f,
                    cfgLabel, !rule.getConfigName().isBlank(), (DD_CUSTOM_PREFIX + i).equals(openDropdownKey), mouseX, mouseY);

            int itemCount = rule.getItemIds().size();
            String itemsBtn = itemCount == 0 ? "Items" : "Items (" + itemCount + ")";
            drawNativeButton(painter, widgets, cx + 286.0f, rowY, 138.0f, 28.0f, itemsBtn, itemCount > 0, mouseX, mouseY);
            drawNativeButton(painter, widgets, cx + 434.0f, rowY, 146.0f, 28.0f, "Capture Hotbar", false, mouseX, mouseY);
            drawNativeButton(painter, widgets, cx + 590.0f, rowY, 74.0f, 28.0f, "Clear", false, mouseX, mouseY);
        }

        renderer.popScissor();

        if (openDropdownKey != null) {
            renderDropdown(painter, manager, mouseX, mouseY);
        }
    }

    private void toggleDropdown(String key, float x, float y, float width) {
        if (key.equals(openDropdownKey)) {
            openDropdownKey = null;
            return;
        }
        openDropdownKey = key;
        dropdownX = x;
        dropdownY = y;
        dropdownW = width;
    }

    private boolean handleDropdownInput(ImmediateUiContext ui, AutoConfigManager manager, float mouseX, float mouseY) {
        List<String> options = dropdownOptions(manager);
        float itemH = 30.0f;
        float totalH = Math.min(260.0f, options.size() * itemH + 8.0f);
        float drawY = dropdownY + totalH > VIEWPORT_Y + VIEWPORT_H ? dropdownY - totalH - 36.0f : dropdownY;
        UiRect box = new UiRect(dropdownX, drawY, dropdownW, totalH);
        if (!box.contains(mouseX, mouseY)) {
            return false;
        }
        int maxVisible = (int) ((totalH - 8.0f) / itemH);
        for (int i = 0; i < Math.min(options.size(), maxVisible); i++) {
            UiRect row = new UiRect(dropdownX + 4.0f, drawY + 4.0f + i * itemH, dropdownW - 8.0f, itemH - 2.0f);
            if (ui.interact(ID_DROPDOWN_ROW + i, row).clicked()) {
                String chosen = i == 0 ? "" : options.get(i);
                applyDropdownSelection(manager, chosen);
                openDropdownKey = null;
                return true;
            }
        }
        return true;
    }

    private void renderDropdown(DashboardPainter painter, AutoConfigManager manager, float mouseX, float mouseY) {
        List<String> options = dropdownOptions(manager);
        float itemH = 30.0f;
        float totalH = Math.min(260.0f, options.size() * itemH + 8.0f);
        float drawY = dropdownY + totalH > VIEWPORT_Y + VIEWPORT_H ? dropdownY - totalH - 36.0f : dropdownY;
        painter.backdropPanel(dropdownX, drawY, dropdownW, totalH, 8.0f, DROPDOWN_FILL, 1.0f, DROPDOWN_BORDER);
        String current = currentDropdownValue(manager);
        int maxVisible = (int) ((totalH - 8.0f) / itemH);
        for (int i = 0; i < Math.min(options.size(), maxVisible); i++) {
            String label = options.get(i);
            String value = i == 0 ? "" : label;
            float ry = drawY + 4.0f + i * itemH;
            boolean hovered = contains(dropdownX + 4.0f, ry, dropdownW - 8.0f, itemH - 2.0f, mouseX, mouseY);
            boolean selected = value.equals(current);
            if (hovered || selected) {
                painter.roundedRect(dropdownX + 4.0f, ry, dropdownW - 8.0f, itemH - 2.0f, 5.0f,
                        selected ? CONTROL_FILL_ACTIVE : CONTROL_FILL);
            }
            painter.text(FontAsset.INTER, ellipsize(painter, label, dropdownW - 22.0f, 12.0f),
                    dropdownX + 12.0f, ry + 8.0f, 12.0f, i == 0 ? TEXT_MUTED : selected ? TEXT_PRIMARY : TEXT_SOFT);
        }
    }

    private List<String> dropdownOptions(AutoConfigManager manager) {
        List<String> options = new ArrayList<>();
        options.add("None");
        options.addAll(manager.availableConfigNames());
        return options;
    }

    private String currentDropdownValue(AutoConfigManager manager) {
        if (DD_FALLBACK.equals(openDropdownKey)) {
            return manager.getFallbackConfig();
        }
        if (DD_SERVER.equals(openDropdownKey)) {
            return manager.getServerOverride(manager.getCurrentServerKey());
        }
        if (openDropdownKey != null && openDropdownKey.startsWith(DD_KIT_PREFIX)) {
            GamemodeKit kit = GamemodeKit.valueOf(openDropdownKey.substring(DD_KIT_PREFIX.length()));
            return manager.getKitConfig(kit);
        }
        if (openDropdownKey != null && openDropdownKey.startsWith(DD_CUSTOM_PREFIX)) {
            int idx = Integer.parseInt(openDropdownKey.substring(DD_CUSTOM_PREFIX.length()));
            CustomKitRule rule = manager.getCustomRule(idx);
            return rule == null ? "" : rule.getConfigName();
        }
        return "";
    }

    private void applyDropdownSelection(AutoConfigManager manager, String configName) {
        if (DD_FALLBACK.equals(openDropdownKey)) {
            manager.setFallbackConfig(configName);
        } else if (DD_SERVER.equals(openDropdownKey)) {
            manager.setServerOverride(manager.getCurrentServerKey(), configName);
        } else if (openDropdownKey != null && openDropdownKey.startsWith(DD_KIT_PREFIX)) {
            GamemodeKit kit = GamemodeKit.valueOf(openDropdownKey.substring(DD_KIT_PREFIX.length()));
            manager.setKitConfig(kit, configName);
        } else if (openDropdownKey != null && openDropdownKey.startsWith(DD_CUSTOM_PREFIX)) {
            int idx = Integer.parseInt(openDropdownKey.substring(DD_CUSTOM_PREFIX.length()));
            manager.setCustomRuleConfig(idx, configName);
        }
    }

    private float maxContentScroll() {
        int kitRows = (GamemodeKit.values().length + 2) / 3;
        int customRows = 2;
        float total = 74.0f + 32.0f + kitRows * (KIT_CARD_H + KIT_ROW_GAP)
                + 18.0f + 32.0f + customRows * (CUSTOM_CARD_H + CUSTOM_ROW_GAP) + 20.0f;
        return Math.max(0.0f, total - (VIEWPORT_H - 16.0f));
    }

    private static void drawToggleSwitch(DashboardPainter painter, float x, float y, boolean on) {
        painter.panel(x, y, 38.0f, 20.0f, 10.0f, on ? TOGGLE_TRACK_ON : CONTROL_FILL, 1.0f,
                on ? CONTROL_BORDER_ACTIVE : CONTROL_BORDER);
        float knobX = on ? x + 21.0f : x + 3.0f;
        painter.roundedRect(knobX, y + 3.0f, 14.0f, 14.0f, 7.0f, on ? TEXT_PRIMARY : TEXT_SECONDARY);
    }

    private static void drawLabeledToggleButton(DashboardPainter painter, float x, float y, float w, float h,
                                                String label, boolean on, float mouseX, float mouseY) {
        boolean hovered = contains(x, y, w, h, mouseX, mouseY);
        painter.panel(x, y, w, h, 6.0f, on ? CONTROL_FILL_ACTIVE : CONTROL_FILL, 1.0f,
                hovered ? CARD_BORDER_HOVER : on ? CONTROL_BORDER_ACTIVE : CONTROL_BORDER);
        painter.text(FontAsset.INTER, label, x + 12.0f, y + 10.0f, 12.5f, on ? TEXT_PRIMARY : TEXT_SOFT);
        drawToggleSwitch(painter, x + w - 48.0f, y + (h - 20.0f) * 0.5f, on);
    }

    private static void drawDropdownButton(DashboardPainter painter, DashboardWidgetRenderer widgets,
                                           float x, float y, float w, float h, String label,
                                           boolean hasValue, boolean open, float mouseX, float mouseY) {
        boolean hovered = contains(x, y, w, h, mouseX, mouseY);
        painter.panel(x, y, w, h, 6.0f, CONTROL_FILL, 1.0f,
                open ? CONTROL_BORDER_ACTIVE : hovered ? CARD_BORDER_HOVER : CONTROL_BORDER);
        painter.text(FontAsset.INTER, ellipsize(painter, label, w - 32.0f, 12.0f),
                x + 11.0f, y + (h - 12.0f) * 0.5f, 12.0f, hasValue ? TEXT_PRIMARY : TEXT_MUTED);
        widgets.smallChevron(open ? DashboardIcon.DROPDOWN_CHEVRON_UP : DashboardIcon.DROPDOWN_CHEVRON,
                x + w - 14.0f, y + h * 0.5f, true);
    }

    private static void drawNativeButton(DashboardPainter painter, DashboardWidgetRenderer widgets,
                                         float x, float y, float w, float h, String label,
                                         boolean highlighted, float mouseX, float mouseY) {
        boolean hovered = contains(x, y, w, h, mouseX, mouseY);
        painter.panel(x, y, w, h, 6.0f, highlighted ? CONTROL_FILL_ACTIVE : CONTROL_FILL, 1.0f,
                hovered ? CARD_BORDER_HOVER : highlighted ? CONTROL_BORDER_ACTIVE : CONTROL_BORDER);
        widgets.centeredText(FontAsset.INTER, label, x + w * 0.5f, y + (h - 12.0f) * 0.5f, 12.0f,
                highlighted || hovered ? TEXT_PRIMARY : TEXT_SOFT);
    }

    private static boolean contains(float x, float y, float w, float h, float mx, float my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String ellipsize(DashboardPainter painter, String text, float maxWidth, float size) {
        if (text == null || text.isEmpty() || painter.textWidth(FontAsset.INTER, text, size) <= maxWidth) {
            return text == null ? "" : text;
        }
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i) + "...";
            if (painter.textWidth(FontAsset.INTER, candidate, size) <= maxWidth) {
                return candidate;
            }
        }
        return "...";
    }
}
