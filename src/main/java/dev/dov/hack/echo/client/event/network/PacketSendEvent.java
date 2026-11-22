package dev.dov.hack.echo.client.event.network;

import dev.dov.hack.echo.client.event.Event;
import net.minecraft.network.packet.Packet;

public class PacketSendEvent extends Event {
    private final Packet<?> packet;
    private boolean flush;

    
    public PacketSendEvent(Packet<?> packet) {
        this.flush = true;
        this.packet = packet;
    }

    
    public Packet<?> getPacket() {
        return packet;
    }

    
    public boolean shouldFlush() {
        return flush;
    }

    
    public void setFlush(boolean flush) {
        this.flush = flush;
    }
}
