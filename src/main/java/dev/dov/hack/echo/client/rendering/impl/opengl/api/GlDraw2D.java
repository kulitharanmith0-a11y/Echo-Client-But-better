package dev.dov.hack.echo.client.rendering.impl.opengl.api;

public final class GlDraw2D implements dev.dov.hack.echo.client.rendering.api.Draw2D {
    private final java.util.List<String> commands = new java.util.ArrayList<>();
    private GlState state = GlState.uiDefault();
    private final float viewportWidth;
    private final float viewportHeight;

    public GlDraw2D(float viewportWidth, float viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public void state(GlState state) {
        this.state = state == null ? GlState.uiDefault() : state;
    }

    public GlState state() {
        return this.state;
    }

    @Override
    public void rect(float x, float y, float width, float height, int argb) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        this.commands.add("rect " + x + "," + y + " " + width + "x" + height
                + " #" + Integer.toHexString(argb));
    }

    @Override
    public void gradient(float x, float y, float width, float height, int topArgb, int bottomArgb) {
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        this.commands.add("grad " + x + "," + y + " " + width + "x" + height
                + " #" + Integer.toHexString(topArgb) + "->" + Integer.toHexString(bottomArgb));
    }

    @Override
    public void text(String text, float x, float y, float scale, int argb) {
        if (text == null || text.isEmpty() || scale <= 0.0f) {
            return;
        }
        this.commands.add("text " + x + "," + y + " " + text);
    }

    @Override
    public void texture(int textureId, float x, float y, float width, float height,
                        float u0, float v0, float u1, float v1, int argb) {
        if (textureId == 0 || width <= 0.0f || height <= 0.0f) {
            return;
        }
        this.commands.add("tex " + textureId + " " + x + "," + y + " " + width + "x" + height);
    }

    @Override
    public void scissor(float x, float y, float width, float height) {
        float clampedWidth = Math.max(0.0f, Math.min(width, this.viewportWidth - x));
        float clampedHeight = Math.max(0.0f, Math.min(height, this.viewportHeight - y));
        this.commands.add("scissor " + x + "," + y + " " + clampedWidth + "x" + clampedHeight);
    }

    @Override
    public void resetScissor() {
        this.commands.add("scissor off");
    }

    @Override
    public float width() {
        return this.viewportWidth;
    }

    @Override
    public float height() {
        return this.viewportHeight;
    }

    public int commandCount() {
        return this.commands.size();
    }

    public void clear() {
        this.commands.clear();
    }

    public java.util.List<String> commands() {
        return java.util.List.copyOf(this.commands);
    }
}
