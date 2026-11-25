package com.blamejared.clumps.client.diagnostics;

import static com.blamejared.clumps.client.MinecraftAccess.CLIENT;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.render.HudRenderEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleManager;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Developer-only structured Flight Recorder compiled exclusively when -PdevDiagnostics=true.
 * Public builds compile src/prod/java instead so zero diagnostic code or strings ship to users.
 */
public final class DevDiagnostics {
    public static final boolean ENABLED = true;

    private static final int RING_CAPACITY = 1200;
    private static final int HUD_LINES = 8;
    private static final long SNAPSHOT_WINDOW_MS = 15_000L;
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter LINE_TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Deque<DiagEntry> RING = new ArrayDeque<>(RING_CAPACITY + 16);
    private static final ConcurrentLinkedQueue<DiagEntry> WRITE_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<String, Long> RATE_LIMITS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> METRICS = new ConcurrentHashMap<>();

    private static volatile boolean hudOverlayEnabled = false;
    private static volatile Path diagDir;
    private static volatile Path sessionLogPath;
    private static volatile Path sessionJsonlPath;
    private static boolean f9WasDown;
    private static long lastFlushMs;

    private DevDiagnostics() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Path runDir = CLIENT != null && CLIENT.runDirectory != null
                    ? CLIENT.runDirectory.toPath()
                    : Path.of(".");
            diagDir = runDir.resolve("logs").resolve("clumps-diag");
            Files.createDirectories(diagDir);
            String stamp = FILE_TS.format(Instant.now());
            sessionLogPath = diagDir.resolve("session-" + stamp + ".log");
            sessionJsonlPath = diagDir.resolve("session-" + stamp + ".jsonl");
        } catch (Throwable ignored) {
        }
        EventBus.register(new DevDiagnostics());
        Runtime.getRuntime().addShutdownHook(new Thread(DevDiagnostics::flushAndWriteSummary, "clumps-diag-shutdown"));
        log("DevDiag", "SESSION_START", "F9=dump last 15s bug report, RightCtrl+F9=toggle live overlay");
    }

    public static void log(String source, String event, String detail) {
        if (!INSTALLED.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        int tick = CLIENT != null && CLIENT.player != null ? CLIENT.player.age : -1;
        DiagEntry entry = new DiagEntry(now, tick, safe(source), safe(event), safe(detail));
        synchronized (RING) {
            RING.addLast(entry);
            while (RING.size() > RING_CAPACITY) {
                RING.pollFirst();
            }
        }
        WRITE_QUEUE.add(entry);
    }

    public static void logRateLimited(String key, long minIntervalMs, String source, String event, String detail) {
        if (!INSTALLED.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long prev = RATE_LIMITS.get(key);
        if (prev != null && now - prev < minIntervalMs) {
            return;
        }
        RATE_LIMITS.put(key, now);
        log(source, event, detail);
    }

    public static void metric(String metricKey) {
        if (!INSTALLED.get() || metricKey == null) {
            return;
        }
        METRICS.computeIfAbsent(metricKey, k -> new LongAdder()).increment();
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        long now = System.currentTimeMillis();
        if (now - lastFlushMs >= 1000L) {
            lastFlushMs = now;
            flushQueueAsync();
        }
        if (CLIENT == null || CLIENT.getWindow() == null) {
            return;
        }
        boolean f9Down = InputUtil.isKeyPressed(CLIENT.getWindow(), GLFW.GLFW_KEY_F9);
        if (f9Down && !f9WasDown) {
            boolean rCtrlDown = InputUtil.isKeyPressed(CLIENT.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
            if (rCtrlDown) {
                hudOverlayEnabled = !hudOverlayEnabled;
                postDevToast("Dev HUD overlay " + (hudOverlayEnabled ? "ON" : "OFF"));
            } else {
                dumpBugSnapshot();
            }
        }
        f9WasDown = f9Down;
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        log("DevDiag", "WORLD_CHANGE", "World context switched");
        flushQueueAsync();
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void onHudRender(HudRenderEvent event) {
        if (!hudOverlayEnabled) {
            return;
        }
        UiRenderer renderer = ClumpsClient.uiRenderer;
        if (renderer == null) {
            return;
        }
        List<DiagEntry> recent = snapshotRecent(HUD_LINES);
        float fontSize = 7.0F;
        float lineH = 9.5F;
        float pad = 5.0F;
        float boxW = 340.0F;
        float boxH = pad * 2.0F + lineH * (recent.size() + 1);
        float x = 8.0F;
        float y = 8.0F;
        renderer.drawRoundedRect(x, y, boxW, boxH, 4.0F, 0xCC0D1117);
        renderer.drawTextWithShadow(FontAsset.INTER, "DEV FLIGHT RECORDER  [F9 Dump | RCtrl+F9 Hide]", x + pad, y + pad, fontSize, 0xFF38BDF8);
        float curY = y + pad + lineH;
        for (DiagEntry e : recent) {
            String line = "[T:" + e.tick() + "] " + e.source() + " " + e.event() + " " + e.detail();
            if (line.length() > 74) {
                line = line.substring(0, 71) + "...";
            }
            int color = e.event().contains("DENY") || e.event().contains("ABORT") || e.event().contains("FAIL")
                    ? 0xFFF87171
                    : (e.event().contains("SKIP") ? 0xFFFACC15 : 0xFFE2E8F0);
            renderer.drawTextWithShadow(FontAsset.INTER, line, x + pad, curY, fontSize, color);
            curY += lineH;
        }
    }

    private static void dumpBugSnapshot() {
        long now = System.currentTimeMillis();
        long cutoff = now - SNAPSHOT_WINDOW_MS;
        List<DiagEntry> window = new ArrayList<>();
        synchronized (RING) {
            for (DiagEntry e : RING) {
                if (e.timeMs() >= cutoff) {
                    window.add(e);
                }
            }
        }
        StringBuilder sb = new StringBuilder(4096);
        sb.append("=== CLUMPS DEV BUG SNAPSHOT ===\n");
        sb.append("Captured: ").append(Instant.ofEpochMilli(now)).append('\n');
        if (CLIENT != null && CLIENT.player != null) {
            var p = CLIENT.player;
            sb.append(String.format("Player: pos=(%.2f, %.2f, %.2f) vel=(%.3f, %.3f, %.3f) fall=%.2f onGround=%s gliding=%s slot=%d hand=%s%n",
                    p.getX(), p.getY(), p.getZ(),
                    p.getVelocity().x, p.getVelocity().y, p.getVelocity().z,
                    p.fallDistance, p.isOnGround(), p.isGliding(),
                    p.getInventory().getSelectedSlot(), p.getMainHandStack().getItem()));
        }
        sb.append("Enabled Modules: ");
        List<String> enabled = new ArrayList<>();
        for (Module m : ModuleManager.allFeatures()) {
            if (m != null && m.isEnabled()) {
                enabled.add(m.getName().toString());
            }
        }
        sb.append(String.join(", ", enabled)).append("\n\n");
        sb.append("--- SESSION METRICS SCORECARD ---\n");
        appendScorecard(sb);
        sb.append("\n--- LAST 15 SECONDS TIMELINE (").append(window.size()).append(" events) ---\n");
        for (DiagEntry e : window) {
            sb.append(e.formatHuman()).append('\n');
        }
        try {
            if (diagDir == null) {
                return;
            }
            Files.createDirectories(diagDir);
            String name = "bug-dump-" + FILE_TS.format(Instant.ofEpochMilli(now)) + ".txt";
            Path out = diagDir.resolve(name);
            Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
            postDevToast("Saved dev dump: logs/clumps-diag/" + name);
        } catch (IOException ignored) {
        }
    }

    private static void flushQueueAsync() {
        if (WRITE_QUEUE.isEmpty() || sessionLogPath == null) {
            return;
        }
        List<DiagEntry> batch = new ArrayList<>();
        DiagEntry item;
        while ((item = WRITE_QUEUE.poll()) != null) {
            batch.add(item);
        }
        if (batch.isEmpty()) {
            return;
        }
        Thread writer = new Thread(() -> writeBatch(batch), "clumps-diag-flush");
        writer.setDaemon(true);
        writer.start();
    }

    private static void writeBatch(List<DiagEntry> batch) {
        if (sessionLogPath == null || sessionJsonlPath == null) {
            return;
        }
        try (BufferedWriter txt = Files.newBufferedWriter(sessionLogPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
             BufferedWriter jsonl = Files.newBufferedWriter(sessionJsonlPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (DiagEntry e : batch) {
                txt.write(e.formatHuman());
                txt.newLine();
                jsonl.write(e.formatJson());
                jsonl.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    private static void flushAndWriteSummary() {
        List<DiagEntry> remaining = new ArrayList<>();
        DiagEntry item;
        while ((item = WRITE_QUEUE.poll()) != null) {
            remaining.add(item);
        }
        if (!remaining.isEmpty()) {
            writeBatch(remaining);
        }
        if (sessionLogPath == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== SESSION HEALTH SCORECARD ===\n");
        appendScorecard(sb);
        try {
            Files.writeString(sessionLogPath, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private static void appendScorecard(StringBuilder sb) {
        TreeMap<String, Long> sorted = new TreeMap<>();
        for (Map.Entry<String, LongAdder> e : METRICS.entrySet()) {
            sorted.put(e.getKey(), e.getValue().sum());
        }
        if (sorted.isEmpty()) {
            sb.append("(no metrics recorded yet)\n");
            return;
        }
        for (Map.Entry<String, Long> e : sorted.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        }
    }

    private static List<DiagEntry> snapshotRecent(int maxLines) {
        List<DiagEntry> out = new ArrayList<>(maxLines);
        synchronized (RING) {
            int skip = Math.max(0, RING.size() - maxLines);
            int idx = 0;
            for (DiagEntry e : RING) {
                if (idx++ >= skip) {
                    out.add(e);
                }
            }
        }
        return out;
    }

    private static void postDevToast(String msg) {
        try {
            if (CLIENT != null && CLIENT.player != null) {
                CLIENT.player.sendMessage(Text.literal("\u00a7b[DevDiag] \u00a7f" + msg), false);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ');
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record DiagEntry(long timeMs, int tick, String source, String event, String detail) {
        String formatHuman() {
            return "[T:" + tick + " " + LINE_TS.format(Instant.ofEpochMilli(timeMs)) + "] [" + source + "] " + event
                    + (detail.isEmpty() ? "" : " " + detail);
        }

        String formatJson() {
            return "{\"ts\":" + timeMs
                    + ",\"tick\":" + tick
                    + ",\"src\":\"" + escapeJson(source)
                    + "\",\"ev\":\"" + escapeJson(event)
                    + "\",\"detail\":\"" + escapeJson(detail) + "\"}";
        }
    }
}
