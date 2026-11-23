package com.blamejared.clumps.client.gfx.util;

import org.slf4j.helpers.MessageFormatter;

public final class RenderChecks {
    public static void require(boolean condition, String message, Object... arguments) {
        if (!condition) {
            throw new RenderException(MessageFormatter.arrayFormat(message, arguments).getMessage());
        }
    }

    private RenderChecks() {}
}
