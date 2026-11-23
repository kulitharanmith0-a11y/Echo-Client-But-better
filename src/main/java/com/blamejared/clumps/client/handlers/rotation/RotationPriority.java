package com.blamejared.clumps.client.handlers.rotation;

public enum RotationPriority {
    CRITICAL, HIGH, NORMAL, LOW;

    
    public boolean higherThan(RotationPriority other) {
        return ordinal() < other.ordinal();
    }
}
