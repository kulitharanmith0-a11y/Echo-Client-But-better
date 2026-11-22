package dev.dov.hack.echo.client.gfx.shader;

import dev.jadyen.hack.echo.natives.NativeModule;
import dev.dov.hack.echo.client.gfx.shader.wgsl.WgslToGlsl;

public record ShaderResourceLayout(ResourceBindingKind[] bindingKinds, int[] bindingCounts) {
    public static ShaderResourceLayout reflect(NativeModule NativeModule) {
        return WgslToGlsl.reflectLayout(NativeModule.wgslSource());
    }
}
