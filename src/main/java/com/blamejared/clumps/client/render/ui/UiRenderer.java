package com.blamejared.clumps.client.render.ui;

import com.blamejared.clumps.natives.NativeCapabilities;
import com.blamejared.clumps.client.gfx.texture.Texture;
import com.blamejared.clumps.client.render.NativeMemoryWrites;

import com.blamejared.clumps.client.render.batch.MultiTextureEffectBatch;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderPass;
import com.blamejared.clumps.client.render.bridge.GlTextureBridge;
import com.blamejared.clumps.client.render.ui.GuiDimensions;
import com.blamejared.clumps.client.render.ui.GuiProjection;
import com.blamejared.clumps.client.render.world.WorldRenderContext;
import com.blamejared.clumps.client.render.batch.RenderBatch;
import com.blamejared.clumps.client.render.BackdropMode;
import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.gfx.EffectBatch;
import com.blamejared.clumps.client.gfx.IndexedTextureEffectBatch;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.gfx.QuadBatch;
import com.blamejared.clumps.client.gfx.buffer.Buffer;
import com.blamejared.clumps.client.gfx.buffer.BufferInfo;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.command.RenderPass;
import com.blamejared.clumps.client.gfx.device.DeviceCapabilities;
import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.client.gfx.effect.BackdropPass;
import com.blamejared.clumps.client.gfx.effect.BackdropPass.BackdropTextures;
import com.blamejared.clumps.client.gfx.effect.ComputeBackdropEffect;
import com.blamejared.clumps.client.gfx.effect.RenderPassBackdropEffect;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.joml.Vector4i;
import com.blamejared.clumps.client.render.batch.ImmediateColorBatch;
import com.blamejared.clumps.client.render.batch.LineBatch;
import com.blamejared.clumps.client.render.batch.TracerBatch;
import com.blamejared.clumps.client.render.batch.BoxBatch;
import com.blamejared.clumps.client.render.batch.OutlineTextBatch;
import com.blamejared.clumps.client.render.batch.ParticleBatch;
import com.blamejared.clumps.client.render.batch.MinecraftTextBatch;
import com.blamejared.clumps.client.render.world.WorldGeometry;
import com.blamejared.clumps.client.render.texture.ItemTextureCache;

public class UiRenderer {
    private static final int IMAGE_BATCH = 0;
    private static final int SCREEN_IMAGE_BATCH = 1;
    private static final int QUAD_BATCH = 2;
    private static final int IMMEDIATE_BATCH = 3;
    private static final int OUTLINE_TEXT_BATCH = 4;
    private static final int MINECRAFT_TEXT_BATCH = 5;
    private static final long CAMERA_BUFFER_BYTES = 64;
    private static final float DEPTH_STEP = 1.0E-5F;

    private final RenderDevice device;
    private final Buffer cameraBuffer;
    private final BackdropPass backdrop;
    private final EffectBatch images;
    private final EffectBatch screenImages;
    private final OutlineTextBatch text;
    private final MinecraftTextBatch minecraftText;
    private final QuadBatch quads;
    private final ImmediateColorBatch immediate;
    private final ItemTextureCache items;
    private final RenderBatch[] batches;
    private final Deque<Vector4i> scissors = new ArrayDeque<>();
    private final Queue<Runnable> frameCalls = new ArrayDeque<>();
    private final Queue<Runnable> retainedCalls = new ArrayDeque<>();
    private CommandEncoder commands;
    private RenderPass pass;
    private Supplier<RenderPass> passFactory;
    private float depth = 1.0F;
    private boolean frameActive;
    private int lastBatch = -1;

    
    public TextureView backdropTexture;
    public TextureView halfResolutionBackdrop;
    public TextureView fullResolutionBackdrop;
    public static DeviceCapabilities NativeCapabilities;
    private int dumpFrames = 0;

    public UiRenderer(RenderDevice device) {
        NativeCapabilities = device.NativeCapabilities();
        this.device = device;
        cameraBuffer = device.createBuffer(BufferInfo.builder().usage(2).size(CAMERA_BUFFER_BYTES)
                .dynamic(true).mapped(true).build());
        BackdropPass createdBackdrop = null;
        if (NativeCapabilities.computeShaders() && NativeCapabilities.storageImages()) {
            try {
                createdBackdrop = new ComputeBackdropEffect(device, 4);
            } catch (Throwable ignored) {
                createdBackdrop = null;
            }
        }
        if (createdBackdrop == null) {
            try {
                Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
                int width = framebuffer != null ? Math.max(1, framebuffer.textureWidth) : 1;
                int height = framebuffer != null ? Math.max(1, framebuffer.textureHeight) : 1;
                createdBackdrop = new RenderPassBackdropEffect(device, 4, width, height);
            } catch (Throwable ignored) {
                createdBackdrop = null;
            }
        }
        backdrop = createdBackdrop;
        images = NativeCapabilities.sampledImageArrays()
                ? new MultiTextureEffectBatch(device, cameraBuffer, false, true, false)
                : new IndexedTextureEffectBatch(device, cameraBuffer, false, true, false);
        screenImages = NativeCapabilities.sampledImageArrays()
                ? new MultiTextureEffectBatch(device, cameraBuffer, true, true, false)
                : new IndexedTextureEffectBatch(device, cameraBuffer, true, true, false);
        text = new OutlineTextBatch(device, cameraBuffer);
        minecraftText = new MinecraftTextBatch(device, cameraBuffer);
        quads = new QuadBatch(device, cameraBuffer);
        immediate = new ImmediateColorBatch(device, cameraBuffer);
        items = new ItemTextureCache(device);
        batches = new RenderBatch[]{images, screenImages, quads, immediate, text, minecraftText};
    }

    private float nextDepth() {
        depth += NativeCapabilities.reversedDepth() ? DEPTH_STEP : -DEPTH_STEP;
        return depth;
    }

    private double clearDepth() {
        return NativeCapabilities.reversedDepth() ? 0.0 : 1.0;
    }

    private void enqueue(Runnable call) {
        (frameActive ? frameCalls : retainedCalls).add(call);
    }

    private void enqueueBatch(int batch, Runnable call) {
        enqueue(() -> {
            selectBatch(batch);
            call.run();
        });
    }

    private void selectBatch(int batch) {
        if (batch < lastBatch) {
            flushBatches(commands, pass);
        }
        lastBatch = batch;
    }

    
    public void flushBatches(CommandEncoder commands, RenderPass pass) {
        for (RenderBatch batch : batches) {
            batch.prepare(commands);
        }
        for (RenderBatch batch : batches) {
            batch.draw(commands, pass);
        }
        lastBatch = -1;
    }

    private void reserveImage(EffectBatch batch) {
        if (batch.isFull()) {
            flushBatches(commands, pass);
        }
    }

    public void drawImage(TextureView image, float x, float y, float width, float height, float radius, float opacity) {
        if (image == null) {
            return;
        }
        enqueueBatch(IMAGE_BATCH, () -> {
            reserveImage(images);
            images.draw(image, x, y, width, height, radius, opacity, -1, false, 0.0F, 0.0F, 1.0F, 1.0F, nextDepth());
        });
    }

    public void drawImage(TextureView image, float x, float y, float width, float height, float radius, float opacity, float u0, float v0, float u1, float v1) {
        if (image == null) {
            return;
        }
        enqueueBatch(IMAGE_BATCH, () -> {
            reserveImage(images);
            images.draw(image, x, y, width, height, radius, opacity, -1, false, u0, v0, u1, v1, nextDepth());
        });
    }

    public void drawMaskedImage(TextureView image, float x, float y, float width, float height, float radius, float opacity, int color) {
        if (image == null) {
            return;
        }
        enqueueBatch(IMAGE_BATCH, () -> {
            reserveImage(images);
            images.draw(image, x, y, width, height, radius, opacity, color, true, 0.0F, 0.0F, 1.0F, 1.0F, nextDepth());
        });
    }

    public void drawBackdrop(TextureView image, float x, float y, float width, float height, float radius, float opacity) {
        drawBackdrop(image, x, y, width, height, radius, opacity, false);
    }

    public void drawBackdrop(TextureView image, float x, float y, float width, float height, float radius, float opacity, boolean mask) {
        drawBackdrop(image, x, y, width, height, radius, opacity, mask, 0);
    }

    public void drawBackdrop(TextureView image, float x, float y, float width, float height, float radius, float opacity, boolean mask, int color) {
        if (image == null) {
            return;
        }
        enqueueBatch(SCREEN_IMAGE_BATCH, () -> {
            reserveImage(screenImages);
            screenImages.draw(image, x, y, width, height, radius, opacity, color, mask, 0.0F, 0.0F, 1.0F, 1.0F, nextDepth());
        });
    }

    public float drawText(FontAsset font, CharSequence value, float x, float y, float size, int argb) {
        enqueueBatch(OUTLINE_TEXT_BATCH, () -> text.draw(font.ordinal(), value, x, y, size, argb, nextDepth()));
        return text.advanceX(font.ordinal(), value, x, size);
    }

    public float drawTextWithShadow(FontAsset font, CharSequence value, float x, float y, float size, int argb) {
        int shadow = textShadowColor(argb);
        enqueueBatch(OUTLINE_TEXT_BATCH, () -> text.draw(font.ordinal(), value, x + 0.75F, y + 0.75F, size, shadow, nextDepth()));
        enqueueBatch(OUTLINE_TEXT_BATCH, () -> text.draw(font.ordinal(), value, x, y, size, argb, nextDepth()));
        return text.advanceX(font.ordinal(), value, x, size);
    }

    private static int textShadowColor(int argb) {
        return UiRenderMath.textShadowColor(argb);
    }

    public float measureText(FontAsset font, CharSequence value, float size) {
        return text.measureText(font.ordinal(), value, size);
    }

    public float measureFontHeight(FontAsset font, float size) {
        return text.fontHeight(font.ordinal(), size);
    }

    public float measureFontHeight(int font, float size) {
        return text.fontHeight(font, size);
    }

    public float drawMinecraftText(CharSequence value, float x, float y, float size, int argb) {
        BakedGlyph[] glyphs = minecraftText.bake(value);
        enqueueBatch(MINECRAFT_TEXT_BATCH, () -> minecraftText.draw(glyphs, x, y, size, argb, nextDepth()));
        return x + minecraftText.measure(glyphs, size);
    }

    public float measureMinecraftText(CharSequence value, float size) {
        return minecraftText.measure(minecraftText.bake(value), size);
    }

    public float measureMinecraftFontHeight(float size) {
        return minecraftText.fontHeight(size);
    }

    public void drawPixelAlignedRect(float x, float y, float width, float height, float radius, int argb, float borderWidth, int borderColor) {
        float left = snapPixel(x);
        float top = snapPixel(y);
        float right = snapPixel(x + width);
        float bottom = snapPixel(y + height);
        drawRoundedRect(left, top, right - left, bottom - top, radius, argb, borderWidth, borderColor);
    }

    
    public static float snapPixel(float coordinate) {
        return (float) (Math.round((double) coordinate * 2.0) / 2.0);
    }

    public void drawRoundedRect(float x, float y, float width, float height, float radius, int argb) {
        enqueueBatch(QUAD_BATCH, () -> quads.addRectangle(x, y, width, height, radius, radius, radius, radius, argb, argb, argb, argb, 0.0F, 0, nextDepth()));
    }

    public void drawRoundedRect(float x, float y, float width, float height, float topLeft, float topRight, float bottomLeft, float bottomRight, int argb) {
        enqueueBatch(QUAD_BATCH, () -> quads.addRectangle(x, y, width, height, topLeft, topRight, bottomLeft, bottomRight, argb, argb, argb, argb, 0.0F, 0, nextDepth()));
    }

    public void drawGradientRect(float x, float y, float width, float height, float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        enqueueBatch(QUAD_BATCH, () -> quads.addRectangle(x, y, width, height, topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor, 0.0F, 0, nextDepth()));
    }

    public void drawRoundedRect(float x, float y, float width, float height, float topLeft, float topRight, float bottomLeft, float bottomRight, int argb, float borderWidth, int borderColor) {
        enqueueBatch(QUAD_BATCH, () -> quads.addRectangle(x, y, width, height, topLeft, topRight, bottomLeft, bottomRight, argb, argb, argb, argb, borderWidth, borderColor, nextDepth()));
    }

    public void drawRoundedRect(float x, float y, float width, float height, float radius, int argb, float borderWidth, int borderColor) {
        enqueueBatch(QUAD_BATCH, () -> quads.addRectangle(x, y, width, height, radius, radius, radius, radius, argb, argb, argb, argb, borderWidth, borderColor, nextDepth()));
    }

    public void drawQuadraticStroke(float startX, float startY, float controlX, float controlY, float endX, float endY, float width, int argb) {
        enqueueBatch(IMMEDIATE_BATCH, () -> immediate.quadraticStroke(startX, startY, controlX, controlY, endX, endY, width, 0.45F, 0.125F, argb, nextDepth()));
    }

    public void drawPointerCursor(float x, float y) {
        retainedCalls.add(() -> {
            flushBatches(commands, pass);
            selectBatch(IMMEDIATE_BATCH);
            float shadowDepth = nextDepth();
            drawCursorPolygon(x + 0.85F, y + 1.05F, 0x55000000, true, shadowDepth);
            float outlineDepth = nextDepth();
            drawCursorPolygon(x, y, 0xFF0A0C12, true, outlineDepth);
            float fillDepth = nextDepth();
            drawCursorPolygon(x, y, 0xFFFFFFFF, false, fillDepth);
        });
    }

    private void drawCursorPolygon(float x, float y, int color, boolean expanded, float depth) {
        if (expanded) {
            float tipX = x - 0.65F, tipY = y - 1.15F;
            float leftX = x - 0.65F, leftY = y + 12.1F;
            float notchLeftX = x + 2.55F, notchLeftY = y + 9.25F;
            float tailLeftX = x + 4.35F, tailLeftY = y + 13.0F;
            float tailRightX = x + 7.1F, tailRightY = y + 11.7F;
            float notchRightX = x + 5.25F, notchRightY = y + 8.15F;
            float rightX = x + 9.15F, rightY = y + 8.15F;
            immediate.fillTriangle(tipX, tipY, leftX, leftY, notchLeftX, notchLeftY, color, depth);
            immediate.fillTriangle(tipX, tipY, notchLeftX, notchLeftY, rightX, rightY, color, depth);
            immediate.fillTriangle(notchLeftX, notchLeftY, tailLeftX, tailLeftY, tailRightX, tailRightY, color, depth);
            immediate.fillTriangle(notchLeftX, notchLeftY, tailRightX, tailRightY, notchRightX, notchRightY, color, depth);
        } else {
            float tipX = x, tipY = y;
            float leftX = x, leftY = y + 10.5F;
            float notchLeftX = x + 2.8F, notchLeftY = y + 8.0F;
            float tailLeftX = x + 4.7F, tailLeftY = y + 12.0F;
            float tailRightX = x + 6.1F, tailRightY = y + 11.3F;
            float notchRightX = x + 4.2F, notchRightY = y + 7.3F;
            float rightX = x + 7.6F, rightY = y + 7.3F;
            immediate.fillTriangle(tipX, tipY, leftX, leftY, notchLeftX, notchLeftY, color, depth);
            immediate.fillTriangle(tipX, tipY, notchLeftX, notchLeftY, rightX, rightY, color, depth);
            immediate.fillTriangle(notchLeftX, notchLeftY, tailLeftX, tailLeftY, tailRightX, tailRightY, color, depth);
            immediate.fillTriangle(notchLeftX, notchLeftY, tailRightX, tailRightY, notchRightX, notchRightY, color, depth);
        }
    }

    public void drawShadow(float x, float y, float width, float height, float radius, float sigma, int argb) {
        enqueueBatch(QUAD_BATCH, () -> quads.addShadow(x, y, width, height, radius, sigma, argb, nextDepth()));
    }

    public void drawHueStrip(float x, float y, float width, float height, float opacity) {
        enqueueBatch(QUAD_BATCH, () -> quads.addHueGradient(x, y, width, height, opacity, nextDepth()));
    }

    public void drawHueStrip(float x, float y, float width, float height, float radius, float opacity) {
        enqueueBatch(QUAD_BATCH, () -> quads.addHueGradient(x, y, width, height, radius, opacity, nextDepth()));
    }

    public void drawSaturationBrightness(float x, float y, float width, float height, float hue, float opacity) {
        enqueueBatch(QUAD_BATCH, () -> quads.addSaturationBrightnessGradient(x, y, width, height, hue, opacity, nextDepth()));
    }

    public void drawSaturationBrightness(float x, float y, float width, float height, float radius, float hue, float opacity) {
        enqueueBatch(QUAD_BATCH, () -> quads.addSaturationBrightnessGradient(x, y, width, height, radius, hue, opacity, nextDepth()));
    }

    public void drawAlphaStrip(float x, float y, float width, float height, float red, float green, float blue, float opacity) {
        enqueueBatch(QUAD_BATCH, () -> quads.addAlphaGradient(x, y, width, height, red, green, blue, opacity, nextDepth()));
    }

    public void drawAlphaStrip(float x, float y, float width, float height, float radius, float red, float green, float blue, float opacity) {
        enqueueBatch(QUAD_BATCH, () -> quads.addAlphaGradient(x, y, width, height, radius, red, green, blue, opacity, nextDepth()));
    }

    public void pushScissor(float x, float y, float width, float height) {
        enqueue(() -> {
            flushBatches(commands, pass);
            Vector4i rectangle = scissorPixels(x, y, width, height);
            if (!scissors.isEmpty()) {
                rectangle = intersectScissors(scissors.peek(), rectangle);
            }
            scissors.push(rectangle);
            pass.setScissor(rectangle.x, rectangle.y, rectangle.z, rectangle.w);
        });
    }

    public void popScissor() {
        enqueue(() -> {
            flushBatches(commands, pass);
            if (!scissors.isEmpty()) {
                scissors.pop();
            }
            if (!scissors.isEmpty()) {
                Vector4i rectangle = scissors.peek();
                pass.setScissor(rectangle.x, rectangle.y, rectangle.z, rectangle.w);
            } else {
                var window = MinecraftClient.getInstance().getWindow();
                pass.setScissor(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
            }
        });
    }

    private static Vector4i scissorPixels(float x, float y, float width, float height) {
        return UiRenderMath.scissorPixels(x, y, width, height);
    }

    private static Vector4i intersectScissors(Vector4i first, Vector4i second) {
        return UiRenderMath.intersectScissors(first, second);
    }

    
    public void flush() {
        enqueue(() -> flushBatches(commands, pass));
    }

    public void beginFrame() {
        if (ClumpsClient.externalRendererActive || ClumpsClient.externalRendererRequested) {
            return;
        }
        resetFrame(ClumpsClient.commandEncoder);
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        passFactory = () -> commands.beginExternalRenderPass(() -> new MinecraftRenderPass(
                framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.of(clearDepth())));
        frameActive = true;
    }

    public boolean hasPendingCalls() {
        return !frameCalls.isEmpty() || !retainedCalls.isEmpty();
    }

    public void beginFrame(CommandEncoder commands, TextureView color, TextureView depth, OptionalInt clearColor) {
        resetFrame(commands);
        passFactory = () -> {
            RenderPass created = commands.beginRenderPass(color, clearColor, depth, OptionalDouble.of(clearDepth()));
            device.setViewportToTexture(created, color);
            return created;
        };
        frameActive = true;
    }

    public void updateBackdrop(CommandEncoder commands) {
        updateBackdrop(commands, BackdropMode.FULL);
    }

    public void updateBackdrop() {
        if (ClumpsClient.externalRendererActive || ClumpsClient.externalRendererRequested) {
            return;
        }
        updateBackdrop(ClumpsClient.commandEncoder);
    }

    public void updateBackdrop(CommandEncoder commands, BackdropMode mode) {
        if (backdrop == null || commands == null) {
            halfResolutionBackdrop = null;
            fullResolutionBackdrop = null;
            backdropTexture = null;
            return;
        }
        try {
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            if (framebuffer == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0
                    || framebuffer.getColorAttachmentView() == null) {
                return;
            }
            float scale = Math.min(framebuffer.textureWidth / 1920.0F, framebuffer.textureHeight / 1080.0F);
            TextureView source = GlTextureBridge.importColor(device, framebuffer.getColorAttachmentView());
            if (source == null) {
                return;
            }
            BackdropTextures result = backdrop.render(commands, source, source.yFlipped, scale, mode);
            if (dumpFrames > 0 && --dumpFrames == 0) {
                DebugTextureDump.dump(source, "source");
                DebugTextureDump.dump(result.halfResolution(), "half");
                DebugTextureDump.dump(result.fullResolution(), "full");
            }
            halfResolutionBackdrop = result.halfResolution();
            fullResolutionBackdrop = result.fullResolution();
            backdropTexture = result.fullResolution();
        } catch (Throwable ignored) {
            halfResolutionBackdrop = null;
            fullResolutionBackdrop = null;
            backdropTexture = null;
        }
    }

    private void resetFrame(CommandEncoder commands) {
        this.commands = commands;
        depth = NativeCapabilities.reversedDepth() ? 0.0F : 1.0F;
        lastBatch = -1;
        NativeMemoryWrites.putMatrix4f(cameraBuffer.mappedAddress(), GuiProjection.create(device));
        scissors.clear();
        for (RenderBatch batch : batches) {
            batch.reset();
        }
        for (RenderBatch batch : batches) {
            batch.prepare(commands);
        }
    }

    public void clearFrameQueues() {
        frameCalls.clear();
        retainedCalls.clear();
    }

    public void endFrame() {
        if (!frameActive) {
            return;
        }
        pass = passFactory.get();
        passFactory = null;
        while (!frameCalls.isEmpty()) {
            frameCalls.poll().run();
        }
        while (!retainedCalls.isEmpty()) {
            retainedCalls.poll().run();
        }
        flushBatches(commands, pass);
        pass.end();
        pass = null;
        frameActive = false;
    }

    public void drawItem(ItemStack item, LivingEntity entity, int seed, float x, float y, float size, float opacity) {
        if (item == null || item.isEmpty() || entity == null) {
            return;
        }
        ItemTextureCache.Entry entry = items.request(item, entity, seed);
        if (entry == null) {
            return;
        }
        drawImage(entry.texture(), x, y, size, size, 0.0F, opacity, entry.uLeft(), entry.vTop(), entry.uRight(), entry.vBottom());
    }

    public void renderPendingItems() {
        items.flush();
    }

    public void destroy() {
        clearItemCache();
        for (RenderBatch batch : batches) {
            batch.destroy();
        }
        if (backdrop != null) {
            backdrop.close();
        }
        device.release(cameraBuffer);
    }

    public void clearItemCache() {
        if (items != null) {
            try {
                items.clear();
            } catch (Throwable ignored) {
            }
        }
    }
}
