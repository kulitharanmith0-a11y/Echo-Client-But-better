package dev.dov.hack.echo.client.rendering.impl.vk.api;

public final class VkMinecraftTarget implements dev.dov.hack.echo.client.rendering.api.FramebufferTarget {
    private final long image;
    private final long view;
    private final int width;
    private final int height;
    private boolean bound;

    public VkMinecraftTarget(long image, long view, int width, int height) {
        this.image = image;
        this.view = view;
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
        return (int) (this.view & 0x7FFFFFFF);
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
        return this.image != 0L && this.width > 0 && this.height > 0;
    }

    public boolean isBound() {
        return this.bound;
    }
}
