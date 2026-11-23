package com.blamejared.clumps.natives;

public final class NativeModule implements AutoCloseable {
    private final String wgslSource;

    public NativeModule(String wgslSource) {
        this.wgslSource = wgslSource;
    }

    public String wgslSource() {
        return wgslSource;
    }

    @Override
    public void close() {}
}
