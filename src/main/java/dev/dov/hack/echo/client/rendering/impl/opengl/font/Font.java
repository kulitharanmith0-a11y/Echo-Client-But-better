package dev.dov.hack.echo.client.rendering.impl.opengl.font;

public final class Font {
    private final String name;
    private final float lineHeight;
    private final float fallbackAdvance;
    private final java.util.Map<Character, Glyph> glyphs = new java.util.HashMap<>();

    public Font(String name, float lineHeight, float fallbackAdvance) {
        this.name = name;
        this.lineHeight = Math.max(1.0f, lineHeight);
        this.fallbackAdvance = Math.max(1.0f, fallbackAdvance);
    }

    public void put(Glyph glyph) {
        if (glyph != null) {
            this.glyphs.put(glyph.character(), glyph);
        }
    }

    public Glyph glyph(char character) {
        return this.glyphs.get(character);
    }

    public float advance(char character) {
        Glyph glyph = this.glyphs.get(character);
        return glyph == null ? this.fallbackAdvance : glyph.advance();
    }

    public float width(String text, float scale) {
        if (text == null) {
            return 0.0f;
        }
        float total = 0.0f;
        for (int index = 0; index < text.length(); index++) {
            total += this.advance(text.charAt(index));
        }
        return total * scale;
    }

    
    public java.util.List<String> wrap(String text, float maxWidth, float scale) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            float used = 0.0f;
            for (String word : paragraph.split(" ")) {
                float wordWidth = this.width(word, scale);
                if (used > 0.0f && used + wordWidth > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                    used = 0.0f;
                }
                if (line.length() > 0) {
                    line.append(' ');
                    used += this.advance(' ') * scale;
                }
                line.append(word);
                used += wordWidth;
            }
            lines.add(line.toString());
        }
        return lines;
    }

    public String name() {
        return this.name;
    }

    public float lineHeight() {
        return this.lineHeight;
    }

    public int glyphCount() {
        return this.glyphs.size();
    }
}
