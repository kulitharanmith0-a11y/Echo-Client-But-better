package dev.dov.hack.echo.client.rendering.impl.vk;

public final class VkVoxelRenderer implements dev.dov.hack.echo.client.rendering.api.Draw3D {

    private final java.util.LinkedHashSet<Long> voxels = new java.util.LinkedHashSet<>();
    private final java.util.Map<Long, Integer> colors = new java.util.HashMap<>();

    public static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (z & 0x1FFFFF) << 21) | (y & 0x1FFFFF);
    }

    @Override
    public void voxel(int x, int y, int z, int argb) {
        long key = key(x, y, z);
        this.voxels.add(key);
        this.colors.put(key, argb);
    }

    public int voxelCount() {
        return this.voxels.size();
    }

    public int faceCount() {
        int faces = 0;
        for (long key : this.voxels) {
            int x = (int) ((key >> 42) & 0x1FFFFF);
            int z = (int) ((key >> 21) & 0x1FFFFF);
            int y = (int) (key & 0x1FFFFF);
            faces += 6;
            if (this.voxels.contains(key(x + 1, y, z))) {
                faces--;
            }
            if (this.voxels.contains(key(x, y + 1, z))) {
                faces--;
            }
            if (this.voxels.contains(key(x, y, z + 1))) {
                faces--;
            }
        }
        return faces;
    }

    public int colorOf(int x, int y, int z) {
        Integer argb = this.colors.get(key(x, y, z));
        return argb == null ? 0xFFFFFFFF : argb;
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
    public void particle(double x, double y, double z, float size, int argb) {
    }

    @Override
    public void flush() {
        this.voxels.clear();
        this.colors.clear();
    }

    @Override
    public int vertexCount() {
        return this.faceCount() * 4;
    }
}
