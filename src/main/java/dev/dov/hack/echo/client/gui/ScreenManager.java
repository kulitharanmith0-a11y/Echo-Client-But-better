package dev.dov.hack.echo.client.gui;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.MinecraftAccess;
import dev.dov.hack.echo.client.settings.Setting;
import net.minecraft.client.MinecraftClient;

public class ScreenManager implements MinecraftAccess {
    
    public static Setting focusedSetting = null;

    private DashboardScreen dashboardScreen;

    
    public void initialize() {
        dashboardScreen = new DashboardScreen();
    }

    
    public void open() {
        if (EchoClient.eventsDisabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        if (dashboardScreen == null) {
            dashboardScreen = new DashboardScreen();
        }
        if (!EchoClient.ensureRenderResources()) {
            return;
        }
        client.setScreen(dashboardScreen);
    }

    
    public boolean isOpen() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && dashboardScreen != null && client.currentScreen == dashboardScreen;
    }

    
    public void close() {
        focusedSetting = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }

    
    public void destroy() {
        close();
        dashboardScreen = null;
    }
}
