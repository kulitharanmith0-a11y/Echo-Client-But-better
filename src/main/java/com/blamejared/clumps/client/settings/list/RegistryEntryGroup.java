package com.blamejared.clumps.client.settings.list;

import java.util.function.Predicate;

public record RegistryEntryGroup<T>(CharSequence label, Predicate<T> predicate) {
    public boolean matches(T entry) {
        return predicate.test(entry);
    }
}
