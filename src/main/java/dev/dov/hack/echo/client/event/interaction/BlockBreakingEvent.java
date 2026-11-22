package dev.dov.hack.echo.client.event.interaction;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.event.EventPhase;

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
