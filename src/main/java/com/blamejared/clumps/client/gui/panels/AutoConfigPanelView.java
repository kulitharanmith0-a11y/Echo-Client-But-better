package com.blamejared.clumps.client.gui.panels;

import com.blamejared.clumps.client.handlers.config.AutoConfigManager;
import com.blamejared.clumps.client.handlers.config.CustomKitRule;
import com.blamejared.clumps.client.handlers.config.GamemodeKit;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.ui.ColorTheme;
import com.blamejared.clumps.client.utils.chat.ChatHudMessages;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;

final class AutoConfigPanelView {
    private static final float LIST_WIDTH = 314.0F;
    private static final float LIST_HEIGHT = 188.0F;
    private static final float ROW_H = 18.0F;
    private static final float CUSTOM_ROW_H = 34.0F;
    private static final float ROW_GAP = 4.0F;
    private static final int ACTIVE_BG = 0x26ffffff;
    private static final int ACTIVE_TEXT = 0xffffffff;

    private record HitBox(float x, float y, float w, float h, Runnable onLeftClick, Runnable onRightClick) { }

    private final List<HitBox> hitBoxes = new ArrayList<>();
    private float scroll;
    private float maxScroll;

    float getScroll() {
        return scroll;
    }

    float getMaxScroll() {
        return maxScroll;
    }

    void scrollBy(float delta) {
        if (maxScroll <= 0.0F) {
            scroll = 0.0F;
            return;
        }
        scroll = Math.clamp(scroll + delta, 0.0F, maxScroll);
    }

    void render(UiRenderer renderer, float listX, float listY, int mouseX, int mouseY, float opacity) {
        hitBoxes.clear();
        AutoConfigManager manager = AutoConfigManager.get();
        if (manager == null) {
            maxScroll = 0.0F;
            scroll = 0.0F;
            return;
        }

        GamemodeKit[] kits = GamemodeKit.values();
        List<CustomKitRule> customs = manager.getCustomRules();
        float contentHeight = 42.0F
                + 14.0F + kits.length * (ROW_H + ROW_GAP)
                + 18.0F + customs.size() * (CUSTOM_ROW_H + ROW_GAP)
                + 6.0F;
        maxScroll = Math.max(0.0F, contentHeight - LIST_HEIGHT);
        scroll = Math.clamp(scroll, 0.0F, maxScroll);

        renderer.pushScissor(listX, listY, LIST_WIDTH, LIST_HEIGHT);
        float y = listY - scroll;

        // Top controls row
        int textSelected = ColorTheme.SELECTED_OPTION_TEXT.getRGB();
        int textSecondary = ColorTheme.SECONDARY_TEXT.getRGB();
        boolean masterOn = manager.isEnabled();
        drawButton(renderer, listX, y, 74.0F, 16.0F, masterOn ? "Auto: ON" : "Auto: OFF",
                masterOn ? ACTIVE_BG : ColorTheme.ROW_BACKGROUND.getRGB(),
                masterOn ? ACTIVE_TEXT : textSecondary, mouseX, mouseY, opacity,
                () -> manager.setEnabled(!manager.isEnabled()), null);

        drawButton(renderer, listX + 78.0F, y, 64.0F, 16.0F, "Delay " + manager.getStabilityDelayMs() + "ms",
                ColorTheme.ROW_BACKGROUND.getRGB(), textSelected, mouseX, mouseY, opacity,
                manager::cycleStabilityDelay, null);

        String fb = manager.getFallbackConfig().isBlank() ? "None" : manager.getFallbackConfig();
        drawButton(renderer, listX + 146.0F, y, 108.0F, 16.0F, "FB: " + ellipsize(fb, 84.0F, 5.5F),
                ColorTheme.ROW_BACKGROUND.getRGB(),
                manager.getFallbackConfig().isBlank() ? textSecondary : textSelected,
                mouseX, mouseY, opacity,
                manager::cycleFallbackConfig, () -> manager.setFallbackConfig(""));

        if (manager.isKitLocked()) {
            drawButton(renderer, listX + 258.0F, y, 54.0F, 16.0F, "Unlock",
                    ColorTheme.ROW_BACKGROUND.getRGB(), textSelected, mouseX, mouseY, opacity,
                    manager::unlockCurrentKit, null);
        } else {
            boolean notifyOn = manager.isNotifyOnSwitch();
            drawButton(renderer, listX + 258.0F, y, 54.0F, 16.0F, notifyOn ? "Msg: ON" : "Msg: OFF",
                    ColorTheme.ROW_BACKGROUND.getRGB(),
                    notifyOn ? textSelected : textSecondary, mouseX, mouseY, opacity,
                    () -> manager.setNotifyOnSwitch(!manager.isNotifyOnSwitch()), null);
        }

        y += 20.0F;
        if (FontAsset.INTER_SEMI_BOLD != null) {
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(manager.getStatusLabel(), LIST_WIDTH - 4.0F, 5.8F),
                    listX + 2.0F, y + 3.0F, 5.8F, ColorTheme.multiplyAlpha(ColorTheme.PRIMARY_TEXT, opacity));
        }
        y += 22.0F;

        // Gamemodes section
        if (FontAsset.INTER_SEMI_BOLD != null) {
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, "Gamemodes (LMB cycle, RMB clear)",
                    listX + 2.0F, y, 6.0F, ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity));
        }
        y += 14.0F;

        for (GamemodeKit kit : kits) {
            if (y + ROW_H >= listY && y <= listY + LIST_HEIGHT) {
                boolean enabled = manager.isKitEnabled(kit);
                String cfg = manager.getKitConfig(kit);
                renderer.drawRoundedRect(listX, y, LIST_WIDTH, ROW_H, 5.0F,
                        ColorTheme.multiplyAlpha(ColorTheme.ROW_BACKGROUND.getRGB(), opacity));
                if (FontAsset.INTER_SEMI_BOLD != null) {
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, kit.displayName(),
                            listX + 6.0F, y + 5.5F, 6.5F, ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity));
                }
                drawButton(renderer, listX + 160.0F, y + 2.0F, 34.0F, 14.0F, enabled ? "ON" : "OFF",
                        enabled ? ACTIVE_BG : ColorTheme.REGISTRY_SURFACE.getRGB(),
                        enabled ? ACTIVE_TEXT : textSecondary, mouseX, mouseY, opacity,
                        () -> manager.setKitEnabled(kit, !manager.isKitEnabled(kit)), null);

                String cfgLabel = cfg.isBlank() ? "None" : ellipsize(cfg, 102.0F, 5.8F);
                drawButton(renderer, listX + 198.0F, y + 2.0F, 112.0F, 14.0F, cfgLabel,
                        ColorTheme.REGISTRY_SURFACE.getRGB(),
                        cfg.isBlank() ? textSecondary : textSelected,
                        mouseX, mouseY, opacity,
                        () -> manager.cycleKitConfig(kit), () -> manager.setKitConfig(kit, ""));
            }
            y += ROW_H + ROW_GAP;
        }

        y += 4.0F;
        if (FontAsset.INTER_SEMI_BOLD != null) {
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, "Custom Kits (Capture grabs your current hotbar)",
                    listX + 2.0F, y, 6.0F, ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity));
        }
        y += 14.0F;

        for (int i = 0; i < customs.size(); i++) {
            final int index = i;
            CustomKitRule rule = customs.get(i);
            if (y + CUSTOM_ROW_H >= listY && y <= listY + LIST_HEIGHT) {
                renderer.drawRoundedRect(listX, y, LIST_WIDTH, CUSTOM_ROW_H, 5.0F,
                        ColorTheme.multiplyAlpha(ColorTheme.ROW_BACKGROUND.getRGB(), opacity));
                if (FontAsset.INTER_SEMI_BOLD != null) {
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, rule.getName(),
                            listX + 6.0F, y + 5.0F, 6.5F, ColorTheme.multiplyAlpha(ColorTheme.SELECTED_OPTION_TEXT, opacity));
                    renderer.drawText(FontAsset.INTER_SEMI_BOLD, ellipsize(rule.itemsSummary(3), 184.0F, 5.4F),
                            listX + 6.0F, y + 21.0F, 5.4F, ColorTheme.multiplyAlpha(ColorTheme.SECONDARY_TEXT, opacity));
                }
                drawButton(renderer, listX + 122.0F, y + 2.0F, 32.0F, 13.0F, rule.isEnabled() ? "ON" : "OFF",
                        rule.isEnabled() ? ACTIVE_BG : ColorTheme.REGISTRY_SURFACE.getRGB(),
                        rule.isEnabled() ? ACTIVE_TEXT : textSecondary, mouseX, mouseY, opacity,
                        () -> manager.setCustomRuleEnabled(index, !rule.isEnabled()), null);

                drawButton(renderer, listX + 158.0F, y + 2.0F, 36.0F, 13.0F, rule.isMatchAll() ? "All" : "Any",
                        ColorTheme.REGISTRY_SURFACE.getRGB(), textSelected, mouseX, mouseY, opacity,
                        () -> manager.toggleCustomRuleMatchMode(index), null);

                String cfgLabel = rule.getConfigName().isBlank() ? "None" : ellipsize(rule.getConfigName(), 102.0F, 5.6F);
                drawButton(renderer, listX + 198.0F, y + 2.0F, 112.0F, 13.0F, cfgLabel,
                        ColorTheme.REGISTRY_SURFACE.getRGB(),
                        rule.getConfigName().isBlank() ? textSecondary : textSelected,
                        mouseX, mouseY, opacity,
                        () -> manager.cycleCustomRuleConfig(index), () -> manager.setCustomRuleConfig(index, ""));

                drawButton(renderer, listX + 198.0F, y + 18.0F, 66.0F, 13.0F, "Capture",
                        ColorTheme.REGISTRY_SURFACE.getRGB(), textSelected, mouseX, mouseY, opacity,
                        () -> {
                            int count = manager.captureHotbarToCustomRule(index);
                            ChatHudMessages.addMessage(Text.literal("Captured " + count + " hotbar item(s) to " + rule.getName()));
                        }, null);

                drawButton(renderer, listX + 268.0F, y + 18.0F, 42.0F, 13.0F, "Clear",
                        ColorTheme.REGISTRY_SURFACE.getRGB(), textSecondary, mouseX, mouseY, opacity,
                        () -> manager.clearCustomRuleItems(index), null);
            }
            y += CUSTOM_ROW_H + ROW_GAP;
        }

        renderer.popScissor();
    }

    boolean mouseClicked(float mouseX, float mouseY, int button) {
        for (HitBox box : hitBoxes) {
            if (ColorTheme.contains(box.x, box.y, box.w, box.h, mouseX, mouseY)) {
                if (button == 1 && box.onRightClick != null) {
                    box.onRightClick.run();
                    return true;
                }
                if (button == 0 && box.onLeftClick != null) {
                    box.onLeftClick.run();
                    return true;
                }
            }
        }
        return false;
    }

    private void drawButton(UiRenderer renderer, float x, float y, float w, float h, String label,
                            int bgColor, int textColor, int mouseX, int mouseY, float opacity,
                            Runnable onLeft, Runnable onRight) {
        boolean hovered = ColorTheme.contains(x, y, w, h, mouseX, mouseY);
        int fill = hovered && bgColor != ACTIVE_BG ? ColorTheme.ROW_HOVER.getRGB() : bgColor;
        renderer.drawRoundedRect(x, y, w, h, 4.0F, ColorTheme.multiplyAlpha(fill, opacity));
        if (FontAsset.INTER_SEMI_BOLD != null) {
            float size = 5.6F;
            float tw = FontAsset.INTER_SEMI_BOLD.measureText(label, size);
            renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, x + (w - tw) * 0.5F, y + (h - size) * 0.5F, size,
                    ColorTheme.multiplyAlpha(textColor, opacity));
        }
        hitBoxes.add(new HitBox(x, y, w, h, onLeft, onRight));
    }

    private static String ellipsize(String text, float maxWidth, float size) {
        if (text == null || FontAsset.INTER_SEMI_BOLD == null) {
            return text == null ? "" : text;
        }
        if (FontAsset.INTER_SEMI_BOLD.measureText(text, size) <= maxWidth) {
            return text;
        }
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i) + "...";
            if (FontAsset.INTER_SEMI_BOLD.measureText(candidate, size) <= maxWidth) {
                return candidate;
            }
        }
        return "...";
    }
}
