package dev.dov.hack.echo.client.utils.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import dev.dov.hack.echo.client.MinecraftAccess;

public final class Packets implements MinecraftAccess {
    private static final double RELATIVE_COORDINATE_SCALE = 4096.0;

    
    public Packets() { }

    
    public static void send(Packet<?> packet) {
        MinecraftClient client = CLIENT;
        if (client.getNetworkHandler() == null) {
            return;
        }
        client.getNetworkHandler().sendPacket(packet);
    }

    
    public static double applyRelativeDelta(double coordinate, long delta) {
        return delta == 0L ? coordinate
                : (Math.round(coordinate * RELATIVE_COORDINATE_SCALE) + delta) / RELATIVE_COORDINATE_SCALE;
    }
}
