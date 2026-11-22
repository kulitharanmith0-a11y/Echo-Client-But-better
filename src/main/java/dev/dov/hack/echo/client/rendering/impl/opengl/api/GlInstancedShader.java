package dev.dov.hack.echo.client.rendering.impl.opengl.api;

public final class GlInstancedShader {
    private final GlShader shader;
    private final int instanceStride;
    private int capacity;

    public GlInstancedShader(GlShader shader, int instanceStride, int capacity) {
        this.shader = shader;
        this.instanceStride = Math.max(1, instanceStride);
        this.capacity = Math.max(1, capacity);
    }

    public boolean ready() {
        return this.shader != null && this.shader.isLinked() && this.instanceStride > 0;
    }

    public boolean canFit(int instances) {
        return this.ready() && instances > 0 && instances <= this.capacity;
    }

    public void ensureCapacity(int instances) {
        if (instances > this.capacity) {
            this.capacity = instances;
        }
    }

    public GlShader shader() {
        return this.shader;
    }

    public int instanceStride() {
        return this.instanceStride;
    }

    public int capacity() {
        return this.capacity;
    }
}
