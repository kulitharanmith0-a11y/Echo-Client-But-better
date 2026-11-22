package dev.dov.hack.echo.client.gfx.pipeline;

import dev.dov.hack.echo.client.gfx.device.RenderDevice;
import dev.jadyen.hack.echo.natives.NativeModule;
import dev.dov.hack.echo.client.gfx.shader.wgsl.WgslParser;

public final class ComputePipelineInfo {
    public final NativeModule Module;
    public int pushConstantBytes;

    public ComputePipelineInfo(RenderDevice device, String path) {
        pushConstantBytes = 0;
        Module = WgslParser.parseSource(device.resourceLoader.loadText(path));
    }
}
