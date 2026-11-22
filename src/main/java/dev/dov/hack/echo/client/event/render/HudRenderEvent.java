package dev.dov.hack.echo.client.event.render;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.render.bridge.DrawContextAdapter;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class HudRenderEvent extends Event {
    private MatrixStack matrices;
    private float tickDelta;
    private DrawContextAdapter drawContext;
    private UiRenderer renderer;
    private TextureView texture;

    
    public HudRenderEvent(MatrixStack matrices, float tickDelta, DrawContextAdapter drawContext, UiRenderer renderer, TextureView texture) {
        this.matrices = matrices;
        this.tickDelta = tickDelta;
        this.drawContext = drawContext;
        this.renderer = renderer;
        this.texture = texture;
    }

    
    public int getWidth() {
        return (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 2.0);
    }

    
    public Matrix4f createIdentityMatrix() { return new Matrix4f(); }

    
    public int getHeight() {
        return (int) Math.ceil(MinecraftClient.getInstance().getWindow().getFramebufferHeight() / 2.0);
    }

    
    public MatrixStack getMatrices() { return matrices; }

    
    public float getTickDelta() { return tickDelta; }

    
    public DrawContextAdapter getDrawContext() { return drawContext; }

    
    public UiRenderer getRenderer() { return renderer; }

    
    public TextureView getTexture() { return texture; }
}
