package dev.dov.hack.echo.client.event;

public abstract class Event {
    public boolean cancelled = false;

    private static int eventCounter = 0;

    protected Event() {}

    protected Event(EventPhase phase) {}

    public final boolean post() {
        return EventBus.post(this);
    }

    public final void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static int getEventCounter() {
        return eventCounter;
    }

    public static void setEventCounter(int value) {
        eventCounter = value;
    }

    public static int getEventCounterFallbackValue() {
        return eventCounter;
    }
}
