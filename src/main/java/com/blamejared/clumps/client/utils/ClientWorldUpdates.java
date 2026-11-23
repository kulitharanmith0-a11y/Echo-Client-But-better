package com.blamejared.clumps.client.utils;

import net.minecraft.client.world.ClientWorld;

public final class ClientWorldUpdates {
    private ClientWorldUpdates() {
    }

    public static void setFixedTime(ClientWorld world, long time) {
        world.setTime(time, time, false);
    }
}
