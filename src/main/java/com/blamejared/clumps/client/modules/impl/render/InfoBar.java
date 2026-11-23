package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.hud.HudElement;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.handlers.LatencyTracker;
import com.blamejared.clumps.client.hud.HudElementLayout;
import com.blamejared.clumps.client.hud.HudElementType;
import com.blamejared.clumps.client.hud.HudIcon;
import com.blamejared.clumps.client.hud.HudLayout;
import com.blamejared.clumps.client.hud.HudRenderer;
import com.blamejared.clumps.natives.OfflineSession;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.ui.UiSize;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfoBar extends HudElement {
    private BooleanSetting showFPS;
    private BooleanSetting showFrameTime;
    private BooleanSetting showPing;
    private BooleanSetting showUsername;
    private String cachedUid;
    private String cachedUsername;

    public InfoBar() {
        super(new ModuleInfo(CatalogText.literal("Info Bar"), CatalogText.literal("Client, FPS, profile and UID"), ModuleCategory.RENDER), HudElementType.INFO_BAR);
    }

    @Override
    protected boolean shouldRender() {
        return !client.debugHudEntryList.isF3Enabled();
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.INFO_BAR.defaultX();
        float y = HudElementType.INFO_BAR.defaultY();
        hud.drawPanel(layout, x, y, layout.getWidth(), 41.0f, 10.0f, 1.0f, 14.0f);
        float cursor = x + 14.0f;
        layout.drawIcon(HudIcon.LOGO, cursor, y + 10.5f, 20.0f, 20.0f, -1, 1.0f);
        cursor += 32.0f;
        layout.drawText(FontAsset.INTER_SEMI_BOLD, "Clumps", cursor, y + 12.5f, 13.5f, -1, 1.0f);
        cursor += layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Clumps", 13.5f) + 4.0f;
        layout.drawText(FontAsset.INTER_SEMI_BOLD, "Client", cursor, y + 12.5f, 13.5f, -4144960, 1.0f);
        cursor += layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Client", 13.5f);
        for (InfoBarSegment segment : collectSegments()) {
            cursor += 13.0f;
            layout.drawSeparator(cursor, y + 16.0f, 1.0f);
            cursor += 14.0f;
            layout.drawIcon(segment.icon(), cursor, y + 14.5f, 12.0f, 12.0f, -1, 1.0f);
            cursor += 18.0f;
            layout.drawText(FontAsset.INTER_SEMI_BOLD, segment.label(), cursor, y + 13.0f, 13.0f, -1, 1.0f);
            cursor += layout.textWidth(FontAsset.INTER_SEMI_BOLD, segment.label(), 13.0f);
            if (segment.value().isEmpty()) {
                continue;
            }
            cursor += 4.0f;
            layout.drawText(FontAsset.INTER, segment.value(), cursor, y + 13.0f, 13.0f, -1929379841, 1.0f);
            cursor += layout.textWidth(FontAsset.INTER, segment.value(), 13.0f);
        }
        layout.drawHeaderHighlight(x + 64.0f, y + 5.0f, 41.0f, 1.0f, 0.5f, 1.0f);
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        float width = 46.0f + layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Clumps", 13.5f)
                + 4.0f + layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Client", 13.5f) + 14.0f;
        for (InfoBarSegment segment : collectSegments()) {
            width += 45.0f + layout.textWidth(FontAsset.INTER_SEMI_BOLD, segment.label(), 13.0f);
            if (!segment.value().isEmpty()) {
                width += 4.0f + layout.textWidth(FontAsset.INTER, segment.value(), 13.0f);
            }
        }
        return new UiSize(HudLayout.roundWidth(width), 41.0f);
    }

    private List<InfoBarSegment> collectSegments() {
        HudRenderer hud = HudRenderer.INSTANCE;
        ArrayList<InfoBarSegment> segments = new ArrayList<>();
        if (showFPS.getValue()) {
            segments.add(new InfoBarSegment(HudIcon.FLASH, Integer.toString(hud.getFramesPerSecond()), "FPS"));
        }
        if (showFrameTime.getValue()) {
            segments.add(new InfoBarSegment(HudIcon.FLASH, String.format(Locale.ROOT, "%.1f", hud.getFrameTimeMillis()), "MS"));
        }
        if (showPing.getValue()) {
            int latency = getLatency();
            segments.add(new InfoBarSegment(HudIcon.GLOBAL, latency < 0 ? "-" : Integer.toString(latency), "MS"));
        }
        if (showUsername.getValue()) {
            segments.add(new InfoBarSegment(HudIcon.PROFILE, getUsername(), ""));
        }
        segments.add(new InfoBarSegment(HudIcon.TAG, getUid(), "UID"));
        return segments;
    }

    private String getUid() {
        if (cachedUid == null) {
            String value = OfflineSession.shared().call("uid", new String[0]);
            if (value == null || value.isEmpty()) {
                return "-";
            }
            cachedUid = value;
        }
        return cachedUid;
    }

    private String getUsername() {
        if (cachedUsername == null) {
            String value = OfflineSession.shared().call("username", new String[0]);
            if (value == null || value.isEmpty()) {
                return HudRenderer.INSTANCE.getUsernameFallback();
            }
            cachedUsername = value;
        }
        return cachedUsername;
    }

    private int getLatency() {
        if (client.player == null || client.getNetworkHandler() == null) {
            return -1;
        }
        return LatencyTracker.milliseconds();
    }

    
    public static void appendBootstrapSettings(InfoBar Module) {
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.showFPS);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.showFrameTime);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.showPing);
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, Module.showUsername);
    }

    
    public static void assignBootstrapSettings(InfoBar Module, com.blamejared.clumps.client.settings.BooleanSetting preparedShowFrameTime, com.blamejared.clumps.client.settings.BooleanSetting preparedShowFPS, com.blamejared.clumps.client.settings.BooleanSetting preparedShowPing, com.blamejared.clumps.client.settings.BooleanSetting preparedShowUsername) {
        Module.showFrameTime = preparedShowFrameTime;
        Module.showFPS = preparedShowFPS;
        Module.showPing = preparedShowPing;
        Module.showUsername = preparedShowUsername;
    }
}
