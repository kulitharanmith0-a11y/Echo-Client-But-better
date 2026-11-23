package com.blamejared.clumps.client.particle;

import java.nio.ByteBuffer;

public record ParticleBuffer(Class<?> particleType, ByteBuffer pixels, int width, int height) {
    @Override
    public Class<?> particleType() {
        try {
            Class.forName(Class.class.getName(), true, Class.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Resolved class initialization failed", exception);
        }
        return particleType;
    }

    @Override
    public ByteBuffer pixels() {
        try {
            Class.forName(ByteBuffer.class.getName(), true, ByteBuffer.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Resolved class initialization failed", exception);
        }
        return pixels;
    }
}
