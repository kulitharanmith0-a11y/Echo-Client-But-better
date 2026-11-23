package com.blamejared.clumps.client.gfx.device;

import com.blamejared.clumps.client.gfx.buffer.Buffer;
import com.blamejared.clumps.client.gfx.buffer.BufferInfo;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.command.RenderPass;
import com.blamejared.clumps.client.gfx.command.TransferPass;
import com.blamejared.clumps.client.gfx.pipeline.ComputePipeline;
import com.blamejared.clumps.client.gfx.pipeline.ComputePipelineInfo;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipeline;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipelineInfo;
import com.blamejared.clumps.client.gfx.queue.CommandQueue;
import com.blamejared.clumps.client.gfx.queue.QueueInfo;
import com.blamejared.clumps.client.gfx.resource.GpuResource;
import com.blamejared.clumps.client.gfx.resource.ResourceLoader;
import com.blamejared.clumps.client.gfx.sampler.Sampler;
import com.blamejared.clumps.client.gfx.sampler.SamplerInfo;
import com.blamejared.clumps.client.gfx.surface.Surface;
import com.blamejared.clumps.client.gfx.surface.SurfaceInfo;
import com.blamejared.clumps.client.gfx.texture.Texture;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureFormat;
import com.blamejared.clumps.client.gfx.texture.TextureInfo;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import org.lwjgl.system.MemoryUtil;

public abstract class RenderDevice {
    public final ResourceLoader resourceLoader;
    private final DeviceCapabilities NativeCapabilities;

    protected RenderDevice(ResourceLoader resourceLoader, DeviceCapabilities NativeCapabilities) {
        this.resourceLoader = resourceLoader;
        this.NativeCapabilities = NativeCapabilities;
    }

    
    public DeviceCapabilities NativeCapabilities() { return NativeCapabilities; }

    public void beginFrame() {
    }

    public abstract CommandEncoder commandEncoder();
    public abstract TransferPass beginTransfer();
    public abstract GraphicsPipeline createGraphicsPipeline(GraphicsPipelineInfo info);
    public abstract ComputePipeline createComputePipeline(ComputePipelineInfo info);
    public abstract Buffer createBuffer(BufferInfo info);
    public abstract Texture createTexture(TextureInfo info);
    public abstract Sampler createSampler(SamplerInfo info);
    public abstract CommandQueue createQueue(QueueInfo info);
    public abstract Surface createSurface(SurfaceInfo info);
    public abstract boolean isVulkan();
    public abstract TextureFormat depthFormat();

    public void setViewportToTexture(RenderPass pass, TextureView view) {
        pass.setViewport(0, 0, view.width(0), view.height(0));
    }

    
    public void release(GpuResource resource) { resource.destroy(); }

    
    public void destroyImmediately(GpuResource resource) { resource.destroy(); }

    
    public Buffer reallocateMappedBuffer(Buffer source, long newSize, long copyBytes) {
        if (!source.mapped) {
            throw new UnsupportedOperationException("realloc requires a mapped buffer");
        }
        Buffer replacement = createBuffer(BufferInfo.builder().usage(source.usage).size(newSize)
                .dynamic(source.dynamic).mapped(true).build());
        MemoryUtil.memCopy(source.mappedAddress(), replacement.mappedAddress(), Math.min(copyBytes, newSize));
        release(source);
        return replacement;
    }
}
