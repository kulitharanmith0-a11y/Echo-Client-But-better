package dev.dov.hack.echo.client.gfx.opengl;

import dev.dov.hack.echo.client.gfx.buffer.BufferAccess;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureAccess;

public final class OpenGlMemoryBarriers {
    private OpenGlMemoryBarriers() {}

    public static int bufferConsumerBits(BufferAccess access) {
        return switch (access) {
            case UNDEFINED -> 0;
            case VERTEX_READ -> 0x0001;
            case INDEX_READ -> 0x0002;
            case UNIFORM_READ -> 0x0004;
            case INDIRECT_READ -> 0x0040;
            case STORAGE_READ, STORAGE_WRITE -> 0x2000;
            case TRANSFER_READ, TRANSFER_WRITE -> 0x0200;
        };
    }

    public static int textureConsumerBits(TextureAccess access) {
        return switch (access) {
            case UNDEFINED, PRESENT -> 0;
            case SAMPLED -> 0x0008;
            case STORAGE_READ, STORAGE_WRITE -> 0x0020;
            case COLOR_ATTACHMENT, DEPTH_ATTACHMENT -> 0x0400;
            case TRANSFER_READ, TRANSFER_WRITE -> 0x0100;
        };
    }
}
