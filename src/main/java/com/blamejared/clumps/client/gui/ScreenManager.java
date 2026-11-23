package com.blamejared.clumps.client.gui;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.settings.Setting;
import net.minecraft.client.MinecraftClient;

public class ScreenManager implements MinecraftAccess {
    
    public static Setting focusedSetting = null;

    private DashboardScreen dashboardScreen;

    
    public void initialize() {
        dashboardScreen = new DashboardScreen();
    }

    
    public void open() {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        if (dashboardScreen == null) {
            dashboardScreen = new DashboardScreen();
        }
        if (!ClumpsClient.ensureRenderResources()) {
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
