package dev.dov.hack.echo.client.rendering.api;

public interface FramebufferTarget {
    void bind();

    void unbind();

    int colorTexture();

    int width();

    int height();

    boolean valid();

    default float aspect() {
        return this.height() <= 0 ? 1.0f : this.width() / (float) this.height();
    }
}
