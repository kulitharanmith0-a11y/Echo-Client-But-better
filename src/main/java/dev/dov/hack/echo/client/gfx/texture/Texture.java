package dev.dov.hack.echo.client.gfx.texture;

import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureFormat;

import dev.dov.hack.echo.client.gfx.resource.GpuResource;

public abstract class Texture extends GpuResource {
    public final int width;
    public final int height;
    public final int depth;
    public final TextureFormat format;
    public final int usage;
    public final int mipCount;
    public TextureView[] views;
    public int stages = 16;

    public Texture(int width, int height, TextureFormat format, int usage, int mipCount) {
        this(width, height, 1, format, usage, mipCount);
    }

    public Texture(int width, int height, int depth, TextureFormat format, int usage, int mipCount) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.format = format;
        this.usage = usage;
        this.mipCount = mipCount;
    }
}
