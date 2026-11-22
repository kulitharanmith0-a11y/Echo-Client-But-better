package dev.dov.hack.echo.client.init;

import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.settings.Setting;

public final class CatalogSettings {
    private CatalogSettings() {
    }

    
    public static void append(Module Module, Object value) {
        if (value instanceof Setting setting && !Module.settings.contains(setting)) {
            Module.settings.add(setting);
        }
    }
}
