package com.blamejared.clumps.client.handlers;

import java.util.Locale;

final class ServerBrandPolicy {
    private ServerBrandPolicy() {
    }

    static boolean allowsHurtTimePrediction(String brand) {
        if (brand == null) {
            return true;
        }
        String normalized = brand.toLowerCase(Locale.ROOT);
        return !normalized.startsWith("vanilla")
                && !normalized.startsWith("fabric")
                && !normalized.startsWith("quilt")
                && !normalized.contains("forge");
    }
}
