package dev.dov.hack.echo.client.render;

import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.render.bridge.GlTextureBridge;

import dev.dov.hack.echo.client.render.bridge.GlFramebufferBridge;
import dev.dov.hack.echo.client.render.bridge.GlFramebufferOperations;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.mixin.accessor.GameRendererAccessor;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.util.Identifier;

public final class FramebufferBlur implements AutoCloseable {
    private static final Identifier EFFECT = Identifier.ofVanilla("blur");
    private static final String FRAMEBUFFER_NAME = "screen_frame";

    private Framebuffer framebuffer;
    private TextureView colorTexture;
    private TextureView depthTexture;
    private boolean active;
    private GpuTextureView importedColorAttachment;

    public void captureMainFramebuffer() {
        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        if (framebuffer == null) {
            framebuffer = new SimpleFramebuffer(FRAMEBUFFER_NAME, main.textureWidth, main.textureHeight, true);
        } else if (framebuffer.textureWidth != main.textureWidth || framebuffer.textureHeight != main.textureHeight) {
            framebuffer.resize(main.textureWidth, main.textureHeight);
        }
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                main.getColorAttachment(), framebuffer.getColorAttachment(), 0, 0, 0, 0, 0, main.textureWidth, main.textureHeight);

        if (framebuffer.getColorAttachmentView() != importedColorAttachment) {
            importedColorAttachment = framebuffer.getColorAttachmentView();
            colorTexture = GlTextureBridge.importColor(EchoClient.renderDevice, importedColorAttachment);
            depthTexture = GlTextureBridge.importView(EchoClient.renderDevice, framebuffer.getDepthAttachmentView(), EchoClient.renderDevice.depthFormat());
        }
        active = true;
    }

    public void applyBlur() {
        PostEffectProcessor effect = MinecraftClient.getInstance().getShaderLoader()
                .loadPostEffect(EFFECT, DefaultFramebufferSet.MAIN_ONLY);
        if (effect == null) {
            return;
        }
        effect.render(framebuffer, ((GameRendererAccessor) MinecraftClient.getInstance().gameRenderer).getPool());
    }

    public void resetFrame() {
        active = false;
    }

    @Override
    public void close() {
        if (framebuffer != null) {
            framebuffer.delete();
        }
    }

    public Framebuffer framebuffer() { return framebuffer; }
    public TextureView colorTexture() { return colorTexture; }
    public TextureView depthTexture() { return depthTexture; }
    public boolean isActive() { return active; }
}
