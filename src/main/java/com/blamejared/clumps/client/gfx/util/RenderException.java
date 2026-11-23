package com.blamejared.clumps.client.gfx.util;

public class RenderException extends RuntimeException {
    public RenderException(String message) {
        super(String.format("Validation error : %s", message));
    }
}
