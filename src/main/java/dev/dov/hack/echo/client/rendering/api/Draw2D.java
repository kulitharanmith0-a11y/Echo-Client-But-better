package dev.dov.hack.echo.client.rendering.api;

public interface Draw2D {
    void rect(float x, float y, float width, float height, int argb);

    void gradient(float x, float y, float width, float height, int topArgb, int bottomArgb);

    void text(String text, float x, float y, float scale, int argb);

    void texture(int textureId, float x, float y, float width, float height, float u0, float v0,
                 float u1, float v1, int argb);

    void scissor(float x, float y, float width, float height);

    void resetScissor();

    float width();

    float height();
}
