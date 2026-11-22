package dev.dov.hack.echo.client.gui.notification;

public final class NotificationEntry {
    public final NotificationMessage message;
    public long visibleNanos;
    public boolean exiting;
    public boolean dismissWhenVisible;
    public boolean forcedExit;

    public NotificationEntry(NotificationMessage message) {
        this.message = message;
    }
}
