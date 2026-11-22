package dev.dov.hack.echo.client.gfx.queue;

import dev.dov.hack.echo.client.gfx.surface.Surface;

public final class QueueInfo {
    public final int NativeCapabilities;
    public final Surface surface;

    QueueInfo(int NativeCapabilities, Surface surface) {
        this.NativeCapabilities = NativeCapabilities;
        this.surface = surface;
    }

    private static Surface defaultSurface() {
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int NativeCapabilities;
        private boolean surfaceSet;
        private Surface surface;

        public Builder NativeCapabilities(int NativeCapabilities) {
            this.NativeCapabilities = NativeCapabilities;
            return this;
        }

        public Builder surface(Surface surface) {
            this.surface = surface;
            surfaceSet = true;
            return this;
        }

        public QueueInfo build() {
            return new QueueInfo(NativeCapabilities, surfaceSet ? surface : defaultSurface());
        }

        @Override
        public String toString() {
            return "SfQueueInfo.SfQueueInfoBuilder(NativeCapabilities=" + NativeCapabilities
                    + ", surface$value=" + String.valueOf(surface) + ")";
        }
    }
}
