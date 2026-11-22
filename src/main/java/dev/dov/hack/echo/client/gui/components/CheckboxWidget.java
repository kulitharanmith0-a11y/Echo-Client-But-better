package dev.dov.hack.echo.client.gui.components;

import dev.dov.hack.echo.client.settings.BooleanSetting;

public final class CheckboxWidget {
    public final String label;
    private boolean checked;
    private boolean hovered;

    public CheckboxWidget(String label, boolean initial) {
        this.label = label;
        this.checked = initial;
    }

    public static CheckboxWidget fromSetting(BooleanSetting setting) {
        return new CheckboxWidget(setting.getName(), setting.getValue());
    }

    public boolean isChecked() { return checked; }

    public void setChecked(boolean checked) { this.checked = checked; }

    public void toggle() { checked = !checked; }

    public boolean isHovered() { return hovered; }

    public void setHovered(boolean hovered) { this.hovered = hovered; }
}