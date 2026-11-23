package com.blamejared.clumps.client.render.texture;

import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.texture.Texture;

public record TexturePassResources(Texture outline, Texture horizontalBlur, Texture verticalBlur) {
    public void release(RenderDevice device) {
        device.release(outline);
        device.release(horizontalBlur);
        device.release(verticalBlur);
    }
}
