package dev.dov.hack.echo.client.modules.impl.internals;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.gui.dashboard.DashboardController;
import dev.dov.hack.echo.client.gui.dashboard.ScriptEditorState;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.dov.hack.echo.client.settings.TextSetting;
import dev.dov.hack.echo.client.ui.ImmediateUiContext;
import dev.dov.hack.echo.client.utils.strings.Concat;
import dev.jadyen.hack.echo.natives.OfflineSession;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class ClientStringCleanup {
    private final Class<?> applicationClass;

    public ClientStringCleanup(Class<?> applicationClass) {
        this.applicationClass = applicationClass;
    }

    private record QueuedObject(Object value, boolean heapOwned) {
    }

    public record CleanupPlan(List<ClientCleanupAction> actions, Set<String> heapStrings, Set<String> literalStrings) {
    }

    public List<ClientCleanupAction> collectActions(Class<?> nameAssignmentsClass, Supplier<?> dashboard, Supplier<?> hud) {
        List<Class<?>> seeds = new ArrayList<>();
        if (applicationClass != null) {
            seeds.add(applicationClass);
        }
        if (nameAssignmentsClass != null) {
            seeds.add(nameAssignmentsClass);
        }
        List<Object> objects = new ArrayList<>();
        if (dashboard != null && dashboard.get() != null) {
            objects.add(dashboard.get());
        }
        if (hud != null && hud.get() != null) {
            objects.add(hud.get());
        }
        CleanupPlan plan = collectPlan(seeds, objects, Set.of());
        List<ClientCleanupAction> combined = new ArrayList<>(plan.actions());
        if (!plan.heapStrings().isEmpty() || !plan.literalStrings().isEmpty()) {
            combined.add(() -> clearCollectedStrings(plan.heapStrings(), plan.literalStrings(), null));
        }
        return combined;
    }

    public CleanupPlan collectPlan(
            Collection<Class<?>> seedClasses,
            Collection<Object> seedObjects,
            Collection<String> preCollectedHeapStrings) {
        StringMemoryAccess memory = StringMemoryAccess.create();
        Set<String> heapStrings = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> literalStrings = Collections.newSetFromMap(new IdentityHashMap<>());
        if (preCollectedHeapStrings != null) {
            heapStrings.addAll(preCollectedHeapStrings);
        }
        if (memory == null) {
            return new CleanupPlan(new ArrayList<>(), heapStrings, literalStrings);
        }

        Set<Object> seenObjects = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Class<?>> seenClasses = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ClientCleanupAction> actions = new ArrayList<>();
        Deque<QueuedObject> queue = new ArrayDeque<>();

        Set<Class<?>> classesToVisit = new LinkedHashSet<>();
        if (seedClasses != null) {
            classesToVisit.addAll(seedClasses);
        }
        if (applicationClass != null) {
            classesToVisit.add(applicationClass);
            memory.readLoadedClasses(applicationClass.getClassLoader(), classesToVisit);
        }
        for (Class<?> type : classesToVisit) {
            if (type != null && !type.isInterface() && !type.isAnnotation() && !type.isEnum()
                    && belongsToApplication(type)) {
                collectFields(type, null, memory, seenClasses, actions, queue, heapStrings, literalStrings);
            }
        }
        if (seedObjects != null) {
            for (Object root : seedObjects) {
                enqueue(queue, root, isHeapOwnedClass(root != null ? root.getClass() : null));
            }
        }

        while (!queue.isEmpty()) {
            QueuedObject entry = queue.removeFirst();
            Object object = entry.value();
            if (object instanceof String || !seenObjects.add(object)) {
                continue;
            }
            if (object instanceof StringBuilder || object instanceof StringBuffer) {
                CharSequence builder = (CharSequence) object;
                actions.add(() -> memory.clearBuilder(builder));
                continue;
            }
            Class<?> type = object.getClass();
            if (type.isArray()) {
                collectArray(type, object, entry.heapOwned(), actions, queue, heapStrings, literalStrings);
                continue;
            }
            if (object instanceof Map<?, ?> map) {
                collectMap(map, entry.heapOwned(), actions, queue, heapStrings, literalStrings);
                continue;
            }
            if (object instanceof Collection<?> collection) {
                collectCollection(collection, entry.heapOwned(), actions, queue, heapStrings, literalStrings);
                continue;
            }
            if (object instanceof Optional<?> optional) {
                optional.ifPresent(value -> enqueue(queue, value, entry.heapOwned()));
                continue;
            }
            if (type.isEnum() || !belongsToApplication(type)) {
                continue;
            }
            for (Class<?> current = type; current != null && belongsToApplication(current);
                    current = current.getSuperclass()) {
                collectFields(current, object, memory, seenClasses, actions, queue, heapStrings, literalStrings);
            }
        }
        return new CleanupPlan(actions, heapStrings, literalStrings);
    }

    public static void clearStrings(Collection<String> strings) {
        clearCollectedStrings(strings, Set.of(), null);
    }

    public static void clearCollectedStrings(
            Collection<String> heapStrings,
            Collection<String> literalStrings,
            String protectedUsername) {
        StringMemoryAccess memory = StringMemoryAccess.create();
        if (memory == null) {
            return;
        }
        if (heapStrings != null) {
            for (String string : heapStrings) {
                if (isSafeToZeroString(string, true, protectedUsername)) {
                    memory.clearString(string);
                }
            }
        }
        if (literalStrings != null) {
            for (String string : literalStrings) {
                if (isSafeToZeroString(string, false, protectedUsername)) {
                    memory.clearString(string);
                }
            }
        }
    }

    private static void collectFields(
            Class<?> type,
            Object object,
            StringMemoryAccess memory,
            Set<Class<?>> seenClasses,
            List<ClientCleanupAction> actions,
            Deque<QueuedObject> queue,
            Set<String> heapStrings,
            Set<String> literalStrings) {
        boolean firstClassVisit = seenClasses.add(type);
        Field[] fields;
        try {
            fields = type.getDeclaredFields();
        } catch (LinkageError ignored) {
            return;
        }
        for (Field field : fields) {
            if (field.getType().isPrimitive()) {
                continue;
            }
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            if (isStatic && !firstClassVisit) {
                continue;
            }
            if (!isStatic && object == null) {
                continue;
            }
            if (isProtectedField(type, field, isStatic)) {
                continue;
            }
            Object owner = isStatic ? null : object;
            Object value = memory.readObject(field, owner);
            if (value == null) {
                continue;
            }
            boolean knownHeap = isHeapOwnedStringSource(type, owner, isStatic);
            if (value instanceof String text) {
                (knownHeap ? heapStrings : literalStrings).add(text);
                actions.add(() -> memory.clearReference(field, owner));
                continue;
            }
            if (value instanceof Enum<?>) {
                if (!isStatic && !Modifier.isFinal(field.getModifiers())) {
                    actions.add(() -> memory.clearReference(field, owner));
                }
                continue;
            }
            enqueue(queue, value, knownHeap);
            if (shouldClearReference(type, owner, field, isStatic, value)) {
                actions.add(() -> memory.clearReference(field, owner));
            }
        }
    }

    private static boolean shouldClearReference(Class<?> type, Object owner, Field field, boolean isStatic, Object value) {
        if (value instanceof Collection<?> || value instanceof Map<?, ?>) {
            if (isImmutableContainer(value)) {
                return !isProtectedFinalOwner(type, owner, field, isStatic);
            }
            return false;
        }
        return !isProtectedFinalOwner(type, owner, field, isStatic);
    }

    private static boolean isProtectedFinalOwner(Class<?> type, Object owner, Field field, boolean isStatic) {
        if (!Modifier.isFinal(field.getModifiers())) {
            return false;
        }
        if (isStatic) {
            return type == dev.dov.hack.echo.client.event.EventBus.class
                    || type == dev.dov.hack.echo.client.keyboard.KeybindingCache.class
                    || type == dev.dov.hack.echo.client.keyboard.KeyStateOverrides.class;
        }
        return owner == DashboardController.INSTANCE
                || owner == HudRenderer.INSTANCE
                || owner instanceof ImmediateUiContext;
    }

    private static boolean isProtectedField(Class<?> type, Field field, boolean isStatic) {
        String name = field.getName();
        if (isStatic) {
            if ("INSTANCE".equals(name) || "SHARED".equals(name) || "CLIENT".equals(name) || "client".equals(name)) {
                return true;
            }
            if (type == EchoClient.class && ("ID".equals(name) || "LOGGER".equals(name))) {
                return true;
            }
            if (type == HudRenderer.class && "HOTBAR_SLOT_LABELS".equals(name)) {
                return true;
            }
            if (type == OfflineSession.class && "SHARED".equals(name)) {
                return true;
            }
            if (type.isEnum() || field.getType().isEnum() || "$VALUES".equals(name)) {
                return true;
            }
        }
        Class<?> fieldType = field.getType();
        String typeName = fieldType.getName();
        return fieldType == Class.class
                || fieldType == Field.class
                || fieldType == Method.class
                || typeName.startsWith("java.lang.invoke.")
                || (fieldType.isArray() && fieldType.getComponentType().getName().startsWith("java.lang.invoke."))
                || typeName.startsWith("org.slf4j.")
                || typeName.startsWith("org.lwjgl.system.SharedLibrary")
                || typeName.startsWith("com.google.gson.")
                || typeName.equals("dev.dov.hack.echo.client.ui.UiId")
                || typeName.equals("dev.dov.hack.echo.client.gui.animations.SpringSpec")
                || typeName.equals("dev.dov.hack.echo.client.gui.animations.TransitionSpec");
    }

    private static boolean isHeapOwnedStringSource(Class<?> type, Object owner, boolean isStatic) {
        if (isStatic) {
            return false;
        }
        return isHeapOwnedClass(type) || owner instanceof TextSetting;
    }

    private static boolean isHeapOwnedClass(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (type == Concat.class || type == ScriptEditorState.class || type == TextSetting.class) {
            return true;
        }
        String name = type.getName();
        return name.contains(".config.")
                || name.contains(".hud.")
                || name.contains(".gui.notification.")
                || name.contains(".gui.dashboard.")
                || name.contains(".friends.")
                || name.contains(".runtime.");
    }

    private static void collectArray(
            Class<?> type,
            Object array,
            boolean heapOwned,
            List<ClientCleanupAction> actions,
            Deque<QueuedObject> queue,
            Set<String> heapStrings,
            Set<String> literalStrings) {
        Class<?> component = type.getComponentType();
        if (component == char.class) {
            char[] chars = (char[]) array;
            actions.add(() -> Arrays.fill(chars, '\0'));
            return;
        }
        if (component.isPrimitive() || component.getName().equals("net.minecraft.item.ItemStack")) {
            return;
        }
        boolean heapArray = heapOwned && type == String[].class;
        boolean containsStrings = false;
        boolean containsObjects = false;
        int length = Array.getLength(array);
        for (int index = 0; index < length; index++) {
            Object value = Array.get(array, index);
            if (value instanceof String text) {
                containsStrings = true;
                (heapArray ? heapStrings : literalStrings).add(text);
            } else if (value != null) {
                containsObjects = true;
                enqueue(queue, value, heapOwned);
            }
        }
        if (containsStrings || containsObjects) {
            actions.add(() -> {
                for (int index = 0; index < Array.getLength(array); index++) {
                    Object element = Array.get(array, index);
                    if (element instanceof String || element != null && !(element instanceof Enum<?>)) {
                        Array.set(array, index, null);
                    }
                }
            });
        }
    }

    private static void collectCollection(
            Collection<?> collection,
            boolean heapOwned,
            List<ClientCleanupAction> actions,
            Deque<QueuedObject> queue,
            Set<String> heapStrings,
            Set<String> literalStrings) {
        boolean heapContainer = heapOwned && !isImmutableContainer(collection);
        boolean hasEntries = false;
        try {
            for (Object value : collection) {
                hasEntries = true;
                if (value instanceof String text) {
                    (heapContainer ? heapStrings : literalStrings).add(text);
                } else {
                    enqueue(queue, value, heapOwned);
                }
            }
        } catch (RuntimeException ignored) {
        }
        if (hasEntries) {
            actions.add(() -> {
                try {
                    collection.clear();
                } catch (RuntimeException unsupportedMutation) {
                    try {
                        collection.removeIf(String.class::isInstance);
                    } catch (RuntimeException ignored) {
                    }
                }
            });
        }
    }

    private static void collectMap(
            Map<?, ?> map,
            boolean heapOwned,
            List<ClientCleanupAction> actions,
            Deque<QueuedObject> queue,
            Set<String> heapStrings,
            Set<String> literalStrings) {
        boolean heapContainer = heapOwned && !isImmutableContainer(map);
        boolean hasEntries = false;
        try {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                hasEntries = true;
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key instanceof String keyText) {
                    (heapContainer ? heapStrings : literalStrings).add(keyText);
                } else {
                    enqueue(queue, key, heapOwned);
                }
                if (value instanceof String valueText) {
                    (heapContainer ? heapStrings : literalStrings).add(valueText);
                } else {
                    enqueue(queue, value, heapOwned);
                }
            }
        } catch (RuntimeException ignored) {
        }
        if (hasEntries) {
            actions.add(() -> {
                try {
                    map.clear();
                } catch (RuntimeException unsupportedMutation) {
                    try {
                        map.entrySet().removeIf(entry -> entry.getKey() instanceof String || entry.getValue() instanceof String);
                    } catch (RuntimeException ignored) {
                    }
                }
            });
        }
    }

    private static boolean isImmutableContainer(Object container) {
        String name = container.getClass().getName();
        return name.startsWith("java.util.ImmutableCollections")
                || name.startsWith("java.util.Collections$Unmodifiable")
                || name.equals("java.util.Collections$EmptyList")
                || name.equals("java.util.Collections$EmptySet")
                || name.equals("java.util.Collections$EmptyMap")
                || name.equals("java.util.Collections$SingletonList")
                || name.equals("java.util.Collections$SingletonSet")
                || name.equals("java.util.Collections$SingletonMap")
                || name.equals("java.util.Arrays$ArrayList");
    }

    private static boolean isSafeToZeroString(String value, boolean knownHeap, String protectedUsername) {
        if (value == null || value.length() <= 1) {
            return false;
        }
        if ("clumps".equals(value)
                || "minecraft".equals(value)
                || "fabric".equals(value)
                || "fabricloader".equals(value)
                || "true".equals(value)
                || "false".equals(value)
                || "null".equals(value)
                || "singleplayer".equals(value)
                || "multiplayer".equals(value)) {
            return false;
        }
        if (protectedUsername != null && !protectedUsername.isEmpty() && value.equals(protectedUsername)) {
            return false;
        }
        if (knownHeap) {
            return true;
        }
        return value.length() >= 12 || containsClientToken(value);
    }

    private static boolean containsClientToken(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("echo")
                || lower.contains("chimera")
                || lower.contains("lorex")
                || lower.contains("dev.dov")
                || lower.contains("dev.jadyen")
                || lower.contains("destruct")
                || lower.contains("renderbase")
                || lower.contains("rage pvp")
                || lower.contains("triggerbot")
                || lower.contains("aimassist")
                || lower.contains("pearlcatch")
                || lower.contains("autototem")
                || lower.contains("killaura")
                || lower.contains("autocrystal")
                || lower.contains("stream proof");
    }

    private boolean belongsToApplication(Class<?> type) {
        if (applicationClass == null || type == null || type.getClassLoader() != applicationClass.getClassLoader()) {
            return false;
        }
        try {
            ProtectionDomain applicationDomain = applicationClass.getProtectionDomain();
            ProtectionDomain candidateDomain = type.getProtectionDomain();
            if (candidateDomain == applicationDomain) {
                return true;
            }
            if (applicationDomain == null || candidateDomain == null || applicationDomain.getCodeSource() == null) {
                return false;
            }
            return applicationDomain.getCodeSource().equals(candidateDomain.getCodeSource());
        } catch (SecurityException denied) {
            return false;
        }
    }

    private static void enqueue(Deque<QueuedObject> queue, Object object, boolean heapOwned) {
        if (object != null) {
            queue.addLast(new QueuedObject(object, heapOwned));
        }
    }
}
