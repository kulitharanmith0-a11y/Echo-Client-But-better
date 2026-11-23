package com.blamejared.clumps.client.utils;

import java.util.HashSet;
import java.util.Set;

public final class WeakRefSet {
    private static final Set<Object> owners = new HashSet<>();

    private WeakRefSet() {
    }

    public static void add(Object owner) {
        owners.add(owner);
    }

    public static void remove(Object owner) {
        owners.remove(owner);
    }

    public static boolean hasOwners() {
        return !owners.isEmpty();
    }

    public static boolean hasOtherOwner(Object excludedOwner) {
        for (Object owner : owners) {
            if (owner != excludedOwner) {
                return true;
            }
        }
        return false;
    }
}
