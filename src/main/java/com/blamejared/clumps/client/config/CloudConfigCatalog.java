package com.blamejared.clumps.client.config;

import com.blamejared.clumps.client.updater.AutoUpdater;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class CloudConfigCatalog {
    public static final String CLOUD_ID_PREFIX = "cloud:";
    public static final String SHARE_PREFIX = "CFG1:";

    private static final String CATALOG_FILE = "configs.json";
    private static final long CACHE_TTL_MS = 5L * 60L * 1000L;
    private static final int MAX_SHARE_PAYLOAD_BYTES = 2 * 1024 * 1024;
    private static final Gson COMPACT_GSON = new GsonBuilder().create();

    private static volatile List<CloudEntry> cachedEntries = List.of();
    private static volatile long lastFetchAtMs = 0L;
    private static final AtomicBoolean REFRESHING = new AtomicBoolean(false);

    public record CloudEntry(StoreMeta meta, String dataJson) { }

    public record DecodedShare(String name, String description, String type, String author, String dataJson) { }

    private CloudConfigCatalog() { }

    public static boolean isCloudId(String id) {
        return id != null && id.startsWith(CLOUD_ID_PREFIX);
    }

    public static List<StoreMeta> cachedMetas() {
        refreshIfStaleAsync(null);
        List<CloudEntry> entries = cachedEntries;
        if (entries.isEmpty()) {
            return List.of();
        }
        List<StoreMeta> metas = new ArrayList<>(entries.size());
        for (CloudEntry entry : entries) {
            metas.add(entry.meta());
        }
        return metas;
    }

    public static CloudEntry findById(String cloudId) {
        if (!isCloudId(cloudId)) {
            return null;
        }
        for (CloudEntry entry : cachedEntries) {
            if (cloudId.equals(entry.meta().id())) {
                return entry;
            }
        }
        return null;
    }

    public static boolean isRefreshing() {
        return REFRESHING.get();
    }

    public static void refreshIfStaleAsync(Runnable onComplete) {
        long now = System.currentTimeMillis();
        if (lastFetchAtMs > 0L && (now - lastFetchAtMs) < CACHE_TTL_MS) {
            return;
        }
        refreshAsync(onComplete);
    }

    public static void refreshAsync(Runnable onComplete) {
        if (Boolean.getBoolean("clumps.noCloudConfigs")) {
            return;
        }
        if (!REFRESHING.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                String raw = fetchCatalogText();
                if (raw != null && !raw.isBlank()) {
                    List<CloudEntry> parsed = parseCatalogJson(raw);
                    cachedEntries = List.copyOf(parsed);
                    lastFetchAtMs = System.currentTimeMillis();
                } else if (lastFetchAtMs == 0L) {
                    // Avoid tight retry loops when offline; retry after a short backoff.
                    lastFetchAtMs = System.currentTimeMillis() - CACHE_TTL_MS + 30_000L;
                }
            } catch (Exception ignored) {
            } finally {
                REFRESHING.set(false);
                if (onComplete != null) {
                    try {
                        onComplete.run();
                    } catch (Exception ignored) {
                    }
                }
            }
        }, "Netty-Catalog-Worker");
        worker.setDaemon(true);
        worker.start();
    }

    static void seedCacheForTests(List<CloudEntry> entries) {
        cachedEntries = entries == null ? List.of() : List.copyOf(entries);
        lastFetchAtMs = System.currentTimeMillis();
    }

    private static String fetchCatalogText() {
        String[] endpoints = AutoUpdater.catalogEndpoints(CATALOG_FILE);
        if (endpoints.length == 0) {
            return null;
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        for (int i = 0; i < endpoints.length; i++) {
            String url = endpoints[i];
            if (url == null || url.isBlank()) {
                continue;
            }
            if (i > 0) {
                url = url + "?t=" + (System.currentTimeMillis() / 1000L);
            }
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "Fabric-Loader/0.16")
                        .header("Cache-Control", "no-cache");
                if (i == 0) {
                    builder.header("Accept", "application/vnd.github.raw+json");
                }
                HttpResponse<String> response = client.send(builder.GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                    String body = unwrapGithubContentsIfNeeded(response.body());
                    if (body != null && !body.isBlank()) {
                        return body;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String unwrapGithubContentsIfNeeded(String body) {
        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            return trimmed;
        }
        try {
            JsonObject obj = JsonParser.parseString(trimmed).getAsJsonObject();
            if (obj.has("encoding") && "base64".equalsIgnoreCase(ProfileCodec.textField(obj, "encoding"))
                    && obj.has("content") && !obj.get("content").isJsonNull()) {
                String b64 = obj.get("content").getAsString().replaceAll("\\s+", "");
                return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return trimmed;
    }

    public static List<CloudEntry> parseCatalogJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(rawJson);
        } catch (Exception ignored) {
            return List.of();
        }
        JsonArray items;
        if (root.isJsonArray()) {
            items = root.getAsJsonArray();
        } else if (root.isJsonObject() && root.getAsJsonObject().has("configs")
                && root.getAsJsonObject().get("configs").isJsonArray()) {
            items = root.getAsJsonObject().getAsJsonArray("configs");
        } else {
            return List.of();
        }

        List<CloudEntry> results = new ArrayList<>();
        for (JsonElement element : items) {
            if (!element.isJsonObject()) {
                continue;
            }
            CloudEntry parsed = parseCatalogEntry(element.getAsJsonObject());
            if (parsed != null) {
                results.add(parsed);
            }
        }
        return results;
    }

    private static CloudEntry parseCatalogEntry(JsonObject obj) {
        String name = ProfileCodec.textField(obj, "name").trim();
        if (name.isEmpty()) {
            return null;
        }
        String rawId = ProfileCodec.textField(obj, "id").trim();
        if (rawId.isEmpty()) {
            rawId = slugify(name);
        }
        String id = rawId.startsWith(CLOUD_ID_PREFIX) ? rawId : CLOUD_ID_PREFIX + rawId;
        String description = ProfileCodec.textField(obj, "description");
        String author = ProfileCodec.textField(obj, "author", "Community");
        if (author.isBlank()) {
            author = "Community";
        }
        String serverName = ProfileCodec.textField(obj, "serverName");
        String serverIp = ProfileCodec.textField(obj, "serverIp");
        String type = "RAGE".equalsIgnoreCase(ProfileCodec.textField(obj, "type")) ? "RAGE" : "GHOST";
        long createdAt = ProfileCodec.timestampField(obj, "createdAt");
        long updatedAt = ProfileCodec.timestampField(obj, "updatedAt");
        if (updatedAt <= 0L) {
            updatedAt = createdAt;
        }

        String dataJson = extractSnapshotJson(obj.get("data"));
        if (dataJson == null) {
            return null;
        }

        StoreMeta meta = new StoreMeta(
                id,
                name,
                description,
                author,
                serverName,
                serverIp,
                null,
                type,
                true,
                createdAt,
                updatedAt,
                false,
                false,
                "",
                "",
                false,
                false
        );
        return new CloudEntry(meta, dataJson);
    }

    public static String encodeShareCode(String name, String description, String type, String author, String dataJson) {
        String cleanData = extractSnapshotJson(parseElement(dataJson));
        if (cleanData == null) {
            return null;
        }
        JsonObject snapshot = JsonParser.parseString(cleanData).getAsJsonObject();
        snapshot.remove("friends");

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("v", 1);
        wrapper.addProperty("name", name == null || name.isBlank() ? "Shared Config" : name.trim());
        wrapper.addProperty("description", description == null ? "" : description.trim());
        wrapper.addProperty("type", "RAGE".equalsIgnoreCase(type) ? "RAGE" : "GHOST");
        wrapper.addProperty("author", author == null || author.isBlank() ? "Player" : author.trim());
        wrapper.add("data", snapshot);

        byte[] utf8 = COMPACT_GSON.toJson(wrapper).getBytes(StandardCharsets.UTF_8);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(128, utf8.length / 2));
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(utf8);
            }
            return SHARE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        } catch (IOException ignored) {
            return null;
        }
    }

    public static DecodedShare decodeShareCode(String rawInput) {
        if (rawInput == null) {
            return null;
        }
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String jsonText;
        if (trimmed.length() > SHARE_PREFIX.length()
                && trimmed.regionMatches(true, 0, SHARE_PREFIX, 0, SHARE_PREFIX.length())) {
            String payload = trimmed.substring(SHARE_PREFIX.length()).replaceAll("\\s+", "");
            try {
                byte[] compressed = Base64.getUrlDecoder().decode(payload);
                jsonText = gunzipBounded(compressed);
            } catch (Exception ignored) {
                return null;
            }
        } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            jsonText = trimmed;
        } else {
            return null;
        }

        JsonElement parsed = parseElement(jsonText);
        if (parsed == null || !parsed.isJsonObject()) {
            return null;
        }
        JsonObject obj = parsed.getAsJsonObject();
        if (obj.has("data")) {
            String dataJson = extractSnapshotJson(obj.get("data"));
            if (dataJson == null) {
                return null;
            }
            String name = ProfileCodec.textField(obj, "name", "Imported Config").trim();
            if (name.isEmpty()) {
                name = "Imported Config";
            }
            String description = ProfileCodec.textField(obj, "description");
            String type = "RAGE".equalsIgnoreCase(ProfileCodec.textField(obj, "type")) ? "RAGE" : "GHOST";
            String author = ProfileCodec.textField(obj, "author", "Shared");
            return new DecodedShare(name, description, type, author, dataJson);
        }
        // Fallback: direct raw snapshot JSON with a "settings" array
        String directData = extractSnapshotJson(obj);
        if (directData == null) {
            return null;
        }
        return new DecodedShare("Imported Config", "", "GHOST", "Shared", directData);
    }

    private static String extractSnapshotJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        JsonObject snapshot;
        if (element.isJsonObject()) {
            snapshot = element.getAsJsonObject();
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            JsonElement inner = parseElement(element.getAsString());
            if (inner == null || !inner.isJsonObject()) {
                return null;
            }
            snapshot = inner.getAsJsonObject();
        } else {
            return null;
        }
        if (!snapshot.has("settings") || !snapshot.get("settings").isJsonArray()) {
            return null;
        }
        return COMPACT_GSON.toJson(snapshot);
    }

    private static JsonElement parseElement(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return JsonParser.parseString(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String gunzipBounded(byte[] compressed) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(compressed.length * 3, 65536));
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                total += read;
                if (total > MAX_SHARE_PAYLOAD_BYTES) {
                    throw new IOException("Share code exceeds maximum size");
                }
                out.write(buffer, 0, read);
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String slugify(String name) {
        String lower = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        lower = lower.replaceAll("^-+|-+$", "");
        return lower.isEmpty() ? "config" : lower;
    }
}
