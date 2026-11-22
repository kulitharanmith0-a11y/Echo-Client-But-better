package dev.dov.hack.echo.client.rendering.impl.gl;

public final class GlParticle implements dev.dov.hack.echo.client.rendering.api.Draw3D {

    private final java.util.List<float[]> billboards = new java.util.ArrayList<>();
    private final float[] right = {1.0f, 0.0f, 0.0f};
    private final float[] up = {0.0f, 1.0f, 0.0f};

    public void basis(float rightX, float rightY, float rightZ,
                      float upX, float upY, float upZ) {
        this.right[0] = rightX;
        this.right[1] = rightY;
        this.right[2] = rightZ;
        this.up[0] = upX;
        this.up[1] = upY;
        this.up[2] = upZ;
    }

    @Override
    public void particle(double x, double y, double z, float size, int argb) {
        float half = Math.max(0.0f, size) * 0.5f;
        float[] quad = new float[12];
        float[][] offsets = {{-1, -1}, {1, -1}, {1, 1}, {-1, -1}, {1, 1}, {-1, 1}};
        for (int index = 0; index < offsets.length; index++) {
            float ox = offsets[index][0] * half;
            float oy = offsets[index][1] * half;
            quad[index * 2] = (float) x + this.right[0] * ox + this.up[0] * oy;
            quad[index * 2 + 1] = (float) y + this.right[1] * ox + this.up[1] * oy;
            quad[index * 2 + 1] = (float) z;
        }
        this.billboards.add(quad);
    }

    public java.util.List<float[]> billboards() {
        return java.util.List.copyOf(this.billboards);
    }

    @Override
    public void box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                    int argb) {
    }

    @Override
    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
    }

    @Override
    public void tracer(double fromX, double fromY, double fromZ, double toX, double toY,
                       double toZ, int argb) {
    }

    @Override
    public void voxel(int x, int y, int z, int argb) {
    }

    @Override
    public void flush() {
        this.billboards.clear();
    }

    @Override
    public int vertexCount() {
        return this.billboards.size() * 6;
    }
}
