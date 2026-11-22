package dev.dov.hack.echo.client.gfx.pipeline;

import dev.dov.hack.echo.client.gfx.resource.GpuResource;

public abstract class GraphicsPipeline extends GpuResource {
    public final GraphicsPipelineInfo info;

    public GraphicsPipeline(GraphicsPipelineInfo info) {
        this.info = info;
    }

    @Override
    public abstract void destroy();
}
