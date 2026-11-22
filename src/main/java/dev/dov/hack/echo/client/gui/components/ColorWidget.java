package dev.dov.hack.echo.client.gui.components;

import dev.dov.hack.echo.client.settings.ColorSetting;

public final class ColorWidget {
    public final String label;
    private int argb;
    private boolean open;

    public ColorWidget(String label, int argb) {
        this.label = label;
        this.argb = argb;
    }

    public static ColorWidget fromSetting(ColorSetting setting) {
        return new ColorWidget(setting.getName(), setting.getArgb());
    }

    public int getArgb() { return argb; }

    public void setArgb(int argb) { this.argb = argb; }

    public int getAlpha() { return (argb >> 24) & 0xFF; }

    public int getRed()   { return (argb >> 16) & 0xFF; }

    public int getGreen() { return (argb >>  8) & 0xFF; }

    public int getBlue()  { return  argb        & 0xFF; }

    public boolean isOpen() { return open; }

    public void setOpen(boolean open) { this.open = open; }
}