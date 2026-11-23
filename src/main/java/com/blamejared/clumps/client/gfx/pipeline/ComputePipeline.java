package com.blamejared.clumps.client.gfx.pipeline;

import com.blamejared.clumps.client.gfx.resource.GpuResource;

public abstract class ComputePipeline extends GpuResource {
    public final ComputePipelineInfo info;

    public ComputePipeline(ComputePipelineInfo info) {
        this.info = info;
    }

    @Override
    public abstract void destroy();
}
