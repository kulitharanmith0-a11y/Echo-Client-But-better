package dev.dov.hack.echo.client.rendering.api;

public interface Draw3D {
    void box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int argb);

    void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb);

    void tracer(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, int argb);

    void particle(double x, double y, double z, float size, int argb);

    void voxel(int x, int y, int z, int argb);

    void flush();

    int vertexCount();
}
