package com.blamejared.clumps.client.gfx.sampler;

import com.blamejared.clumps.client.gfx.sampler.SamplerInfo.Filter;
import com.blamejared.clumps.client.gfx.sampler.SamplerInfo.MipmapMode;
import com.blamejared.clumps.client.gfx.sampler.SamplerInfo.WrapMode;

import com.blamejared.clumps.client.gfx.resource.GpuResource;

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
