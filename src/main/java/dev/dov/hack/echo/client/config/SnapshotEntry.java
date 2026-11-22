package dev.dov.hack.echo.client.config;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import java.util.Objects;

public record SnapshotEntry(String id, CharSequence name, ModuleCategory moduleCategory, String keyLabel, boolean enabled, boolean pressed) {
    
    public SnapshotEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(keyLabel, "keyLabel");
    }
}
