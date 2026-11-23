package com.blamejared.clumps.client.utils.network;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

public final class VelocityPackets {
    private VelocityPackets() { }

    
    public static int entityId(EntityVelocityUpdateS2CPacket packet) {
        return packet.getEntityId();
    }

    
    public static Vec3d velocity(EntityVelocityUpdateS2CPacket packet) {
        return packet.getVelocity();
    }
}
