package dev.dov.hack.echo.client.rendering.impl.vulkan.impl;

public final class VkText {

    private static final int FLOATS = 7;

    private final float[] data;
    private int count;
    private int atlasPage;

    public VkText(int capacity) {
        this.data = new float[Math.max(1, capacity) * FLOATS];
    }

    public boolean add(float x, float y, float scale, float width, int argb) {
        if ((this.count + 1) * FLOATS > this.data.length || scale <= 0.0f) {
            return false;
        }
        int base = this.count * FLOATS;
        this.data[base] = x;
        this.data[base + 1] = y;
        this.data[base + 2] = scale;
        this.data[base + 3] = width;
        this.data[base + 4] = (argb & 0xFFFFFF) / 16777216.0f;
        this.data[base + 5] = ((argb >>> 24) & 0xFF) / 255.0f;
        this.data[base + 6] = this.atlasPage;
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
