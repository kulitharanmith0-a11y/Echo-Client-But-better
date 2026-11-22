package dev.dov.hack.echo.client.gui.panels;

public enum NavigationPage {
    FEATURES("\ue2ad", "Features"), CONFIGS("\ue0cf", "Configs"), FRIENDS("\ue19f", "Friends"), HUD_EDITOR("\ue1d8", "HUD Editor");

    
    public final String icon;
    
    public final String label;

    NavigationPage(String icon, String label) {
        this.icon = icon;
        this.label = label;
    }
}
