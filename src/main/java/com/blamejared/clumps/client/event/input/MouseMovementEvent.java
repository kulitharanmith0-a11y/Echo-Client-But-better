package com.blamejared.clumps.client.event.input;

import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.event.EventPhase;

public class MouseMovementEvent extends Event {
    public double deltaX;
    public double deltaY;

    protected MouseMovementEvent(EventPhase phase, double deltaX, double deltaY) {
        super(phase);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    
    public static class Pre extends MouseMovementEvent {
        public Pre(double deltaX, double deltaY) {
            super(EventPhase.PRE, deltaX, deltaY);
        }
    }

    
    public static class Post extends MouseMovementEvent {
        public Post(double deltaX, double deltaY) {
            super(EventPhase.POST, deltaX, deltaY);
        }
    }
}
