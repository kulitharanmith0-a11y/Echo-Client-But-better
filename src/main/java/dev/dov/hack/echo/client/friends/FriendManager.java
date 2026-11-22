package dev.dov.hack.echo.client.friends;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FriendManager {
    private final Map<String, String> friendsByName = new LinkedHashMap<>();

    public FriendManager() {}

    public synchronized boolean add(String name) {
        String displayName = trimName(name);
        if (displayName == null) return false;
        return friendsByName.put(normalizeName(displayName), displayName) == null;
    }

    public synchronized boolean remove(String name) {
        String normalized = normalizeName(name);
        return normalized != null && friendsByName.remove(normalized) != null;
    }

    public synchronized boolean isFriend(String name) { return contains(name); }

    public synchronized boolean contains(String name) {
        String normalized = normalizeName(name);
        return normalized != null && friendsByName.containsKey(normalized);
    }

    public synchronized List<String> names() { return new ArrayList<>(friendsByName.values()); }

    public synchronized void replaceAll(Collection<String> names) {
        friendsByName.clear();
        if (names != null) for (String name : names) add(name);
    }

    public synchronized void clear() { friendsByName.clear(); }

    public synchronized int size() { return friendsByName.size(); }

    private static String trimName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeName(String name) {
        String trimmed = trimName(name);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
