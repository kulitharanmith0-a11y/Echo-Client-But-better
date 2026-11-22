package dev.dov.hack.echo.client.runtime.api;

import dev.dov.hack.echo.client.event.Event;

public class EventView {
    protected final Event event;

    
    public EventView(Event event) { this.event = event; }

    
    public void cancel() { event.cancel(); }

    
    public boolean isCancelled() { return event.cancelled; }
}
