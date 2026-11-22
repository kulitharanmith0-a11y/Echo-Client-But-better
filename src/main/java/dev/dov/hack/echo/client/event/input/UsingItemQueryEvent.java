package dev.dov.hack.echo.client.event.input;

import dev.dov.hack.echo.client.event.Event;

public class UsingItemQueryEvent extends Event {
    private boolean usingItem;

    
    public UsingItemQueryEvent(boolean usingItem) {
        this.usingItem = usingItem;
    }

    
    public boolean isUsingItem() { return usingItem; }

    
    public void setUsingItem(boolean usingItem) { this.usingItem = usingItem; }
}
