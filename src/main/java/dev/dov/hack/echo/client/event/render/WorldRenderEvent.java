package dev.dov.hack.echo.client.event.render;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.event.input.KeybindingEvent;
import dev.dov.hack.echo.client.render.world.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class WorldRenderEvent extends Event {
    private final MatrixStack matrices;
    private final float tickProgress;
    private final float dynamicDeltaTicks;
    private final Camera camera;
    private final GameRenderer gameRenderer;
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;
    private final WorldRenderer worldRenderer;
    private final WorldRenderContext renderContext;

    
    public WorldRenderEvent(MatrixStack matrices, float tickProgress, float dynamicDeltaTicks, Camera camera, GameRenderer gameRenderer, Matrix4f projectionMatrix, Matrix4f viewMatrix, WorldRenderer worldRenderer, WorldRenderContext renderContext) {
        this.matrices = matrices;
        this.tickProgress = tickProgress;
        KeybindingEvent.getInputSentinel();
        this.dynamicDeltaTicks = dynamicDeltaTicks;
        this.camera = camera;
        this.gameRenderer = gameRenderer;
        this.projectionMatrix = projectionMatrix;
        this.viewMatrix = viewMatrix;
        this.worldRenderer = worldRenderer;
        this.renderContext = renderContext;
        if (Event.getEventCounter() != 0) {
            KeybindingEvent.setInputSentinel(new String[1]);
        }
    }

    
    public MatrixStack getMatrices() { return matrices; }
    
    public float getTickProgress() { return tickProgress; }
    
    public float getDynamicDeltaTicks() { return dynamicDeltaTicks; }
    
    public Camera getCamera() { return camera; }
    
    public GameRenderer getGameRenderer() { return gameRenderer; }
    
    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
    
    public Matrix4f getViewMatrix() { return viewMatrix; }
    
    public WorldRenderer getWorldRenderer() { return worldRenderer; }
    
    public WorldRenderContext getRenderContext() { return renderContext; }
}
