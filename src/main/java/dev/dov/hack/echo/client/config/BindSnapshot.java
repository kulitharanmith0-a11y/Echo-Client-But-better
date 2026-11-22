package dev.dov.hack.echo.client.config;

import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.keyboard.BindMode;
import dev.dov.hack.echo.client.keyboard.InputKey;
import java.util.Objects;

public record BindSnapshot(Module Module, String id, CharSequence label, boolean enabled, InputKey binding, BindMode mode, boolean pressed) {
    
    public BindSnapshot {
        Objects.requireNonNull(Module, "Module");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(mode, "mode");
    }
}
