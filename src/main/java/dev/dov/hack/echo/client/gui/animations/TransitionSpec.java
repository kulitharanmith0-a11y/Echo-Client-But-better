package dev.dov.hack.echo.client.gui.animations;

public record TransitionSpec(float durationSeconds, float controlX1, float controlY1, float controlX2, float controlY2) {
    public static final TransitionSpec DURATION_140_MS = easeOut(0.14f);
    public static final TransitionSpec DURATION_160_MS = easeOut(0.16f);
    public static final TransitionSpec DURATION_180_MS = easeOut(0.18f);
    public static final TransitionSpec DURATION_220_MS = easeOut(0.22f);
    public static final TransitionSpec DURATION_240_MS = easeOut(0.24f);
    public static final TransitionSpec DURATION_260_MS = easeOut(0.26f);

    public TransitionSpec {
        if (durationSeconds < 0.0f) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
    }

    public static TransitionSpec easeOut(float durationSeconds) {
        return new TransitionSpec(durationSeconds, 0.23f, 1.0f, 0.32f, 1.0f);
    }
}
