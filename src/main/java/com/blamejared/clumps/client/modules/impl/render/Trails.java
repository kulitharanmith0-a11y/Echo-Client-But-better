package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.modules.impl.render.TrailMath;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.world.WorldGeometry;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.utils.MathRng;
import java.util.ArrayDeque;
import java.util.Objects;

public class Trails extends Module {
    public static Trails instance;
    private static final long SAMPLE_INTERVAL_MILLIS = 25L;
    private static final double MIN_SAMPLE_DISTANCE_SQUARED = 0.0004;
    private static final double TELEPORT_DISTANCE_SQUARED = 64.0;
    private ArrayDeque<Point> trailSamples;
    private long lastTrailSampleTimeMillis;
    private static ColorSetting color;
    private static FloatSetting lifetime;
    private static FloatSetting height;
    private static BooleanSetting gradient;
    private static final String COLOR_SECTION = "Appearance";

    public Trails() {
        super(new ModuleInfo(CatalogText.literal("Trails"), CatalogText.literal("Leaves a fading ribbon wall behind you as you move"), ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        trailSamples = new ArrayDeque<>();
        instance = this;
        color.tooltip(COLOR_SECTION);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Objects.requireNonNull(trailSamples, "object reference is null").clear();
        lastTrailSampleTimeMillis = 0L;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Objects.requireNonNull(trailSamples, "object reference is null").clear();
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        long nowMillis = System.currentTimeMillis();
        long lifetimeMillis = (long) (lifetime.getValue() * 1000.0f);
        float tickDelta = event.getTickProgress();
        double x = MathRng.lerp(client.player.lastX, client.player.getX(), (double) tickDelta);
        double y = MathRng.lerp(client.player.lastY, client.player.getY(), (double) tickDelta);
        double z = MathRng.lerp(client.player.lastZ, client.player.getZ(), (double) tickDelta);

        while (!trailSamples.isEmpty() && nowMillis - trailSamples.peekLast().timeMillis() > lifetimeMillis) {
            trailSamples.removeLast();
        }
        Point newest = trailSamples.peekFirst();
        if (newest != null && TrailMath.squaredDistance(newest.x(), newest.y(), newest.z(), x, y, z)
                > TELEPORT_DISTANCE_SQUARED) {
            trailSamples.clear();
            newest = null;
        }
        if (nowMillis - lastTrailSampleTimeMillis >= SAMPLE_INTERVAL_MILLIS
                && (newest == null || TrailMath.squaredDistance(newest.x(), newest.y(), newest.z(), x, y, z)
                    > MIN_SAMPLE_DISTANCE_SQUARED)) {
            trailSamples.addFirst(new Point(x, y, z, nowMillis));
            lastTrailSampleTimeMillis = nowMillis;
        }
        if (trailSamples.isEmpty()) {
            return;
        }

        WorldGeometry triangles = event.getRenderContext().getDepthTestedGeometry();
        double ribbonHeight = (double) height.getValue();
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int alpha = color.getAlpha();
        float topAlphaScale = gradient.getValue() ? 0.1f : 1.0f;
        double previousX = x;
        double previousY = y;
        double previousZ = z;
        long previousTime = nowMillis;
        for (Point point : trailSamples) {
            int previousAlpha = fadeAlpha(alpha, nowMillis, previousTime, lifetimeMillis);
            int pointAlpha = fadeAlpha(alpha, nowMillis, point.timeMillis(), lifetimeMillis);
            if (previousAlpha >= 4 || pointAlpha >= 4) {
                appendRibbonSegment(triangles, ribbonHeight, red, green, blue, topAlphaScale, previousX, previousY, previousZ, previousAlpha, point.x(), point.y(), point.z(), pointAlpha);
            }
            previousX = point.x();
            previousY = point.y();
            previousZ = point.z();
            previousTime = point.timeMillis();
        }
    }

    private static void appendRibbonSegment(WorldGeometry triangles, double height, int red, int green, int blue, float topAlphaScale, double x1, double y1, double z1, int alpha1, double x2, double y2, double z2, int alpha2) {
        if (TrailMath.squaredDistance(x1, y1, z1, x2, y2, z2) < 0.000001) {
            return;
        }
        int bottom1 = TrailMath.packArgb(red, green, blue, alpha1);
        int top1 = TrailMath.packArgb(red, green, blue, Math.round((float) alpha1 * topAlphaScale));
        int bottom2 = TrailMath.packArgb(red, green, blue, alpha2);
        int top2 = TrailMath.packArgb(red, green, blue, Math.round((float) alpha2 * topAlphaScale));
        triangles.drawTriangle(x1, y1, z1, bottom1, x2, y2, z2, bottom2, x1, y1 + height, z1, top1);
        triangles.drawTriangle(x1, y1 + height, z1, top1, x2, y2, z2, bottom2, x2, y2 + height, z2, top2);
    }

    private static int fadeAlpha(int alpha, long nowMillis, long sampleMillis, long lifetimeMillis) {
        float remaining = 1.0f - (float) (nowMillis - sampleMillis) / (float) lifetimeMillis;
        return Math.round((float) alpha * MathRng.clamp(remaining, 0.0f, 1.0f));
    }

    public static void appendBootstrapSettings(Trails Module) {
        CatalogSettings.append(Module, color);
        CatalogSettings.append(Module, lifetime);
        CatalogSettings.append(Module, height);
        CatalogSettings.append(Module, gradient);
    }

    public static void assignBootstrapSettings(Trails Module, BooleanSetting preparedGradient, FloatSetting preparedHeight, FloatSetting preparedLifetime, ColorSetting preparedColor) {
        gradient = preparedGradient;
        height = preparedHeight;
        lifetime = preparedLifetime;
        color = preparedColor;
    }

    private record Point(double x, double y, double z, long timeMillis) {
    }
}
