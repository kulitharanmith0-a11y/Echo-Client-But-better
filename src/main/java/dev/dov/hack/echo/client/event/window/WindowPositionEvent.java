package dev.dov.hack.echo.client.event.window;

import dev.dov.hack.echo.client.event.Event;

public class WindowPositionEvent extends Event {
    
    public final int x;
    
    public final int y;

    
    public WindowPositionEvent(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
