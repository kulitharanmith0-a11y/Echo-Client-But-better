package dev.dov.hack.echo.client.rendering.impl.vk;

public final class VkLine implements dev.dov.hack.echo.client.rendering.api.Draw3D {

    private final float[] data;
    private int vertices;
    private final int capacity;

    public VkLine(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.data = new float[this.capacity * 3];
    }

    @Override
    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        this.push((float) x1, (float) y1, (float) z1);
        this.push((float) x2, (float) y2, (float) z2);
    }

    private void push(float x, float y, float z) {
        if (this.vertices >= this.capacity) {
            return;
        }
        this.data[this.vertices * 3] = x;
        this.data[this.vertices * 3 + 1] = y;
        this.data[this.vertices * 3 + 2] = z;
        this.vertices++;
    }

    public float[] data() {
        return this.data;
    }

    @Override
    public void box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                    int argb) {
    }

    @Override
    public void tracer(double fromX, double fromY, double fromZ, double toX, double toY,
                       double toZ, int argb) {
        this.line(fromX, fromY, fromZ, toX, toY, toZ, argb);
    }

    @Override
    public void particle(double x, double y, double z, float size, int argb) {
    }

    @Override
    public void voxel(int x, int y, int z, int argb) {
    }

    @Override
    public void flush() {
        this.vertices = 0;
    }

    @Override
    public int vertexCount() {
        return this.vertices;
    }
}
