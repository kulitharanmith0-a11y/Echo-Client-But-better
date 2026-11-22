package dev.dov.hack.echo.client.rendering.impl.gl;

public final class GlTracers implements dev.dov.hack.echo.client.rendering.api.Draw3D {

    private double eyeX;
    private double eyeY;
    private double eyeZ;
    private final java.util.List<double[]> targets = new java.util.ArrayList<>();

    public void eye(double x, double y, double z) {
        this.eyeX = x;
        this.eyeY = y;
        this.eyeZ = z;
    }

    @Override
    public void tracer(double fromX, double fromY, double fromZ, double toX, double toY,
                       double toZ, int argb) {
        this.targets.add(new double[] {toX, toY, toZ});
    }

    
    public double[] segments() {
        double[] out = new double[this.targets.size() * 6];
        for (int index = 0; index < this.targets.size(); index++) {
            double[] target = this.targets.get(index);
            out[index * 6] = this.eyeX;
            out[index * 6 + 1] = this.eyeY;
            out[index * 6 + 2] = this.eyeZ;
            out[index * 6 + 3] = target[0];
            out[index * 6 + 4] = target[1];
            out[index * 6 + 5] = target[2];
        }
        return out;
    }

    @Override
    public void box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                    int argb) {
    }

    @Override
    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
    }

    @Override
    public void particle(double x, double y, double z, float size, int argb) {
    }

    @Override
    public void voxel(int x, int y, int z, int argb) {
    }

    @Override
    public void flush() {
        this.targets.clear();
    }

    @Override
    public int vertexCount() {
        return this.targets.size() * 2;
    }
}
