package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.handlers.LatencyTracker;
import com.blamejared.clumps.client.hud.HudElementLayout;
import com.blamejared.clumps.client.hud.HudElementType;
import com.blamejared.clumps.client.hud.HudRenderer;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.hud.HudElement;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.ui.UiSize;
import java.util.ArrayDeque;
import java.util.Deque;

public class Keystrokes extends HudElement {
    private static final float KEY_SIZE = 38.0f;
    private static final float GAP = 5.0f;
    private static final float TOTAL_WIDTH = KEY_SIZE * 3.0f + GAP * 2.0f; // 124
    private static final float MOUSE_HEIGHT = 32.0f;
    private static final float SPACE_HEIGHT = 24.0f;
    private static final float PING_HEIGHT = 22.0f;

    public static BooleanSetting showCps;
    public static BooleanSetting showSpace;
    public static BooleanSetting showPing;

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    private boolean prevLeftDown;
    private boolean prevRightDown;

    public Keystrokes() {
        super(new ModuleInfo(
                CatalogText.literal("Keystrokes"),
                CatalogText.literal("Frosted glass WASD, LMB/RMB CPS, Space, and live Ping display"),
                ModuleCategory.RENDER), HudElementType.KEYSTROKES);
    }

    @Override
    public void postInitialize() {
        showCps.description(CatalogText.literal("Show LMB and RMB tiles with live clicks-per-second counters"));
        showSpace.description(CatalogText.literal("Show the Spacebar tile"));
        showPing.description(CatalogText.literal("Show a compact live ping footer pill"));
    }

    @Override
    public void onDisable() {
        leftClicks.clear();
        rightClicks.clear();
        prevLeftDown = false;
        prevRightDown = false;
        super.onDisable();
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean lmb = InputKey.MOUSE_LEFT.isPressed() || (client.options != null && client.options.attackKey.isPressed());
        boolean rmb = InputKey.MOUSE_RIGHT.isPressed() || (client.options != null && client.options.useKey.isPressed());
        if (lmb && !prevLeftDown) {
            leftClicks.addLast(now);
        }
        if (rmb && !prevRightDown) {
            rightClicks.addLast(now);
        }
        prevLeftDown = lmb;
        prevRightDown = rmb;
        pruneClicks(leftClicks, now);
        pruneClicks(rightClicks, now);
    }

    private static void pruneClicks(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() > 1000L) {
            deque.pollFirst();
        }
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        long now = System.currentTimeMillis();
        pruneClicks(leftClicks, now);
        pruneClicks(rightClicks, now);

        float x = HudElementType.KEYSTROKES.defaultX();
        float y = HudElementType.KEYSTROKES.defaultY();

        boolean w = client.options != null && client.options.forwardKey.isPressed();
        boolean a = client.options != null && client.options.leftKey.isPressed();
        boolean s = client.options != null && client.options.backKey.isPressed();
        boolean d = client.options != null && client.options.rightKey.isPressed();
        boolean space = client.options != null && client.options.jumpKey.isPressed();
        boolean lmb = InputKey.MOUSE_LEFT.isPressed() || (client.options != null && client.options.attackKey.isPressed());
        boolean rmb = InputKey.MOUSE_RIGHT.isPressed() || (client.options != null && client.options.useKey.isPressed());

        // Row 1: W
        drawKeyTile(layout, x + KEY_SIZE + GAP, y, KEY_SIZE, KEY_SIZE, "W", null, w);
        y += KEY_SIZE + GAP;

        // Row 2: A S D
        drawKeyTile(layout, x, y, KEY_SIZE, KEY_SIZE, "A", null, a);
        drawKeyTile(layout, x + KEY_SIZE + GAP, y, KEY_SIZE, KEY_SIZE, "S", null, s);
        drawKeyTile(layout, x + (KEY_SIZE + GAP) * 2.0f, y, KEY_SIZE, KEY_SIZE, "D", null, d);
        y += KEY_SIZE + GAP;

        // Row 3: LMB / RMB + CPS
        if (showCps == null || showCps.getValue()) {
            float halfW = (TOTAL_WIDTH - GAP) * 0.5f;
            drawKeyTile(layout, x, y, halfW, MOUSE_HEIGHT, "LMB", leftClicks.size() + " CPS", lmb);
            drawKeyTile(layout, x + halfW + GAP, y, halfW, MOUSE_HEIGHT, "RMB", rightClicks.size() + " CPS", rmb);
            y += MOUSE_HEIGHT + GAP;
        }

        // Row 4: Space
        if (showSpace == null || showSpace.getValue()) {
            drawSpaceTile(layout, x, y, TOTAL_WIDTH, SPACE_HEIGHT, space);
            y += SPACE_HEIGHT + GAP;
        }

        // Row 5: Ping
        if (showPing == null || showPing.getValue()) {
            int ping = LatencyTracker.milliseconds();
            HudRenderer.INSTANCE.drawPanel(layout, x, y, TOTAL_WIDTH, PING_HEIGHT, 7.0f, 1.0f, 12.0f);
            String label = "PING";
            String value = ping + " ms";
            layout.drawText(FontAsset.INTER_SEMI_BOLD, label, x + 9.0f, y + 6.0f, 9.5f, 0x99FFFFFF, 1.0f);
            float valW = layout.textWidth(FontAsset.INTER_SEMI_BOLD, value, 10.0f);
            layout.drawText(FontAsset.INTER_SEMI_BOLD, value, x + TOTAL_WIDTH - 9.0f - valW, y + 5.8f, 10.0f, 0xFFFFFFFF, 1.0f);
        }
    }

    private static void drawKeyTile(HudElementLayout layout, float x, float y, float w, float h, String title, String sub, boolean pressed) {
        HudRenderer.INSTANCE.drawPanel(layout, x, y, w, h, 8.0f, 1.0f, 12.0f);
        if (pressed) {
            layout.drawRoundedRect(x + 1.0f, y + 1.0f, w - 2.0f, h - 2.0f, 7.0f, 0x3DFFFFFF, 1.0f);
        }
        int primaryColor = pressed ? 0xFFFFFFFF : 0xCCFFFFFF;
        if (sub == null) {
            float tw = layout.textWidth(FontAsset.INTER_SEMI_BOLD, title, 13.0f);
            float th = layout.fontHeight(FontAsset.INTER_SEMI_BOLD, 13.0f);
            layout.drawText(FontAsset.INTER_SEMI_BOLD, title, x + (w - tw) * 0.5f, y + (h - th) * 0.5f, 13.0f, primaryColor, 1.0f);
        } else {
            float tw = layout.textWidth(FontAsset.INTER_SEMI_BOLD, title, 10.5f);
            float sw = layout.textWidth(FontAsset.INTER, sub, 8.5f);
            layout.drawText(FontAsset.INTER_SEMI_BOLD, title, x + (w - tw) * 0.5f, y + 5.0f, 10.5f, primaryColor, 1.0f);
            layout.drawText(FontAsset.INTER, sub, x + (w - sw) * 0.5f, y + 17.5f, 8.5f, pressed ? 0xEEFFFFFF : 0x99FFFFFF, 1.0f);
        }
    }

    private static void drawSpaceTile(HudElementLayout layout, float x, float y, float w, float h, boolean pressed) {
        HudRenderer.INSTANCE.drawPanel(layout, x, y, w, h, 7.0f, 1.0f, 12.0f);
        if (pressed) {
            layout.drawRoundedRect(x + 1.0f, y + 1.0f, w - 2.0f, h - 2.0f, 6.0f, 0x3DFFFFFF, 1.0f);
        }
        float barW = 46.0f;
        layout.drawRoundedRect(x + (w - barW) * 0.5f, y + (h - 2.5f) * 0.5f, barW, 2.5f, 1.25f, pressed ? 0xFFFFFFFF : 0xAAFFFFFF, 1.0f);
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        float height = (KEY_SIZE + GAP) * 2.0f;
        if (showCps == null || showCps.getValue()) {
            height += MOUSE_HEIGHT + GAP;
        }
        if (showSpace == null || showSpace.getValue()) {
            height += SPACE_HEIGHT + GAP;
        }
        if (showPing == null || showPing.getValue()) {
            height += PING_HEIGHT;
        } else {
            height -= GAP;
        }
        return new UiSize(TOTAL_WIDTH, Math.max(82.0f, height));
    }

    public static void appendBootstrapSettings(Keystrokes module) {
        CatalogSettings.append(module, showCps);
        CatalogSettings.append(module, showSpace);
        CatalogSettings.append(module, showPing);
    }

    public static void assignBootstrapSettings(
            Keystrokes module,
            BooleanSetting preparedShowCps,
            BooleanSetting preparedShowSpace,
            BooleanSetting preparedShowPing) {
        showCps = preparedShowCps;
        showSpace = preparedShowSpace;
        showPing = preparedShowPing;
    }
}
