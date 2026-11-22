package dev.dov.hack.echo.client.modules.impl.render;

public final class TrailMath {
    private TrailMath() {
    }

    
    public static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    
    public static int packArgb(int red, int green, int blue, int alpha) {
        return (alpha & 255) << 24 | (red & 255) << 16
            | (green & 255) << 8 | blue & 255;
    }

    
    public static int fadeAlpha(int alpha, long nowMillis, long sampleMillis, long lifetimeMillis) {
        float remaining = 1.0f - (float) (nowMillis - sampleMillis) / (float) lifetimeMillis;
        return Math.round((float) alpha * Math.clamp(remaining, 0.0f, 1.0f));
    }
}
