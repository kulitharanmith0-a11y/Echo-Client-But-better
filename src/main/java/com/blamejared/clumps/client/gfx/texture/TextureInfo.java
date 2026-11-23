package com.blamejared.clumps.client.gfx.texture;

public final class TextureInfo {
    public final int width;
    public final int height;
    public final int depth;
    public final TextureFormat format;
    public final int usage;
    public final int mipCount;

    TextureInfo(int width, int height, int depth, TextureFormat format, int usage, int mipCount) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.format = format;
        this.usage = usage;
        this.mipCount = mipCount;
    }

    private static int defaultDepth() { return 1; }
    private static int defaultMipCount() { return 1; }
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int width;
        private int height;
        private int depth;
        private boolean depthSet;
        private TextureFormat format;
        private int usage;
        private int mipCount;
        private boolean mipCountSet;

        public Builder width(int width) { this.width = width; return this; }
        public Builder height(int height) { this.height = height; return this; }
        public Builder depth(int depth) { this.depth = depth; depthSet = true; return this; }
        public Builder format(TextureFormat format) { this.format = format; return this; }
        public Builder usage(int usage) { this.usage = usage; return this; }
        public Builder mipCount(int mipCount) { this.mipCount = mipCount; mipCountSet = true; return this; }

        public TextureInfo build() {
            return new TextureInfo(width, height, depthSet ? depth : defaultDepth(), format, usage, mipCountSet ? mipCount : defaultMipCount());
        }

        @Override
        public String toString() {
            return "SfTextureInfo.SfTextureInfoBuilder(width=" + width + ", height=" + height
                    + ", depth$value=" + depth + ", format=" + format + ", usage=" + usage
                    + ", mipCount$value=" + mipCount + ")";
        }
    }

    public enum TextureAccess { UNDEFINED, SAMPLED, STORAGE_READ, STORAGE_WRITE, COLOR_ATTACHMENT, DEPTH_ATTACHMENT, TRANSFER_READ, TRANSFER_WRITE, PRESENT }
    public enum TextureLayout { UNDEFINED, GENERAL, SHADER_READ_ONLY, COLOR_ATTACHMENT, DEPTH_ATTACHMENT, TRANSFER_SRC, TRANSFER_DST, PRESENT } public enum TextureFormat { Depth32Float(4, true, false), Depth32FloatStencil8(5, true, true), Depth24UnormStencil8(4, true, true), Depth24UnormX8(4, true, false), BGRA8Unorm(4, false, false), RGBA8Unorm(4, false, false), RGBA32Float(16, false, false), RGBA16Uint(8, false, false);

        public final int bytesPerPixel;
        public final boolean depth;
        public final boolean stencil;

        TextureFormat(int bytesPerPixel, boolean depth, boolean stencil) {
            this.bytesPerPixel = bytesPerPixel;
            this.depth = depth;
            this.stencil = stencil;
        }
    }
}