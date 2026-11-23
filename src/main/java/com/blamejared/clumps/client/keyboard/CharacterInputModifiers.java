package com.blamejared.clumps.client.keyboard;

import net.minecraft.client.input.CharInput;

public final class CharacterInputModifiers {
    private CharacterInputModifiers() {
    }

    public static int modifiers(CharInput input) {
        return input.modifiers();
    }
}
