package com.blamejared.clumps.client.gui.notification;

public final class NotificationTiming {
    private NotificationTiming() {
    }

    
    public static float transitionSeconds() {
        return 0.22f;
    }

    
    public static long remainingMillis(long duration, long elapsed) {
        return Math.max(250L, Math.max(0L, duration) - Math.max(0L, elapsed));
    }

    
    public static float stackOpacity(int index, int count) {
        int size = Math.max(1, count);
        int clampedIndex = Math.max(0, Math.min(index, size - 1));
        int age = size - 1 - clampedIndex;
        return Math.max(0.28f, (float) Math.pow(0.82f, age));
    }

    
    public static long staggerMillis(int index, long duration) {
        int clampedIndex = Math.max(0, index);
        long clampedDuration = Math.max(1L, duration);
        long pair = clampedIndex / 2;
        long secondInPair = clampedIndex % 2;
        return 40L + pair * (clampedDuration + 300L) + secondInPair * 250L;
    }

    
    public static float fadeAfterHold(float value, long now, long startedAt, long duration) {
        float fade = Math.max(0.0f, 1.0f
                - (float) Math.max(0L, now - startedAt - 250L)
                / Math.max(250.0f, (float) duration - 250.0f));
        return Math.max(0.0f, value) * fade;
    }
}
