package dev.dov.hack.echo.client.rendering.impl.opengl.instanced;

public final class GlSBPicker {

    private float x;
    private float y;
    private float size = 60.0f;
    private float saturation = 1.0f;
    private float brightness = 1.0f;

    public void setBounds(float x, float y, float size) {
        this.x = x;
        this.y = y;
        this.size = Math.max(1.0f, size);
    }

    public boolean setFromCursor(float mouseX, float mouseY) {
        float nextSaturation = Math.max(0.0f, Math.min(1.0f, (mouseX - this.x) / this.size));
        float nextBrightness = 1.0f - Math.max(0.0f, Math.min(1.0f, (mouseY - this.y) / this.size));
        if (Math.abs(nextSaturation - this.saturation) < 1.0e-4f
                && Math.abs(nextBrightness - this.brightness) < 1.0e-4f) {
            return false;
        }
        this.saturation = nextSaturation;
        this.brightness = nextBrightness;
        return true;
    }

    public float saturation() {
        return this.saturation;
    }

    public float brightness() {
        return this.brightness;
    }

    public int colorAt(float hue) {
        return java.awt.Color.HSBtoRGB(hue, this.saturation, this.brightness);
    }
}
