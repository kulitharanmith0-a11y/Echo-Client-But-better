package dev.dov.hack.echo.client.gfx.command;

import dev.dov.hack.echo.client.gfx.buffer.BufferSlice;
import dev.dov.hack.echo.client.gfx.pipeline.ComputePipeline;
import dev.dov.hack.echo.client.gfx.sampler.Sampler;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureAccess;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import dev.dov.hack.echo.client.gfx.util.RenderChecks;

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
