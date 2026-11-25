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
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.Setting;
import com.blamejared.clumps.client.settings.TextSetting;
import com.blamejared.clumps.client.utils.inventory.InventoryActionScheduler;
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
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
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
 * Covers every registered module automatically via EventBus, Module, Setting, and handler hooks.
 * Public builds compile src/prod/java instead so zero diagnostic code or strings ship to users.
 */
public final class DevDiagnostics {
    public static final boolean ENABLED = true;

    private static final int RING_CAPACITY = 2400;
    private static final int HUD_LINES = 10;
    private static final long SNAPSHOT_WINDOW_MS = 15_000L;
    private static final long SLOW_HANDLER_NANOS = 2_000_000L; // 2.0 ms
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter LINE_TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private enum HudMode { STREAM, HEALTH, FILTER }

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Deque<DiagEntry> RING = new ArrayDeque<>(RING_CAPACITY + 16);
    private static final ConcurrentLinkedQueue<DiagEntry> WRITE_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<String, Long> RATE_LIMITS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> METRICS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ModuleHealth> MODULE_HEALTH = new ConcurrentHashMap<>();

    private static volatile boolean hudOverlayEnabled = false;
    private static volatile HudMode hudMode = HudMode.STREAM;
    private static volatile String focusedModule = "";
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
        log("DevDiag", "SESSION_START", "F9=15s dump | RCtrl+F9=HUD | RShift+F9=Mode(Stream/Health/Filter) | RAlt+F9=CycleFilter");
    }

    public static void log(String source, String event, String detail) {
        if (!INSTALLED.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        int tick = currentTick();
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

    public static void recordEventDispatch(String moduleName, String eventName, long durationNanos, boolean cancelledEvent) {
        if (!INSTALLED.get() || moduleName == null || moduleName.isEmpty()) {
            return;
        }
        ModuleHealth health = healthOf(moduleName);
        health.recordCall(durationNanos, currentTick(), cancelledEvent);
        if (durationNanos >= SLOW_HANDLER_NANOS) {
            metric("diag.slow_handler." + moduleName);
            logRateLimited("slow:" + moduleName + ":" + eventName, 1000L, moduleName, "SLOW_HANDLER",
                    String.format(Locale.ROOT, "event=%s took=%.2fms", eventName, durationNanos / 1_000_000.0));
        }
        if (cancelledEvent) {
            metric("diag.event_cancel." + moduleName);
            logRateLimited("cancel:" + moduleName + ":" + eventName, 350L, moduleName, "EVENT_CANCEL", "event=" + eventName);
        }
    }

    public static void recordCrash(String moduleName, String eventName, String methodName, Throwable error) {
        if (!INSTALLED.get()) {
            return;
        }
        String mod = (moduleName == null || moduleName.isEmpty()) ? "Unknown" : moduleName;
        String errType = error != null ? error.getClass().getSimpleName() : "Throwable";
        String errMsg = error != null && error.getMessage() != null ? error.getMessage() : "";
        String frame = topStackFrame(error);
        String summary = errType + (errMsg.isEmpty() ? "" : ": " + errMsg) + " @ " + frame;
        healthOf(mod).recordCrash(summary, currentTick());
        metric("diag.crash.total");
        metric("diag.crash." + mod);
        logRateLimited("crash:" + mod + ":" + eventName + ":" + errType, 1000L, mod, "MODULE_CRASH",
                "event=" + eventName + " method=" + methodName + " err=" + summary);
        long now = System.currentTimeMillis();
        String toastKey = "crash-toast:" + mod + ":" + errType;
        Long prev = RATE_LIMITS.get(toastKey);
        if (prev == null || now - prev >= 5000L) {
            RATE_LIMITS.put(toastKey, now);
            postDevToast("\u00a7cCRASH in " + mod + " (" + eventName + "): " + errType);
        }
    }

    public static void recordDeny(String moduleName, String category, String reason) {
        if (!INSTALLED.get()) {
            return;
        }
        String mod = (moduleName == null || moduleName.isEmpty()) ? "Client" : moduleName;
        healthOf(mod).recordDeny(currentTick());
        metric("diag.deny." + safe(category));
        metric("diag.deny.mod." + mod);
        logRateLimited("deny:" + mod + ":" + category + ":" + reason, 200L, mod, category + "_DENY", reason);
    }

    public static void recordModuleState(String moduleName, String stateEvent, String detail) {
        if (!INSTALLED.get() || moduleName == null) {
            return;
        }
        ModuleHealth h = healthOf(moduleName);
        h.lastActiveTick = currentTick();
        if ("MODULE_ENABLE".equals(stateEvent)) {
            h.enableTick = h.lastActiveTick;
        }
        metric("diag.lifecycle." + stateEvent);
        log(moduleName, stateEvent, detail);
    }

    public static void recordSettingChange(Setting setting) {
        if (!INSTALLED.get() || setting == null) {
            return;
        }
        String settingName = setting.getName();
        String owner = resolveSettingOwner(settingName);
        String val = formatSettingValue(setting);
        metric("diag.setting_change");
        logRateLimited("set:" + owner + ":" + settingName, 120L, owner, "SETTING_CHANGE",
                safe(settingName) + "=" + safe(val != null ? val : "?"));
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
            boolean rCtrl = InputUtil.isKeyPressed(CLIENT.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
            boolean rShift = InputUtil.isKeyPressed(CLIENT.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean rAlt = InputUtil.isKeyPressed(CLIENT.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
            if (rCtrl) {
                hudOverlayEnabled = !hudOverlayEnabled;
                postDevToast("Dev HUD " + (hudOverlayEnabled ? "ON (" + hudMode + ")" : "OFF"));
            } else if (rShift) {
                HudMode[] modes = HudMode.values();
                hudMode = modes[(hudMode.ordinal() + 1) % modes.length];
                hudOverlayEnabled = true;
                postDevToast("Dev HUD mode -> " + hudMode + (hudMode == HudMode.FILTER ? " [" + (focusedModule.isEmpty() ? "ALL" : focusedModule) + "]" : ""));
            } else if (rAlt) {
                cycleFocusedModule();
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
        if (hudMode == HudMode.HEALTH) {
            renderHealthHud(renderer);
        } else {
            renderStreamHud(renderer, hudMode == HudMode.FILTER ? focusedModule : "");
        }
    }

    private static void renderStreamHud(UiRenderer renderer, String moduleFilter) {
        List<DiagEntry> recent = snapshotRecent(HUD_LINES, moduleFilter);
        float fontSize = 7.0F;
        float lineH = 9.5F;
        float pad = 5.0F;
        float boxW = 365.0F;
        float boxH = pad * 2.0F + lineH * (Math.max(1, recent.size()) + 1);
        float x = 8.0F;
        float y = 8.0F;
        String header = moduleFilter.isEmpty()
                ? "DEV FLIGHT RECORDER [STREAM]  (F9 Dump | RShift+F9 Mode | RCtrl+F9 Hide)"
                : "DEV FLIGHT RECORDER [FILTER: " + moduleFilter + "]  (RAlt+F9 Next)";
        renderer.drawRoundedRect(x, y, boxW, boxH, 4.0F, 0xCC0D1117);
        renderer.drawTextWithShadow(FontAsset.INTER, header, x + pad, y + pad, fontSize, 0xFF38BDF8);
        float curY = y + pad + lineH;
        if (recent.isEmpty()) {
            renderer.drawTextWithShadow(FontAsset.INTER, "(no matching events yet)", x + pad, curY, fontSize, 0xFF94A3B8);
            return;
        }
        for (DiagEntry e : recent) {
            String line = "[T:" + e.tick() + "] " + e.source() + " " + e.event() + " " + e.detail();
            if (line.length() > 78) {
                line = line.substring(0, 75) + "...";
            }
            int color = e.event().contains("CRASH") || e.event().contains("DENY") || e.event().contains("ABORT") || e.event().contains("FAIL")
                    ? 0xFFF87171
                    : (e.event().contains("SLOW") || e.event().contains("SKIP") || e.event().contains("CANCEL") ? 0xFFFACC15 : 0xFFE2E8F0);
            renderer.drawTextWithShadow(FontAsset.INTER, line, x + pad, curY, fontSize, color);
            curY += lineH;
        }
    }

    private static void renderHealthHud(UiRenderer renderer) {
        List<Module> enabled = collectEnabledModules();
        enabled.sort(Comparator
                .comparingLong((Module m) -> -healthOf(m.getName().toString()).crashCount.sum())
                .thenComparingLong(m -> -healthOf(m.getName().toString()).denyCount.sum())
                .thenComparingLong(m -> -healthOf(m.getName().toString()).peakNanos)
                .thenComparing(m -> m.getName().toString()));
        int shown = Math.min(HUD_LINES, enabled.size());
        float fontSize = 7.0F;
        float lineH = 9.5F;
        float pad = 5.0F;
        float boxW = 365.0F;
        float boxH = pad * 2.0F + lineH * (Math.max(1, shown) + 1);
        float x = 8.0F;
        float y = 8.0F;
        renderer.drawRoundedRect(x, y, boxW, boxH, 4.0F, 0xCC0D1117);
        renderer.drawTextWithShadow(FontAsset.INTER,
                "ALL-MODULE HEALTH (" + enabled.size() + " enabled)  [RShift+F9 Mode | F9 Dump]",
                x + pad, y + pad, fontSize, 0xFF34D399);
        float curY = y + pad + lineH;
        if (shown == 0) {
            renderer.drawTextWithShadow(FontAsset.INTER, "(no modules enabled)", x + pad, curY, fontSize, 0xFF94A3B8);
            return;
        }
        for (int i = 0; i < shown; i++) {
            String name = enabled.get(i).getName().toString();
            ModuleHealth h = healthOf(name);
            long calls = h.callCount.sum();
            long denies = h.denyCount.sum();
            long crashes = h.crashCount.sum();
            long cancels = h.cancelCount.sum();
            double peakMs = h.peakNanos / 1_000_000.0;
            String status = crashes > 0 ? "ERR" : (peakMs >= 2.0 ? "SLOW" : (calls == 0 ? "IDLE" : "OK"));
            String line = String.format(Locale.ROOT, "%-16s [%s] calls=%d peak=%.2fms deny=%d cancel=%d err=%d",
                    truncate(name, 16), status, calls, peakMs, denies, cancels, crashes);
            int color = crashes > 0 ? 0xFFF87171 : (denies > 0 || peakMs >= 2.0 ? 0xFFFACC15 : (calls == 0 ? 0xFF94A3B8 : 0xFFE2E8F0));
            renderer.drawTextWithShadow(FontAsset.INTER, line, x + pad, curY, fontSize, color);
            curY += lineH;
        }
    }

    private static void cycleFocusedModule() {
        List<Module> enabled = collectEnabledModules();
        if (enabled.isEmpty()) {
            focusedModule = "";
            postDevToast("Dev Filter: (no enabled modules)");
            return;
        }
        enabled.sort(Comparator.comparing(m -> m.getName().toString()));
        int nextIdx = 0;
        for (int i = 0; i < enabled.size(); i++) {
            if (enabled.get(i).getName().toString().equalsIgnoreCase(focusedModule)) {
                nextIdx = (i + 1) % enabled.size();
                break;
            }
        }
        focusedModule = enabled.get(nextIdx).getName().toString();
        hudMode = HudMode.FILTER;
        hudOverlayEnabled = true;
        postDevToast("Dev Filter -> " + focusedModule);
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
        StringBuilder sb = new StringBuilder(8192);
        sb.append("=== CLUMPS DEV BUG SNAPSHOT (ALL-MODULE) ===\n");
        sb.append("Captured: ").append(Instant.ofEpochMilli(now)).append('\n');
        if (CLIENT != null && CLIENT.player != null) {
            var p = CLIENT.player;
            sb.append(String.format(Locale.ROOT,
                    "Player: pos=(%.2f, %.2f, %.2f) vel=(%.3f, %.3f, %.3f) fall=%.2f onGround=%s gliding=%s slot=%d hand=%s%n",
                    p.getX(), p.getY(), p.getZ(),
                    p.getVelocity().x, p.getVelocity().y, p.getVelocity().z,
                    p.fallDistance, p.isOnGround(), p.isGliding(),
                    p.getInventory().getSelectedSlot(), p.getMainHandStack().getItem()));
        }
        sb.append("InventoryTransferring: ").append(InventoryActionScheduler.isTransferring()).append('\n');

        List<Module> enabled = collectEnabledModules();
        enabled.sort(Comparator.comparing(m -> m.getName().toString()));
        sb.append("\n--- ENABLED MODULE HEALTH & CONFIGS (").append(enabled.size()).append(") ---\n");
        for (Module m : enabled) {
            String name = m.getName().toString();
            ModuleHealth h = healthOf(name);
            long calls = h.callCount.sum();
            double avgUs = calls > 0 ? (h.totalNanos.sum() / (double) calls) / 1000.0 : 0.0;
            double peakMs = h.peakNanos / 1_000_000.0;
            String flag = h.crashCount.sum() > 0 ? "CRASH" : (peakMs >= 2.0 ? "SLOW" : (calls == 0 ? "IDLE" : "OK"));
            sb.append(String.format(Locale.ROOT,
                    "  %-20s [%s] calls=%d avg=%.1fus peak=%.2fms denies=%d cancels=%d crashes=%d%n",
                    name, flag, calls, avgUs, peakMs, h.denyCount.sum(), h.cancelCount.sum(), h.crashCount.sum()));
            if (h.lastCrashSummary != null && !h.lastCrashSummary.isEmpty()) {
                sb.append("      lastCrash: ").append(h.lastCrashSummary).append('\n');
            }
            String settingsLine = summarizeSettings(m);
            if (!settingsLine.isEmpty()) {
                sb.append("      settings: ").append(settingsLine).append('\n');
            }
        }

        sb.append("\n--- SESSION METRICS SCORECARD ---\n");
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

    private static String summarizeSettings(Module module) {
        if (module == null || module.settings == null || module.settings.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Setting s : module.settings) {
            if (s == null || !s.isVisible()) {
                continue;
            }
            String val = formatSettingValue(s);
            if (val == null) {
                continue;
            }
            if (shown > 0) {
                sb.append(", ");
            }
            sb.append(s.getName()).append('=').append(val);
            if (++shown >= 10) {
                if (module.settings.size() > shown) {
                    sb.append(", ...");
                }
                break;
            }
        }
        return sb.toString();
    }

    private static String formatSettingValue(Setting s) {
        if (s instanceof BooleanSetting b) return String.valueOf(b.getValue());
        if (s instanceof ChoiceSetting c) return String.valueOf(c.getValue());
        if (s instanceof FloatSetting f) return String.format(Locale.ROOT, "%.2f", f.getValue());
        if (s instanceof IntSetting i) return String.valueOf(i.getValue());
        if (s instanceof FloatRangeSetting r) return String.format(Locale.ROOT, "%.1f..%.1f", r.getLower(), r.getUpper());
        if (s instanceof TextSetting t) return String.valueOf(t.getText());
        return null;
    }

    private static String resolveSettingOwner(String settingName) {
        if (settingName == null) {
            return "Setting";
        }
        for (Module m : ModuleManager.allFeatures()) {
            if (m == null || m.settings == null) {
                continue;
            }
            for (Setting s : m.settings) {
                if (s != null && settingName.equals(s.getName())) {
                    return m.getName().toString();
                }
            }
        }
        return "Setting";
    }

    private static List<Module> collectEnabledModules() {
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.allFeatures()) {
            if (m != null && m.isEnabled()) {
                out.add(m);
            }
        }
        return out;
    }

    private static ModuleHealth healthOf(String moduleName) {
        return MODULE_HEALTH.computeIfAbsent(moduleName, k -> new ModuleHealth());
    }

    private static int currentTick() {
        return CLIENT != null && CLIENT.player != null ? CLIENT.player.age : -1;
    }

    private static String topStackFrame(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        StackTraceElement[] trace = error.getStackTrace();
        if (trace == null || trace.length == 0) {
            return "no-stack";
        }
        for (StackTraceElement el : trace) {
            if (el.getClassName().startsWith("com.blamejared.clumps")) {
                return el.getClassName().substring(el.getClassName().lastIndexOf('.') + 1)
                        + "." + el.getMethodName() + ":" + el.getLineNumber();
            }
        }
        StackTraceElement first = trace[0];
        return first.getClassName() + "." + first.getMethodName() + ":" + first.getLineNumber();
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
        sb.append("\n=== PER-MODULE SESSION HEALTH ===\n");
        TreeMap<String, ModuleHealth> sortedHealth = new TreeMap<>(MODULE_HEALTH);
        for (Map.Entry<String, ModuleHealth> e : sortedHealth.entrySet()) {
            ModuleHealth h = e.getValue();
            long calls = h.callCount.sum();
            double avgUs = calls > 0 ? (h.totalNanos.sum() / (double) calls) / 1000.0 : 0.0;
            sb.append(String.format(Locale.ROOT,
                    "  %-22s calls=%d avg=%.1fus peak=%.2fms denies=%d cancels=%d crashes=%d%s%n",
                    e.getKey(), calls, avgUs, h.peakNanos / 1_000_000.0,
                    h.denyCount.sum(), h.cancelCount.sum(), h.crashCount.sum(),
                    h.lastCrashSummary != null ? " lastCrash=" + h.lastCrashSummary : ""));
        }
        sb.append("\n=== SESSION METRICS SCORECARD ===\n");
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

    private static List<DiagEntry> snapshotRecent(int maxLines, String moduleFilter) {
        List<DiagEntry> matched = new ArrayList<>(maxLines);
        synchronized (RING) {
            if (moduleFilter == null || moduleFilter.isEmpty()) {
                int skip = Math.max(0, RING.size() - maxLines);
                int idx = 0;
                for (DiagEntry e : RING) {
                    if (idx++ >= skip) {
                        matched.add(e);
                    }
                }
            } else {
                for (DiagEntry e : RING) {
                    if (e.source().equalsIgnoreCase(moduleFilter) || e.detail().contains(moduleFilter)) {
                        matched.add(e);
                    }
                }
                if (matched.size() > maxLines) {
                    matched = new ArrayList<>(matched.subList(matched.size() - maxLines, matched.size()));
                }
            }
        }
        return matched;
    }

    private static void postDevToast(String msg) {
        try {
            if (CLIENT != null && CLIENT.player != null) {
                CLIENT.player.sendMessage(Text.literal("\u00a7b[DevDiag] \u00a7f" + msg), false);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ');
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class ModuleHealth {
        final LongAdder callCount = new LongAdder();
        final LongAdder totalNanos = new LongAdder();
        final LongAdder denyCount = new LongAdder();
        final LongAdder cancelCount = new LongAdder();
        final LongAdder crashCount = new LongAdder();
        volatile long peakNanos;
        volatile int lastActiveTick = -1;
        volatile int enableTick = -1;
        volatile String lastCrashSummary;

        void recordCall(long nanos, int tick, boolean cancelled) {
            callCount.increment();
            totalNanos.add(nanos);
            if (nanos > peakNanos) {
                peakNanos = nanos;
            }
            lastActiveTick = tick;
            if (cancelled) {
                cancelCount.increment();
            }
        }

        void recordDeny(int tick) {
            denyCount.increment();
            lastActiveTick = tick;
        }

        void recordCrash(String summary, int tick) {
            crashCount.increment();
            lastCrashSummary = summary;
            lastActiveTick = tick;
        }
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
