package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.particle.AnimatedParticle;
import com.blamejared.clumps.client.render.world.WorldGeometry;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.utils.MathRng;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Particles extends Module {
    public static Particles instance;
    private ArrayList<AnimatedParticle> particles;
    private static ColorSetting color;
    private static IntSetting density;
    private static IntSetting radius;
    private static FloatSetting size;
    private static FloatSetting speed;
    private static BooleanSetting twinkle;

    public Particles() {
        super(new ModuleInfo(
                CatalogText.literal("Particles"),
                CatalogText.literal("Fills the world with ambient glowing ember particles"),
                ModuleCategory.RENDER));
    }

    public static void appendBootstrapSettings(Particles Module) {
        CatalogSettings.append(Module, color);
        CatalogSettings.append(Module, density);
        CatalogSettings.append(Module, radius);
        CatalogSettings.append(Module, size);
        CatalogSettings.append(Module, speed);
        CatalogSettings.append(Module, twinkle);
    }

    public static void assignBootstrapSettings(
            Particles Module,
            ColorSetting preparedColor,
            IntSetting preparedRadius,
            FloatSetting preparedSpeed,
            IntSetting preparedDensity,
            BooleanSetting preparedTwinkle,
            FloatSetting preparedSize) {
        color = preparedColor;
        radius = preparedRadius;
        speed = preparedSpeed;
        density = preparedDensity;
        twinkle = preparedTwinkle;
        size = preparedSize;
    }

    @Override
    public void postInitialize() {
        particles = new ArrayList<>();
        instance = this;
        color.tooltip("Appearance");
        size.tooltip("Appearance");
        speed.tooltip("Timing");
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled() && !instance.isOutsideWorld();
    }

    public static float getSizeMultiplier() { return size != null ? size.getValue() : 1.0F; }
    public static float getSpeedMultiplier() { return speed != null ? speed.getValue() : 1.0F; }
    public static boolean isPulsing() { return twinkle != null && twinkle.getValue(); }
    public static int getRed() { return color != null ? color.getRed() : 255; }
    public static int getGreen() { return color != null ? color.getGreen() : 210; }
    public static int getBlue() { return color != null ? color.getBlue() : 140; }
    public static int getAlpha() { return color != null ? color.getAlpha() : 220; }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (isOutsideWorld() || particles == null) {
            return;
        }
        particles.removeIf(AnimatedParticle::isDead);
        int desiredCount = Math.max(1, density.getValue());
        int remaining = desiredCount - particles.size();
        if (remaining <= 0) {
            return;
        }
        int spawnCount = Math.min(remaining, Math.max(2, desiredCount / 18));
        double playerX = client.player.getX();
        double playerY = client.player.getY();
        double playerZ = client.player.getZ();
        int spawnRadius = Math.max(4, radius.getValue());

        for (int index = 0; index < spawnCount; index++) {
            for (int attempt = 0; attempt < 4; attempt++) {
                double angle = MathRng.randomDouble(0.0, Math.PI * 2.0);
                double distance = Math.sqrt(MathRng.randomDouble(0.0, 1.0)) * spawnRadius;
                double x = playerX + Math.cos(angle) * distance;
                double y = playerY + MathRng.randomDouble(-4.0, 12.0);
                double z = playerZ + Math.sin(angle) * distance;
                if (!client.world.getBlockState(BlockPos.ofFloored(x, y, z)).isAir()) {
                    continue;
                }
                particles.add(new AnimatedParticle(x, y, z));
                break;
            }
        }
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld() || particles == null || particles.isEmpty()) {
            return;
        }
        Camera camera = event.getCamera();
        if (camera == null) {
            return;
        }
        WorldGeometry geometry = event.getRenderContext().getDepthTestedGeometry();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        Vec3d right = Vec3d.fromPolar(0.0F, yaw + 90.0F).normalize();
        Vec3d up = Vec3d.fromPolar(pitch - 90.0F, yaw).normalize();
        float tickProgress = event.getTickProgress();

        Iterator<AnimatedParticle> it = particles.iterator();
        while (it.hasNext()) {
            AnimatedParticle p = it.next();
            p.tick(tickProgress);
            if (p.isDead()) {
                it.remove();
                continue;
            }
            int argb = p.color;
            int alpha = (argb >>> 24) & 0xFF;
            if (alpha <= 2) {
                continue;
            }
            double half = Math.max(0.025, p.getSize());
            double glowHalf = half * 1.9;
            int glowArgb = ((alpha / 3) << 24) | (argb & 0x00FFFFFF);

            double rx = right.x;
            double ry = right.y;
            double rz = right.z;
            double ux = up.x;
            double uy = up.y;
            double uz = up.z;
            double px = p.x;
            double py = p.y;
            double pz = p.z;

            // Outer soft glow diamond
            drawBillboardQuad(geometry, px, py, pz, rx, ry, rz, ux, uy, uz, glowHalf, glowArgb);
            // Bright crisp ember core
            drawBillboardQuad(geometry, px, py, pz, rx, ry, rz, ux, uy, uz, half, argb);
        }
    }

    private static void drawBillboardQuad(
            WorldGeometry geometry,
            double cx, double cy, double cz,
            double rx, double ry, double rz,
            double ux, double uy, double uz,
            double half,
            int color) {
        double x1 = cx + (-rx - ux) * half;
        double y1 = cy + (-ry - uy) * half;
        double z1 = cz + (-rz - uz) * half;

        double x2 = cx + (rx - ux) * half;
        double y2 = cy + (ry - uy) * half;
        double z2 = cz + (rz - uz) * half;

        double x3 = cx + (rx + ux) * half;
        double y3 = cy + (ry + uy) * half;
        double z3 = cz + (rz + uz) * half;

        double x4 = cx + (-rx + ux) * half;
        double y4 = cy + (-ry + uy) * half;
        double z4 = cz + (-rz + uz) * half;

        geometry.drawTriangle(x1, y1, z1, color, x2, y2, z2, color, x3, y3, z3, color);
        geometry.drawTriangle(x1, y1, z1, color, x3, y3, z3, color, x4, y4, z4, color);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (particles != null) {
            particles.clear();
        }
    }
}
