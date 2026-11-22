package dev.dov.hack.echo.client.gui.components;

import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.IntSetting;

public final class SliderWidget {
    public final String label;
    public final float min;
    public final float max;
    private float value;
    private boolean dragging;

    public SliderWidget(String label, float min, float max, float initial) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.value = initial;
    }

    public static SliderWidget fromFloat(FloatSetting setting) {
        return new SliderWidget(setting.getName(), setting.getMinimum(), setting.getMaximum(), setting.getValue());
    }

    public static SliderWidget fromInt(IntSetting setting) {
        return new SliderWidget(setting.getName(), setting.getMinimum(), setting.getMaximum(), setting.getValue());
    }

    public float getValue() { return value; }

    public void setValue(float v) { value = Math.max(min, Math.min(max, v)); }

    public float getPercent() { return (value - min) / (max - min); }

    public void setFromPercent(float pct) { setValue(min + (max - min) * Math.max(0f, Math.min(1f, pct))); }

    public boolean isDragging() { return dragging; }

    public void setDragging(boolean dragging) { this.dragging = dragging; }
}