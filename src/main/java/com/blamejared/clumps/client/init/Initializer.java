package com.blamejared.clumps.client.init;

import net.fabricmc.api.ModInitializer;

public final class Initializer implements ModInitializer {
    
    @Override
    public void onInitialize() {
        ClientBootstrap.initializeCatalog();
    }
}
