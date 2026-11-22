package dev.dov.hack.echo.client.render.batch;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.render.bridge.GlTextureBridge;
import dev.dov.hack.echo.client.render.NativeMemoryWrites;

import dev.dov.hack.echo.client.render.batch.RenderBatch;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.dov.hack.echo.client.gfx.buffer.Buffer;
import dev.dov.hack.echo.client.gfx.buffer.BufferAccess;
import dev.dov.hack.echo.client.gfx.buffer.BufferInfo;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.command.RenderPass;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipeline;
import dev.dov.hack.echo.client.gfx.pipeline.GraphicsPipelineInfo;
import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo.Filter;
import dev.dov.hack.echo.client.gfx.sampler.Sampler;
import dev.dov.hack.echo.client.gfx.sampler.SamplerInfo;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import dev.dov.hack.echo.client.gfx.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.BakedGlyphImpl;
import net.minecraft.client.font.GlyphProvider;
import net.minecraft.text.StyleSpriteSource;
import java.lang.reflect.Field;

public class MinecraftTextBatch implements RenderBatch {
    private static final int STRIDE = 44;
    private static final int VERTEX_USAGE = 1;
    private static final int VERTEX_STAGE = 1;
    private static final String SHADER = "shaders/wgsl/mc_text.wgsl";

    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final GraphicsPipeline pipeline;
    private final Sampler sampler;
    private Buffer instances;
    private int capacity = 1024;
    private int firstInstance;
    private int pendingCount;
    private int firstTextureRun;
    private final List<TextureView> textureRuns = new ArrayList<>();
    private final IntList runStarts = new IntArrayList();
    private TextureView currentTexture;
    private final Map<GpuTextureView, TextureView> importedTextures = new HashMap<>();

    public MinecraftTextBatch(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        this.cameraUniforms = cameraUniforms;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, SHADER)
                .vertexAttribute(0, VertexFormat.Float4).vertexAttribute(1, VertexFormat.Float4)
                .vertexAttribute(2, VertexFormat.Int2).vertexAttribute(3, VertexFormat.Float);
        pipeline = device.createGraphicsPipeline(info);
        instances = device.createBuffer(BufferInfo.builder().usage(VERTEX_USAGE).size((long) STRIDE * capacity)
                .dynamic(true).mapped(true).build());
        sampler = device.createSampler(SamplerInfo.builder().filter(Filter.NEAREST).build());
    }

    private static Field FONTS_FIELD;
    private static Field GLYPH_MIN_X, GLYPH_MIN_Y, GLYPH_MAX_X, GLYPH_MAX_Y;
    private static Field GLYPH_MIN_U, GLYPH_MIN_V, GLYPH_MAX_U, GLYPH_MAX_V;
    private static Field GLYPH_TEXTURE_VIEW;

    static {
        try {
            FONTS_FIELD = MinecraftClient.class.getDeclaredField("textRenderer");
        } catch (Exception ignored) {}
        try {
            Class<?> cls = BakedGlyphImpl.class;
            GLYPH_MIN_X = getField(cls, "minX"); GLYPH_MIN_Y = getField(cls, "minY");
            GLYPH_MAX_X = getField(cls, "maxX"); GLYPH_MAX_Y = getField(cls, "maxY");
            GLYPH_MIN_U = getField(cls, "minU"); GLYPH_MIN_V = getField(cls, "minV");
            GLYPH_MAX_U = getField(cls, "maxU"); GLYPH_MAX_V = getField(cls, "maxV");
            GLYPH_TEXTURE_VIEW = getField(cls, "textureView");
        } catch (Exception ignored) {}
    }

    private static Field getField(Class<?> cls, String name) throws Exception {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static float glyphField(Field f, BakedGlyphImpl g) {
        try { return f.getFloat(g); } catch (Exception e) { return 0f; }
    }

    private static GlyphProvider getFonts() {
        try {
            Field f = MinecraftClient.getInstance().textRenderer.getClass().getDeclaredField("fonts");
            f.setAccessible(true);
            Object fontsObj = f.get(MinecraftClient.getInstance().textRenderer);
            java.lang.reflect.Method m = fontsObj.getClass().getMethod("getGlyphs", StyleSpriteSource.class);
            return (GlyphProvider) m.invoke(fontsObj, StyleSpriteSource.DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException("Cannot access textRenderer.fonts", e);
        }
    }

    public BakedGlyph[] bake(CharSequence text) {
        GlyphProvider provider = getFonts();
        BakedGlyph[] glyphs = new BakedGlyph[text.length()];
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character != '\n') glyphs[index] = provider.get(character);
        }
        return glyphs;
    }

    public float draw(BakedGlyph[] glyphs, float x, float y, float size, int color, float depth) {
        float scale = size / 8.0f;
        float lineStart = x;
        for (BakedGlyph glyph : glyphs) {
            if (glyph == null) {
                x = lineStart;
                y += fontHeight(size);
                continue;
            }
            if (glyph instanceof BakedGlyphImpl drawable) {
                addGlyph(drawable, x, y, scale, color, depth);
            }
            x += glyph.getMetrics().getAdvance() * scale;
        }
        return x;
    }

    public float measure(BakedGlyph[] glyphs, float size) {
        float scale = size / 8.0f;
        float lineWidth = 0.0f;
        float maximum = 0.0f;
        for (BakedGlyph glyph : glyphs) {
            if (glyph == null) {
                maximum = Math.max(maximum, lineWidth);
                lineWidth = 0.0f;
            } else {
                lineWidth += glyph.getMetrics().getAdvance() * scale;
            }
        }
        return Math.max(maximum, lineWidth);
    }

    public float fontHeight(float size) {
        Objects.requireNonNull(MinecraftClient.getInstance().textRenderer);
        return size * 9.0f / 8.0f;
    }

    private void addGlyph(BakedGlyphImpl glyph, float x, float y, float scale, int color, float depth) {
        if (firstInstance + pendingCount >= capacity) grow();
        com.mojang.blaze3d.textures.GpuTextureView gpuTex = null;
        try { gpuTex = (com.mojang.blaze3d.textures.GpuTextureView) GLYPH_TEXTURE_VIEW.get(glyph); } catch (Exception ignored) {}
        TextureView texture = gpuTex != null ? importedTextures.computeIfAbsent(gpuTex, source -> GlTextureBridge.importColor(device, source)) : null;
        if (texture != currentTexture) {
            currentTexture = texture;
            if (texture != null) textureRuns.add(texture);
            runStarts.add(firstInstance + pendingCount);
        }
        int monochrome = (gpuTex != null && gpuTex.texture().getFormat() == TextureFormat.RED8) ? 1 : 0;
        long address = instances.mappedAddress() + (long) (firstInstance + pendingCount) * STRIDE;
        float minX = glyphField(GLYPH_MIN_X, glyph), minY = glyphField(GLYPH_MIN_Y, glyph);
        float maxX = glyphField(GLYPH_MAX_X, glyph), maxY = glyphField(GLYPH_MAX_Y, glyph);
        float minU = glyphField(GLYPH_MIN_U, glyph), minV = glyphField(GLYPH_MIN_V, glyph);
        float maxU = glyphField(GLYPH_MAX_U, glyph), maxV = glyphField(GLYPH_MAX_V, glyph);
        NativeMemoryWrites.putFloat4(address, x + minX * scale, y + minY * scale, (maxX - minX) * scale, (maxY - minY) * scale);
        NativeMemoryWrites.putFloat4(address + 16, minU, minV, maxU, maxV);
        NativeMemoryWrites.putInt2(address + 32, color, monochrome);
        NativeMemoryWrites.putFloat(address + 40, depth);
        pendingCount++;
    }

    private void grow() {
        capacity *= 2;
        instances = device.reallocateMappedBuffer(instances, (long) STRIDE * capacity, (long) (firstInstance + pendingCount) * STRIDE);
    }

    @Override
    public void prepare(CommandEncoder commands) {
        commands.transitionBuffer(instances.wholeSlice(), VERTEX_STAGE, BufferAccess.VERTEX_READ);
    }

    @Override
    public void reset() {
        firstInstance = 0;
        pendingCount = 0;
        firstTextureRun = 0;
        currentTexture = null;
        textureRuns.clear();
        runStarts.clear();
        importedTextures.clear();
    }

    @Override
    public void draw(CommandEncoder commands, RenderPass pass) {
        if (pendingCount == 0) return;
        int start = firstInstance;
        int end = firstInstance + pendingCount;
        pass.setPipeline(pipeline);
        pass.setBuffer(0, cameraUniforms.wholeSlice());
        pass.setSampler(2, sampler);
        int totalRuns = textureRuns.size();
        int run = firstTextureRun;
        for (; run < totalRuns; run++) {
            if (runStarts.getInt(run) >= end) break;
            int runStart = runStarts.getInt(run);
            int runEnd = run + 1 < totalRuns ? runStarts.getInt(run + 1) : end;
            int from = Math.max(runStart, start);
            int to = Math.min(runEnd, end);
            if (to > from) {
                pass.setVertexBuffer(instances.slice((long) from * STRIDE, (long) (to - from) * STRIDE));
                pass.setTexture(1, textureRuns.get(run));
                pass.draw(4, to - from, 0);
            }
            if (run + 1 >= totalRuns || runStarts.getInt(run + 1) > end) break;
        }
        firstTextureRun = run;
        firstInstance = end;
        pendingCount = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(instances);
        device.release(sampler);
    }
}
