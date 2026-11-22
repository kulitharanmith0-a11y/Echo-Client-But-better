package dev.dov.hack.echo.client.init;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.chunk.ChunkScanManager;
import dev.dov.hack.echo.client.chunk.ChunkController;
import dev.dov.hack.echo.client.config.ModuleConfig;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleManager;
import dev.dov.hack.echo.client.friends.FriendManager;
import dev.dov.hack.echo.client.handlers.HandlerManager;
import dev.jadyen.hack.echo.natives.OfflineSession;
import dev.dov.hack.echo.client.particle.ParticleManager;
import dev.dov.hack.echo.client.gui.ScreenManager;
import dev.dov.hack.echo.client.utils.audio.AudioUtils;
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

    
    public record Result(EchoClient client, List<Module> allocated, List<Module> registered, Map<ModuleCategory, List<Module>> categories, List<SkippedFeature> skipped) {
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
            EchoClient.friendManager = new FriendManager();
            EchoClient.particleManager = new ParticleManager();
            EchoClient.handlerManager = new HandlerManager();
            EchoClient.ChunkController = new ChunkController();
            AudioUtils.preload();

            EchoClient.ModuleManager = new ModuleManager();
            EchoClient.ModuleConfig = new ModuleConfig();
            EchoClient.ModuleConfig.loading = true;
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
                sorted = EchoClient.ModuleManager.sort(featureList);
            } catch (Throwable ignored) {
                sorted = featureList;
            }
            if (sorted == null) {
                throw new IllegalStateException("ModuleManager.sort returned no list");
            }
            EchoClient.ModuleManager.addFeatures(sorted);

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
            EchoClient.ModuleManager.addCategoryCache(categories);

            EchoClient.screenManager = new ScreenManager();
            EchoClient.screenManager.initialize();
            EchoClient.ChunkScanManager = new ChunkScanManager();
            EchoClient.handlerManager.initialize();
            EchoClient client = new EchoClient();
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
