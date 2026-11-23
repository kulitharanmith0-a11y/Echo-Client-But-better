package com.blamejared.clumps.client.gfx.queue;

import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.resource.GpuResource;

public abstract class CommandQueue extends GpuResource {
    public abstract int NativeCapabilities();
    public abstract CommandEncoder beginCommands();
    public abstract void submit(CommandEncoder commands);
    public abstract void waitIdle();
}
