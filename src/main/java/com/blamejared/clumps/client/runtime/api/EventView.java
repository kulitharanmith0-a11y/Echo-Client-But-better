package com.blamejared.clumps.client.runtime.api;

import com.blamejared.clumps.client.event.Event;

public class EventView {
    protected final Event event;

    
    public EventView(Event event) { this.event = event; }

    
    public void cancel() { event.cancel(); }

    
    public boolean isCancelled() { return event.cancelled; }
}
