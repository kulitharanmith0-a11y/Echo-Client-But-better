package com.blamejared.clumps.client.render.world;

import com.blamejared.clumps.client.gfx.texture.Texture;
import com.blamejared.clumps.client.gfx.texture.TextureInfo;
import com.blamejared.clumps.client.particle.Particle;
import com.blamejared.clumps.client.render.bridge.GlTextureBridge;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.utils.Rotation;

import com.blamejared.clumps.client.render.batch.BoxBatch;
import com.blamejared.clumps.client.render.batch.LineBatch;
import com.blamejared.clumps.client.render.batch.ParticleBatch;
import com.blamejared.clumps.client.render.batch.RenderBatch;
import com.blamejared.clumps.client.render.batch.TracerBatch;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.particle.ParticleUv;
import com.blamejared.clumps.client.gfx.TriangleBatch;
import com.blamejared.clumps.client.gfx.buffer.Buffer;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.command.RenderPass;
import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureAccess;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureLayout;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import com.blamejared.clumps.client.render.world.WorldGeometry;

public final class DepthTestedWorldGeometry extends WorldGeometry {
    private final RenderDevice device;
    private final List<RenderBatch> batches = new ArrayList<>();
    private final BoxBatch boxes;
    private final TracerBatch tracers;
    private final LineBatch lines;
    private final TriangleBatch triangles;
    private final ParticleBatch particles;
    private Object minecraftDepthSource;
    private TextureView minecraftDepthView;

    public DepthTestedWorldGeometry(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        boxes = new BoxBatch(device, cameraUniforms);
        tracers = new TracerBatch(device, cameraUniforms);
        lines = new LineBatch(device, cameraUniforms);
        triangles = new TriangleBatch(device, cameraUniforms);
        particles = new ParticleBatch(device, cameraUniforms);
        batches.add(boxes);
        batches.add(tracers);
        batches.add(lines);
        batches.add(triangles);
        batches.add(particles);
    }

    @Override
    public void beginFrame() {
        batches.forEach(RenderBatch::reset);
    }

    public void renderTo(TextureView colorTarget) {
        CommandEncoder commands = ClumpsClient.commandEncoder;
        batches.forEach(batch -> batch.prepare(commands));
        TextureView depth = depthTexture();
        commands.transitionTexture(colorTarget, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        commands.transitionTexture(depth, 8, TextureAccess.DEPTH_ATTACHMENT, TextureLayout.DEPTH_ATTACHMENT);
        RenderPass pass = commands.beginRenderPass(colorTarget, OptionalInt.of(0), depth, OptionalDouble.empty());
        device.setViewportToTexture(pass, colorTarget);
        batches.forEach(batch -> batch.draw(commands, pass));
        pass.end();
    }

    private TextureView depthTexture() {
        GpuTextureView source = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance()).getDepthAttachmentView();
        if (source != minecraftDepthSource) {
            minecraftDepthSource = source;
            minecraftDepthView = GlTextureBridge.importView(device, source, device.depthFormat());
        }
        return minecraftDepthView;
    }

    @Override
    public void destroy() {
        batches.forEach(RenderBatch::destroy);
    }

    @Override
    public void drawBox(double x, double y, double z, double width, double height, double depth, int color) {
        boxes.add(x, y, z, width, height, depth, color);
    }

    @Override
    public void drawTracer(double x, double y, double z, int color) {
        tracers.add(x, y, z, color, ClumpsClient.worldRenderer.cameraPosition);
    }

    @Override
    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        lines.add(x1, y1, z1, x2, y2, z2, color, ClumpsClient.worldRenderer.cameraPosition);
    }

    @Override
    public void drawTriangle(double x1, double y1, double z1, int color1, double x2, double y2, double z2, int color2, double x3, double y3, double z3, int color3) {
        triangles.addTriangle(ClumpsClient.worldRenderer.cameraPosition, x1, y1, z1, color1, x2, y2, z2, color2, x3, y3, z3, color3);
    }

    @Override
    public void drawParticle(double x, double y, double z, float size, int color, ParticleUv uv, float rotation) {
        particles.add(x, y, z, size, color, uv, rotation, ClumpsClient.worldRenderer.cameraPosition);
    }
}
