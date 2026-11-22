package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.network.PacketReceiveEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.utils.ClientWorldUpdates;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class Ambience extends Module {
    private static final CharSequence CLEAR = CatalogText.literal("Clear");
    private static final CharSequence RAIN = CatalogText.literal("Rain");
    private static final CharSequence THUNDER = CatalogText.literal("Thunder");
    private static final CharSequence SNOW = CatalogText.literal("Snow");
    private static BooleanSetting customTime;
    private static IntSetting time;
    private static BooleanSetting modifyWeather;
    private static ChoiceSetting weather;
    private static BooleanSetting fogColorBoolean;
    private static ColorSetting fogColorColor;
    private static BooleanSetting gradient;
    private static ColorSetting gradientColor;
    private static IntSetting fogDistance;

    public Ambience() {
        super(new ModuleInfo(CatalogText.literal("Ambience"), CatalogText.literal("Modifies world ambience"), ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        time.setVisibilityPredicate(ignored -> customTime.getValue());
        fogColorColor.setVisibilityPredicate(ignored -> fogColorBoolean.getValue());
        gradient.setVisibilityPredicate(ignored -> fogColorBoolean.getValue());
        gradientColor.setVisibilityPredicate(ignored -> {
            boolean controlState = Freecam.isPerspectiveRefreshBypassed();
            BooleanSetting condition = fogColorBoolean;
            if (!controlState) {
                if (!condition.getValue()) return false;
                condition = gradient;
            }
            return condition.getValue();
        });
        fogDistance.setVisibilityPredicate(ignored -> fogColorBoolean.getValue());
        fogColorBoolean.tooltip("Appearance");
        fogColorColor.tooltip("Appearance");
        gradientColor.tooltip("Appearance");
    }

    public static boolean isCustomTimeEnabled() { return customTime.getValue(); }
    public static long getConfiguredTime() { return time.getValue(); }
    public static boolean isWeatherModified() { return modifyWeather.getValue(); }
    public static CharSequence getWeatherSelection() { return weather.getValue(); }
    public static CharSequence getWeatherValue() { return weather.getValue(); }
    public static boolean isFogColorEnabled() { return fogColorBoolean.getValue(); }
    public static boolean isGradientEnabled() { return gradient.getValue(); }
    public static int getFogRed() { return fogColorColor.getRed(); }
    public static int getFogGreen() { return fogColorColor.getGreen(); }
    public static int getFogBlue() { return fogColorColor.getBlue(); }
    public static int getGradientRed() { return gradientColor.getRed(); }
    public static int getGradientGreen() { return gradientColor.getGreen(); }
    public static int getGradientBlue() { return gradientColor.getBlue(); }
    public static float getFogDistanceChunks() { return fogDistance.getValue(); }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        boolean controlState = Freecam.isPerspectiveRefreshBypassed();
        if (isOutsideWorld()) return;
        Packet<?> packet = event.getPacket();
        if (!controlState && !(packet instanceof WorldTimeUpdateS2CPacket)) return;
        WorldTimeUpdateS2CPacket timeUpdate = (WorldTimeUpdateS2CPacket) packet;
        BooleanSetting weatherGate = customTime;
        if (!controlState) {
            if (weatherGate.getValue()) {
                ClientWorldUpdates.setFixedTime(client.world, getConfiguredTime());
                event.cancel();
            }
            weatherGate = modifyWeather;
        }
        boolean clearWeather = weatherGate.getValue();
        if (controlState) {
            if (clearWeather) {
                client.world.setRainGradient(0.0f);
                client.world.setThunderGradient(0.0f);
            }
            if (weather.isSelected(RAIN)) {
                client.world.setRainGradient(1.0f);
                client.world.setThunderGradient(1.0f);
            }
            if (weather.isSelected(SNOW)) {
                client.world.setRainGradient(0.7f);
                client.world.setThunderGradient(0.0f);
            }
            event.cancel();
            client.world.setRainGradient(0.0f);
            client.world.setThunderGradient(0.0f);
        } else if (clearWeather) {
            if (weather.isSelected(CLEAR)) {
                client.world.setRainGradient(0.0f);
                client.world.setThunderGradient(0.0f);
            } else if (weather.isSelected(RAIN)) {
                client.world.setRainGradient(1.0f);
                client.world.setThunderGradient(0.0f);
            } else if (weather.isSelected(THUNDER)) {
                client.world.setRainGradient(1.0f);
                client.world.setThunderGradient(1.0f);
            } else if (weather.isSelected(SNOW)) {
                client.world.setRainGradient(0.7f);
                client.world.setThunderGradient(0.0f);
            }
            event.cancel();
        } else {
            client.world.setRainGradient(0.0f);
            client.world.setThunderGradient(0.0f);
        }
    }

    @Override
    public void onDisable() {
        boolean resetRain = Freecam.isPerspectiveRefreshEnabled();
        super.onDisable();
        try {
            Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        MinecraftClient observedClient = client;
        if (resetRain && Objects.requireNonNull(observedClient, "object reference is null").world == null) return;
        if (resetRain) {
            try {
                Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader());
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("Resolved class initialization failed", failure);
            }
            MinecraftClient rainClient = client;
            Objects.requireNonNull(Objects.requireNonNull(rainClient, "object reference is null").world, "object reference is null").setRainGradient(0.0f);
            try {
                Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader());
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("Resolved class initialization failed", failure);
            }
            observedClient = client;
        }
        Objects.requireNonNull(Objects.requireNonNull(observedClient, "object reference is null").world, "object reference is null").setThunderGradient(0.0f);
    }

    public static void appendBootstrapSettings(Ambience Module) {
        CatalogSettings.append(Module, customTime);
        CatalogSettings.append(Module, time);
        CatalogSettings.append(Module, modifyWeather);
        CatalogSettings.append(Module, weather);
        CatalogSettings.append(Module, fogColorBoolean);
        CatalogSettings.append(Module, fogColorColor);
        CatalogSettings.append(Module, gradient);
        CatalogSettings.append(Module, gradientColor);
        CatalogSettings.append(Module, fogDistance);
    }

    public static void assignBootstrapSettings(Ambience Module, ColorSetting preparedGradientColor, ColorSetting preparedFogColorColor, IntSetting preparedTime, BooleanSetting preparedFogColorBoolean, BooleanSetting preparedGradient, ChoiceSetting preparedWeather, IntSetting preparedFogDistance, BooleanSetting preparedCustomTime, BooleanSetting preparedModifyWeather) {
        gradientColor = preparedGradientColor;
        fogColorColor = preparedFogColorColor;
        time = preparedTime;
        fogColorBoolean = preparedFogColorBoolean;
        gradient = preparedGradient;
        weather = preparedWeather;
        fogDistance = preparedFogDistance;
        customTime = preparedCustomTime;
        modifyWeather = preparedModifyWeather;
    }
}
