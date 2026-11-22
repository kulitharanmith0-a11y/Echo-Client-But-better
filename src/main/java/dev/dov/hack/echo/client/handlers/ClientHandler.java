package dev.dov.hack.echo.client.handlers;

import dev.dov.hack.echo.client.MinecraftAccess;
import net.minecraft.client.MinecraftClient;

public class ClientHandler implements MinecraftAccess {
    protected static final MinecraftClient CLIENT = MinecraftClient.getInstance();
}
