package dev.dov.hack.echo.client;
import dev.dov.hack.echo.client.platform.Platform;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public interface MinecraftAccess {
    MinecraftClient CLIENT = MinecraftClient.getInstance();

    
    static void setTickSpeed(float multiplier) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        RenderTickCounter counter = client.getRenderTickCounter();
        if (counter instanceof Platform.TickControl control) {
            control.setTickSpeedMultiplier(multiplier);
        }
    }
}
