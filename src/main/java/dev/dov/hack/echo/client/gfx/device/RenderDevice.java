package dev.dov.hack.echo.client.gfx.device;

import dev.dov.hack.echo.client.gfx.buffer.Buffer;
import dev.dov.hack.echo.client.gfx.buffer.BufferInfo;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.command.RenderPass;
import dev.dov.hack.echo.client.gfx.command.TransferPass;
import dev.dov.hack.echo.client.gfx.pipeline.ComputePipeline;
import dev.dov.hack.echo.client.gfx.pipeline.ComputePipelineInfo;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipeline;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipelineInfo;
import dev.dov.hack.echo.client.gfx.queue.CommandQueue;
import dev.dov.hack.echo.client.gfx.queue.QueueInfo;
import dev.dov.hack.echo.client.gfx.resource.GpuResource;
import dev.dov.hack.echo.client.gfx.resource.ResourceLoader;
import dev.dov.hack.echo.client.gfx.sampler.Sampler;
import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo;
import dev.dov.hack.echo.client.gfx.surface.Surface;
import dev.dov.hack.echo.client.gfx.surface.SurfaceInfo;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureFormat;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
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
