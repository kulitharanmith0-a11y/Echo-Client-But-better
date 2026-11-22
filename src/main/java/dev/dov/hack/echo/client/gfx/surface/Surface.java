package dev.dov.hack.echo.client.gfx.surface;

import dev.dov.hack.echo.client.gfx.surface.SurfaceInfo.PresentMode;

import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.queue.CommandQueue;
import dev.dov.hack.echo.client.gfx.resource.GpuResource;
import dev.dov.hack.echo.client.gfx.texture.TextureView;

public abstract class Surface extends GpuResource {
    public abstract void resize(int width, int height, PresentMode presentMode);
    public abstract boolean needsResize();
    public abstract TextureView acquireTexture();
    public abstract void present(CommandQueue queue, CommandEncoder commands);
}
