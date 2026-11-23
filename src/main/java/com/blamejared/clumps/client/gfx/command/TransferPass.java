package com.blamejared.clumps.client.gfx.command;

import com.blamejared.clumps.client.gfx.buffer.BufferSlice;
import com.blamejared.clumps.client.gfx.texture.TextureView;

public abstract class TransferPass {
    public abstract void uploadTexture(TextureView destination, BufferSlice source);
    public abstract void end();
}
