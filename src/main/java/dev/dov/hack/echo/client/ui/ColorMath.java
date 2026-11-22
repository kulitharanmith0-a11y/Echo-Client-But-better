package dev.dov.hack.echo.client.ui;

public final class ColorMath {
    private ColorMath() {
    }

    public static int multiplyAlpha(int argb, float opacity) {
        int alpha = Math.round((argb >>> 24 & 0xff) * Math.clamp(opacity, 0.0f, 1.0f));
        return alpha << 24 | argb & 0x00ffffff;
    }

    public static int interpolate(int from, int to, float amount) {
        int alpha = interpolateChannel(from >>> 24, to >>> 24, amount);
        int red = interpolateChannel(from >> 16 & 0xff, to >> 16 & 0xff, amount);
        int green = interpolateChannel(from >> 8 & 0xff, to >> 8 & 0xff, amount);
        int blue = interpolateChannel(from & 0xff, to & 0xff, amount);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int interpolateChannel(int from, int to, float amount) {
        return Math.clamp(Math.round(from + (to - from) * amount), 0, 255);
    }
}
