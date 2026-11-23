package com.blamejared.clumps.client.gfx.shader;

import com.blamejared.clumps.natives.NativeModule;
import com.blamejared.clumps.client.gfx.shader.wgsl.WgslToGlsl;

public record ShaderResourceLayout(ResourceBindingKind[] bindingKinds, int[] bindingCounts) {
    public static ShaderResourceLayout reflect(NativeModule NativeModule) {
        return WgslToGlsl.reflectLayout(NativeModule.wgslSource());
    }
}
