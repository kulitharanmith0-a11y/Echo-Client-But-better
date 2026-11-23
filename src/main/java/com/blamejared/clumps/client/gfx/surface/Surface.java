package com.blamejared.clumps.client.gfx.surface;

import com.blamejared.clumps.client.gfx.surface.SurfaceInfo.PresentMode;

import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.queue.CommandQueue;
import com.blamejared.clumps.client.gfx.resource.GpuResource;
import com.blamejared.clumps.client.gfx.texture.TextureView;

public abstract class Surface extends GpuResource {
    public abstract void resize(int width, int height, PresentMode presentMode);
    public abstract boolean needsResize();
    public abstract TextureView acquireTexture();
    public abstract void present(CommandQueue queue, CommandEncoder commands);
}
