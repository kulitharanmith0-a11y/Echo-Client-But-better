package dev.dov.hack.echo.client.rendering.impl.vulkan.impl;

public final class VkHueSlider {

    private float x;
    private float width = 80.0f;
    private float hue;

    public void setBounds(float x, float width) {
        this.x = x;
        this.width = Math.max(1.0f, width);
    }

    public boolean setFromCursor(float mouseX) {
        float fraction = Math.max(0.0f, Math.min(1.0f, (mouseX - this.x) / this.width));
        if (Math.abs(fraction - this.hue) < 1.0e-4f) {
            return false;
        }
        this.hue = fraction;
        return true;
    }

    public float hue() {
        return this.hue;
    }

    public int colorAt(float fraction) {
        return java.awt.Color.HSBtoRGB(Math.max(0.0f, Math.min(1.0f, fraction)), 0.85f, 1.0f);
    }
}
