package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.modules.hud.HudElement;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.handlers.LatencyTracker;
import dev.dov.hack.echo.client.hud.HudElementLayout;
import dev.dov.hack.echo.client.hud.HudElementType;
import dev.dov.hack.echo.client.hud.HudIcon;
import dev.dov.hack.echo.client.hud.HudLayout;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.jadyen.hack.echo.natives.OfflineSession;
import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.ui.UiSize;
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
        layout.drawText(FontAsset.INTER_SEMI_BOLD, "Echo", cursor, y + 12.5f, 13.5f, -1, 1.0f);
        cursor += layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Echo", 13.5f) + 4.0f;
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
        float width = 46.0f + layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Echo", 13.5f)
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
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Module.showFPS);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Module.showFrameTime);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Module.showPing);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Module.showUsername);
    }

    
    public static void assignBootstrapSettings(InfoBar Module, dev.dov.hack.echo.client.settings.BooleanSetting preparedShowFrameTime, dev.dov.hack.echo.client.settings.BooleanSetting preparedShowFPS, dev.dov.hack.echo.client.settings.BooleanSetting preparedShowPing, dev.dov.hack.echo.client.settings.BooleanSetting preparedShowUsername) {
        Module.showFrameTime = preparedShowFrameTime;
        Module.showFPS = preparedShowFPS;
        Module.showPing = preparedShowPing;
        Module.showUsername = preparedShowUsername;
    }
}
