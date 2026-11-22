package dev.dov.hack.echo.client.rendering.impl.opengl.api;

public final class GlAttributeBuilder {
    private final java.util.List<int[]> attributes = new java.util.ArrayList<>();
    private int stride;

    public GlAttributeBuilder attribute(int index, int components, int type) {
        int size = Math.max(1, components) * 4;
        this.attributes.add(new int[] {index, components, type, this.stride});
        this.stride += size;
        return this;
    }

    public GlAttributeBuilder padding(int bytes) {
        this.stride += Math.max(0, bytes);
        return this;
    }

    public int stride() {
        return this.stride;
    }

    public int count() {
        return this.attributes.size();
    }

    public java.util.List<int[]> attributes() {
        return java.util.List.copyOf(this.attributes);
    }

    
    public int offsetOf(int index) {
        for (int[] attribute : this.attributes) {
            if (attribute[0] == index) {
                return attribute[3];
            }
        }
        return -1;
    }
}
