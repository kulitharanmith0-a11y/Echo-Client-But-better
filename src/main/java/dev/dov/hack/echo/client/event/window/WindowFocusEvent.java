package dev.dov.hack.echo.client.event.window;

import dev.dov.hack.echo.client.event.Event;

public class WindowFocusEvent extends Event {
    
    public final boolean focused;

    
    public WindowFocusEvent(boolean focused) {
        this.focused = focused;
    }
}
