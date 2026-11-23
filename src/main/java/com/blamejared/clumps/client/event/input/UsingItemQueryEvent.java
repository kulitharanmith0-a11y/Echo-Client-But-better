package com.blamejared.clumps.client.event.input;

import com.blamejared.clumps.client.event.Event;

public class UsingItemQueryEvent extends Event {
    private boolean usingItem;

    
    public UsingItemQueryEvent(boolean usingItem) {
        this.usingItem = usingItem;
    }

    
    public boolean isUsingItem() { return usingItem; }

    
    public void setUsingItem(boolean usingItem) { this.usingItem = usingItem; }
}
