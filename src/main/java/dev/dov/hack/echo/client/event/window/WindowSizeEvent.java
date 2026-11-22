package dev.dov.hack.echo.client.event.window;

import dev.dov.hack.echo.client.event.Event;

public class WindowSizeEvent extends Event {
    
    public final int width;
    
    public final int height;

    
    public WindowSizeEvent(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
