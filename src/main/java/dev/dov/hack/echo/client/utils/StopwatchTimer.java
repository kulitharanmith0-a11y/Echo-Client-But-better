package dev.dov.hack.echo.client.utils;

public class StopwatchTimer {
    private long startedAtNanos;

    
    public StopwatchTimer() {
        reset();
    }

    
    public boolean hasElapsedSeconds(double seconds) {
        return nanosToMillis(System.nanoTime() - startedAtNanos) >= (long) (seconds * 1000.0);
    }

    public boolean hasElapsedMillis(long milliseconds) {
        return nanosToMillis(System.nanoTime() - startedAtNanos) >= milliseconds;
    }

    
    public boolean resetIfElapsed(long milliseconds) {
        boolean elapsed = nanosToMillis(System.nanoTime() - startedAtNanos) >= milliseconds;
        if (elapsed) {
            reset();
        }
        return elapsed;
    }

    
    public void setElapsedMillis(long milliseconds) {
        startedAtNanos = System.nanoTime() - milliseconds * 1_000_000L;
    }

    public long elapsedMillis() {
        return nanosToMillis(System.nanoTime() - startedAtNanos);
    }

    public void reset() {
        startedAtNanos = System.nanoTime();
    }

    public long nanosToMillis(long nanoseconds) {
        return nanoseconds / 1_000_000L;
    }

    
    public long getElapsedMillis() {
        return nanosToMillis(System.nanoTime() - startedAtNanos);
    }
}
