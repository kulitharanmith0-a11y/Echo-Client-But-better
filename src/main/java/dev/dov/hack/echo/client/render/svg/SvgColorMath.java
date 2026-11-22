package dev.dov.hack.echo.client.render.svg;

public final class SvgColorMath {
    private SvgColorMath() {
    }

    
    public static int clampChannel(int channel) {
        return Math.max(0, Math.min(255, channel));
    }

    
    public static double srgbToLinear(int channel) {
        double srgb = channel / 255.0;
        if (srgb <= 0.04045) {
            return srgb / 12.92;
        }
        return Math.pow((srgb + 0.055) / 1.055, 2.4);
    }
}
