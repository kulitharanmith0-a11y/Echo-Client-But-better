package dev.dov.hack.echo.client.rendering.impl.opengl.utils;

public final class RenderUtil {
    private RenderUtil() {
    }

    public static float[] scissor(float x, float y, float width, float height,
                                  float viewportWidth, float viewportHeight) {
        float left = Math.max(0.0f, Math.min(x, viewportWidth));
        float top = Math.max(0.0f, Math.min(y, viewportHeight));
        float right = Math.max(left, Math.min(x + width, viewportWidth));
        float bottom = Math.max(top, Math.min(y + height, viewportHeight));
        return new float[] {left, top, right - left, bottom - top};
    }

    public static boolean inside(double mouseX, double mouseY, float x, float y,
                                 float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static float scaleFor(float guiScale, float target) {
        return guiScale <= 0.0f ? target : target / guiScale;
    }

    public static int packArgb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    public static int multiplyAlpha(int argb, float factor) {
        int alpha = (int) (((argb >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, factor)));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
