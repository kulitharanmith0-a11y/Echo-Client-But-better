package dev.dov.hack.echo.client.utils.trajectory;

public record LaunchCandidate(float yaw, float pitch, double hitFraction, double robustnessScore, int meanHitTick) {
}
