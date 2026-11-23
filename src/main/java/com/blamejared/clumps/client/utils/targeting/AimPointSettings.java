package com.blamejared.clumps.client.utils.targeting;

public record AimPointSettings(AimPointMode mode, float pointWeight, boolean randomize, boolean allowOccluded) {
    public AimPointSettings {
        pointWeight = Math.clamp(pointWeight, 0.0F, 100.0F);
    }

    
    public static AimPointSettings defaults() {
        return new AimPointSettings(AimPointMode.STRAIGHT, 50.0F, false, false);
    }
}
