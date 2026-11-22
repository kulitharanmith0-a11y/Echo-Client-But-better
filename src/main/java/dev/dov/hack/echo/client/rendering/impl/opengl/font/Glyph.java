package dev.dov.hack.echo.client.rendering.impl.opengl.font;

public record Glyph(char character, float u0, float v0, float u1, float v1, float advance) {
    public float width(float scale) {
        return (this.u1 - this.u0) * scale;
    }
}
