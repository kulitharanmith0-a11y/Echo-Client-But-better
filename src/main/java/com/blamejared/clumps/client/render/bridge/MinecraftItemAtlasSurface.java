package com.blamejared.clumps.client.render.bridge;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.util.math.MatrixStack;

public final class MinecraftItemAtlasSurface {
    public static final int SIZE = 2048;

    private final MatrixStack matrices;
    private final ProjectionMatrix2 projection;
    private GpuTexture color;
    private GpuTextureView colorView;
    private GpuTexture depth;
    private GpuTextureView depthView;

    public MinecraftItemAtlasSurface() {
        this(new MatrixStack(), new ProjectionMatrix2("items", -1000.0F, 1000.0F, true));
    }

    MinecraftItemAtlasSurface(MatrixStack matrices, ProjectionMatrix2 projection) {
        this.matrices = matrices;
        this.projection = projection;
    }

    
    public void allocate() {
        allocate(RenderSystem.getDevice());
    }

    void allocate(GpuDevice device) {
        color = device.createTexture("items atlas", 12, TextureFormat.RGBA8, SIZE, SIZE, 1, 1);
        colorView = device.createTextureView(color);
        depth = device.createTexture("items atlas depth", 8, TextureFormat.DEPTH32, SIZE, SIZE, 1, 1);
        depthView = device.createTextureView(depth);
        device.createCommandEncoder().clearColorAndDepthTextures(color, 0, depth, 1.0);
    }

    public GpuTextureView colorView() {
        return colorView;
    }

    public boolean hasColorTexture() {
        return color != null;
    }

    
    public void renderTile(KeyedItemRenderState state, int x, int y, int tileSize, boolean clearExisting) {
        if (color == null || colorView == null || depth == null || depthView == null || state == null) {
            return;
        }
        int bottom = y + tileSize;
        if (clearExisting) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                    color, 0, depth, 1.0, x, SIZE - bottom, tileSize, tileSize);
        }
        GpuTextureView previousColorOverride = RenderSystem.outputColorTextureOverride;
        GpuTextureView previousDepthOverride = RenderSystem.outputDepthTextureOverride;
        RenderSystem.backupProjectionMatrix();
        matrices.push();
        try {
            matrices.translate(x + tileSize / 2.0F, y + tileSize / 2.0F, 0.0F);
            matrices.scale(tileSize, -tileSize, tileSize);
            RenderSystem.outputColorTextureOverride = colorView;
            RenderSystem.outputDepthTextureOverride = depthView;
            RenderSystem.setProjectionMatrix(projection.set(SIZE, SIZE), ProjectionType.ORTHOGRAPHIC);
            RenderSystem.enableScissorForRenderTypeDraws(x, SIZE - bottom, tileSize, tileSize);
            MinecraftClient client = MinecraftClient.getInstance();
            client.gameRenderer.getDiffuseLighting().setShaderLights(
                    state.isSideLit() ? DiffuseLighting.Type.ITEMS_3D : DiffuseLighting.Type.ITEMS_FLAT);
            state.render(matrices, client.gameRenderer.getEntityRenderCommandQueue(), 15728880, OverlayTexture.DEFAULT_UV, 0);
            client.gameRenderer.getEntityRenderDispatcher().render();
            client.getBufferBuilders().getEntityVertexConsumers().draw();
        } finally {
            RenderSystem.disableScissorForRenderTypeDraws();
            RenderSystem.outputColorTextureOverride = previousColorOverride;
            RenderSystem.outputDepthTextureOverride = previousDepthOverride;
            RenderSystem.restoreProjectionMatrix();
            matrices.pop();
        }
    }

    
    public void releaseTextures() {
        if (color == null) {
            return;
        }
        colorView.close();
        color.close();
        depthView.close();
        depth.close();
        color = null;
        colorView = null;
        depth = null;
        depthView = null;
    }
}
