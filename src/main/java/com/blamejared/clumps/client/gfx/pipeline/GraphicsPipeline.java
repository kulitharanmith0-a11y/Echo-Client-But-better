package com.blamejared.clumps.client.gfx.pipeline;

import com.blamejared.clumps.client.gfx.resource.GpuResource;

public abstract class GraphicsPipeline extends GpuResource {
    public final GraphicsPipelineInfo info;

    public GraphicsPipeline(GraphicsPipelineInfo info) {
        this.info = info;
    }

    @Override
    public abstract void destroy();
}
