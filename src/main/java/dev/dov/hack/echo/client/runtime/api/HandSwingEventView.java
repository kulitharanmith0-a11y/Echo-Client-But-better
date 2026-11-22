package dev.dov.hack.echo.client.runtime.api;

import dev.dov.hack.echo.client.event.interaction.HandSwingEvent;

public class HandSwingEventView extends EventView {
    private final HandSwingEvent swingEvent;

    
    public HandSwingEventView(HandSwingEvent event) {
        super(event);
        swingEvent = event;
    }

    
    public PlayerView getPlayer() { return new PlayerView(swingEvent.getPlayer()); }

    
    public String getHand() { return swingEvent.getHand().name(); }
}
