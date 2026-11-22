package dev.dov.hack.echo.client.rendering.impl.vk.api;

public final class VkDraw3D implements dev.dov.hack.echo.client.rendering.api.Draw3D {
    private final java.util.List<Float> vertices = new java.util.ArrayList<>();
    private int budget;

    public VkDraw3D(int budget) {
        this.budget = Math.max(64, budget);
    }

    private void push(float x, float y, float z) {
        if (this.vertices.size() + 3 > this.budget * 3) {
            return;
        }
        this.vertices.add(x);
        this.vertices.add(y);
        this.vertices.add(z);
    }

    @Override
    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        this.push((float) x1, (float) y1, (float) z1);
        this.push((float) x2, (float) y2, (float) z2);
    }

    @Override
    public void box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                    int argb) {
        this.line(minX, minY, minZ, maxX, minY, minZ, argb);
        this.line(maxX, minY, minZ, maxX, minY, maxZ, argb);
        this.line(maxX, minY, maxZ, minX, minY, maxZ, argb);
        this.line(minX, minY, maxZ, minX, minY, minZ, argb);
        this.line(minX, maxY, minZ, maxX, maxY, minZ, argb);
        this.line(maxX, maxY, minZ, maxX, maxY, maxZ, argb);
        this.line(maxX, maxY, maxZ, minX, maxY, maxZ, argb);
        this.line(minX, maxY, maxZ, minX, maxY, minZ, argb);
        this.line(minX, minY, minZ, minX, maxY, minZ, argb);
        this.line(maxX, minY, minZ, maxX, maxY, minZ, argb);
        this.line(maxX, minY, maxZ, maxX, maxY, maxZ, argb);
        this.line(minX, minY, maxZ, minX, maxY, maxZ, argb);
    }

    @Override
    public void tracer(double fromX, double fromY, double fromZ, double toX, double toY,
                       double toZ, int argb) {
        this.line(fromX, fromY, fromZ, toX, toY, toZ, argb);
    }

    @Override
    public void particle(double x, double y, double z, float size, int argb) {
        this.push((float) x, (float) y, (float) z);
    }

    @Override
    public void voxel(int x, int y, int z, int argb) {
        this.box(x, y, z, x + 1.0, y + 1.0, z + 1.0, argb);
    }

    @Override
    public void flush() {
        this.vertices.clear();
    }

    @Override
    public int vertexCount() {
        return this.vertices.size() / 3;
    }

    public float[] toArray() {
        float[] out = new float[this.vertices.size()];
        for (int index = 0; index < out.length; index++) {
            out[index] = this.vertices.get(index);
        }
        return out;
    }
}
