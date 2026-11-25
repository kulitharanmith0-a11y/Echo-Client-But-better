package com.blamejared.clumps.client.particle;

import java.nio.ByteBuffer;

public record ParticleBuffer(Class<?> particleType, ByteBuffer pixels, int width, int height) {
    @Override
    public Class<?> particleType() {
        return particleType;
    }

    @Override
    public ByteBuffer pixels() {
        return pixels;
    }
}
