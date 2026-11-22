package dev.dov.hack.echo.client.rendering.impl.opengl.instanced;

public final class GlAlphaSlider {

    private float x;
    private float y;
    private float width = 80.0f;
    private int alpha = 255;

    public void setBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = Math.max(1.0f, width);
    }

    public boolean setFromCursor(float mouseX) {
        float fraction = (mouseX - this.x) / this.width;
        int next = Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * 255.0f);
        if (next == this.alpha) {
            return false;
        }
        this.alpha = next;
        return true;
    }

    public float knobX() {
        return this.x + this.width * (this.alpha / 255.0f);
    }

    public int alpha() {
        return this.alpha;
    }

    public float[] quad() {
        return new float[] {this.x, this.y, this.width, 6.0f};
    }
}
