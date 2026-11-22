package dev.dov.hack.echo.client.render.batch;

import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.particle.Particle;
import dev.dov.hack.echo.client.particle.ParticleManager;
import dev.dov.hack.echo.client.render.NativeMemoryWrites;
import dev.dov.hack.echo.client.utils.Rotation;

import dev.dov.hack.echo.client.render.batch.RenderBatch;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.particle.ParticleUv;
import dev.dov.hack.echo.client.gfx.buffer.Buffer;
import dev.dov.hack.echo.client.gfx.buffer.BufferAccess;
import dev.dov.hack.echo.client.gfx.buffer.BufferInfo;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.command.RenderPass;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipelineInfo.BlendMode;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipeline;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipelineInfo;
import dev.dov.hack.echo.client.gfx.sampler.Sampler;
import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import dev.dov.hack.echo.client.gfx.vertex.VertexFormat;
import net.minecraft.util.math.Vec3d;

public class ParticleBatch implements RenderBatch {
    private static final int STRIDE = 40;
    private static final int VERTEX_USAGE = 1;
    private static final int VERTEX_STAGE = 1;
    private static final String SHADER = "shaders/wgsl/3d/particle.wgsl";

    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final GraphicsPipeline pipeline;
    private final Sampler sampler;
    private Buffer instances;
    private long capacity = 256;
    private int count;

    public ParticleBatch(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        this.cameraUniforms = cameraUniforms;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, SHADER)
                .vertexAttribute(0, VertexFormat.Float3)
                .vertexAttribute(1, VertexFormat.Float)
                .vertexAttribute(2, VertexFormat.Int)
                .vertexAttribute(3, VertexFormat.Float4)
                .vertexAttribute(4, VertexFormat.Float);
        info.blendMode = BlendMode.ADDITIVE;
        info.depthWriteEnabled = false;
        info.cullEnabled = true;
        pipeline = device.createGraphicsPipeline(info);
        sampler = device.createSampler(SamplerInfo.builder().build());
        instances = device.createBuffer(BufferInfo.builder().usage(VERTEX_USAGE).size(capacity * STRIDE)
                .mapped(true).dynamic(true).build());
    }

    public void add(double x, double y, double z, float size, int color, ParticleUv uv, float rotation, Vec3d camera) {
        if (count >= capacity) {
            grow();
        }
        long address = instances.mappedAddress() + (long) count * STRIDE;
        NativeMemoryWrites.putFloat3(address, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z));
        NativeMemoryWrites.putFloat(address + 12, size);
        NativeMemoryWrites.putInt(address + 16, color);
        NativeMemoryWrites.putFloat4(address + 20, uv.minU(), uv.minV(), uv.maxU(), uv.maxV());
        NativeMemoryWrites.putFloat(address + 36, rotation);
        count++;
    }

    private void grow() {
        capacity <<= 1;
        instances = device.reallocateMappedBuffer(instances, capacity * STRIDE, (long) count * STRIDE);
    }

    @Override
    public void prepare(CommandEncoder commands) {
        commands.transitionBuffer(instances.wholeSlice(), VERTEX_STAGE, BufferAccess.VERTEX_READ);
    }

    @Override
    public void draw(CommandEncoder commands, RenderPass pass) {
        TextureView atlas = EchoClient.particleManager.getTexture();
        if (count == 0 || atlas == null) {
            count = 0;
            return;
        }
        pass.setPipeline(pipeline);
        pass.setVertexBuffer(instances.wholeSlice());
        pass.setBuffer(0, cameraUniforms.wholeSlice());
        pass.setTexture(1, atlas);
        pass.setSampler(2, sampler);
        pass.draw(4, count, 0);
        count = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instances);
        device.release(sampler);
    }
}
