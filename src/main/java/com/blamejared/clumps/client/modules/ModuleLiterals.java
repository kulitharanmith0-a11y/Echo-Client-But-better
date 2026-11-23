package com.blamejared.clumps.client.modules;

import com.blamejared.clumps.client.utils.strings.Concat;

public final class ModuleLiterals {
    private ModuleLiterals() {
    }

    public static Concat literal(String text) {
        return Concat.strings(text.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toArray(String[]::new));
    }
}
