package dev.dov.hack.echo.client.rendering.impl.vulkan.impl;

public final class VkLiquidGlass {

    private float x;
    private float y;
    private float width;
    private float height;
    private float radius = 6.0f;
    private float blur = 8.0f;
    private int tint = 0x40FFFFFF;

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0.0f, width);
        this.height = Math.max(0.0f, height);
    }

    public void configure(float radius, float blur, int tint) {
        this.radius = Math.max(0.0f, radius);
        this.blur = Math.max(0.0f, blur);
        this.tint = tint;
    }

    public int blurPasses() {
        return this.blur <= 0.0f ? 0 : Math.max(1, Math.min(6, (int) (this.blur / 2.0f)));
    }

    public float[] bounds() {
        return new float[] {this.x, this.y, this.width, this.height, this.radius};
    }

    public int tint() {
        return this.tint;
    }
}
