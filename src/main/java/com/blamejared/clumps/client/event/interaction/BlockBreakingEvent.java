package com.blamejared.clumps.client.event.interaction;

import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.event.EventPhase;

public class BlockBreakingEvent extends Event {
    public BlockBreakingEvent(EventPhase phase) {
        super(phase);
    }

    public static class Pre extends BlockBreakingEvent {
        public Pre() {
            super(EventPhase.PRE);
        }
    }
}
