package com.blamejared.clumps.client.gfx.shader.wgsl;

import com.blamejared.clumps.natives.NativeModule;

public final class WgslParser {
    public NativeModule parse(String source) {
        return parseSource(source);
    }

    public static NativeModule parseSource(String source) {
        return new NativeModule(source);
    }
}
