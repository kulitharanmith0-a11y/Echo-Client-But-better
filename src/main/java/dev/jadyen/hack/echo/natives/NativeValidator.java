package dev.jadyen.hack.echo.natives;

import java.util.Objects;

public final class NativeValidator {
    public int flags;
    public long NativeCapabilities;

    public NativeValidator(int flags, long NativeCapabilities) {
        this.flags = flags;
        this.NativeCapabilities = NativeCapabilities;
    }

    public NativeModuleInfo validate(NativeModule NativeModule) {
        Objects.requireNonNull(NativeModule, "NativeModule");
        return new NativeModuleInfo();
    }
}
