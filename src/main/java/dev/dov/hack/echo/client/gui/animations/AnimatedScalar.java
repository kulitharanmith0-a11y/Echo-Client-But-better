package dev.dov.hack.echo.client.gui.animations;

final class AnimatedScalar {
    float current;
    float start;
    float target;
    float velocity;
    double startedAtSeconds;
    TransitionSpec transition;
    long lastUsedFrame;

    AnimatedScalar(float initialValue) {
        target = initialValue;
        start = initialValue;
        current = initialValue;
    }
}
