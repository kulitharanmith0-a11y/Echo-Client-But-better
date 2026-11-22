package dev.dov.hack.echo.client.rendering.impl.opengl.instanced;

public final class GlRoundedRect {

    private static final int FLOATS = 9;

    private final float[] data;
    private int count;
    private final int capacity;

    public GlRoundedRect(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.data = new float[this.capacity * FLOATS];
    }

    public boolean add(float x, float y, float width, float height, float radius, int argb) {
        if (this.count >= this.capacity || width <= 0.0f || height <= 0.0f) {
            return false;
        }
        int base = this.count * FLOATS;
        this.data[base] = x;
        this.data[base + 1] = y;
        this.data[base + 2] = width;
        this.data[base + 3] = height;
        this.data[base + 4] = Math.max(0.0f, Math.min(Math.min(width, height) * 0.5f, radius));
        this.data[base + 5] = ((argb >> 16) & 0xFF) / 255.0f;
        this.data[base + 6] = ((argb >> 8) & 0xFF) / 255.0f;
        this.data[base + 7] = (argb & 0xFF) / 255.0f;
        this.data[base + 8] = ((argb >>> 24) & 0xFF) / 255.0f;
        this.count++;
        return true;
    }

    public int count() {
        return this.count;
    }

    public float[] data() {
        return this.data;
    }

    public void clear() {
        this.count = 0;
    }

    public int strideFloats() {
        return FLOATS;
    }
}
