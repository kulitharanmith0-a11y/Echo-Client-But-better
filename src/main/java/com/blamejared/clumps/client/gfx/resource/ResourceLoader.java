package com.blamejared.clumps.client.gfx.resource;

public interface ResourceLoader {
    String loadText(String path);

    byte[] loadBytes(String path);
}
