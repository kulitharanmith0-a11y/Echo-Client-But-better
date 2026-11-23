package com.blamejared.clumps.client.event.input;

import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.event.EventPhase;

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
