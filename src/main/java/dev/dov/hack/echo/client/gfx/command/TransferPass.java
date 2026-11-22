package dev.dov.hack.echo.client.gfx.command;

import dev.dov.hack.echo.client.gfx.buffer.BufferSlice;
import dev.dov.hack.echo.client.gfx.texture.TextureView;

public abstract class TransferPass {
    public abstract void uploadTexture(TextureView destination, BufferSlice source);
    public abstract void end();
}
