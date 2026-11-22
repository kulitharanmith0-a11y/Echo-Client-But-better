package dev.dov.hack.echo.client.render.math;

import org.joml.Vector4f;

public final class RenderMath {
    private RenderMath() {
    }

    
    public static int workgroupsForExtent(int extent) {
        return (extent + 8 - 1) / 8;
    }

    
    public static int trailAlpha(int alpha, long nowMillis, long sampleMillis) {
        float remaining = 1.0F - (float) (nowMillis - sampleMillis) / 420.0F;
        return Math.round(alpha * 0.35F * remaining * remaining);
    }

    
    public static int packArgb(int red, int green, int blue, int alpha) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16
                | (green & 0xFF) << 8 | blue & 0xFF;
    }

    
    public static Vector4f interpolateClipPosition(Vector4f from, Vector4f to, float fraction) {
        return new Vector4f(
                from.x() + (to.x() - from.x()) * fraction, from.y() + (to.y() - from.y()) * fraction, from.z() + (to.z() - from.z()) * fraction, from.w() + (to.w() - from.w()) * fraction);
    }
}
