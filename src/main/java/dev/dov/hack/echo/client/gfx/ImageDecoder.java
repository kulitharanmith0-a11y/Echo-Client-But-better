package dev.dov.hack.echo.client.gfx;

import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.gfx.util.ClientResources;
import dev.dov.hack.echo.client.gfx.buffer.Buffer;
import dev.dov.hack.echo.client.gfx.buffer.BufferInfo;
import dev.dov.hack.echo.client.gfx.command.TransferPass;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo.TextureFormat;
import dev.dov.hack.echo.client.gfx.texture.TextureInfo;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class ImageDecoder {
    private ImageDecoder() {
    }

    public static TextureView loadTexture(RenderDevice device, String path) {
        byte[] encoded = ClientResources.readBytes(path);
        if (encoded == null) {
            throw new IllegalArgumentException("Image not found: " + path);
        }
        return decodeTexture(device, encoded);
    }

    public static TextureView decodeTexture(RenderDevice device, byte[] bytes) {
        initializeFontDefinitions();
        ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
        try {
            encoded.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
                if (pixels == null) {
                    throw new IllegalStateException("Failed to decode image: " + STBImage.stbi_failure_reason());
                }
                try {
                    return uploadRgba(device, pixels, width.get(0), height.get(0));
                } finally {
                    STBImage.stbi_image_free(pixels);
                }
            }
        } finally {
            MemoryUtil.memFree((java.nio.Buffer) encoded);
        }
    }

    
    public static TextureView uploadRgba(RenderDevice device, ByteBuffer pixels, int width, int height) {
        Texture texture = device.createTexture(TextureInfo.builder()
                .width(width).height(height).format(TextureFormat.RGBA8Unorm).usage(5).build());
        long byteCount = (long) width * height * 4L;
        Buffer staging = device.createBuffer(BufferInfo.builder()
                .usage(4).size(byteCount).mapped(true).build());
        MemoryUtil.memCopy(MemoryUtil.memAddress(pixels), staging.mappedAddress(), byteCount);
        TransferPass transfer = device.beginTransfer();
        transfer.uploadTexture(texture.views[0], staging.wholeSlice());
        transfer.end();
        device.release(staging);
        return texture.views[0];
    }

    
    private static void initializeFontDefinitions() {
        try {
            Class.forName(FontAsset.class.getName(), true, FontAsset.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }
}
