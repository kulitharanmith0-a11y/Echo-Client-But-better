package dev.dov.hack.echo.client.config;

import dev.dov.hack.echo.client.modules.Module;
import java.util.Objects;

public record AssignmentHistory(long sequence, Module Module, CharSequence label, boolean enabled, long timestampNanos, CharSequence suffix) {
    
    public AssignmentHistory {
        Objects.requireNonNull(Module, "Module");
        Objects.requireNonNull(label, "label");
    }

    
    public AssignmentHistory(long sequence, Module Module, CharSequence label, boolean enabled, long timestampNanos) {
        this(sequence, Module, label, enabled, timestampNanos, null);
    }
}
