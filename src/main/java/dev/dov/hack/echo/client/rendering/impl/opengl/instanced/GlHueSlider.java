package dev.dov.hack.echo.client.rendering.impl.opengl.instanced;

public final class GlHueSlider {

    private float x;
    private float y;
    private float width = 80.0f;
    private float hue;

    public void setBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
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

    public float knobX() {
        return this.x + this.width * this.hue;
    }
}
