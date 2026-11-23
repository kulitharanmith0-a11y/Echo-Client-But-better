package com.blamejared.clumps.client.event.input;

import com.blamejared.clumps.client.event.Event;

public class KeyboardEvent extends Event {
    private int key;
    private int action;

    
    public KeyboardEvent(int key, int action) {
        this.key = key;
        this.action = action;
    }

    
    public int getKey() { return key; }

    
    public int getAction() { return action; }
}
