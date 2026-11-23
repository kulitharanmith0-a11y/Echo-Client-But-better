package com.blamejared.clumps.client.render.batch;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.render.NativeMemoryWrites;

import com.blamejared.clumps.client.render.batch.RenderBatch;
import com.blamejared.clumps.client.gfx.buffer.Buffer;
import com.blamejared.clumps.client.gfx.buffer.BufferAccess;
import com.blamejared.clumps.client.gfx.buffer.BufferInfo;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.command.RenderPass;
import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipeline;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipelineInfo;
import com.blamejared.clumps.client.gfx.pipeline.GraphicsPipelineInfo.PrimitiveTopology;
import com.blamejared.clumps.client.gfx.vertex.VertexFormat;
import net.minecraft.util.math.Vec3d;

public final class TracerBatch implements RenderBatch {
    private static final int STRIDE = 16;
    private static final int VERTEX_BUFFER_USAGE = 1;
    private static final int VERTEX_STAGE = 1;
    private final RenderDevice device;
    private final Buffer worldUniforms;
    private final GraphicsPipeline pipeline;
    private Buffer instances;
    private long capacity = 256L;
    private int count;

    public TracerBatch(RenderDevice device, Buffer worldUniforms) {
        this.device = device;
        this.worldUniforms = worldUniforms;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, "shaders/wgsl/3d/tracer.wgsl")
                .vertexAttribute(0, VertexFormat.Float3).vertexAttribute(1, VertexFormat.Int);
        info.topology = PrimitiveTopology.TRIANGLE_STRIP;
        pipeline = device.createGraphicsPipeline(info);
        instances = device.createBuffer(BufferInfo.builder().usage(VERTEX_BUFFER_USAGE)
                .size(capacity * STRIDE).mapped(true).dynamic(true).build());
    }

    
    public void add(double x, double y, double z, int color, Vec3d camera) {
        if (count >= capacity) {
            grow();
        }
        long address = instances.mappedAddress() + (long) count * STRIDE;
        NativeMemoryWrites.putFloat3(address, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z));
        NativeMemoryWrites.putInt(address + 12, color);
        count++;
    }

    private void grow() {
        capacity <<= 1;
        instances = device.reallocateMappedBuffer(instances, capacity * STRIDE, (long) count * STRIDE);
    }

    @Override
    public void prepare(CommandEncoder encoder) {
        encoder.transitionBuffer(instances.wholeSlice(), VERTEX_STAGE, BufferAccess.VERTEX_READ);
    }

    @Override
    public void draw(CommandEncoder encoder, RenderPass pass) {
        pass.setPipeline(pipeline);
        pass.setVertexBuffer(instances.wholeSlice());
        pass.setBuffer(0, worldUniforms.wholeSlice());
        pass.draw(4, count, 0);
        count = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instances);
    }
}
