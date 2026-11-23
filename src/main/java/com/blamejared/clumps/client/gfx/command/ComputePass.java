package com.blamejared.clumps.client.gfx.command;

import com.blamejared.clumps.client.gfx.buffer.BufferSlice;
import com.blamejared.clumps.client.gfx.pipeline.ComputePipeline;
import com.blamejared.clumps.client.gfx.sampler.Sampler;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureAccess;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import com.blamejared.clumps.client.gfx.util.RenderChecks;

public abstract class ComputePass {
    public abstract void setPipeline(ComputePipeline pipeline);

    public void setBuffer(int binding, BufferSlice slice) {
        RenderChecks.require((slice.buffer().usage & 4) == 0, "buffer must not be a staging buffer");
    }

    public void setTexture(int binding, TextureView view) {
        RenderChecks.require(view.access == TextureAccess.SAMPLED
                        || view.access == TextureAccess.STORAGE_READ
                        || view.access == TextureAccess.STORAGE_WRITE, "texture bound at {} must be in a SAMPLED or STORAGE state", binding);
    }

    public abstract void setSampler(int binding, Sampler sampler);
    public abstract void setPushConstants(long address, int bytes);
    public abstract void dispatch(int x, int y, int z);
    public abstract void end();
}
