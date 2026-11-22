package dev.dov.hack.echo.client.rendering.impl.vulkan.impl;

public final class VkRoundedRect {

    private static final int FLOATS = 8;

    private final float[] data;
    private int count;

    public VkRoundedRect(int capacity) {
        this.data = new float[Math.max(1, capacity) * FLOATS];
    }

    public boolean add(float x, float y, float width, float height, float radius, int argb) {
        if ((this.count + 1) * FLOATS > this.data.length || width <= 0.0f || height <= 0.0f) {
            return false;
        }
        int base = this.count * FLOATS;
        this.data[base] = x;
        this.data[base + 1] = y;
        this.data[base + 2] = width;
        this.data[base + 3] = height;
        this.data[base + 4] = Math.max(0.0f, Math.min(Math.min(width, height) * 0.5f, radius));
        this.data[base + 5] = (argb & 0xFFFFFF) / 16777216.0f;
        this.data[base + 6] = ((argb >>> 24) & 0xFF) / 255.0f;
        this.data[base + 7] = 0.0f;
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
