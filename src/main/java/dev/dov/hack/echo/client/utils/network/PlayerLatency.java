package dev.dov.hack.echo.client.utils.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import dev.dov.hack.echo.client.MinecraftAccess;

public final class PlayerLatency {
    public PlayerLatency() { }

    
    public static int milliseconds() {
        MinecraftClient client = MinecraftAccess.CLIENT;
        if (client.player == null || client.getNetworkHandler() == null) {
            return 0;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    
    public static int ticks() {
        return Math.round(milliseconds() / 50.0F);
    }
}
