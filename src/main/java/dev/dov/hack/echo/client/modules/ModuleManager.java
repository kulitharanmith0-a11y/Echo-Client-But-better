package dev.dov.hack.echo.client.modules;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.EventBus;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModuleManager {
    private final CopyOnWriteArrayList<Module> features;
    private final Map<Class<? extends Module>, Module> featuresByClass;
    private final Map<ModuleCategory, List<Module>> featuresByCategory;
    private volatile boolean shuttingDown;

    public ModuleManager() {
        features = new CopyOnWriteArrayList<>();
        featuresByClass = new ConcurrentHashMap<>();
        featuresByCategory = new ConcurrentHashMap<>();
    }

    
    public void addFeatures(CopyOnWriteArrayList<Module> additions) {
        features.addAll(additions);
        for (Module feature : additions) {
            featuresByClass.put(feature.getClass(), feature);
        }
    }

    
    public CopyOnWriteArrayList<Module> sort(CopyOnWriteArrayList<Module> input) {
        input.removeIf(Module::isFilteredFromDefaultList);
        input.sort((first, second) -> compareNamesIgnoreCase(first.getName(), second.getName()));
        return input;
    }

    
    public void addCategoryCache(Map<ModuleCategory, List<Module>> categories) {
        featuresByCategory.putAll(categories);
    }

    
    public void addFeatureToLists(Module feature) {
        features.add(feature);
        featuresByCategory.get(feature.getCategory()).add(feature);
    }


    public void removeFeatureFromLists(Module feature) {
        features.remove(feature);
        featuresByCategory.get(feature.getCategory()).remove(feature);
    }

    public Module findFeature(Class<?> type) {
        if (shuttingDown) {
            return null;
        }
        return featuresByClass.get(type);
    }

    public List<Module> featuresInCategory(ModuleCategory moduleCategory) {
        if (shuttingDown) {
            return Collections.emptyList();
        }
        return featuresByCategory.getOrDefault(moduleCategory, Collections.emptyList());
    }

    
    public static List<Module> allFeatures() {
        ModuleManager manager = EchoClient.ModuleManager;
        if (manager == null || manager.shuttingDown) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(manager.features);
    }

    public static Optional<Module> find(Class<?> type) {
        ModuleManager manager = EchoClient.ModuleManager;
        if (manager == null || manager.shuttingDown) {
            return Optional.empty();
        }
        return Optional.ofNullable(manager.findFeature(type));
    }

    
    public void shutdownExcept(Module exempt) {
        shuttingDown = true;
        for (Module feature : features) {
            if (feature == null || feature == exempt) {
                continue;
            }
            try {
                feature.setEnabled(false);
            } catch (Exception ignored) {
            }
            EventBus.unregister(feature);
            feature.settings.clear();
        }
        features.clear();
        featuresByClass.clear();
        featuresByCategory.clear();
    }

    public CopyOnWriteArrayList<Module> getFeatures() {
        return features;
    }

    public Map<Class<? extends Module>, Module> getFeaturesByClass() {
        return featuresByClass;
    }

    public Map<ModuleCategory, List<Module>> getFeaturesByCategory() {
        return featuresByCategory;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    private static int compareNamesIgnoreCase(CharSequence first, CharSequence second) {
        if (first == second) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        int firstLength = first.length();
        int secondLength = second.length();
        int sharedLength = Math.min(firstLength, secondLength);
        for (int index = 0; index < sharedLength; index++) {
            char left = Character.toLowerCase(first.charAt(index));
            char right = Character.toLowerCase(second.charAt(index));
            if (left != right) {
                return left - right;
            }
        }
        return firstLength - secondLength;
    }
}
