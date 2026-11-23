package com.blamejared.clumps.client.gfx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.modules.impl.render.SkyShaders;
import com.blamejared.clumps.client.render.bridge.GlTextureBridge;
import com.blamejared.clumps.client.render.NativeMemoryWrites;
import com.blamejared.clumps.client.gfx.buffer.Buffer;
import com.blamejared.clumps.client.gfx.buffer.BufferAccess;
import com.blamejared.clumps.client.gfx.buffer.BufferInfo;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.command.RenderPass;
import com.blamejared.clumps.client.gfx.command.TransferPass;
import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipeline;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipelineInfo;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipelineInfo.PrimitiveTopology;
import com.blamejared.clumps.client.gfx.sampler.Sampler;
import com.blamejared.clumps.client.gfx.sampler.SamplerInfo;
import com.blamejared.clumps.client.gfx.sampler.SamplerInfo.WrapMode;
import com.blamejared.clumps.client.gfx.texture.Texture;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureAccess;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureFormat;
import com.blamejared.clumps.client.gfx.texture.TextureInfo;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureLayout;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

public final class FullscreenEffects {
    private static final int UNIFORM_BYTES = 112;
    private static final int MATRIX_BYTES = 64;
    private static final int NOISE_EDGE = 64;
    private static final int NOISE_BYTES = 1048576;
    private static final String SHADER_PATH = "shaders/wgsl/sky.wgsl";
    private static final String FAILURE_PREFIX = "Sky shader failed, falling back to vanilla sky: ";

    private static final Matrix4f projection = new Matrix4f();
    private static final Matrix4f modelViewProjection = new Matrix4f();
    private static GraphicsPipeline pipeline;
    private static Buffer uniformBuffer;
    private static Buffer indexBuffer;
    private static Texture noiseTexture;
    private static Sampler noiseSampler;
    private static GpuTextureView cachedMinecraftView;
    private static TextureView cachedColorView;
    private static boolean failed;
    private static long previousUpdateNanos;
    private static float elapsedTime;

    private FullscreenEffects() {
    }

    public static void captureProjection(Matrix4fc matrix) {
        projection.set(matrix);
    }

    public static boolean renderCustomSky() {
        if (failed || ClumpsClient.renderDevice == null || ClumpsClient.commandEncoder == null) {
            return false;
        }
        try {
            RenderDevice device = ClumpsClient.renderDevice;
            if (pipeline == null) {
                initializeResources(device);
            }
            long now = System.nanoTime();
            if (previousUpdateNanos != 0L) {
                float deltaSeconds = (float) (now - previousUpdateNanos) / 1.0E9F;
                elapsedTime += Math.min(deltaSeconds, 0.1F) * SkyShaders.getSpeed();
            }
            previousUpdateNanos = now;
            updateUniforms();

            CommandEncoder encoder = ClumpsClient.commandEncoder;
            TextureView target = mainColorView(device);
            encoder.transitionBuffer(indexBuffer.wholeSlice(), 1, BufferAccess.INDEX_READ);
            encoder.transitionTexture(noiseTexture.views[0], 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
            encoder.transitionTexture(target, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
            RenderPass pass = encoder.beginRenderPass(target, OptionalInt.empty(), null, OptionalDouble.empty());
            device.setViewportToTexture(pass, target);
            pass.setPipeline(pipeline);
            pass.setBuffer(0, uniformBuffer.wholeSlice());
            pass.setTexture(1, noiseTexture.views[0]);
            pass.setSampler(2, noiseSampler);
            pass.setIndexBuffer(indexBuffer.wholeSlice());
            pass.drawIndexed(36, 1, 0);
            pass.end();
            return true;
        } catch (Exception exception) {
            failed = true;
            return false;
        }
    }

    private static void initializeResources(RenderDevice device) {
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, SHADER_PATH);
        info.blendEnabled = false;
        info.cullEnabled = false;
        info.depthTestEnabled = false;
        info.depthWriteEnabled = false;
        info.topology = PrimitiveTopology.TRIANGLE_LIST;
        pipeline = device.createGraphicsPipeline(info);
        uniformBuffer = device.createBuffer(BufferInfo.builder().usage(2).size(UNIFORM_BYTES)
                .mapped(true).dynamic(true).build());
        indexBuffer = device.createBuffer(BufferInfo.builder().usage(32).size(144L).mapped(true).build());
        noiseSampler = device.createSampler(SamplerInfo.builder().wrap(WrapMode.REPEAT).build());
        noiseTexture = createNoiseTexture(device);
        long address = indexBuffer.mappedAddress();
        for (int face = 0; face < 6; face++) {
            int first = face * 4;
            int[] indices = {first, first + 1, first + 2, first, first + 2, first + 3};
            for (int triangleIndex = 0; triangleIndex < 6; triangleIndex++) {
                NativeMemoryWrites.putInt(address + (face * 6 + triangleIndex) * 4L, indices[triangleIndex]);
            }
        }
    }

    private static Texture createNoiseTexture(RenderDevice device) {
        Texture texture = device.createTexture(TextureInfo.builder().width(NOISE_EDGE).height(NOISE_EDGE)
                .depth(NOISE_EDGE).format(TextureFormat.RGBA8Unorm).usage(5).build());
        Buffer staging = device.createBuffer(BufferInfo.builder().usage(4).size(NOISE_BYTES).mapped(true).build());
        Random random = new Random(24301L);
        byte[] bytes = new byte[NOISE_BYTES];
        random.nextBytes(bytes);
        long address = staging.mappedAddress();
        for (int index = 0; index < bytes.length; index++) {
            MemoryUtil.memPutByte(address + index, bytes[index]);
        }
        TransferPass transfer = device.beginTransfer();
        transfer.uploadTexture(texture.views[0], staging.wholeSlice());
        transfer.end();
        device.release(staging);
        return texture;
    }

    private static void updateUniforms() {
        long address = uniformBuffer.mappedAddress();
        NativeMemoryWrites.putMatrix4f(address, modelViewProjection.set(projection).mul(RenderSystem.getModelViewMatrix()));
        NativeMemoryWrites.putFloat4(address + MATRIX_BYTES, SkyShaders.getPrimaryChannel(0), SkyShaders.getPrimaryChannel(1), SkyShaders.getPrimaryChannel(2), SkyShaders.getBrightness());
        NativeMemoryWrites.putFloat4(address + 80L, SkyShaders.getSecondaryChannel(0), SkyShaders.getSecondaryChannel(1), SkyShaders.getSecondaryChannel(2), SkyShaders.getStars());
        NativeMemoryWrites.putFloat4(address + 96L, elapsedTime, SkyShaders.getShaderMode(), 0.0F, 0.0F);
    }

    private static TextureView mainColorView(RenderDevice device) {
        GpuTextureView source = MinecraftAccess.CLIENT.getFramebuffer().getColorAttachmentView();
        if (source != cachedMinecraftView) {
            cachedMinecraftView = source;
            cachedColorView = GlTextureBridge.importColor(device, source);
        }
        return cachedColorView;
    }

    public static void releaseResources() {
        RenderDevice device = ClumpsClient.renderDevice;
        if (device != null) {
            if (uniformBuffer != null) device.release(uniformBuffer);
            if (indexBuffer != null) device.release(indexBuffer);
            if (noiseTexture != null) device.release(noiseTexture);
            if (noiseSampler != null) device.release(noiseSampler);
            if (pipeline != null) device.release(pipeline);
        }
        uniformBuffer = null;
        indexBuffer = null;
        noiseTexture = null;
        noiseSampler = null;
        pipeline = null;
        cachedMinecraftView = null;
        cachedColorView = null;
        failed = false;
        previousUpdateNanos = 0L;
    }
}
