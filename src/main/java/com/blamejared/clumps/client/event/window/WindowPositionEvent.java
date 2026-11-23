package com.blamejared.clumps.client.event.window;

import com.blamejared.clumps.client.event.Event;

public class WindowPositionEvent extends Event {
    
    public final int x;
    
    public final int y;

    
    public WindowPositionEvent(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
