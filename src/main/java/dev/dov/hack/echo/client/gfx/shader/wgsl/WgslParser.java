package dev.dov.hack.echo.client.gfx.shader.wgsl;

import dev.jadyen.hack.echo.natives.NativeModule;

public final class WgslParser {
    public NativeModule parse(String source) {
        return parseSource(source);
    }

    public static NativeModule parseSource(String source) {
        return new NativeModule(source);
    }
}
