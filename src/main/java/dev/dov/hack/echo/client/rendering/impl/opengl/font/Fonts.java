package dev.dov.hack.echo.client.rendering.impl.opengl.font;

import dev.dov.hack.echo.client.rendering.impl.opengl.font.Font;

public final class Fonts {
    private final java.util.Map<String, Font> fonts = new java.util.HashMap<>();

    public void register(String key, Font font) {
        if (key != null && font != null) {
            this.fonts.put(key, font);
        }
    }

    public Font get(String key) {
        return this.fonts.get(key);
    }

    public Font getOr(String key, Font fallback) {
        Font font = this.fonts.get(key);
        return font == null ? fallback : font;
    }

    public java.util.Set<String> keys() {
        return java.util.Set.copyOf(this.fonts.keySet());
    }

    public int size() {
        return this.fonts.size();
    }
}
