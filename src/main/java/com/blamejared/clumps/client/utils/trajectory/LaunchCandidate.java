package com.blamejared.clumps.client.utils.trajectory;

public record LaunchCandidate(float yaw, float pitch, double hitFraction, double robustnessScore, int meanHitTick) {
}
