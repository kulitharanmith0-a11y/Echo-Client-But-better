package dev.dov.hack.echo.client.gfx.pipeline;

import dev.dov.hack.echo.client.gfx.resource.GpuResource;

public abstract class ComputePipeline extends GpuResource {
    public final ComputePipelineInfo info;

    public ComputePipeline(ComputePipelineInfo info) {
        this.info = info;
    }

    @Override
    public abstract void destroy();
}
