package com.blamejared.clumps.client.event;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.diagnostics.DevDiagnostics;
import com.blamejared.clumps.client.modules.Module;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class EventBus {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final MethodType subscriberType = MethodType.methodType(void.class, Event.class);
    private static final MethodHandle[] emptySubscribers = new MethodHandle[0];
    private static final Map<Class<?>, SortedMap<EventPriority, LinkedHashMap<Object, MethodHandle[]>>>
            subscribersByPriority = new LinkedHashMap<>();
    private static final Map<Object, Boolean> registeredOwners = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle[]> dispatchCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String[]> dispatchOwners = DevDiagnostics.ENABLED ? new ConcurrentHashMap<>() : null;
    private static final Map<Class<?>, String[]> dispatchMethods = DevDiagnostics.ENABLED ? new ConcurrentHashMap<>() : null;
    private static final Map<MethodHandle, String> handleMethodNames = DevDiagnostics.ENABLED ? new ConcurrentHashMap<>() : null;
    private static final Map<Class<?>, ConcurrentHashMap<Object, Consumer<Event>>>
            functionalSubscribers = new ConcurrentHashMap<>();

    public static void register(Object owner) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        if (owner == null || registeredOwners.putIfAbsent(owner, Boolean.TRUE) != null) {
            return;
        }
        synchronized (EventBus.class) {
            for (Method method : owner.getClass().getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length != 1 || !Event.class.isAssignableFrom(parameters[0])
                        || !method.isAnnotationPresent(Subscribe.class)) {
                    continue;
                }
                method.setAccessible(true);
                MethodHandle subscriber;
                try {
                    subscriber = lookup.unreflect(method).bindTo(owner).asType(subscriberType);
                } catch (IllegalAccessException failure) {
                    continue;
                }
                if (DevDiagnostics.ENABLED) {
                    handleMethodNames.put(subscriber, method.getName());
                }
                EventPriority priority = method.getAnnotation(Subscribe.class).priority();
                Map<Object, MethodHandle[]> owners = subscribersByPriority
                        .computeIfAbsent(parameters[0], eventType -> new TreeMap<>())
                        .computeIfAbsent(priority, eventPriority -> new LinkedHashMap<>());
                MethodHandle[] previous = owners.get(owner);
                if (previous == null) {
                    owners.put(owner, new MethodHandle[] {subscriber});
                } else {
                    MethodHandle[] next = new MethodHandle[previous.length + 1];
                    System.arraycopy(previous, 0, next, 0, previous.length);
                    next[previous.length] = subscriber;
                    owners.put(owner, next);
                }
                rebuildDispatchCache(parameters[0]);
            }
        }
    }

    public static void unregister(Object owner) {
        if (registeredOwners.remove(owner) == null) {
            return;
        }
        synchronized (EventBus.class) {
            for (var entry : subscribersByPriority.entrySet()) {
                boolean changed = false;
                for (var owners : entry.getValue().values()) {
                    changed |= owners.remove(owner) != null;
                }
                if (changed) {
                    rebuildDispatchCache(entry.getKey());
                }
            }
        }
    }

    private static void rebuildDispatchCache(Class<?> eventType) {
        var priorities = subscribersByPriority.get(eventType);
        if (priorities == null) {
            dispatchCache.remove(eventType);
            if (DevDiagnostics.ENABLED) {
                dispatchOwners.remove(eventType);
                dispatchMethods.remove(eventType);
            }
            return;
        }
        ArrayList<MethodHandle> subscribers = new ArrayList<>();
        ArrayList<String> ownersList = DevDiagnostics.ENABLED ? new ArrayList<>() : null;
        ArrayList<String> methodsList = DevDiagnostics.ENABLED ? new ArrayList<>() : null;
        for (var owners : priorities.values()) {
            for (var ownerEntry : owners.entrySet()) {
                Object owner = ownerEntry.getKey();
                String ownerName = null;
                if (DevDiagnostics.ENABLED) {
                    if (owner instanceof DevDiagnostics) {
                        ownerName = "";
                    } else if (owner instanceof Module module) {
                        ownerName = module.getName().toString();
                    } else {
                        ownerName = owner.getClass().getSimpleName();
                    }
                }
                for (MethodHandle method : ownerEntry.getValue()) {
                    subscribers.add(method);
                    if (DevDiagnostics.ENABLED) {
                        ownersList.add(ownerName);
                        methodsList.add(handleMethodNames.getOrDefault(method, "?"));
                    }
                }
            }
        }
        if (subscribers.isEmpty()) {
            dispatchCache.remove(eventType);
            if (DevDiagnostics.ENABLED) {
                dispatchOwners.remove(eventType);
                dispatchMethods.remove(eventType);
            }
        } else {
            dispatchCache.put(eventType, subscribers.toArray(emptySubscribers));
            if (DevDiagnostics.ENABLED) {
                dispatchOwners.put(eventType, ownersList.toArray(new String[0]));
                dispatchMethods.put(eventType, methodsList.toArray(new String[0]));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> void registerCallback(Class<T> eventType, Object owner, Consumer<T> callback) {
        Consumer<Event> erased = (Consumer<Event>) (Consumer<?>) callback;
        functionalSubscribers.computeIfAbsent(eventType, type -> new ConcurrentHashMap<>())
                .put(owner, erased);
    }

    public static void unregisterCallbacks(Object owner) {
        for (var callbacks : functionalSubscribers.values()) {
            callbacks.remove(owner);
        }
    }

    public static boolean hasSubscribers(Class<? extends Event> eventType) {
        if (dispatchCache.containsKey(eventType)) {
            return true;
        }
        var callbacks = functionalSubscribers.get(eventType);
        return callbacks != null && !callbacks.isEmpty();
    }

    public static boolean post(Event event) {
        if (ClumpsClient.eventsDisabled) {
            return false;
        }
        Class<?> eventClass = event.getClass();
        MethodHandle[] methods = dispatchCache.get(eventClass);
        if (methods != null) {
            if (DevDiagnostics.ENABLED) {
                String[] owners = dispatchOwners.get(eventClass);
                String[] names = dispatchMethods.get(eventClass);
                String eventName = eventClass.getSimpleName();
                for (int i = 0; i < methods.length; i++) {
                    String owner = (owners != null && i < owners.length) ? owners[i] : "";
                    boolean track = owner != null && !owner.isEmpty();
                    boolean wasCancelled = event.cancelled;
                    long start = track ? System.nanoTime() : 0L;
                    try {
                        methods[i].invokeExact(event);
                    } catch (Throwable err) {
                        if (track) {
                            String mName = (names != null && i < names.length) ? names[i] : "?";
                            DevDiagnostics.recordCrash(owner, eventName, mName, err);
                        }
                    }
                    if (track) {
                        DevDiagnostics.recordEventDispatch(owner, eventName, System.nanoTime() - start, !wasCancelled && event.cancelled);
                    }
                }
            } else {
                for (MethodHandle method : methods) {
                    try {
                        method.invokeExact(event);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        var callbacks = functionalSubscribers.get(eventClass);
        if (callbacks != null) {
            for (var entry : callbacks.entrySet()) {
                if (DevDiagnostics.ENABLED) {
                    Object ownerObj = entry.getKey();
                    String owner = ownerObj instanceof Module m ? m.getName().toString()
                            : (ownerObj != null ? ownerObj.getClass().getSimpleName() : "Callback");
                    boolean wasCancelled = event.cancelled;
                    long start = System.nanoTime();
                    try {
                        entry.getValue().accept(event);
                    } catch (Throwable err) {
                        DevDiagnostics.recordCrash(owner, eventClass.getSimpleName(), "callback", err);
                    }
                    DevDiagnostics.recordEventDispatch(owner, eventClass.getSimpleName(), System.nanoTime() - start, !wasCancelled && event.cancelled);
                } else {
                    entry.getValue().accept(event);
                }
            }
        }
        return event.cancelled;
    }

    public static void clear() {
        synchronized (EventBus.class) {
            subscribersByPriority.clear();
            dispatchCache.clear();
            if (DevDiagnostics.ENABLED) {
                dispatchOwners.clear();
                dispatchMethods.clear();
                handleMethodNames.clear();
            }
        }
        registeredOwners.clear();
        functionalSubscribers.clear();
    }
}
