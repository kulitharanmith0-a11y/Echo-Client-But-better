package dev.jadyen.hack.echo.natives;

import net.fabricmc.loader.api.FabricLoader;

public final class SodiumBridge {
    private static Boolean loaded;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().isModLoaded("sodium");
        }
        return loaded;
    }
}
