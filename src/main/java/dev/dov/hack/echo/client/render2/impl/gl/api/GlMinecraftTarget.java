package dev.dov.hack.echo.client.rendering.impl.gl.api;

public final class GlMinecraftTarget implements dev.dov.hack.echo.client.rendering.api.FramebufferTarget {
    private final int colorTexture;
    private final int width;
    private final int height;
    private boolean bound;

    public GlMinecraftTarget(int colorTexture, int width, int height) {
        this.colorTexture = colorTexture;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    @Override
    public void bind() {
        this.bound = true;
    }

    @Override
    public void unbind() {
        this.bound = false;
    }

    @Override
    public int colorTexture() {
        return this.colorTexture;
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public boolean valid() {
        return this.width > 0 && this.height > 0;
    }

    public boolean isBound() {
        return this.bound;
    }
}
