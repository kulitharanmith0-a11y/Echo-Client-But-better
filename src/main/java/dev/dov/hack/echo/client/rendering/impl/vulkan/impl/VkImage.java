package dev.dov.hack.echo.client.rendering.impl.vulkan.impl;

public final class VkImage {

    private static final int FLOATS = 8;

    private final float[] data;
    private int count;

    public VkImage(int capacity) {
        this.data = new float[Math.max(1, capacity) * FLOATS];
    }

    public boolean add(float x, float y, float width, float height,
                       float u0, float v0, float u1, float v1, int argb) {
        if ((this.count + 1) * FLOATS > this.data.length || width <= 0.0f || height <= 0.0f) {
            return false;
        }
        int base = this.count * FLOATS;
        this.data[base] = x;
        this.data[base + 1] = y;
        this.data[base + 2] = width;
        this.data[base + 3] = height;
        this.data[base + 4] = u0;
        this.data[base + 5] = v0;
        this.data[base + 6] = u1;
        this.data[base + 7] = v1;
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
}
