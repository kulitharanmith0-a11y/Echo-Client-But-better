package dev.dov.hack.echo.client.modules.impl.combat;

import net.minecraft.network.packet.Packet;

final class DelayedKnockbackPacket {
    final Packet<?> packet;
    final long queuedAtMillis;

    DelayedKnockbackPacket(Packet<?> packet, long queuedAtMillis) {
        this.packet = packet;
        this.queuedAtMillis = queuedAtMillis;
    }
}
