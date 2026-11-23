package dev.dov.hack.echo.client.gfx.svg;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import dev.dov.hack.echo.client.render.svg.SvgColorMode;
import dev.dov.hack.echo.client.render.svg.SvgRasterizer;
import dev.dov.hack.echo.client.render.svg.SvgTextureKey;
import dev.dov.hack.echo.client.gfx.util.ClientResources;
import dev.dov.hack.echo.client.gfx.ImageDecoder;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public final class SvgTextureCache implements AutoCloseable {
    private final RenderDevice device;
    private final Logger logger;
    private final Rasterizer rasterizer = new Rasterizer();
    private final Map<SvgTextureKey, TextureView> textures = new HashMap<>();
    private final Map<SvgTextureKey, CompletableFuture<ByteBuffer>> pending = new HashMap<>();
    private final ExecutorService rasterThread = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("Worker-Raster").daemon(true).factory());

    public SvgTextureCache(RenderDevice device, Logger logger) {
        this.device = device;
        this.logger = logger;
    }

    public TextureView getOrSchedule(String resourcePath, int width, int height, SvgColorMode mode) {
        int safeWidth = Math.max(1, Math.min(1024, width));
        int safeHeight = Math.max(1, Math.min(1024, height));
        SvgTextureKey key = new SvgTextureKey(resourcePath, safeWidth, safeHeight, mode);
        TextureView texture = textures.get(key);
        if (texture != null) {
            return texture;
        }
        if (rasterThread.isShutdown()) {
            return null;
        }
        CompletableFuture<ByteBuffer> task = pending.computeIfAbsent(key, this::rasterizeAsync);
        if (!task.isDone() || task.isCompletedExceptionally() || task.isCancelled()) {
            return null;
        }
        ByteBuffer pixels;
        try {
            pixels = task.join();
        } catch (Throwable ignored) {
            pending.remove(key);
            return null;
        }
        pending.remove(key);
        if (pixels == null) {
            return null;
        }
        try {
            texture = ImageDecoder.uploadRgba(device, pixels, key.width(), key.height());
            textures.put(key, texture);
            return texture;
        } catch (Throwable ignored) {
            return null;
        } finally {
            MemoryUtil.memFree((java.nio.Buffer) pixels);
        }
    }

    private CompletableFuture<ByteBuffer> rasterizeAsync(SvgTextureKey key) {
        return CompletableFuture.supplyAsync(() -> rasterizer.rasterize(key), rasterThread);
    }

    public void invalidateSized() {
        for (TextureView texture : textures.values()) {
            device.release(texture.texture);
        }
        textures.clear();
        pending.values().forEach(task -> task.thenAccept(pixels -> MemoryUtil.memFree((java.nio.Buffer) pixels)));
        pending.clear();
    }

    public int cachedTextureCount() {
        return textures.size();
    }

    @Override
    public void close() {
        rasterThread.shutdownNow();
        invalidateSized();
        rasterizer.clear();
    }

    private static final class Rasterizer {
        private final Map<String, SVGDocument> documents = new ConcurrentHashMap<>();

        ByteBuffer rasterize(SvgTextureKey key) {
            ByteBuffer pixels = MemoryUtil.memAlloc(key.width() * key.height() * 4);
            try {
                SVGDocument document = documents.computeIfAbsent(key.resourcePath(), this::load);
                SvgRasterizer.downsample(SvgRasterizer.renderSupersampled(document, key.width(), key.height()), key.width(), key.height(), key.colorMode(), pixels);
                return pixels.flip();
            } catch (RuntimeException | Error failure) {
                MemoryUtil.memFree((java.nio.Buffer) pixels);
                throw failure;
            }
        }

        SVGDocument load(String resourcePath) {
            byte[] contents = ClientResources.readBytes(resourcePath);
            if (contents == null) {
                throw new IllegalArgumentException("SVG not found: " + resourcePath);
            }
            SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(contents), URI.create("resource:/" + resourcePath.replace(' ', '_')), LoaderContext.createDefault());
            if (document == null) {
                throw new IllegalStateException("Failed to parse SVG: " + resourcePath);
            }
            return document;
        }

        void clear() {
            documents.clear();
        }
    }
}
