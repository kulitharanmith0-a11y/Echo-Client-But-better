package dev.dov.hack.echo.client.rendering.api;

public record CrossTexture(int id, int width, int height, float u0, float v0, float u1, float v1) {
    public boolean valid() {
        return this.id != 0 && this.width > 0 && this.height > 0;
    }

    public float uSpan() {
        return this.u1 - this.u0;
    }

    public float vSpan() {
        return this.v1 - this.v0;
    }

    public float aspect() {
        return this.height <= 0 ? 1.0f : this.width / (float) this.height;
    }
}
