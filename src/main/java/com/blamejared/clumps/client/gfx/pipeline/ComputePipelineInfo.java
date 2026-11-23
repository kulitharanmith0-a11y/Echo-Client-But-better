package com.blamejared.clumps.client.gfx.pipeline;

import com.blamejared.clumps.client.gfx.device.RenderDevice;
import com.blamejared.clumps.natives.NativeModule;
import com.blamejared.clumps.client.gfx.shader.wgsl.WgslParser;

public final class ComputePipelineInfo {
    public final NativeModule Module;
    public int pushConstantBytes;

    public ComputePipelineInfo(RenderDevice device, String path) {
        pushConstantBytes = 0;
        Module = WgslParser.parseSource(device.resourceLoader.loadText(path));
    }
}
