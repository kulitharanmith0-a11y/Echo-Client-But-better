package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
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
        boolean eventsDisabled = EchoClient.eventsDisabled;
        if (bypass) return eventsDisabled;
        if (eventsDisabled) return false;
        FullBright instance = activeInstance;
        return instance != null && instance.isEnabled();
    }
}
