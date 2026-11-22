package dev.dov.hack.echo.client.gfx;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.impl.render.HandChams;
import dev.dov.hack.echo.client.render.texture.TexturePasses;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

public class HandChamsRenderer {
    private static final int CAPTURE_OUTLINE_COLOR = -1;
    private static final String FRAMEBUFFER_NAME = "hand_chams";

    private static boolean capturing;
    private static boolean flushingOutlines;
    private static boolean hasCapturedOutlines;
    private static Framebuffer framebuffer;

    
    public HandChamsRenderer() {
    }

    public static void beginCapture() {
        capturing = findEnabledFeature() != null;
    }

    public static void endCapture() {
        capturing = false;
    }

    
    public static boolean isFlushingOutlines() {
        return flushingOutlines;
    }

    public static Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public static int overrideOutlineColor(int outlineColor) {
        if (capturing && outlineColor == 0) {
            hasCapturedOutlines = true;
            return CAPTURE_OUTLINE_COLOR;
        }
        return outlineColor;
    }

    public static void flushCapturedOutlines() {
        if (!hasCapturedOutlines) {
            return;
        }
        hasCapturedOutlines = false;
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer mainFramebuffer = client.getFramebuffer();
        ensureFramebuffer(mainFramebuffer.textureWidth, mainFramebuffer.textureHeight);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        FramebufferOperations.clear(encoder, framebuffer);
        flushingOutlines = true;
        try {
            client.getBufferBuilders().getOutlineVertexConsumers().draw();
        } finally {
            flushingOutlines = false;
        }

        HandChams material = findEnabledFeature();
        if (material != null) {
            TexturePasses.applyMaterial(material, framebuffer);
        }
        if (EchoClient.externalRendererActive && EchoClient.externalRenderer != null && EchoClient.worldRenderer != null) {
            EchoClient.worldRenderer.queueExternalOverlay(framebuffer.getColorAttachmentView());
        } else {
            FramebufferOperations.blitColor(framebuffer, MinecraftClient.getInstance().getFramebuffer());
        }
    }

    public static void reset() {
        capturing = false;
        hasCapturedOutlines = false;
        if (framebuffer != null) {
            framebuffer.delete();
            framebuffer = null;
        }
        TexturePasses.releaseResources();
    }

    private static void ensureFramebuffer(int width, int height) {
        if (framebuffer == null) {
            framebuffer = FramebufferOperations.create(FRAMEBUFFER_NAME, width, height, true);
        } else if (framebuffer.textureWidth != width || framebuffer.textureHeight != height) {
            framebuffer.resize(width, height);
        }
    }

    private static HandChams findEnabledFeature() {
        if (EchoClient.eventsDisabled || EchoClient.ModuleManager == null) {
            return null;
        }
        HandChams Module = (HandChams) EchoClient.ModuleManager.findFeature(HandChams.class);
        return Module != null && Module.isEnabled() ? Module : null;
    }
}
