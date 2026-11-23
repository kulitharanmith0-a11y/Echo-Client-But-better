package com.blamejared.clumps.client.config;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.prefs.Preferences;

public final class ProfileService {
    public static final String PREFERENCES_NODE = "fabric_client_profiles";
    private final ProfileStore store;

    public ProfileService(Path directory) {
        store = new ProfileStore(directory);
    }

    public ProfileService(Preferences preferences) {
        store = new ProfileStore(preferences);
    }

    public static ProfileService forClient() {
        return ClientInstance.SERVICE;
    }

    public boolean isAvailable() {
        return store.isAvailable();
    }

    public JsonObject listProfiles() {
        return store.listProfiles();
    }

    public JsonObject getProfile(String id) {
        return store.getProfile(id);
    }

    public JsonObject saveProfile(ProfileStore.SaveRequest request) {
        return store.saveProfile(request);
    }

    public JsonObject selectProfile(String id) {
        return store.selectProfile(id);
    }

    public JsonObject deleteProfile(String id) {
        return store.deleteProfile(id);
    }

    public String executeOperation(String operation, String[] arguments) {
        return store.executeOperation(operation, arguments);
    }

    private static final class ClientInstance {
        private static final ProfileService SERVICE = new ProfileService(
                Preferences.userRoot().node(PREFERENCES_NODE));
    }
}
