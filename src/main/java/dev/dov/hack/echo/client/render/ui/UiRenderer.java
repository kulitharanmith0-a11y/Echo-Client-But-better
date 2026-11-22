package dev.dov.hack.echo.client.render.ui;

import dev.jadyen.hack.echo.natives.NativeCapabilities;
import dev.dov.hack.echo.client.gfx.texture.Texture;
import dev.dov.hack.echo.client.render.NativeMemoryWrites;

import dev.dov.hack.echo.client.render.batch.MultiTextureEffectBatch;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderPass;
import dev.dov.hack.echo.client.render.bridge.GlTextureBridge;
import dev.dov.hack.echo.client.render.ui.GuiDimensions;
import dev.dov.hack.echo.client.render.ui.GuiProjection;
import dev.dov.hack.echo.client.render.world.WorldRenderContext;
import dev.dov.hack.echo.client.render.batch.RenderBatch;
import dev.dov.hack.echo.client.render.BackdropMode;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.gfx.EffectBatch;
import dev.dov.hack.echo.client.gfx.IndexedTextureEffectBatch;
import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.gfx.QuadBatch;
import dev.dov.hack.echo.client.gfx.buffer.Buffer;
import dev.dov.hack.echo.client.gfx.buffer.BufferInfo;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.command.RenderPass;
import dev.dov.hack.echo.client.gfx.device.DeviceCapabilities;
import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.dov.hack.echo.client.gfx.effect.BackdropPass;
import dev.dov.hack.echo.client.gfx.effect.BackdropPass.BackdropTextures;
import dev.dov.hack.echo.client.gfx.effect.ComputeBackdropEffect;
import dev.dov.hack.echo.client.gfx.effect.RenderPassBackdropEffect;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
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
import dev.dov.hack.echo.client.render.batch.ImmediateColorBatch;
import dev.dov.hack.echo.client.render.batch.LineBatch;
import dev.dov.hack.echo.client.render.batch.TracerBatch;
import dev.dov.hack.echo.client.render.batch.BoxBatch;
import dev.dov.hack.echo.client.render.batch.OutlineTextBatch;
import dev.dov.hack.echo.client.render.batch.ParticleBatch;
import dev.dov.hack.echo.client.render.batch.MinecraftTextBatch;
import dev.dov.hack.echo.client.render.world.WorldGeometry;
import dev.dov.hack.echo.client.render.texture.ItemTextureCache;

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
        if (NativeCapabilities.computeShaders() && NativeCapabilities.storageImages()) {
            backdrop = new ComputeBackdropEffect(device, 4);
        } else {
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            backdrop = new RenderPassBackdropEffect(device, 4, framebuffer.textureWidth, framebuffer.textureHeight);
        }
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
        enqueueBatch(IMAGE_BATCH, () -> {
            reserveImage(images);
            images.draw(image, x, y, width, height, radius, opacity, -1, false, 0.0F, 0.0F, 1.0F, 1.0F, nextDepth());
        });
    }

    public void drawImage(TextureView image, float x, float y, float width, float height, float radius, float opacity, float u0, float v0, float u1, float v1) {
        enqueueBatch(IMAGE_BATCH, () -> {
            reserveImage(images);
            images.draw(image, x, y, width, height, radius, opacity, -1, false, u0, v0, u1, v1, nextDepth());
        });
    }

    public void drawMaskedImage(TextureView image, float x, float y, float width, float height, float radius, float opacity, int color) {
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
        if (EchoClient.externalRendererActive) {
            return;
        }
        resetFrame(EchoClient.commandEncoder);
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        passFactory = () -> commands.beginExternalRenderPass(() -> new MinecraftRenderPass(
                framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.of(clearDepth())));
        frameActive = true;
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
        if (EchoClient.externalRendererActive) {
            return;
        }
        updateBackdrop(EchoClient.commandEncoder);
    }

    public void updateBackdrop(CommandEncoder commands, BackdropMode mode) {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        float scale = Math.min(framebuffer.textureWidth / 1920.0F, framebuffer.textureHeight / 1080.0F);
        TextureView source = GlTextureBridge.importColor(device, framebuffer.getColorAttachmentView());
        BackdropTextures result = backdrop.render(commands, source, source.yFlipped, scale, mode);
        if (dumpFrames > 0 && --dumpFrames == 0) {
            DebugTextureDump.dump(source, "source");
            DebugTextureDump.dump(result.halfResolution(), "half");
            DebugTextureDump.dump(result.fullResolution(), "full");
        }
        halfResolutionBackdrop = result.halfResolution();
        fullResolutionBackdrop = result.fullResolution();
        backdropTexture = result.fullResolution();
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
        for (RenderBatch batch : batches) {
            batch.destroy();
        }
        backdrop.close();
        device.release(cameraBuffer);
    }
}
