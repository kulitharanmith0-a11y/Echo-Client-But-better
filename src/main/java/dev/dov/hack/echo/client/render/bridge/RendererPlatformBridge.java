package dev.dov.hack.echo.client.render.bridge;

import dev.dov.hack.echo.client.gfx.util.ClientResources;
import dev.dov.hack.echo.client.gfx.resource.ResourceLoader;

public final class RendererPlatformBridge implements ResourceLoader {
    @Override
    public String loadText(String path) {
        return ClientResources.readText(path);
    }

    @Override
    public byte[] loadBytes(String path) {
        return ClientResources.readBytes(path);
    }
}
