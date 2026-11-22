package dev.dov.hack.echo.client.rendering.impl.vulkan.api;

public final class VkDraw2D implements dev.dov.hack.echo.client.rendering.api.Draw2D {
    private final float[] vertices;
    private int vertexCount;
    private final java.util.List<String> draws = new java.util.ArrayList<>();
    private final float viewportWidth;
    private final float viewportHeight;

    public VkDraw2D(float viewportWidth, float viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.vertices = new float[4096 * 4];
    }

    private void quad(float x, float y, float width, float height, int argb) {
        if (this.vertexCount + 6 > this.vertices.length / 4) {
            return;
        }
        float alpha = ((argb >>> 24) & 0xFF) / 255.0f;
        float red = ((argb >> 16) & 0xFF) / 255.0f;
        float green = ((argb >> 8) & 0xFF) / 255.0f;
        float blue = (argb & 0xFF) / 255.0f;
        float[][] corners = {{x, y}, {x + width, y}, {x + width, y + height},
                {x, y}, {x + width, y + height}, {x, y + height}};
        for (float[] corner : corners) {
            int base = this.vertexCount * 4;
            this.vertices[base] = corner[0];
            this.vertices[base + 1] = corner[1];
            this.vertices[base + 2] = red;
            this.vertices[base + 3] = green;
            this.vertices[base + 3] = blue;
            this.vertices[base + 3] = alpha;
            this.vertexCount++;
        }
    }

    @Override
    public void rect(float x, float y, float width, float height, int argb) {
        this.quad(x, y, width, height, argb);
        this.draws.add("rect");
    }

    @Override
    public void gradient(float x, float y, float width, float height, int topArgb, int bottomArgb) {
        this.quad(x, y, width, height, topArgb);
        this.draws.add("gradient");
    }

    @Override
    public void text(String text, float x, float y, float scale, int argb) {
        if (text == null || text.isEmpty()) {
            return;
        }
        this.draws.add("text:" + text.length());
    }

    @Override
    public void texture(int textureId, float x, float y, float width, float height,
                        float u0, float v0, float u1, float v1, int argb) {
        if (textureId == 0) {
            return;
        }
        this.quad(x, y, width, height, argb);
        this.draws.add("texture:" + textureId);
    }

    @Override
    public void scissor(float x, float y, float width, float height) {
        this.draws.add("scissor");
    }

    @Override
    public void resetScissor() {
        this.draws.add("scissor off");
    }

    @Override
    public float width() {
        return this.viewportWidth;
    }

    @Override
    public float height() {
        return this.viewportHeight;
    }

    public int vertexCount() {
        return this.vertexCount;
    }

    public int drawCount() {
        return this.draws.size();
    }

    public void clear() {
        this.vertexCount = 0;
        this.draws.clear();
    }
}
