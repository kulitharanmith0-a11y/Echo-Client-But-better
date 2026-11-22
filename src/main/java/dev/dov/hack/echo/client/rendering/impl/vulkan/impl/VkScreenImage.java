package dev.dov.hack.echo.client.rendering.impl.vulkan.impl;

public final class VkScreenImage {

    private final float[] quad = new float[16];
    private boolean flipVertical = true;

    public void set(float width, float height) {
        float u0 = 0.0f;
        float v0 = this.flipVertical ? 1.0f : 0.0f;
        float u1 = 1.0f;
        float v1 = this.flipVertical ? 0.0f : 1.0f;
        float[] values = {0.0f, 0.0f, u0, v0, width, 0.0f, u1, v0,
                width, height, u1, v1, 0.0f, height, u0, v1};
        System.arraycopy(values, 0, this.quad, 0, values.length);
    }

    public void setFlipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
    }

    public float[] quad() {
        return this.quad.clone();
    }
}
