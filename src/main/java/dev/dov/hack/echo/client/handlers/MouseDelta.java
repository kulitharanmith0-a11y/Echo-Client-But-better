package dev.dov.hack.echo.client.handlers;

public record MouseDelta(int horizontalCounts, int verticalCounts) {
    public static final MouseDelta ZERO = new MouseDelta(0, 0);
}
