package com.blamejared.clumps.client.gfx.opengl;

import com.blamejared.clumps.client.gfx.buffer.Buffer;
import com.blamejared.clumps.client.gfx.buffer.BufferInfo;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.command.TransferPass;
import com.blamejared.clumps.client.gfx.device.DeviceCapabilities;
import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.pipeline.ComputePipeline;
import com.blamejared.clumps.client.gfx.pipeline.ComputePipelineInfo;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipeline;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipelineInfo;
import com.blamejared.clumps.client.gfx.queue.CommandQueue;
import com.blamejared.clumps.client.gfx.queue.QueueInfo;
import com.blamejared.clumps.client.gfx.resource.ResourceLoader;
import com.blamejared.clumps.client.gfx.sampler.Sampler;
import com.blamejared.clumps.client.gfx.sampler.SamplerInfo;
import com.blamejared.clumps.client.gfx.surface.Surface;
import com.blamejared.clumps.client.gfx.surface.SurfaceInfo;
import com.blamejared.clumps.client.gfx.texture.Texture;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureFormat;
import com.blamejared.clumps.client.gfx.texture.TextureInfo;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL30C.*;

public class OpenGlDevice extends RenderDevice {
    public final GlFramebufferAccess framebuffers;
    public final int glslVersion;
    public final boolean baseInstanceSupported;

    public OpenGlDevice(GlFramebufferAccess framebuffers, ResourceLoader resources, DeviceCapabilities NativeCapabilities) {
        super(resources, NativeCapabilities);
        this.framebuffers = framebuffers;
        int major = glGetInteger(GL_MAJOR_VERSION);
        int minor = glGetInteger(GL_MINOR_VERSION);
        int rawVersion = major * 100 + minor * 10;
        if (rawVersion < 330) {
            rawVersion = 330;
        } else if (rawVersion > 460) {
            rawVersion = 460;
        }
        glslVersion = rawVersion;
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
        var caps = GL.getCapabilities();
        boolean hasStorageImages = version >= 420 && caps != null && caps.OpenGL42
                && caps.glTexStorage2D != 0L && caps.glTexStorage3D != 0L
                && caps.glBindImageTexture != 0L && caps.glMemoryBarrier != 0L;
        boolean hasCompute = version >= 430 && hasStorageImages && caps.OpenGL43
                && caps.glDispatchCompute != 0L && caps.glTextureView != 0L;
        boolean hasMultiDrawIndirect = version >= 430 && caps != null && caps.OpenGL43
                && caps.glMultiDrawArraysIndirect != 0L;
        return new DeviceCapabilities(hasMultiDrawIndirect, hasCompute, false, hasStorageImages, reversedDepth);
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
