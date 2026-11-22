package dev.dov.hack.echo.client.render.font;

public final class GlyphAtlasRows {
    public static final int WIDTH = 2048;

    private GlyphAtlasRows() {}

    public static int rowsFor(int texelCount) {
        return Math.max(1, (texelCount + WIDTH - 1) / WIDTH);
    }
}
