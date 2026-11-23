package com.blamejared.clumps.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ProfileStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_STORE_BYTES = 33554432;
    private static final int PREF_CHUNK_CHARS = 6000;
    private static final String PREF_COUNT_KEY = "count";
    private static final String PREF_PART_PREFIX = "p";
    private static final String PREF_BACKUP_COUNT_KEY = "b_count";
    private static final String PREF_BACKUP_PART_PREFIX = "b";

    private final Path directory;
    private final Preferences preferences;

    public record SaveRequest(String id, String name, String description, String visibility, String type, String data) { }

    public ProfileStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        this.preferences = null;
    }

    public ProfileStore(Preferences preferences) {
        this.directory = null;
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    public boolean isAvailable() {
        return true;
    }

    public JsonObject listProfiles() {
        return JsonParser.parseString(executeOperation("list_profiles_json", null)).getAsJsonObject();
    }

    public JsonObject getProfile(String id) {
        return JsonParser.parseString(executeOperation("get_profile_json", new String[]{id})).getAsJsonObject();
    }

    public JsonObject saveProfile(SaveRequest request) {
        String[] arguments = request == null ? null : new String[]{request.id(), request.name(), request.description(), request.visibility(), request.type(), request.data()};
        return JsonParser.parseString(executeOperation("save_profile_json", arguments)).getAsJsonObject();
    }

    public JsonObject selectProfile(String id) {
        return JsonParser.parseString(executeOperation("select_profile_json", new String[]{id})).getAsJsonObject();
    }

    public JsonObject deleteProfile(String id) {
        return JsonParser.parseString(executeOperation("delete_profile_json", new String[]{id})).getAsJsonObject();
    }

    public synchronized String executeOperation(String operation, String[] arguments) {
        if ("is_available".equals(operation)) {
            return "true";
        }
        if (!Set.of("list_profiles_json", "get_profile_json", "save_profile_json", "select_profile_json", "delete_profile_json").contains(operation == null ? "" : operation)) {
            return JSON.toJson(failureResponse("This operation is unavailable offline"));
        }
        try {
            if (this.preferences != null) {
                return executeWithStore(operation, arguments);
            }
            Files.createDirectories(this.directory);
            try (FileChannel channel = FileChannel.open(this.directory.resolve("profiles.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    FileLock lock = channel.tryLock()) {
                if (lock == null) {
                    return JSON.toJson(failureResponse("Local configs are in use by another instance"));
                }
                return executeWithStore(operation, arguments);
            }
        } catch (Exception exception) {
            String error = exception instanceof IllegalArgumentException ? exception.getMessage()
                    : "Could not read or save local configs (" + exception.getClass().getSimpleName() + ")";
            return JSON.toJson(failureResponse(error));
        }
    }

    private String executeWithStore(String operation, String[] arguments) throws IOException {
        JsonObject store = this.readStore();
        JsonObject response;
        boolean changed = false;
        switch (operation) {
            case "list_profiles_json": {
                response = successResponse(store);
                JsonArray configs = new JsonArray();
                for (JsonElement element : store.getAsJsonArray("profiles")) {
                    configs.add(profileMetadata(element.getAsJsonObject(), textField(store, "activeConfigId")));
                }
                response.add("configs", configs);
                break;
            }
            case "get_profile_json": {
                String id = argument(arguments, 0);
                if (id.isEmpty()) {
                    id = textField(store, "activeConfigId");
                }
                JsonObject profile = findProfile(store, id);
                response = profile == null
                        ? failureResponse("Local config not found") : profileResponse(store, profile);
                break;
            }
            case "save_profile_json": {
                if (arguments == null || arguments.length != 6) {
                    throw new IllegalArgumentException("Invalid config save arguments");
                }
                String id = argument(arguments, 0);
                String name = argument(arguments, 1);
                String data = argument(arguments, 5);
                if (name.isBlank()) {
                    throw new IllegalArgumentException("Config name is required");
                }
                JsonObject profile = id.isEmpty() ? null : findProfile(store, id);
                if (!id.isEmpty() && profile == null) {
                    throw new IllegalArgumentException("Local config not found");
                }
                if (!data.isEmpty() && !JsonParser.parseString(data).isJsonObject()) {
                    throw new IllegalArgumentException("Config data must be a JSON object");
                }
                long now = System.currentTimeMillis();
                if (profile == null) {
                    profile = new JsonObject();
                    JsonObject config = new JsonObject();
                    id = UUID.randomUUID().toString();
                    config.addProperty("id", id);
                    config.addProperty("author", "Local");
                    config.addProperty("createdAt", now);
                    for (String field : new String[]{"serverName", "serverIp", "serverIconBase64", "sourceId", "publishedId"}) {
                        config.addProperty(field, "");
                    }
                    config.addProperty("owned", true);
                    config.addProperty("publishedChanges", false);
                    config.addProperty("updateAvailable", false);
                    profile.add("config", config);
                    profile.addProperty("data", "{}");
                    store.getAsJsonArray("profiles").add(profile);
                    if (textField(store, "activeConfigId").isEmpty()) {
                        store.addProperty("activeConfigId", id);
                    }
                }
                JsonObject config = profile.getAsJsonObject("config");
                config.addProperty("name", name);
                config.addProperty("description", argument(arguments, 2));
                config.addProperty("visibility", "public".equalsIgnoreCase(argument(arguments, 3)) ? "public" : "private");
                config.addProperty("type", "rage".equalsIgnoreCase(argument(arguments, 4)) ? "rage" : "legit");
                config.addProperty("updatedAt", now);
                if (!data.isEmpty()) {
                    profile.addProperty("data", data);
                }
                response = profileResponse(store, profile);
                changed = true;
                break;
            }
            case "select_profile_json": {
                String id = argument(arguments, 0);
                JsonObject profile = findProfile(store, id);
                if (profile == null && !id.isEmpty()) {
                    throw new IllegalArgumentException("Local config not found");
                }
                store.addProperty("activeConfigId", id);
                response = profile == null ? successResponse(store) : profileResponse(store, profile);
                if (profile == null) {
                    response.add("config", JsonNull.INSTANCE);
                    response.addProperty("data", "");
                }
                changed = true;
                break;
            }
            case "delete_profile_json": {
                String id = argument(arguments, 0);
                JsonObject profile = findProfile(store, id);
                if (profile == null) {
                    throw new IllegalArgumentException("Local config not found");
                }
                store.getAsJsonArray("profiles").remove(profile);
                if (id.equals(textField(store, "activeConfigId"))) {
                    store.addProperty("activeConfigId", "");
                }
                response = successResponse(store);
                response.addProperty("deletedConfigId", id);
                changed = true;
                break;
            }
            default: {
                throw new AssertionError(operation);
            }
        }
        if (changed) {
            this.writeStore(store);
        }
        return JSON.toJson(response);
    }

    public synchronized void importRawStoreJson(String rawJson) throws IOException {
        JsonObject store = parseAndValidateStore(rawJson);
        writeStore(store);
    }

    private JsonObject readStore() throws IOException {
        if (this.preferences != null) {
            return readPreferencesStore();
        }
        Path file = this.directory.resolve("profiles.json");
        if (!Files.exists(file)) {
            return emptyStore();
        }
        if (Files.size(file) > MAX_STORE_BYTES) {
            throw new IOException("Config store is too large");
        }
        return parseAndValidateStore(Files.readString(file, StandardCharsets.UTF_8));
    }

    private JsonObject readPreferencesStore() throws IOException {
        try {
            this.preferences.sync();
        } catch (BackingStoreException ignored) {
        }
        String primary = readPreferenceChunks(PREF_COUNT_KEY, PREF_PART_PREFIX);
        if (primary != null) {
            try {
                return parseAndValidateStore(decompressFromBase64(primary));
            } catch (Exception primaryCorrupt) {
                String backup = readPreferenceChunks(PREF_BACKUP_COUNT_KEY, PREF_BACKUP_PART_PREFIX);
                if (backup != null) {
                    return parseAndValidateStore(decompressFromBase64(backup));
                }
                throw new IOException("Corrupt config store", primaryCorrupt);
            }
        }
        String backup = readPreferenceChunks(PREF_BACKUP_COUNT_KEY, PREF_BACKUP_PART_PREFIX);
        if (backup != null) {
            return parseAndValidateStore(decompressFromBase64(backup));
        }
        return emptyStore();
    }

    private String readPreferenceChunks(String countKey, String partPrefix) {
        int count = this.preferences.getInt(countKey, 0);
        if (count <= 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder(count * PREF_CHUNK_CHARS);
        for (int i = 0; i < count; i++) {
            String chunk = this.preferences.get(partPrefix + i, null);
            if (chunk == null) {
                return null;
            }
            builder.append(chunk);
        }
        return builder.toString();
    }

    private void writeStore(JsonObject store) throws IOException {
        byte[] bytes = JSON.toJson(store).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STORE_BYTES) {
            throw new IOException("Config store is too large");
        }
        if (this.preferences != null) {
            writePreferencesStore(bytes);
            return;
        }
        Path file = this.directory.resolve("profiles.json");
        Path temporaryFile = Files.createTempFile(this.directory, "profiles-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporaryFile, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            if (Files.exists(file)) {
                Files.copy(file, this.directory.resolve("profiles.json.bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void writePreferencesStore(byte[] utf8Bytes) throws IOException {
        String existingPrimary = readPreferenceChunks(PREF_COUNT_KEY, PREF_PART_PREFIX);
        if (existingPrimary != null) {
            writePreferenceChunks(PREF_BACKUP_COUNT_KEY, PREF_BACKUP_PART_PREFIX, existingPrimary);
        }
        String encoded = compressToBase64(utf8Bytes);
        writePreferenceChunks(PREF_COUNT_KEY, PREF_PART_PREFIX, encoded);
        try {
            this.preferences.flush();
        } catch (BackingStoreException exception) {
            throw new IOException("Failed to flush config preferences", exception);
        }
    }

    private void writePreferenceChunks(String countKey, String partPrefix, String encoded) {
        int previousCount = this.preferences.getInt(countKey, 0);
        int chunks = Math.max(1, (encoded.length() + PREF_CHUNK_CHARS - 1) / PREF_CHUNK_CHARS);
        for (int i = 0; i < chunks; i++) {
            int start = i * PREF_CHUNK_CHARS;
            int end = Math.min(encoded.length(), start + PREF_CHUNK_CHARS);
            this.preferences.put(partPrefix + i, encoded.substring(start, end));
        }
        this.preferences.putInt(countKey, chunks);
        for (int i = chunks; i < previousCount; i++) {
            this.preferences.remove(partPrefix + i);
        }
    }

    private static String compressToBase64(byte[] input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(256, input.length / 3));
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(input);
        }
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static String decompressFromBase64(String encoded) throws IOException {
        byte[] compressed = Base64.getDecoder().decode(encoded);
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 3);
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                total += read;
                if (total > MAX_STORE_BYTES) {
                    throw new IOException("Config store is too large");
                }
                out.write(buffer, 0, read);
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static JsonObject emptyStore() {
        JsonObject store = new JsonObject();
        store.addProperty("formatVersion", 1);
        store.addProperty("activeConfigId", "");
        store.add("profiles", new JsonArray());
        return store;
    }

    private static JsonObject parseAndValidateStore(String rawJson) throws IOException {
        JsonObject store = JsonParser.parseString(rawJson).getAsJsonObject();
        if (store.get("formatVersion").getAsInt() != 1 || !store.get("profiles").isJsonArray()) {
            throw new IOException("Unsupported config store format");
        }
        Set<String> ids = new HashSet<>();
        for (JsonElement element : store.getAsJsonArray("profiles")) {
            JsonObject profile = element.getAsJsonObject();
            JsonObject config = profile.getAsJsonObject("config");
            String id = textField(config, "id");
            if (id.isEmpty() || !ids.add(id) || !JsonParser.parseString(textField(profile, "data")).isJsonObject()) {
                throw new IOException("Invalid config store");
            }
            for (String field : new String[]{"name", "description", "author", "visibility", "type", "serverName", "serverIp", "serverIconBase64", "sourceId", "publishedId"}) {
                textField(config, field);
            }
            config.get("createdAt").getAsLong();
            config.get("updatedAt").getAsLong();
        }
        String activeId = textField(store, "activeConfigId");
        if (!activeId.isEmpty() && !ids.contains(activeId)) {
            throw new IOException("Invalid selected config");
        }
        return store;
    }

    private static String textField(JsonObject object, String key) {
        return object.get(key).getAsString();
    }

    private static String argument(String[] arguments, int index) {
        return arguments == null || index >= arguments.length || arguments[index] == null ? "" : arguments[index];
    }

    private static JsonObject findProfile(JsonObject store, String id) {
        for (JsonElement element : store.getAsJsonArray("profiles")) {
            JsonObject profile = element.getAsJsonObject();
            if (id.equals(textField(profile.getAsJsonObject("config"), "id"))) {
                return profile;
            }
        }
        return null;
    }

    private static JsonObject profileMetadata(JsonObject profile, String activeId) {
        JsonObject config = profile.getAsJsonObject("config").deepCopy();
        config.addProperty("active", activeId.equals(textField(config, "id")));
        return config;
    }

    private static JsonObject successResponse(JsonObject store) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("error", "");
        response.addProperty("activeConfigId", textField(store, "activeConfigId"));
        return response;
    }

    private static JsonObject profileResponse(JsonObject store, JsonObject profile) {
        JsonObject response = successResponse(store);
        response.add("config", profileMetadata(profile, textField(store, "activeConfigId")));
        response.addProperty("data", textField(profile, "data"));
        return response;
    }

    private static JsonObject failureResponse(String error) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", error);
        return response;
    }
}
