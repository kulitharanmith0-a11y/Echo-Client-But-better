package com.blamejared.clumps.client.render.world;

import com.blamejared.clumps.natives.NativeCapabilities;
import com.blamejared.clumps.client.render.ui.UiRenderer;

import com.blamejared.clumps.client.render.batch.BoxBatch;
import com.blamejared.clumps.client.render.batch.LineBatch;
import com.blamejared.clumps.client.render.batch.TracerBatch;
import com.blamejared.clumps.client.render.batch.ParticleBatch;
import com.blamejared.clumps.client.render.batch.VoxelBatch;
import com.blamejared.clumps.client.render.batch.RenderBatch;
import com.blamejared.clumps.client.render.batch.MultiTextureEffectBatch;
import com.blamejared.clumps.client.render.batch.MinecraftTextBatch;
import com.blamejared.clumps.client.render.world.DepthTestedWorldGeometry;
import com.blamejared.clumps.client.render.world.OverlayWorldGeometry;
import com.blamejared.clumps.client.render.texture.TexturePasses;
import com.blamejared.clumps.client.render.texture.ItemTextureCache;
import com.blamejared.clumps.client.render.bridge.GlTextureBridge;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderPass;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.render.ui.GuiDimensions;
import com.blamejared.clumps.client.render.ui.GuiProjection;
import com.blamejared.clumps.client.render.NativeMemoryWrites;
import com.blamejared.clumps.client.render.FramebufferBlur;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.gfx.EffectBatch;
import com.blamejared.clumps.client.gfx.IndexedTextureEffectBatch;
import com.blamejared.clumps.client.gui.ClientScreen;
import com.blamejared.clumps.client.gfx.buffer.Buffer;
import com.blamejared.clumps.client.gfx.buffer.BufferInfo;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.command.RenderPass;
import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.texture.Texture;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureAccess;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureFormat;
import com.blamejared.clumps.client.gfx.texture.TextureInfo;
import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureLayout;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class WorldRenderContext {
    private static final int UNIFORM_USAGE = 2;
    private static final long CAMERA_UNIFORM_BYTES = 160;
    private static final long COMPOSITION_UNIFORM_BYTES = 64;
    private static final int COLOR_TEXTURE_USAGE = 9;

    
    public Vec3d cameraPosition = Vec3d.ZERO;
    
    public Matrix4f viewProjectionMatrix;

    private final DepthTestedWorldGeometry depthTestedGeometry;
    private final OverlayWorldGeometry overlayGeometry;
    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final Buffer compositionUniforms;
    private final EffectBatch worldComposite;
    private final EffectBatch backgroundComposite;
    private final EffectBatch externalOverlayComposite;
    private final List<TextureView> externalOverlays = new ArrayList<>();
    private final List<GpuTextureView> externalOverlaySources = new ArrayList<>();
    private final List<TextureView> externalOverlayViews = new ArrayList<>();
    private Texture worldColorTexture;
    private GpuTextureView minecraftColorSource;
    private TextureView minecraftColorView;
    private boolean drewWorldOverlaysThisFrame;

    public WorldRenderContext(RenderDevice device) {
        this.device = device;
        cameraUniforms = device.createBuffer(BufferInfo.builder().usage(UNIFORM_USAGE)
                .size(CAMERA_UNIFORM_BYTES).mapped(true).dynamic(true).build());
        depthTestedGeometry = new DepthTestedWorldGeometry(device, cameraUniforms);
        overlayGeometry = new OverlayWorldGeometry(device, cameraUniforms);
        compositionUniforms = device.createBuffer(BufferInfo.builder().usage(UNIFORM_USAGE)
                .size(COMPOSITION_UNIFORM_BYTES).dynamic(true).mapped(true).build());
        worldComposite = device.NativeCapabilities().sampledImageArrays()
                ? new MultiTextureEffectBatch(device, compositionUniforms, false, false, true)
                : new IndexedTextureEffectBatch(device, compositionUniforms, false, false, true);
        backgroundComposite = device.NativeCapabilities().sampledImageArrays()
                ? new MultiTextureEffectBatch(device, compositionUniforms, true, false, false)
                : new IndexedTextureEffectBatch(device, compositionUniforms, true, false, false);
        externalOverlayComposite = device.NativeCapabilities().sampledImageArrays()
                ? new MultiTextureEffectBatch(device, compositionUniforms, false, false, false)
                : new IndexedTextureEffectBatch(device, compositionUniforms, false, false, false);
        allocateWorldColor();
    }

    private void allocateWorldColor() {
        Framebuffer framebuffer = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance());
        worldColorTexture = device.createTexture(TextureInfo.builder()
                .width(framebuffer.textureWidth).height(framebuffer.textureHeight)
                .format(TextureFormat.BGRA8Unorm).usage(COLOR_TEXTURE_USAGE).build());
        worldColorTexture.views[0].yFlipped = GlTextureBridge.isYFlipped();
    }

    
    public void setMatrices(Matrix4f view, Matrix4f projection) {
        Camera camera = MinecraftRenderState.getCamera(MinecraftClient.getInstance());
        cameraPosition = camera.getCameraPos();
        viewProjectionMatrix = new Matrix4f(projection).mul(view);
        long address = cameraUniforms.mappedAddress();
        NativeMemoryWrites.putMatrix4f(address, view);
        NativeMemoryWrites.putMatrix4f(address + 64L, projection);
        NativeMemoryWrites.putFloat3(address + 128L, (float) cameraPosition.x, (float) cameraPosition.y, (float) cameraPosition.z);
        Framebuffer framebuffer = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance());
        NativeMemoryWrites.putFloat2(address + 144L, framebuffer.textureWidth, framebuffer.textureHeight);
    }

    
    public void beginFrame() {
        drewWorldOverlaysThisFrame = false;
        Framebuffer framebuffer = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance());
        if (worldColorTexture.width != framebuffer.textureWidth
                || worldColorTexture.height != framebuffer.textureHeight) {
            device.release(worldColorTexture);
            allocateWorldColor();
        }
        updateCompositionProjection();
        depthTestedGeometry.beginFrame();
        overlayGeometry.beginFrame();
        worldComposite.reset();
        backgroundComposite.reset();
        externalOverlayComposite.reset();
    }

    public void queueExternalOverlay(GpuTextureView source) {
        if (source == null) {
            return;
        }
        int index = externalOverlaySources.indexOf(source);
        TextureView view;
        if (index >= 0) {
            view = externalOverlayViews.get(index);
        } else {
            view = GlTextureBridge.importColor(device, source);
            externalOverlaySources.add(source);
            externalOverlayViews.add(view);
        }
        if (!externalOverlays.contains(view)) {
            externalOverlays.add(view);
        }
    }

    public void clearExternalOverlays() {
        externalOverlays.clear();
        externalOverlayComposite.reset();
        drewWorldOverlaysThisFrame = false;
    }

    public boolean hasWorldOverlayContent() {
        return drewWorldOverlaysThisFrame || !externalOverlays.isEmpty();
    }

    public void compositeExternalOverlays(CommandEncoder commands, TextureView target, OptionalInt clearColor) {
        if (externalOverlays.isEmpty()) {
            return;
        }
        prepareExternalOverlays(commands);
        commands.transitionTexture(target, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        RenderPass pass = commands.beginRenderPass(target, clearColor, null, OptionalDouble.empty());
        device.setViewportToTexture(pass, target);
        externalOverlayComposite.draw(commands, pass);
        pass.end();
    }

    private void prepareExternalOverlays(CommandEncoder commands) {
        updateCompositionProjection();
        externalOverlayComposite.reset();
        if (externalOverlays.isEmpty()) {
            return;
        }
        float width = GuiDimensions.width();
        float height = GuiDimensions.height();
        for (TextureView overlay : externalOverlays) {
            commands.transitionTexture(overlay, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
            externalOverlayComposite.draw(overlay, 0.0F, 0.0F, width, height, 0.0F, 1.0F, 0.0F);
        }
        externalOverlayComposite.prepare(commands);
    }

    private void updateCompositionProjection() {
        long address = compositionUniforms.mappedAddress();
        Matrix4f projection = GlTextureBridge.isYFlipped()
                ? new Matrix4f().ortho(0.0F, GuiDimensions.width(), 0.0F, GuiDimensions.height(), -1000.0F, 1000.0F, device.isVulkan())
                : GuiProjection.create(device);
        NativeMemoryWrites.putMatrix4f(address, projection);
    }

    
    public void flush() {
        boolean hasGeometry = depthTestedGeometry.hasGeometry() || overlayGeometry.hasGeometry();
        drewWorldOverlaysThisFrame = hasGeometry;
        depthTestedGeometry.renderTo(worldColorTexture.views[0]);
        overlayGeometry.renderTo(worldColorTexture.views[0]);
        CommandEncoder commands = ClumpsClient.commandEncoder;
        if (!(MinecraftRenderState.getScreen() instanceof ClientScreen)) {
            ClumpsClient.uiRenderer.updateBackdrop(commands);
        }
        commands.transitionTexture(worldColorTexture.views[0], 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        worldComposite.draw(worldColorTexture.views[0], 0.0F, 0.0F, GuiDimensions.width(), GuiDimensions.height(), 0.0F, 1.0F, 0.0F);
        if (ClumpsClient.externalRendererActive || ClumpsClient.externalRendererRequested) {
            return;
        }
        beginTarget(commands, mainColorTexture(), OptionalInt.empty());
    }

    public void compositeClientOverlaysOnly(CommandEncoder commands, TextureView target) {
        updateCompositionProjection();
        prepareExternalOverlays(commands);
        worldComposite.prepare(commands);
        commands.transitionTexture(target, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        RenderPass pass = commands.beginRenderPass(target, OptionalInt.of(0x00000000), null, OptionalDouble.empty());
        device.setViewportToTexture(pass, target);
        if (!externalOverlays.isEmpty()) {
            externalOverlayComposite.draw(commands, pass);
        }
        worldComposite.draw(commands, pass);
        pass.end();
    }

    
    public void beginTarget(CommandEncoder commands, TextureView target, OptionalInt clearColor) {
        updateCompositionProjection();
        worldComposite.prepare(commands);
        commands.transitionTexture(target, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        RenderPass pass = commands.beginRenderPass(target, clearColor, null, OptionalDouble.empty());
        device.setViewportToTexture(pass, target);
        worldComposite.draw(commands, pass);
        pass.end();
    }

    
    public void compositeToTarget(CommandEncoder commands, TextureView target, TextureView source) {
        compositeToTarget(commands, target, source, true);
    }

    public void compositeToTarget(CommandEncoder commands, TextureView target, TextureView source, boolean includeWorldOverlays) {
        updateCompositionProjection();
        backgroundComposite.reset();
        commands.transitionTexture(source, 2, TextureAccess.SAMPLED, TextureLayout.SHADER_READ_ONLY);
        backgroundComposite.draw(source, 0.0F, 0.0F, GuiDimensions.width(), GuiDimensions.height(), 0.0F, 1.0F, 0, GlTextureBridge.isYFlipped(), 0.0F);
        backgroundComposite.prepare(commands);
        if (includeWorldOverlays) {
            prepareExternalOverlays(commands);
            worldComposite.prepare(commands);
        }
        commands.transitionTexture(target, 2, TextureAccess.COLOR_ATTACHMENT, TextureLayout.COLOR_ATTACHMENT);
        RenderPass pass = commands.beginRenderPass(target, OptionalInt.of(0xFF000000), null, OptionalDouble.empty());
        device.setViewportToTexture(pass, target);
        backgroundComposite.draw(commands, pass);
        if (includeWorldOverlays) {
            if (!externalOverlays.isEmpty()) {
                externalOverlayComposite.draw(commands, pass);
            }
            worldComposite.draw(commands, pass);
        }
        pass.end();
    }

    
    public TextureView mainColorTexture() {
        GpuTextureView source = MinecraftRenderState.getFramebuffer(MinecraftClient.getInstance()).getColorAttachmentView();
        if (source != minecraftColorSource) {
            minecraftColorSource = source;
            minecraftColorView = GlTextureBridge.importColor(device, source);
        }
        return minecraftColorView;
    }

    
    public void destroy() {
        externalOverlays.clear();
        externalOverlaySources.clear();
        externalOverlayViews.clear();
        worldComposite.destroy();
        backgroundComposite.destroy();
        externalOverlayComposite.destroy();
        depthTestedGeometry.destroy();
        overlayGeometry.destroy();
        if (worldColorTexture != null) {
            try { device.release(worldColorTexture); } catch (Throwable ignored) {}
            worldColorTexture = null;
        }
        try { device.release(cameraUniforms); } catch (Throwable ignored) {}
        try { device.release(compositionUniforms); } catch (Throwable ignored) {}
    }

    public DepthTestedWorldGeometry getDepthTestedGeometry() {
        return depthTestedGeometry;
    }

    public OverlayWorldGeometry getOverlayGeometry() {
        return overlayGeometry;
    }
}
