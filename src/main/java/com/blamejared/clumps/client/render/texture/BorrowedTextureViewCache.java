package com.blamejared.clumps.client.render.texture;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.gfx.texture.Texture;

import com.blamejared.clumps.client.render.bridge.GlTextureBridge;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.texture.TextureView;

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
