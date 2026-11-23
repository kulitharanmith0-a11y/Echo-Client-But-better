package com.blamejared.clumps.client.gfx.surface;

import com.blamejared.clumps.client.gfx.texture.TextureInfo.TextureFormat;
import com.blamejared.clumps.client.gfx.window.GlfwWindowSystem;
import com.blamejared.clumps.client.gfx.window.WindowSystem;

public final class SurfaceInfo {
    public final long windowHandle;
    public final WindowSystem windowSystem;
    public final TextureFormat format;
    public final PresentMode presentMode;

    private static WindowSystem defaultWindowSystem() {
        return new GlfwWindowSystem();
    }

    private static TextureFormat defaultFormat() {
        return TextureFormat.BGRA8Unorm;
    }

    private static PresentMode defaultPresentMode() {
        return PresentMode.FIFO;
    }

    SurfaceInfo(long windowHandle, WindowSystem windowSystem, TextureFormat format, PresentMode presentMode) {
        this.windowHandle = windowHandle;
        this.windowSystem = windowSystem;
        this.format = format;
        this.presentMode = presentMode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long windowHandle;
        private boolean windowSystemSet;
        private WindowSystem windowSystem;
        private boolean formatSet;
        private TextureFormat format;
        private boolean presentModeSet;
        private PresentMode presentMode;

        Builder() {}

        public Builder windowHandle(long windowHandle) {
            this.windowHandle = windowHandle;
            return this;
        }

        public Builder windowSystem(WindowSystem windowSystem) {
            this.windowSystem = windowSystem;
            windowSystemSet = true;
            return this;
        }

        public Builder format(TextureFormat format) {
            this.format = format;
            formatSet = true;
            return this;
        }

        public Builder presentMode(PresentMode presentMode) {
            this.presentMode = presentMode;
            presentModeSet = true;
            return this;
        }

        public SurfaceInfo build() {
            WindowSystem selectedWindowSystem = windowSystem;
            if (!windowSystemSet) {
                selectedWindowSystem = defaultWindowSystem();
            }
            TextureFormat selectedFormat = format;
            if (!formatSet) {
                selectedFormat = defaultFormat();
            }
            PresentMode selectedPresentMode = presentMode;
            if (!presentModeSet) {
                selectedPresentMode = defaultPresentMode();
            }
            return new SurfaceInfo(windowHandle, selectedWindowSystem, selectedFormat, selectedPresentMode);
        }

        @Override
        public String toString() {
            String modeText = String.valueOf(presentMode);
            String formatText = String.valueOf(format);
            String windowText = String.valueOf(windowSystem);
            return "SfSurfaceInfo.SfSurfaceInfoBuilder(windowHandle=" + windowHandle
                    + ", window$value=" + windowText + ", format$value=" + formatText
                    + ", presentMode$value=" + modeText + ")";
        }
    }

    public enum PresentMode { IMMEDIATE, MAILBOX, FIFO, FIFO_RELAXED }
}