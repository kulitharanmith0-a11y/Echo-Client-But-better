package dev.dov.hack.echo.client.gfx.sampler;

import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo.Filter;
import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo.MipmapMode;
import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo.WrapMode;

import dev.dov.hack.echo.client.gfx.resource.GpuResource;

public abstract class Sampler extends GpuResource {
    public final Filter filter;
    public final MipmapMode mipMode;
    public final WrapMode wrap;

    public Sampler(Filter filter, MipmapMode mipMode, WrapMode wrap) {
        this.filter = filter;
        this.mipMode = mipMode;
        this.wrap = wrap;
    }
}
