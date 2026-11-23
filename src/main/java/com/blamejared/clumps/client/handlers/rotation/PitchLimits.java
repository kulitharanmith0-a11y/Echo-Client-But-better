package com.blamejared.clumps.client.handlers.rotation;

public final class PitchLimits {
    private PitchLimits() {
    }

    
    public static float clampDegrees(float pitch) {
        float lowerClamped = -90.0F > pitch ? -90.0F : pitch;
        return 90.0F < lowerClamped ? 90.0F : lowerClamped;
    }
}
