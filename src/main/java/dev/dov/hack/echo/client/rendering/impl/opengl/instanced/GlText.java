package dev.dov.hack.echo.client.rendering.impl.opengl.instanced;

public final class GlText {

    private static final int FLOATS = 8;

    private final float[] data;
    private int count;
    private final int capacity;

    public GlText(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.data = new float[this.capacity * FLOATS];
    }

    public boolean add(float x, float y, float scale, float width, int argb) {
        if (this.count >= this.capacity || scale <= 0.0f) {
            return false;
        }
        int base = this.count * FLOATS;
        this.data[base] = x;
        this.data[base + 1] = y;
        this.data[base + 2] = scale;
        this.data[base + 3] = width;
        this.data[base + 4] = ((argb >> 16) & 0xFF) / 255.0f;
        this.data[base + 5] = ((argb >> 8) & 0xFF) / 255.0f;
        this.data[base + 6] = (argb & 0xFF) / 255.0f;
        this.data[base + 7] = ((argb >>> 24) & 0xFF) / 255.0f;
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
