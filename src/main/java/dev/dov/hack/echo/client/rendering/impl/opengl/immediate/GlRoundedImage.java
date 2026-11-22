package dev.dov.hack.echo.client.rendering.impl.opengl.immediate;

public final class GlRoundedImage {
    private static final int CORNER_SEGMENTS = 6;

    private GlRoundedImage() {
    }

    
    public static float[] vertices(float x, float y, float width, float height, float radius) {
        float r = Math.max(0.0f, Math.min(Math.min(width, height) * 0.5f, radius));
        float centerX = x + width * 0.5f;
        float centerY = y + height * 0.5f;
        int points = (CORNER_SEGMENTS + 1) * 4;
        float[] out = new float[(points + 2) * 2];
        out[0] = centerX;
        out[1] = centerY;
        int cursor = 2;
        float[][] corners = {
                {x + width - r, y + r, -90.0f},
                {x + width - r, y + height - r, 0.0f},
                {x + r, y + height - r, 90.0f},
                {x + r, y + r, 180.0f}
        };
        for (float[] corner : corners) {
            for (int step = 0; step <= CORNER_SEGMENTS; step++) {
                double angle = Math.toRadians(corner[2] + step * (90.0 / CORNER_SEGMENTS));
                out[cursor++] = corner[0] + (float) Math.cos(angle) * r;
                out[cursor++] = corner[1] + (float) Math.sin(angle) * r;
            }
        }
        return out;
    }

    public static int triangleCount(float radius) {
        return radius <= 0.0f ? 2 : (CORNER_SEGMENTS + 1) * 4;
    }
}
