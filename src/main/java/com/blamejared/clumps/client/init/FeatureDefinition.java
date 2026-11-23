package com.blamejared.clumps.client.init;

import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record FeatureDefinition(String name, ModuleCategory moduleCategory, Supplier<? extends Module> constructor, Consumer<Module> initializeSettings) {
}
