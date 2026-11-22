package dev.dov.hack.echo.client.config;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import java.util.Objects;
import java.util.Optional;

public record SnapshotDiff(String id, CharSequence name, ModuleCategory moduleCategory, Optional<String> keyLabel, boolean pressed) {
    
    public SnapshotDiff {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(keyLabel, "keyLabel");
    }
}
