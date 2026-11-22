package dev.dov.hack.echo.client.config;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleManager;
import dev.dov.hack.echo.client.modules.HudModule;
import dev.dov.hack.echo.client.modules.internals.ClickGui;
import dev.dov.hack.echo.client.keyboard.BindMode;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.utils.strings.Concat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NameRegistry {
    private static final Map<Module, String> assignedNames = new HashMap<>();
    private static final Map<Module, Boolean> previousStates = new HashMap<>();
    private static final ArrayDeque<AssignmentHistory> history = new ArrayDeque<>();
    private static final int HISTORY_LIMIT = 64;
    private static long nextSequence = 1L;

    private NameRegistry() { }

    
    public static List<ModuleCategory> categories() {
        List<ModuleCategory> result = new ArrayList<>();
        for (ModuleCategory moduleCategory : ModuleCategory.values()) {
            if (!features(moduleCategory).isEmpty()) {
                result.add(moduleCategory);
            }
        }
        return result;
    }

    
    public static List<Module> features(ModuleCategory moduleCategory) {
        return features(moduleCategory, false);
    }

    
    public static List<Module> features(ModuleCategory moduleCategory, boolean includeInvisible) {
        if (EchoClient.ModuleManager == null) {
            return List.of();
        }
        List<Module> result = new ArrayList<>();
        for (Module Module : EchoClient.ModuleManager.featuresInCategory(moduleCategory)) {
            if ((includeInvisible || Module.isVisible()) && !(Module instanceof HudModule)) {
                result.add(Module);
            }
        }
        return result;
    }

    
    public static List<Module> overlays() {
        List<Module> result = new ArrayList<>();
        for (Module Module : ModuleManager.allFeatures()) {
            if (Module.isVisible() && Module.canEnable() && Module instanceof HudModule) {
                result.add(Module);
            }
        }
        result.sort(NameRegistry::compareOverlayLabels);
        return result;
    }

    
    public static String id(Module Module) {
        return assignedNames.computeIfAbsent(Module, NameRegistry::createId);
    }

    
    public static CharSequence label(Module Module) {
        return Module.getName();
    }

    
    public static CharSequence description(Module Module) {
        return Module.getDescription();
    }

    
    public static boolean isEnabled(Module Module) {
        return Module.isEnabled();
    }

    
    public static void setEnabled(Module Module, boolean enabled) {
        Module.setEnabled(enabled);
    }

    
    public static void toggle(Module Module) {
        Module.toggle();
    }

    
    public static InputKey binding(Module Module) {
        return Module.getKey();
    }

    
    public static boolean isBindingPressed(Module Module) {
        return Module.getKey().isPressed();
    }

    
    public static BindMode bindMode(Module Module) {
        return Module.isHoldMode() ? BindMode.HOLD : BindMode.TOGGLE;
    }

    
    public static void setBindMode(Module Module, BindMode mode) {
        boolean hold = mode == BindMode.HOLD;
        if (Module.isHoldMode() == hold) {
            return;
        }
        if (Module.isHoldMode() && Module.isEnabled()) {
            Module.setEnabled(false);
        }
        Module.setHoldMode(hold);
    }

    
    public static boolean isReserved(InputKey key) {
        if (key == null || key == InputKey.NONE || key.isMouse()) {
            return false;
        }
        ClickGui clickGui = EchoClient.ModuleManager == null ? null
                : (ClickGui) EchoClient.ModuleManager.findFeature(ClickGui.class);
        return clickGui != null && clickGui.getKey() == key;
    }

    
    public static AssignmentResult assignBinding(Module Module, InputKey key) {
        if (isReserved(key)) {
            return AssignmentResult.RESERVED;
        }
        if (Module.getKey() == key) {
            return AssignmentResult.UNCHANGED;
        }
        if (key == InputKey.NONE && Module instanceof ClickGui) {
            return AssignmentResult.RESERVED;
        }
        if (Module.isHoldMode() && Module.isEnabled()) {
            Module.setEnabled(false);
        }
        Module.setKey(key);
        return key == InputKey.NONE ? AssignmentResult.CLEARED : AssignmentResult.ASSIGNED;
    }

    
    public static List<BindSnapshot> capture() {
        List<Module> features = ModuleManager.allFeatures();
        List<BindSnapshot> result = new ArrayList<>(features.size());
        for (Module Module : features) {
            if (!Module.isVisible() || Module instanceof HudModule) {
                continue;
            }
            result.add(new BindSnapshot(Module, id(Module), label(Module), Module.isEnabled(), binding(Module), bindMode(Module), isBindingPressed(Module)));
        }
        recordStateChanges(result);
        return List.copyOf(result);
    }

    
    public static synchronized List<AssignmentHistory> changesAfter(long sequence) {
        List<AssignmentHistory> result = new ArrayList<>();
        for (AssignmentHistory entry : history) {
            if (entry.sequence() > sequence) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    
    public static synchronized void acknowledgeChanges(long sequence) {
        while (!history.isEmpty() && history.peekFirst().sequence() <= sequence) {
            history.removeFirst();
        }
    }

    
    public static synchronized long latestChangeSequence() {
        return nextSequence - 1L;
    }

    
    public static synchronized void clearAssignments() {
        assignedNames.clear();
        previousStates.clear();
        history.clear();
        nextSequence = 1L;
    }

    
    public static synchronized void collectAssignedNames(Collection<String> destination) {
        destination.addAll(assignedNames.values());
    }

    
    public static synchronized void postNotification(Module Module, CharSequence suffix) {
        history.addLast(new AssignmentHistory(nextSequence++, Module, label(Module), Module.isEnabled(), System.nanoTime(), suffix));
        trimHistory();
    }

    private static synchronized void recordStateChanges(List<BindSnapshot> snapshots) {
        for (BindSnapshot snapshot : snapshots) {
            Boolean previous = previousStates.put(snapshot.Module(), snapshot.enabled());
            if (previous == null || previous == snapshot.enabled()) {
                continue;
            }
            if (!snapshot.Module().recordsStateChanges()) {
                continue;
            }
            history.addLast(new AssignmentHistory(nextSequence++, snapshot.Module(), snapshot.label(), snapshot.enabled(), System.nanoTime()));
        }
        trimHistory();
    }

    private static void trimHistory() {
        while (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
    }

    private static String createId(Module Module) {
        return Concat.shortHash(Module.getName());
    }

    private static int compareOverlayLabels(Module first, Module second) {
        return Concat.compareLowercase(label(first), label(second));
    }
}
