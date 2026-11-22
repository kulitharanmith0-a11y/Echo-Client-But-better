package dev.dov.hack.echo.client.gfx.texture;

import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureFormat;

import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureAccess;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureLayout;

import dev.dov.hack.echo.client.gfx.resource.GpuResource;

public abstract class TextureView extends GpuResource {
    public final Texture texture;
    public final int baseMipLevel;
    public final int mipCount;
    public final TextureFormat format;
    public TextureAccess access = TextureAccess.UNDEFINED;
    public TextureLayout layout = TextureLayout.UNDEFINED;
    public int stages = 16;
    public boolean yFlipped;
    public boolean keepGeneralLayout;

    public TextureView(Texture texture, int baseMipLevel, int mipCount) {
        this.texture = texture;
        this.baseMipLevel = baseMipLevel;
        this.mipCount = mipCount;
        this.format = texture.format;
    }

    public int width(int relativeMipLevel) {
        return Math.max(1, texture.width >> (baseMipLevel + relativeMipLevel));
    }

    public int height(int relativeMipLevel) {
        return Math.max(1, texture.height >> (baseMipLevel + relativeMipLevel));
    }

    public int depth(int relativeMipLevel) {
        return Math.max(1, texture.depth >> (baseMipLevel + relativeMipLevel));
    }

    @Override
    public abstract void destroy();
}
