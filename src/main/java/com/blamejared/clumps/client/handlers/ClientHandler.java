package com.blamejared.clumps.client.handlers;

import com.blamejared.clumps.client.MinecraftAccess;
import net.minecraft.client.MinecraftClient;

public class ClientHandler implements MinecraftAccess {
    protected static final MinecraftClient CLIENT = MinecraftClient.getInstance();
}
