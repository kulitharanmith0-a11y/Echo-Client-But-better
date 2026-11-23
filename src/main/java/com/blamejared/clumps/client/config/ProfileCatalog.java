package com.blamejared.clumps.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ProfileCatalog {
    private final ProfileIo io;
    private volatile String activeConfigId = "";
    private volatile boolean available;
    private volatile List<StoreMeta> cachedProfiles = List.of();

    
    public ProfileCatalog(ProfileIo io) {
        this.io = Objects.requireNonNull(io, "io");
    }

    
    public String activeConfigId() {
        return activeConfigId;
    }

    
    void acceptActiveConfigId(String id) {
        activeConfigId = id;
    }

    
    public boolean isAvailable() {
        return available;
    }

    
    public List<StoreMeta> refresh() {
        if (!io.isAvailable()) {
            available = false;
            return List.of();
        }
        List<StoreMeta> result = readProfiles();
        available = true;
        return result;
    }

    
    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (StoreMeta profile : refresh()) {
            names.add(profile.name());
        }
        return names;
    }

    private List<StoreMeta> readProfiles() {
        JsonObject response = io.listProfiles();
        if (!ProfileCodec.isSuccessful(response)) {
            return cachedProfiles;
        }
        activeConfigId = ProfileCodec.textField(response, "activeConfigId");
        JsonArray configs = response.has("configs") && response.get("configs").isJsonArray()
                ? response.getAsJsonArray("configs") : new JsonArray();
        List<StoreMeta> profiles = new ArrayList<>();
        for (JsonElement config : configs) {
            if (config.isJsonObject()) {
                profiles.add(ProfileCodec.metadata(config.getAsJsonObject()));
            }
        }
        cachedProfiles = List.copyOf(profiles);
        return profiles;
    }

    
    public StoreMeta findActive() {
        for (StoreMeta profile : readProfiles()) {
            if (profile.id().equals(activeConfigId) || profile.active()) {
                return profile;
            }
        }
        return null;
    }

    
    public StoreMeta findByName(String name, boolean requirePrivateOwned) {
        if (name == null || name.isBlank()) {
            return null;
        }
        List<StoreMeta> matches = new ArrayList<>();
        for (StoreMeta profile : refresh()) {
            if (!profile.name().equalsIgnoreCase(name)) {
                continue;
            }
            if (requirePrivateOwned && (!profile.owned() || profile.publicVisibility())) {
                continue;
            }
            matches.add(profile);
        }
        if (matches.isEmpty()) {
            return null;
        }
        matches.sort(Comparator.comparing(ProfileCatalog::isUnowned)
                .thenComparing(ProfileCatalog::isInactive)
                .thenComparing(StoreMeta::updatedAt, Comparator.reverseOrder()));
        return matches.getFirst();
    }

    private static Boolean isUnowned(StoreMeta profile) {
        return !profile.owned();
    }

    private static Boolean isInactive(StoreMeta profile) {
        return !profile.active();
    }

    
    public StoreMeta save(String id, String name, String description, boolean publicVisibility, String type, String data) {
        JsonObject response = io.saveProfile(new ProfileStore.SaveRequest(
                id == null ? "" : id, name, description == null ? "" : description, publicVisibility ? "PUBLIC" : "PRIVATE", type == null ? "GHOST" : type, data));
        if (!ProfileCodec.isSuccessful(response)) {
            return null;
        }
        activeConfigId = ProfileCodec.textField(response, "activeConfigId");
        JsonObject metadata = ProfileCodec.objectField(response, "config");
        return metadata == null ? null : ProfileCodec.metadata(metadata);
    }

    
    public boolean saveSnapshot(String name, String data) {
        if (!io.isAvailable()) {
            return false;
        }
        boolean autosave = "_autosave".equals(name);
        StoreMeta existing = autosave ? findActive() : findByName(name, true);
        String id = existing == null ? "" : existing.id();
        String displayName = autosave ? (existing == null ? "Default" : existing.name()) : name;
        String description = existing == null ? "" : existing.description();
        String type = existing == null ? "GHOST" : existing.type();
        return save(id, displayName, description, false, type, data) != null;
    }
}
