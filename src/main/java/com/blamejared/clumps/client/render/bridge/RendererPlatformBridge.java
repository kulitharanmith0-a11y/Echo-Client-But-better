package com.blamejared.clumps.client.render.bridge;

import com.blamejared.clumps.client.gfx.util.ClientResources;
import com.blamejared.clumps.client.gfx.resource.ResourceLoader;

public final class RendererPlatformBridge implements ResourceLoader {
    @Override
    public String loadText(String path) {
        return ClientResources.readText(path);
    }

    @Override
    public byte[] loadBytes(String path) {
        return ClientResources.readBytes(path);
    }
}
