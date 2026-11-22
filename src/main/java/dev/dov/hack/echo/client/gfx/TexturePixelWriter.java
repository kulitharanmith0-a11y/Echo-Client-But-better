package dev.dov.hack.echo.client.gfx;

import java.nio.ByteBuffer;

public final class TexturePixelWriter {
    private TexturePixelWriter() {
    }

    
    public static void putRgba(ByteBuffer destination, int argb) {
        destination.put((byte) ((argb >> 16) & 255));
        destination.put((byte) ((argb >> 8) & 255));
        destination.put((byte) (argb & 255));
        destination.put((byte) ((argb >> 24) & 255));
    }
}
