package com.blamejared.clumps.client.init;

import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.settings.Setting;

public final class CatalogSettings {
    private CatalogSettings() {
    }

    
    public static void append(Module Module, Object value) {
        if (value instanceof Setting setting && !Module.settings.contains(setting)) {
            Module.settings.add(setting);
        }
    }
}
