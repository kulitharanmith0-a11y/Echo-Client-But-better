package dev.dov.hack.echo.client.rendering.impl.opengl.api;

public final class GlState {
    private boolean blend = true;
    private boolean depthTest;
    private boolean cull;
    private int blendSrc = 770;
    private int blendDst = 771;

    public static GlState uiDefault() {
        GlState state = new GlState();
        state.blend = true;
        state.depthTest = false;
        state.cull = false;
        return state;
    }

    public static GlState worldDefault() {
        GlState state = new GlState();
        state.blend = true;
        state.depthTest = true;
        state.cull = true;
        return state;
    }

    public GlState blend(boolean blend) {
        this.blend = blend;
        return this;
    }

    public GlState depthTest(boolean depthTest) {
        this.depthTest = depthTest;
        return this;
    }

    public GlState cull(boolean cull) {
        this.cull = cull;
        return this;
    }

    public GlState blendFunc(int src, int dst) {
        this.blendSrc = src;
        this.blendDst = dst;
        return this;
    }

    public boolean isBlend() {
        return this.blend;
    }

    public boolean isDepthTest() {
        return this.depthTest;
    }

    public boolean isCull() {
        return this.cull;
    }

    public int blendSrc() {
        return this.blendSrc;
    }

    public int blendDst() {
        return this.blendDst;
    }
}
