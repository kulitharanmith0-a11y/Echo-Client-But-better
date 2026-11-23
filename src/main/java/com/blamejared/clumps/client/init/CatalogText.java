package com.blamejared.clumps.client.init;

import com.blamejared.clumps.client.utils.strings.Concat;

public final class CatalogText {
    private CatalogText() {
    }

    
    public static Concat literal(String text) {
        return Concat.strings(text.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toArray(String[]::new));
    }
}
