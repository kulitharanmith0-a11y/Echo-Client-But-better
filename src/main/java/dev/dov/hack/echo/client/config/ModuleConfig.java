package dev.dov.hack.echo.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleManager;
import dev.dov.hack.echo.client.modules.internals.ClickGui;
import dev.dov.hack.echo.client.friends.FriendManager;
import dev.dov.hack.echo.client.settings.Setting;
import dev.dov.hack.echo.client.utils.strings.Concat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class ModuleConfig implements AutoCloseable {
    private final Gson json = new GsonBuilder().setPrettyPrinting().create();
    private final ClientState clientState;
    private final ProfileIo io;
    private final ProfileCatalog catalog;
    private boolean preserveClickGuiBindings;
    private boolean skipFriendsOnApply;

    
    public boolean loading;

    
    public interface ClientState {
        
        ModuleManager features();
        
        FriendManager friends();
        
        ServerInfo server();
        
        boolean writesDisabled();
        
        boolean isClickGui(Module feature);
    }

    
    public ModuleConfig() {
        this(ProfileService.forClient(), new LiveClientState());
    }

    
    public ModuleConfig(ProfileService profiles, ClientState clientState) {
        this.clientState = Objects.requireNonNull(clientState, "clientState");
        io = new ProfileIo(profiles);
        catalog = new ProfileCatalog(io);
    }

    
    public String getActiveConfigId() {
        return catalog.activeConfigId();
    }

    
    public boolean isAvailable() {
        return catalog.isAvailable();
    }

    
    public List<StoreMeta> list() {
        return catalog.refresh();
    }

    
    public List<String> names() {
        return catalog.names();
    }

    
    public CompletableFuture<List<StoreMeta>> listAsync() {
        return io.submit(this::list);
    }

    
    public JsonObject capture() {
        JsonObject snapshot = new JsonObject();
        JsonArray features = new JsonArray();
        if (clientState.features() == null || clientState.features().getFeatures() == null) {
            snapshot.add("settings", features);
            snapshot.add("friends", new JsonArray());
            snapshot.add("server", new JsonObject());
            return snapshot;
        }
        List<Module> current = clientState.features().getFeatures();
        for (Module feature : current) {
            if (!feature.isPersistent()) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("name", Concat.shortHash(feature.getName()));
            entry.addProperty("enabled", feature.persistsEnabledState() && feature.isEnabled());
            entry.addProperty("keybind", feature.getKey().toString());
            entry.addProperty("hold", feature.isHoldMode());
            entry.addProperty("vis", feature.isVisible());
            if (feature.hasSettings()) {
                JsonObject settings = new JsonObject();
                Set<String> usedKeys = new HashSet<>();
                for (Setting setting : feature.settings) {
                    String key = SettingCodec.uniqueKey(setting, usedKeys);
                    try {
                        JsonElement value = SettingCodec.encode(setting);
                        if (value != null) {
                            settings.add(key, value);
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (!settings.entrySet().isEmpty()) {
                    entry.add("settings", settings);
                }
            }
            features.add(entry);
        }
        captureServer(snapshot);
        captureFriends(snapshot);
        snapshot.add("settings", features);
        return snapshot;
    }

    
    public String serialize() {
        return json.toJson(capture());
    }

    
    public boolean apply(JsonObject snapshot) {
        if (snapshot == null || !snapshot.has("settings")) {
            return false;
        }
        JsonArray entries = snapshot.getAsJsonArray("settings");
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String nameHash = ProfileCodec.textField(entry, "name");
            Module feature = findFeature(nameHash);
            if (feature == null || !feature.isPersistent()) {
                continue;
            }
            if (entry.has("enabled") && feature.persistsEnabledState()) {
                feature.setEnabled(entry.get("enabled").getAsBoolean());
            }
            boolean keepBinding = preserveClickGuiBindings && clientState.isClickGui(feature);
            if (!keepBinding && entry.has("keybind")) {
                feature.setKey(SettingCodec.readKey(entry.get("keybind")));
            }
            if (!keepBinding && entry.has("hold")) {
                feature.setHoldMode(entry.get("hold").getAsBoolean());
            }
            if (entry.has("vis")) {
                feature.setVisible(entry.get("vis").getAsBoolean());
            }
            if (!entry.has("settings")) {
                continue;
            }
            JsonObject settings = entry.getAsJsonObject("settings");
            Set<String> usedKeys = new HashSet<>();
            for (Setting setting : feature.settings) {
                String key = SettingCodec.uniqueKey(setting, usedKeys);
                if (!settings.has(key)) {
                    continue;
                }
                try {
                    SettingCodec.apply(setting, settings.get(key));
                } catch (Exception ignored) {
                }
            }
        }
        if (!skipFriendsOnApply) {
            applyFriends(snapshot);
        }
        preserveClickGuiBindings = true;
        return true;
    }

    public boolean loadForAutoConfig(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        skipFriendsOnApply = true;
        try {
            return load(name);
        } finally {
            skipFriendsOnApply = false;
        }
    }

    
    public boolean loadSnapshot(String data) {
        loading = true;
        try {
            return apply(json.fromJson(data, JsonObject.class));
        } catch (Exception failure) {
            return false;
        } finally {
            loading = false;
        }
    }

    
    public boolean load(String name) {
        if (!io.isAvailable()) {
            return false;
        }
        if ("_autosave".equals(name)) {
            return loadById("");
        }
        StoreMeta profile = catalog.findByName(name, false);
        return profile != null && loadById(profile.id());
    }

    
    public boolean loadById(String id) {
        JsonObject response = io.getProfile(id == null ? "" : id);
        if (!ProfileCodec.isSuccessful(response)) {
            return false;
        }
        catalog.acceptActiveConfigId(ProfileCodec.textField(response, "activeConfigId"));
        String data = ProfileCodec.textField(response, "data");
        return data.isBlank() || loadSnapshot(data);
    }

    
    public boolean save(String name) {
        if (clientState.writesDisabled()) {
            return false;
        }
        return catalog.saveSnapshot(name, serialize());
    }

    
    public void saveAsync(String name) {
        if (clientState.writesDisabled()) {
            return;
        }
        JsonObject snapshot = capture();
        io.execute(() -> saveCapturedSnapshot(name, snapshot));
    }

    private void saveCapturedSnapshot(String name, JsonObject snapshot) {
        catalog.saveSnapshot(name, json.toJson(snapshot));
    }

    
    public StoreMeta create(String name, String description, String type) {
        if (clientState.writesDisabled()) {
            return null;
        }
        String data = serialize();
        return io.isAvailable() ? catalog.save("", name, description, false, type, data) : null;
    }

    
    public StoreMeta updatePrivate(String id, String name, String description, String type, boolean captureState) {
        if (clientState.writesDisabled()) {
            return null;
        }
        String data = captureState ? serialize() : "";
        return io.isAvailable() ? catalog.save(id, name, description, false, type, data) : null;
    }

    
    public StoreMeta updatePublic(String id, String name, String description, String type, boolean captureState) {
        if (clientState.writesDisabled() || !io.isAvailable()) {
            return null;
        }
        return catalog.save(id, name, description, true, type, captureState ? serialize() : "");
    }

    
    public StoreMeta updateMetadataAndApply(String id, String name, String description, String type) {
        if (clientState.writesDisabled() || id == null || id.isBlank() || !io.isAvailable()) {
            return null;
        }
        return applyProfileResponse(io.saveProfile(new ProfileStore.SaveRequest(id, name, description, "PRIVATE", type == null ? "GHOST" : type, "")));
    }

    
    public StoreMeta selectById(String id) {
        if (!io.isAvailable()) {
            return null;
        }
        return applyProfileResponse(io.selectProfile(id == null ? "" : id));
    }

    
    public StoreMeta selectAndApplyPreferred(String selectedId, String preferredId) {
        if (!io.isAvailable()) {
            return null;
        }
        JsonObject first = io.selectProfile(selectedId == null ? "" : selectedId);
        if (!ProfileCodec.isSuccessful(first)) {
            return null;
        }
        StoreMeta preferred = selectById(preferredId);
        return preferred != null ? preferred : applyProfileResponse(first);
    }

    
    public boolean delete(String id) {
        return id != null && !id.isBlank() && io.isAvailable() && deleteById(id);
    }

    
    public boolean deleteByName(String name) {
        if (!io.isAvailable()) {
            return false;
        }
        StoreMeta profile = catalog.findByName(name, true);
        return profile != null && deleteById(profile.id());
    }

    
    public boolean deleteById(String id) {
        if (!io.isAvailable()) {
            return false;
        }
        String previous = catalog.activeConfigId();
        JsonObject response = io.deleteProfile(id == null ? "" : id);
        if (!ProfileCodec.isSuccessful(response)) {
            return false;
        }
        catalog.acceptActiveConfigId(ProfileCodec.textField(response, "activeConfigId"));
        if (previous.equals(id) && !catalog.activeConfigId().isBlank()) {
            loadById(catalog.activeConfigId());
        }
        return true;
    }

    
    private StoreMeta applyProfileResponse(JsonObject response) {
        if (!ProfileCodec.isSuccessful(response)) {
            return null;
        }
        catalog.acceptActiveConfigId(ProfileCodec.textField(response, "activeConfigId"));
        String data = ProfileCodec.textField(response, "data");
        if (!data.isBlank()) {
            loadSnapshot(data);
        }
        JsonObject config = ProfileCodec.objectField(response, "config");
        return config == null ? null : ProfileCodec.metadata(config);
    }

    private Module findFeature(String hash) {
        if (hash == null || clientState.features() == null || clientState.features().getFeatures() == null) {
            return null;
        }
        for (Module feature : clientState.features().getFeatures()) {
            if (feature.getName() != null && hash.equals(Concat.shortHash(feature.getName()))) {
                return feature;
            }
        }
        return null;
    }

    private void captureServer(JsonObject snapshot) {
        JsonObject metadata = new JsonObject();
        ServerInfo server = clientState.server();
        if (server == null) {
            metadata.addProperty("name", "singleplayer");
            metadata.addProperty("ip", "");
            snapshot.add("server", metadata);
            return;
        }
        metadata.addProperty("name", Objects.requireNonNullElse(server.name, ""));
        metadata.addProperty("ip", Objects.requireNonNullElse(server.address, ""));
        byte[] icon = server.getFavicon();
        if (icon != null && icon.length > 0) {
            metadata.addProperty("icon", Base64.getEncoder().encodeToString(icon));
        }
        snapshot.add("server", metadata);
    }

    private void captureFriends(JsonObject snapshot) {
        JsonArray names = new JsonArray();
        List<String> friends = clientState.friends() == null ? List.of() : clientState.friends().names();
        for (String friend : friends) {
            if (friend != null && !friend.isBlank()) {
                names.add(friend);
            }
        }
        snapshot.add("friends", names);
    }

    private void applyFriends(JsonObject snapshot) {
        if (clientState.friends() == null) {
            return;
        }
        List<String> names = new ArrayList<>();
        if (snapshot != null && snapshot.has("friends") && snapshot.get("friends").isJsonArray()) {
            for (JsonElement friend : snapshot.getAsJsonArray("friends")) {
                if (friend != null && !friend.isJsonNull()) {
                    names.add(friend.getAsString());
                }
            }
        }
        clientState.friends().replaceAll(names);
    }

    
    @Override
    public void close() {
        io.close();
    }

    private static final class LiveClientState implements ClientState {
        @Override public ModuleManager features() { return EchoClient.ModuleManager; }
        @Override public FriendManager friends() { return EchoClient.friendManager; }
        @Override public ServerInfo server() { return MinecraftClient.getInstance().getCurrentServerEntry(); }
        @Override public boolean writesDisabled() { return EchoClient.eventsDisabled; }
        @Override public boolean isClickGui(Module feature) { return feature instanceof ClickGui; }
    }
}
