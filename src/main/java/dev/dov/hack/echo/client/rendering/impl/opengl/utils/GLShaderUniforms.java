package dev.dov.hack.echo.client.rendering.impl.opengl.utils;

public final class GLShaderUniforms {
    private final java.util.Map<String, Integer> locations = new java.util.HashMap<>();
    private final java.util.Map<String, float[]> values = new java.util.HashMap<>();

    public int location(String name, int resolved) {
        return this.locations.computeIfAbsent(name, key -> resolved);
    }

    public void put(String name, float... value) {
        if (name != null && value != null) {
            this.values.put(name, value.clone());
        }
    }

    public float[] get(String name) {
        return this.values.get(name);
    }

    public boolean has(String name) {
        return this.values.containsKey(name);
    }

    public void clear() {
        this.locations.clear();
        this.values.clear();
    }

    public int size() {
        return this.values.size();
    }
}
