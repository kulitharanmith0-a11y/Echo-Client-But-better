package dev.dov.hack.echo.client.event.input;

import dev.dov.hack.echo.client.event.Event;

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
