package com.blamejared.clumps.client.event.window;

import com.blamejared.clumps.client.event.Event;

public class WindowFocusEvent extends Event {
    
    public final boolean focused;

    
    public WindowFocusEvent(boolean focused) {
        this.focused = focused;
    }
}
