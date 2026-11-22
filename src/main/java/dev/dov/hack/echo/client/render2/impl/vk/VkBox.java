package dev.dov.hack.echo.client.rendering.impl.vk;

public final class VkBox implements dev.dov.hack.echo.client.rendering.api.Draw3D {

    private final java.util.List<float[]> edges = new java.util.ArrayList<>();

    @Override
    public void box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                    int argb) {
        if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
            return;
        }
        float[] corners = {
                (float) minX, (float) minY, (float) minZ,
                (float) maxX, (float) minY, (float) minZ,
                (float) maxX, (float) minY, (float) maxZ,
                (float) minX, (float) minY, (float) maxZ
        };
        int[][] pairs = {{0, 1}, {1, 2}, {2, 3}, {3, 0}};
        for (int[] pair : pairs) {
            this.edge(corners, pair[0], pair[1], minY, argb);
            this.edge(corners, pair[0], pair[1], maxY, argb);
        }
        for (int index = 0; index < 4; index++) {
            this.add(corners[index * 3], (float) minY, corners[index * 3 + 2],
                    corners[index * 3], (float) maxY, corners[index * 3 + 2], argb);
        }
    }

    private void edge(float[] corners, int first, int second, double y, int argb) {
        this.add(corners[first * 3], (float) y, corners[first * 3 + 2],
                corners[second * 3], (float) y, corners[second * 3 + 2], argb);
    }

    private void add(float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        this.edges.add(new float[] {x1, y1, z1, x2, y2, z2, argb});
    }

    public java.util.List<float[]> edges() {
        return java.util.List.copyOf(this.edges);
    }

    @Override
    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        this.add((float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, argb);
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
        this.box(x, y, z, x + 1.0, y + 1.0, z + 1.0, argb);
    }

    @Override
    public void flush() {
        this.edges.clear();
    }

    @Override
    public int vertexCount() {
        return this.edges.size() * 2;
    }
}
