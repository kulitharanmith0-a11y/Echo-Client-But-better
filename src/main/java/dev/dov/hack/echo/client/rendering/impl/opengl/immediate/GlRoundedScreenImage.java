package dev.dov.hack.echo.client.rendering.impl.opengl.immediate;

public final class GlRoundedScreenImage {
    private GlRoundedScreenImage() {
    }

    public static float[] clamp(float x, float y, float width, float height,
                                float viewportWidth, float viewportHeight) {
        float left = Math.max(0.0f, x);
        float top = Math.max(0.0f, y);
        float right = Math.min(viewportWidth, x + width);
        float bottom = Math.min(viewportHeight, y + height);
        if (right <= left || bottom <= top) {
            return null;
        }
        return new float[] {left, top, right - left, bottom - top};
    }

    
    public static float[] uv(float originalX, float originalY, float width, float height,
                             float[] clamped) {
        if (clamped == null || width <= 0.0f || height <= 0.0f) {
            return new float[] {0.0f, 0.0f, 0.0f, 0.0f};
        }
        float u0 = (clamped[0] - originalX) / width;
        float v0 = (clamped[1] - originalY) / height;
        float u1 = u0 + clamped[2] / width;
        float v1 = v0 + clamped[3] / height;
        return new float[] {u0, v0, u1, v1};
    }
}
