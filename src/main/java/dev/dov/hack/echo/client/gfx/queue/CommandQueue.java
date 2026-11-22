package dev.dov.hack.echo.client.gfx.queue;

import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.resource.GpuResource;

public abstract class CommandQueue extends GpuResource {
    public abstract int NativeCapabilities();
    public abstract CommandEncoder beginCommands();
    public abstract void submit(CommandEncoder commands);
    public abstract void waitIdle();
}
