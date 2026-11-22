package dev.dov.hack.echo.client.render.bridge;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.dov.hack.echo.client.gfx.surface.RenderTargetLease;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class MinecraftRenderPass implements RenderTargetLease {
    private final RenderPass pass;
    private final int width;
    private final int height;

    public MinecraftRenderPass(GpuTextureView color, OptionalInt clearColor, GpuTextureView depth, OptionalDouble clearDepth) {
        width = color.getWidth(0);
        height = color.getHeight(0);
        pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "Gui", color, clearColor, depth, clearDepth);
    }

    @Override public int width() { return width; }
    @Override public int height() { return height; }

    
    @Override public boolean isYFlipped() { return true; }

    @Override public void close() { pass.close(); }
}
