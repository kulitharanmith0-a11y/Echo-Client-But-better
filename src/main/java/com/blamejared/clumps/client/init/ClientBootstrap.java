package com.blamejared.clumps.client.init;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.chunk.ChunkScanManager;
import com.blamejared.clumps.client.chunk.ChunkController;
import com.blamejared.clumps.client.config.ModuleConfig;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleManager;
import com.blamejared.clumps.client.friends.FriendManager;
import com.blamejared.clumps.client.handlers.HandlerManager;
import com.blamejared.clumps.natives.OfflineSession;
import com.blamejared.clumps.client.particle.ParticleManager;
import com.blamejared.clumps.client.gui.ScreenManager;
import com.blamejared.clumps.client.utils.audio.AudioUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class ClientBootstrap {
    public enum State { NEW, PREPARED, INITIALIZING, INITIALIZED, FAILED }
    public record SkippedFeature(String name, Throwable cause) {
    }

    
    public record Result(ClumpsClient client, List<Module> allocated, List<Module> registered, Map<ModuleCategory, List<Module>> categories, List<SkippedFeature> skipped) {
    }

    private static State state = State.NEW;
    private static Result result;
    private static Throwable failure;

    private ClientBootstrap() {
    }

    
    public static synchronized void prepare() {
        if (state == State.PREPARED || state == State.INITIALIZED) {
            return;
        }
        if (state != State.NEW) {
            throw new IllegalStateException("Cannot preLaunch from " + state, failure);
        }
        OfflineSession.shared().markInitialized();
        state = State.PREPARED;
    }

    
    public static synchronized Result initializeCatalog() {
        if (state == State.INITIALIZED) {
            return result;
        }
        requirePrepared();
        return initializeGraph(FeatureCatalog::definitions);
    }

    
    public static synchronized Result initialize(List<FeatureDefinition> definitions) {
        if (state == State.INITIALIZED) {
            return result;
        }
        requirePrepared();
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one Module definition");
        }
        return initializeGraph(() -> definitions);
    }

    private static void requirePrepared() {
        if (state != State.PREPARED) {
            throw new IllegalStateException("Expected preLaunch, state=" + state, failure);
        }
    }

    
    private static Result initializeGraph(Supplier<List<FeatureDefinition>> definitionSource) {
        state = State.INITIALIZING;
        try {
            ClumpsClient.friendManager = new FriendManager();
            ClumpsClient.particleManager = new ParticleManager();
            ClumpsClient.handlerManager = new HandlerManager();
            ClumpsClient.ChunkController = new ChunkController();
            AudioUtils.preload();

            ClumpsClient.ModuleManager = new ModuleManager();
            ClumpsClient.ModuleConfig = new ModuleConfig();
            ClumpsClient.ModuleConfig.loading = true;
            List<FeatureDefinition> definitions = definitionSource.get();
            if (definitions == null || definitions.isEmpty()) {
                throw new IllegalArgumentException("Expected at least one Module definition");
            }
            List<Module> allocated = new ArrayList<>();
            List<FeatureDefinition> allocatedDefinitions = new ArrayList<>();
            List<SkippedFeature> skipped = new ArrayList<>();
            for (FeatureDefinition definition : definitions) {
                Module Module;
                try {
                    Module = definition.constructor().get();
                    if (Module == null) {
                        throw new IllegalStateException("Module constructor returned null: " + definition.name());
                    }
                } catch (Throwable exception) {
                    skipped.add(new SkippedFeature(definition.name(), exception));
                    continue;
                }
                allocated.add(Module);
                allocatedDefinitions.add(definition);
            }

            CopyOnWriteArrayList<Module> featureList = new CopyOnWriteArrayList<>();
            Map<Module, ModuleCategory> featureCategories = new IdentityHashMap<>();
            for (int index = 0; index < allocated.size(); index++) {
                Module Module = allocated.get(index);
                FeatureDefinition definition = allocatedDefinitions.get(index);
                definition.initializeSettings().accept(Module);
                Module.attachInputConfig();
                featureList.add(Module);
                featureCategories.put(Module, definition.moduleCategory());
            }

            CopyOnWriteArrayList<Module> sorted;
            try {
                sorted = ClumpsClient.ModuleManager.sort(featureList);
            } catch (Throwable ignored) {
                sorted = featureList;
            }
            if (sorted == null) {
                throw new IllegalStateException("ModuleManager.sort returned no list");
            }
            ClumpsClient.ModuleManager.addFeatures(sorted);

            Map<ModuleCategory, List<Module>> categories = new ConcurrentHashMap<>();
            ModuleCategory[] categoryValues = ModuleCategory.values();
            for (ModuleCategory moduleCategory : categoryValues) {
                categories.put(moduleCategory, new ArrayList<>());
            }
            for (Module Module : sorted) {
                ModuleCategory moduleCategory = featureCategories.getOrDefault(Module, ModuleCategory.INTERNALS);
                categories.get(moduleCategory).add(Module);
            }
            for (ModuleCategory moduleCategory : categoryValues) {
                categories.put(moduleCategory, Collections.unmodifiableList(categories.get(moduleCategory)));
            }
            ClumpsClient.ModuleManager.addCategoryCache(categories);

            ClumpsClient.screenManager = new ScreenManager();
            ClumpsClient.screenManager.initialize();
            ClumpsClient.ChunkScanManager = new ChunkScanManager();
            ClumpsClient.handlerManager.initialize();
            ClumpsClient client = new ClumpsClient();
            client.onInitialize();

            result = new Result(client, List.copyOf(allocated), List.copyOf(sorted), Collections.unmodifiableMap(categories), List.copyOf(skipped));
            state = State.INITIALIZED;
            return result;
        } catch (RuntimeException | Error exception) {
            state = State.FAILED;
            failure = exception;
            throw exception;
        }
    }

    
    public static synchronized State state() {
        return state;
    }
}
