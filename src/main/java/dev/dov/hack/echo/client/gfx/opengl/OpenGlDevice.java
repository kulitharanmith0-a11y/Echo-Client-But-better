package dev.dov.hack.echo.client.gfx.opengl;

import dev.dov.hack.echo.client.gfx.buffer.Buffer;
import dev.dov.hack.echo.client.gfx.buffer.BufferInfo;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.command.TransferPass;
import dev.dov.hack.echo.client.gfx.device.DeviceCapabilities;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.pipeline.ComputePipeline;
import dev.dov.hack.echo.client.gfx.pipeline.ComputePipelineInfo;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipeline;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipelineInfo;
import dev.dov.hack.echo.client.gfx.queue.CommandQueue;
import dev.dov.hack.echo.client.gfx.queue.QueueInfo;
import dev.dov.hack.echo.client.gfx.resource.ResourceLoader;
import dev.dov.hack.echo.client.gfx.sampler.Sampler;
import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo;
import dev.dov.hack.echo.client.gfx.surface.Surface;
import dev.dov.hack.echo.client.gfx.surface.SurfaceInfo;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureFormat;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL30C.*;

public class OpenGlDevice extends RenderDevice {
    public final GlFramebufferAccess framebuffers;
    public final int glslVersion;
    public final boolean baseInstanceSupported;

    public OpenGlDevice(GlFramebufferAccess framebuffers, ResourceLoader resources, DeviceCapabilities NativeCapabilities) {
        super(resources, NativeCapabilities);
        this.framebuffers = framebuffers;
        glslVersion = glGetInteger(GL_MAJOR_VERSION) * 100 + glGetInteger(GL_MINOR_VERSION) * 10;
        baseInstanceSupported = GL.getCapabilities().glDrawArraysInstancedBaseInstance != 0;
    }

    @Override public CommandEncoder commandEncoder() { return new OpenGlCommandEncoder(this); }
    @Override public TransferPass beginTransfer() { return new OpenGlTransferPass(); }
    @Override public GraphicsPipeline createGraphicsPipeline(GraphicsPipelineInfo info) { return new OpenGlGraphicsPipeline(this, info); }
    @Override public ComputePipeline createComputePipeline(ComputePipelineInfo info) { return new OpenGlComputePipeline(this, info); }

    @Override
    public Buffer createBuffer(BufferInfo info) {
        return new OpenGlBuffer(info.usage, info.size, info.dynamic, info.mapped, info.explicitFlush);
    }

    @Override
    public Texture createTexture(TextureInfo info) {
        if ((info.usage & 2) != 0 && !NativeCapabilities().storageImages()) {
            throw new IllegalStateException("Device does not support shader write textures (requires GL 4.2)");
        }
        return new OpenGlTexture(info.width, info.height, info.depth, info.format, info.usage, info.mipCount);
    }

    @Override
    public Sampler createSampler(SamplerInfo info) {
        return new OpenGlSampler(info.filter, info.mipMode, info.wrap);
    }

    public TextureView importTextureView(int textureId, int width, int height, TextureFormat format, int internalFormat) {
        return OpenGlTexture.importTexture(width, height, format, textureId, internalFormat).views[0];
    }

    public static DeviceCapabilities queryCapabilities(boolean reversedDepth) {
        int version = glGetInteger(GL_MAJOR_VERSION) * 100 + glGetInteger(GL_MINOR_VERSION) * 10;
        boolean gl43 = version >= 430;
        return new DeviceCapabilities(gl43, gl43, false, version >= 420, reversedDepth);
    }

    @Override public boolean isVulkan() { return false; }
    @Override public TextureFormat depthFormat() { return OpenGlTextureFormats.fromInternalFormat(framebuffers.depthInternalFormat()); }

    @Override
    public CommandQueue createQueue(QueueInfo info) {
        int supported = OpenGlQueue.supportedCapabilities(this);
        if ((supported & info.NativeCapabilities) != info.NativeCapabilities) {
            throw new IllegalStateException("OpenGL cannot provide a queue with the requested NativeCapabilities: " + info.NativeCapabilities);
        }
        return new OpenGlQueue(this, supported);
    }

    @Override public Surface createSurface(SurfaceInfo info) { return new OpenGlSurface(info); }
}
