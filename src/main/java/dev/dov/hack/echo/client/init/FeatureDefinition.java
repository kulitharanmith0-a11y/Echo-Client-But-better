package dev.dov.hack.echo.client.init;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record FeatureDefinition(String name, ModuleCategory moduleCategory, Supplier<? extends Module> constructor, Consumer<Module> initializeSettings) {
}
