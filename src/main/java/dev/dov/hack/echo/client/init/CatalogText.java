package dev.dov.hack.echo.client.init;

import dev.dov.hack.echo.client.utils.strings.Concat;

public final class CatalogText {
    private CatalogText() {
    }

    
    public static Concat literal(String text) {
        return Concat.strings(text.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toArray(String[]::new));
    }
}
