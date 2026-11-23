package com.blamejared.clumps.client.config;

import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.keyboard.BindMode;
import com.blamejared.clumps.client.keyboard.InputKey;
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
