package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import java.util.Objects;

public class FullBright extends Module {
    private static volatile FullBright activeInstance;

    public FullBright() {
        super(new ModuleInfo(CatalogText.literal("Full Bright"), CatalogText.literal("Lights up the world so you can see in the dark"), ModuleCategory.RENDER));
    }

    @Override
    public void onEnable() {
        activeInstance = this;
        super.onEnable();
    }

    
    @Override
    public void onDisable() {
        boolean clearCurrentInstance = Freecam.isPerspectiveRefreshEnabled();
        FullBright receiver = activeInstance;
        if (clearCurrentInstance) {
            if (receiver == this) activeInstance = null;
            receiver = this;
        }
        Objects.requireNonNull(receiver, "object reference is null").unregisterBaseListeners();
    }

    
    private void unregisterBaseListeners() {
        super.onDisable();
    }

    
    public static boolean isFullBrightActive() {
        boolean bypass = Freecam.isPerspectiveRefreshBypassed();
        boolean eventsDisabled = ClumpsClient.eventsDisabled;
        if (bypass) return eventsDisabled;
        if (eventsDisabled) return false;
        FullBright instance = activeInstance;
        return instance != null && instance.isEnabled();
    }
}
