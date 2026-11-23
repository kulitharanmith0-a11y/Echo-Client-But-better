package com.blamejared.clumps.client.gfx.surface;

public interface RenderTargetLease extends AutoCloseable {
    int width();

    int height();

    
    default boolean isYFlipped() {
        return false;
    }

    
    @Override
    void close();
}
