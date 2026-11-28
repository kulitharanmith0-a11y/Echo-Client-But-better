package com.blamejared.clumps.client.gui.dashboard;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.gfx.ImageDecoder;
import com.blamejared.clumps.client.gfx.texture.TextureView;
import com.blamejared.clumps.client.modules.internals.ClickGui;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class GuiBackgroundMedia {
    private static final int MAX_ANIMATED_FRAMES = 120;
    private static final int DEFAULT_VIDEO_FPS = 15;
    private static final int MAX_VIDEO_SECONDS = 15;
    private static final long MAX_FILE_BYTES = 80L * 1024L * 1024L;

    private record DecodedFrames(long generation, String path, int width, int height, List<byte[]> rgbaFrames, int[] delaysMs) { }

    private final AtomicLong generationCounter = new AtomicLong(0L);
    private final AtomicBoolean pickerOpen = new AtomicBoolean(false);

    private String requestedPath = "";
    private volatile DecodedFrames readyToUpload;
    private volatile String statusNote = "";

    private TextureView[] textures;
    private int[] frameDelaysMs;
    private int mediaWidth;
    private int mediaHeight;
    private int frameIndex;
    private long lastFrameSwitchMs;

    public String statusNote() {
        return statusNote;
    }

    public boolean isPickerOpen() {
        return pickerOpen.get();
    }

    public void openFilePickerAsync() {
        if (!pickerOpen.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String[] patterns = new String[]{"*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.mp4", "*.webm", "*.mov"};
                PointerBuffer filter = stack.mallocPointer(patterns.length);
                for (String p : patterns) {
                    filter.put(stack.UTF8(p));
                }
                filter.flip();
                String current = ClickGui.backgroundMediaFile();
                String defaultPath = "";
                if (current != null && !current.isBlank()) {
                    try {
                        Path p = Path.of(current);
                        if (Files.exists(p)) {
                            defaultPath = p.toAbsolutePath().toString();
                        } else if (p.getParent() != null && Files.isDirectory(p.getParent())) {
                            defaultPath = p.getParent().toAbsolutePath().toString() + java.io.File.separator;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                String chosen = TinyFileDialogs.tinyfd_openFileDialog(
                        "Choose GUI Background Image, GIF, or Video",
                        defaultPath,
                        filter,
                        "Images, GIFs & Videos (*.png, *.jpg, *.jpeg, *.gif, *.mp4, *.webm)",
                        false);
                if (chosen != null && !chosen.isBlank()) {
                    ClickGui.backgroundMediaPath.setText(chosen.trim());
                }
            } catch (Throwable ignored) {
            } finally {
                pickerOpen.set(false);
            }
        }, "GuiMediaPicker");
        worker.setDaemon(true);
        worker.start();
    }

    public void render(UiRenderer renderer, float x, float y, float width, float height, float cornerRadius, float opacity) {
        syncConfiguredMedia();
        uploadPendingFrames();
        if (textures == null || textures.length == 0 || mediaWidth <= 0 || mediaHeight <= 0 || opacity <= 0.001f) {
            return;
        }
        TextureView view = currentFrame();
        if (view == null) {
            return;
        }

        String fit = ClickGui.backgroundMediaFitMode();
        float drawX = x;
        float drawY = y;
        float drawW = width;
        float drawH = height;

        if (!"STRETCH".equals(fit)) {
            float mediaAspect = (float) mediaWidth / (float) mediaHeight;
            float boxAspect = width / Math.max(1.0f, height);
            boolean cover = !"CONTAIN".equals(fit);
            if ((cover && mediaAspect > boxAspect) || (!cover && mediaAspect < boxAspect)) {
                drawH = height;
                drawW = height * mediaAspect;
                drawX = x + (width - drawW) * 0.5f;
            } else {
                drawW = width;
                drawH = width / Math.max(0.001f, mediaAspect);
                drawY = y + (height - drawH) * 0.5f;
            }
        }

        renderer.pushScissor(x, y, width, height);
        renderer.drawImage(view, drawX, drawY, drawW, drawH, cornerRadius, opacity);
        renderer.popScissor();
    }

    private void syncConfiguredMedia() {
        String configured = ClickGui.backgroundMediaFile();
        if (configured.equals(requestedPath)) {
            return;
        }
        requestedPath = configured;
        long gen = generationCounter.incrementAndGet();
        readyToUpload = null;
        disposeTextures();
        if (configured.isEmpty()) {
            statusNote = "";
            return;
        }
        Path file;
        try {
            file = Path.of(configured);
        } catch (Exception invalid) {
            statusNote = "Invalid path";
            return;
        }
        if (!Files.isRegularFile(file)) {
            statusNote = "File not found";
            return;
        }
        statusNote = "Loading media...";
        Thread loader = new Thread(() -> decodeAsync(gen, configured, file), "GuiMediaLoader");
        loader.setDaemon(true);
        loader.start();
    }

    private void decodeAsync(long gen, String rawPath, Path file) {
        try {
            long size = Files.size(file);
            if (size <= 0L || size > MAX_FILE_BYTES) {
                if (generationCounter.get() == gen) statusNote = "File too large (max 80MB)";
                return;
            }
            String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
            DecodedFrames decoded;
            if (lower.endsWith(".gif")) {
                decoded = decodeGif(gen, rawPath, Files.readAllBytes(file));
            } else if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov")
                    || lower.endsWith(".mkv") || lower.endsWith(".avi")) {
                decoded = decodeVideoViaFfmpeg(gen, rawPath, file);
            } else {
                decoded = decodeStaticImage(gen, rawPath, Files.readAllBytes(file));
            }
            if (generationCounter.get() != gen) {
                return;
            }
            if (decoded != null && !decoded.rgbaFrames().isEmpty()) {
                readyToUpload = decoded;
                statusNote = decoded.rgbaFrames().size() > 1
                        ? "Animated (" + decoded.rgbaFrames().size() + " frames)"
                        : "Loaded";
            } else if (statusNote.equals("Loading media...")) {
                statusNote = "Unsupported or unreadable media";
            }
        } catch (Throwable failure) {
            if (generationCounter.get() == gen) {
                statusNote = "Failed to load media";
            }
        }
    }

    private DecodedFrames decodeStaticImage(long gen, String rawPath, byte[] bytes) {
        ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
        try {
            encoded.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer c = stack.mallocInt(1);
                ByteBuffer rgba = STBImage.stbi_load_from_memory(encoded, w, h, c, 4);
                if (rgba != null) {
                    try {
                        int width = w.get(0);
                        int height = h.get(0);
                        byte[] copy = new byte[width * height * 4];
                        rgba.get(copy);
                        return new DecodedFrames(gen, rawPath, width, height, List.of(copy), new int[]{1000});
                    } finally {
                        STBImage.stbi_image_free(rgba);
                    }
                }
            }
        } finally {
            MemoryUtil.memFree((java.nio.Buffer) encoded);
        }
        return decodeStaticImageViaImageIo(gen, rawPath, bytes);
    }

    private DecodedFrames decodeStaticImageViaImageIo(long gen, String rawPath, byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return null;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
            byte[] rgba = new byte[width * height * 4];
            for (int i = 0; i < argb.length; i++) {
                int p = argb[i];
                int o = i << 2;
                rgba[o] = (byte) ((p >>> 16) & 0xFF);
                rgba[o + 1] = (byte) ((p >>> 8) & 0xFF);
                rgba[o + 2] = (byte) (p & 0xFF);
                rgba[o + 3] = (byte) ((p >>> 24) & 0xFF);
            }
            return new DecodedFrames(gen, rawPath, width, height, List.of(rgba), new int[]{1000});
        } catch (Throwable ignored) {
            return null;
        }
    }

    private DecodedFrames decodeGif(long gen, String rawPath, byte[] bytes) {
        ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
        try {
            encoded.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer delaysPtr = stack.mallocPointer(1);
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer z = stack.mallocInt(1);
                IntBuffer c = stack.mallocInt(1);
                ByteBuffer allFrames = STBImage.stbi_load_gif_from_memory(encoded, delaysPtr, w, h, z, c, 4);
                if (allFrames == null) {
                    return decodeStaticImage(gen, rawPath, bytes);
                }
                long delaysAddr = delaysPtr.get(0);
                try {
                    int width = w.get(0);
                    int height = h.get(0);
                    int count = z.get(0);
                    if (width <= 0 || height <= 0 || count <= 0) {
                        return null;
                    }
                    int stride = Math.max(1, (count + MAX_ANIMATED_FRAMES - 1) / MAX_ANIMATED_FRAMES);
                    int frameBytes = width * height * 4;
                    List<byte[]> frames = new ArrayList<>();
                    List<Integer> delays = new ArrayList<>();
                    IntBuffer delayBuf = delaysAddr != 0L ? MemoryUtil.memIntBuffer(delaysAddr, count) : null;
                    for (int i = 0; i < count && frames.size() < MAX_ANIMATED_FRAMES; i += stride) {
                        byte[] frame = new byte[frameBytes];
                        allFrames.position(i * frameBytes);
                        allFrames.get(frame);
                        frames.add(frame);
                        int delayMs = delayBuf != null ? delayBuf.get(i) : 66;
                        if (delayMs < 20) delayMs = 66;
                        delays.add(delayMs * stride);
                    }
                    int[] delayArr = new int[delays.size()];
                    for (int i = 0; i < delays.size(); i++) delayArr[i] = delays.get(i);
                    return new DecodedFrames(gen, rawPath, width, height, frames, delayArr);
                } finally {
                    STBImage.stbi_image_free(allFrames);
                    if (delaysAddr != 0L) {
                        MemoryUtil.nmemFree(delaysAddr);
                    }
                }
            }
        } finally {
            MemoryUtil.memFree((java.nio.Buffer) encoded);
        }
    }

    private DecodedFrames decodeVideoViaFfmpeg(long gen, String rawPath, Path videoFile) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("clumps-bg-vid-");
            Path pattern = tempDir.resolve("f_%04d.jpg");
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                    "-i", videoFile.toAbsolutePath().toString(),
                    "-t", String.valueOf(MAX_VIDEO_SECONDS),
                    "-vf", "fps=" + DEFAULT_VIDEO_FPS + ",scale='min(960,iw)':-2",
                    "-frames:v", String.valueOf(MAX_ANIMATED_FRAMES),
                    "-q:v", "4",
                    pattern.toAbsolutePath().toString());
            Process proc = pb.start();
            boolean finished = proc.waitFor(20L, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                if (generationCounter.get() == gen) statusNote = "Video decode timed out";
                return null;
            }
            if (proc.exitValue() != 0) {
                if (generationCounter.get() == gen) statusNote = "ffmpeg failed to decode video";
                return null;
            }
            List<Path> frameFiles = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir, "f_*.jpg")) {
                for (Path p : stream) frameFiles.add(p);
            }
            frameFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));
            if (frameFiles.isEmpty()) {
                return null;
            }
            List<byte[]> rgbaFrames = new ArrayList<>(frameFiles.size());
            int width = 0;
            int height = 0;
            for (Path frameFile : frameFiles) {
                if (generationCounter.get() != gen) return null;
                DecodedFrames one = decodeStaticImage(gen, rawPath, Files.readAllBytes(frameFile));
                if (one == null || one.rgbaFrames().isEmpty()) continue;
                if (width == 0) {
                    width = one.width();
                    height = one.height();
                }
                if (one.width() == width && one.height() == height) {
                    rgbaFrames.add(one.rgbaFrames().get(0));
                }
            }
            if (rgbaFrames.isEmpty()) {
                return null;
            }
            int[] delays = new int[rgbaFrames.size()];
            int frameMs = Math.max(20, 1000 / DEFAULT_VIDEO_FPS);
            java.util.Arrays.fill(delays, frameMs);
            return new DecodedFrames(gen, rawPath, width, height, rgbaFrames, delays);
        } catch (IOException missingFfmpeg) {
            if (generationCounter.get() == gen) {
                statusNote = "Video needs ffmpeg on PATH (GIF/PNG/JPG work natively)";
            }
            return null;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (tempDir != null) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                    for (Path p : stream) Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
                try {
                    Files.deleteIfExists(tempDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void uploadPendingFrames() {
        DecodedFrames pending = readyToUpload;
        if (pending == null || ClumpsClient.renderDevice == null) {
            return;
        }
        readyToUpload = null;
        if (pending.generation() != generationCounter.get()) {
            return;
        }
        disposeTextures();
        int count = pending.rgbaFrames().size();
        TextureView[] uploaded = new TextureView[count];
        ByteBuffer scratch = MemoryUtil.memAlloc(pending.width() * pending.height() * 4);
        try {
            for (int i = 0; i < count; i++) {
                scratch.clear();
                scratch.put(pending.rgbaFrames().get(i)).flip();
                uploaded[i] = ImageDecoder.uploadRgba(ClumpsClient.renderDevice, scratch, pending.width(), pending.height());
            }
            textures = uploaded;
            frameDelaysMs = pending.delaysMs();
            mediaWidth = pending.width();
            mediaHeight = pending.height();
            frameIndex = 0;
            lastFrameSwitchMs = System.currentTimeMillis();
        } catch (Throwable failure) {
            for (TextureView v : uploaded) {
                releaseView(v);
            }
            statusNote = "GPU upload failed";
        } finally {
            MemoryUtil.memFree((java.nio.Buffer) scratch);
        }
    }

    private TextureView currentFrame() {
        if (textures == null || textures.length == 0) {
            return null;
        }
        if (textures.length == 1) {
            return textures[0];
        }
        long now = System.currentTimeMillis();
        int delay = (frameDelaysMs != null && frameIndex < frameDelaysMs.length) ? Math.max(20, frameDelaysMs[frameIndex]) : 66;
        if (now - lastFrameSwitchMs >= delay) {
            frameIndex = (frameIndex + 1) % textures.length;
            lastFrameSwitchMs = now;
        }
        return textures[frameIndex];
    }

    public void dispose() {
        generationCounter.incrementAndGet();
        readyToUpload = null;
        requestedPath = "";
        statusNote = "";
        disposeTextures();
    }

    private void disposeTextures() {
        TextureView[] current = textures;
        textures = null;
        frameDelaysMs = null;
        mediaWidth = 0;
        mediaHeight = 0;
        frameIndex = 0;
        if (current != null) {
            for (TextureView view : current) {
                releaseView(view);
            }
        }
    }

    private static void releaseView(TextureView view) {
        if (view != null && view.texture != null && ClumpsClient.renderDevice != null) {
            try {
                ClumpsClient.renderDevice.release(view.texture);
            } catch (Throwable ignored) {
            }
        }
    }
}
