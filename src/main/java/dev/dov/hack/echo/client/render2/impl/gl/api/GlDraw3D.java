package dev.dov.hack.echo.client.rendering.impl.gl.api;

public final class GlDraw3D implements dev.dov.hack.echo.client.rendering.api.Draw3D {
    private final java.util.List<float[]> lines = new java.util.ArrayList<>();
    private final java.util.LinkedHashSet<Long> voxels = new java.util.LinkedHashSet<>();
    private int budget;

    public GlDraw3D(int budget) {
        this.budget = Math.max(64, budget);
    }

    @Override
    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        if (this.lines.size() * 6 >= this.budget * 6) {
            return;
        }
        this.lines.add(new float[] {(float) x1, (float) y1, (float) z1,
                (float) x2, (float) y2, (float) z2});
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
    }

    @Override
    public void voxel(int x, int y, int z, int argb) {
        if (this.voxels.size() < this.budget) {
            this.voxels.add(((long) (x & 0x1FFFFF) << 42) | ((long) (z & 0x1FFFFF) << 21)
                    | (y & 0x1FFFFF));
        }
    }

    @Override
    public void flush() {
        this.lines.clear();
        this.voxels.clear();
    }

    @Override
    public int vertexCount() {
        return this.lines.size() * 2 + this.voxels.size() * 8;
    }

    public int voxelCount() {
        return this.voxels.size();
    }
}
