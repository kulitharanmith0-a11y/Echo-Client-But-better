package dev.dov.hack.echo.client.gui.dashboard;

public final class DashboardFrameClock {
    private long previousFrameNanos;
    private float deltaSeconds;

    
    public DashboardFrameClock(long initialFrameNanos) {
        previousFrameNanos = initialFrameNanos;
    }

    
    public void update() {
        long now = System.nanoTime();
        deltaSeconds = Math.clamp((float) (now - previousFrameNanos) / 1.0e9f, 0.0f, 0.05f);
        previousFrameNanos = now;
    }

    
    public float deltaSeconds() {
        return deltaSeconds;
    }
}
