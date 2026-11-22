package dev.dov.hack.echo.client.render.texture;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.gfx.texture.Texture;

import dev.dov.hack.echo.client.render.bridge.GlTextureBridge;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.texture.TextureView;

final class BorrowedTextureViewCache {
    private GpuTextureView source;
    private TextureView view;

    TextureView get(RenderDevice device, GpuTextureView nextSource) {
        if (source != nextSource) {
            source = nextSource;
            view = GlTextureBridge.importColor(device, nextSource);
        }
        return view;
    }
}
