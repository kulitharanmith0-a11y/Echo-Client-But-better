package dev.dov.hack.echo.client.render.world;

import dev.jadyen.hack.echo.natives.NativeCapabilities;
import dev.dov.hack.echo.client.particle.Particle;
import dev.dov.hack.echo.client.render.batch.VoxelBatch;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.utils.Rotation;
import dev.dov.hack.echo.client.chunk.ChunkController;

import dev.dov.hack.echo.client.render.batch.BoxBatch;
import dev.dov.hack.echo.client.render.batch.LineBatch;
import dev.dov.hack.echo.client.render.batch.ParticleBatch;
import dev.dov.hack.echo.client.render.batch.RenderBatch;
import dev.dov.hack.echo.client.render.batch.TracerBatch;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.particle.ParticleUv;
import dev.dov.hack.echo.client.gfx.TriangleBatch;
import dev.dov.hack.echo.client.gfx.buffer.Buffer;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.command.RenderPass;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureAccess;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureLayout;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import dev.dov.hack.echo.client.render.world.WorldGeometry;

public final class OverlayWorldGeometry extends WorldGeometry {
    private final RenderDevice device;
    private final BoxBatch boxes;
    private final TracerBatch tracers;
    private final LineBatch lines;
    private final TriangleBatch triangles;
    private final ParticleBatch particles;
    private final List<RenderBatch> batches = new ArrayList<>();
    private Texture overlayDepth;

    public OverlayWorldGeometry(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        boxes = new BoxBatch(device, cameraUniforms);
        tracers = new TracerBatch(device, cameraUniforms);
        lines = new LineBatch(device, cameraUniforms);
        triangles = new TriangleBatch(device, cameraUniforms);
        particles = new ParticleBatch(device, cameraUniforms);
        VoxelBatch voxels = new VoxelBatch(device, cameraUniforms);
        batches.add(boxes);
        batches.add(tracers);
        batches.add(lines);
        batches.add(triangles);
        batches.add(particles);
        batches.add(voxels);
        EchoClient.ChunkController.setListener(voxels);
        allocateDepth();
    }

    private void allocateDepth() {
        Framebuffer framebuffer = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance());
        overlayDepth = device.createTexture(TextureInfo.builder()
                .width(framebuffer.textureWidth).height(framebuffer.textureHeight)
                .format(device.depthFormat()).usage(8).build());
    }

    @Override
    public void beginFrame() {
        Framebuffer framebuffer = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance());
        if (overlayDepth.width != framebuffer.textureWidth || overlayDepth.height != framebuffer.textureHeight) {
            device.release(overlayDepth);
            allocateDepth();
        }
        batches.forEach(RenderBatch::reset);
    }

    public void renderTo(TextureView colorTarget) {
        CommandEncoder commands = EchoClient.commandEncoder;
        batches.forEach(batch -> batch.prepare(commands));
        commands.transitionTexture(colorTarget, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        commands.transitionTexture(overlayDepth.views[0], 8, TextureAccess.DEPTH_ATTACHMENT, TextureLayout.DEPTH_ATTACHMENT);
        double clearDepth = UiRenderer.NativeCapabilities.reversedDepth() ? 0.0 : 1.0;
        RenderPass pass = commands.beginRenderPass(colorTarget, OptionalInt.empty(), overlayDepth.views[0], OptionalDouble.of(clearDepth));
        device.setViewportToTexture(pass, colorTarget);
        batches.forEach(batch -> batch.draw(commands, pass));
        pass.end();
    }

    @Override
    public void destroy() {
        batches.forEach(RenderBatch::destroy);
        device.release(overlayDepth);
    }

    @Override
    public void drawBox(double x, double y, double z, double width, double height, double depth, int color) {
        boxes.add(x, y, z, width, height, depth, color);
    }

    @Override
    public void drawTracer(double x, double y, double z, int color) {
        tracers.add(x, y, z, color, EchoClient.worldRenderer.cameraPosition);
    }

    @Override
    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        lines.add(x1, y1, z1, x2, y2, z2, color, EchoClient.worldRenderer.cameraPosition);
    }

    @Override
    public void drawTriangle(double x1, double y1, double z1, int color1, double x2, double y2, double z2, int color2, double x3, double y3, double z3, int color3) {
        triangles.addTriangle(EchoClient.worldRenderer.cameraPosition, x1, y1, z1, color1, x2, y2, z2, color2, x3, y3, z3, color3);
    }

    @Override
    public void drawParticle(double x, double y, double z, float size, int color, ParticleUv uv, float rotation) {
        particles.add(x, y, z, size, color, uv, rotation, EchoClient.worldRenderer.cameraPosition);
    }
}
