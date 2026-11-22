package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.particle.AnimatedParticle;
import dev.dov.hack.echo.client.particle.ParticleManager;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.settings.Setting;
import dev.dov.hack.echo.client.utils.MathRng;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class Particles extends Module {
    public static Particles instance;
    private ArrayList<AnimatedParticle> particles;
    private static ColorSetting color;
    private static IntSetting density;
    private static IntSetting radius;
    private static FloatSetting size;
    private static FloatSetting speed;
    private static BooleanSetting twinkle;

    static {
        MethodHandles.lookup().lookupClass();
    }

    public Particles() {
        super(new ModuleInfo(CatalogText.literal("Particles"), CatalogText.literal("Fills the world with ambient ember particles"), ModuleCategory.RENDER));
    }

    public static void appendBootstrapSettings(Particles Module) {
        CatalogSettings.append(Module, color);
        CatalogSettings.append(Module, density);
        CatalogSettings.append(Module, radius);
        CatalogSettings.append(Module, size);
        CatalogSettings.append(Module, speed);
        CatalogSettings.append(Module, twinkle);
    }

    public static void assignBootstrapSettings(Particles Module, ColorSetting preparedColor, IntSetting preparedRadius, FloatSetting preparedSpeed, IntSetting preparedDensity, BooleanSetting preparedTwinkle, FloatSetting preparedSize) {
        color = preparedColor;
        radius = preparedRadius;
        speed = preparedSpeed;
        density = preparedDensity;
        twinkle = preparedTwinkle;
        size = preparedSize;
    }

    @Override
    public void postInitialize() {
        ArrayList<AnimatedParticle> freshParticles = new ArrayList<>();
        initializeReferencedClass(ArrayList.class);
        particles = freshParticles;
        instance = this;
        initializeReferencedClass(ColorSetting.class);
        ColorSetting colorSetting = color;
        initializeReferencedClass(Setting.class);
        colorSetting.tooltip("Appearance");
        initializeReferencedClass(FloatSetting.class);
        FloatSetting sizeSetting = size;
        initializeReferencedClass(Setting.class);
        sizeSetting.tooltip("Appearance");
        initializeReferencedClass(FloatSetting.class);
        FloatSetting speedSetting = speed;
        initializeReferencedClass(Setting.class);
        speedSetting.tooltip("Timing");
    }

    public static boolean isActive() {
        boolean checkAvailability = Freecam.isPerspectiveRefreshEnabled();
        if (checkAvailability) {
            if (instance == null) return false;
            if (!instance.isEnabled()) return false;
        }
        boolean outsideWorld = instance.isOutsideWorld();
        return checkAvailability ? !outsideWorld : outsideWorld;
    }

    public static float getSizeMultiplier() {
        initializeReferencedClass(FloatSetting.class);
        FloatSetting setting = size;
        initializeReferencedClass(FloatSetting.class);
        return setting.getValue();
    }

    public static float getSpeedMultiplier() {
        initializeReferencedClass(FloatSetting.class);
        FloatSetting setting = speed;
        initializeReferencedClass(FloatSetting.class);
        return setting.getValue();
    }

    public static boolean isPulsing() {
        initializeReferencedClass(BooleanSetting.class);
        BooleanSetting setting = twinkle;
        initializeReferencedClass(BooleanSetting.class);
        return setting.getValue();
    }

    public static int getRed() {
        initializeReferencedClass(ColorSetting.class);
        ColorSetting setting = color;
        initializeReferencedClass(ColorSetting.class);
        return setting.getRed();
    }

    public static int getGreen() {
        initializeReferencedClass(ColorSetting.class);
        ColorSetting setting = color;
        initializeReferencedClass(ColorSetting.class);
        return setting.getGreen();
    }

    public static int getBlue() {
        initializeReferencedClass(ColorSetting.class);
        ColorSetting setting = color;
        initializeReferencedClass(ColorSetting.class);
        return setting.getBlue();
    }

    public static int getAlpha() {
        initializeReferencedClass(ColorSetting.class);
        ColorSetting setting = color;
        initializeReferencedClass(ColorSetting.class);
        return setting.getAlpha();
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        boolean checkAvailability = Freecam.isPerspectiveRefreshEnabled();
        if (checkAvailability && isOutsideWorld()) return;
        initializeReferencedClass(ArrayList.class);
        particles.removeIf(AnimatedParticle::isDead);
        initializeReferencedClass(IntSetting.class);
        IntSetting densitySetting = density;
        initializeReferencedClass(IntSetting.class);
        int desiredCount = densitySetting.getValue();
        initializeReferencedClass(ArrayList.class);
        int remaining = desiredCount - particles.size();
        int spawnCount = remaining;
        if (checkAvailability) {
            if (remaining <= 0) return;
            spawnCount = Math.min(remaining, Math.max(1, desiredCount / 40));
        }
        initializeReferencedClass(MinecraftClient.class);
        double playerX = client.player.getX();
        initializeReferencedClass(MinecraftClient.class);
        double playerY = client.player.getY();
        initializeReferencedClass(MinecraftClient.class);
        double playerZ = client.player.getZ();
        initializeReferencedClass(IntSetting.class);
        IntSetting radiusSetting = radius;
        initializeReferencedClass(IntSetting.class);
        int spawnRadius = radiusSetting.getValue();
        for (int index = 0; index < spawnCount; index++) {
            for (int attempt = 0; attempt < 4; attempt++) {
                double angle = MathRng.randomDouble(0.0, Math.PI * 2.0);
                double distance = Math.sqrt(MathRng.randomDouble(0.0, 1.0)) * spawnRadius;
                double x = playerX + Math.cos(angle) * distance;
                double y = playerY + MathRng.randomDouble(-6.0, 14.0);
                if (!checkAvailability) return;
                double z = playerZ + Math.sin(angle) * distance;
                initializeReferencedClass(MinecraftClient.class);
                if (!client.world.getBlockState(BlockPos.ofFloored(x, y, z)).isAir()) continue;
                AnimatedParticle particle = new AnimatedParticle(x, y, z);
                initializeReferencedClass(ArrayList.class);
                particles.add(particle);
                initializeReferencedClass(ParticleManager.class);
                ParticleManager manager = EchoClient.particleManager;
                initializeReferencedClass(ParticleManager.class);
                manager.add(particle);
                break;
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MethodHandles.lookup();
        initializeReferencedClass(ArrayList.class);
        Objects.requireNonNull(particles, "object reference is null").clear();
    }

    
    private static void initializeReferencedClass(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }
}
