package dev.dov.hack.echo.client.keyboard;

import net.minecraft.client.input.KeyInput;

public final class KeyInputCodes {
    private KeyInputCodes() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated");
    }

    
    public static int scanCode(KeyInput input) {
        return input.scancode();
    }
}
