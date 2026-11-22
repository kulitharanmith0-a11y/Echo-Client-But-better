package dev.dov.hack.echo.client.event.input;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.event.EventPhase;

public class HandleInputEvent extends Event {
    public HandleInputEvent(EventPhase phase) {
        super(phase);
    }

    
    public static class Pre extends HandleInputEvent {
        public Pre() {
            super(EventPhase.EARLY);
        }
    }

    
    public static class Post extends HandleInputEvent {
        public Post() {
            super(EventPhase.POST);
        }
    }
}
