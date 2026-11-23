package com.blamejared.clumps.client.event.interaction;

import com.blamejared.clumps.client.event.Event;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class HandSwingEvent extends Event {
    private final PlayerEntity player;
    private final Hand hand;

    
    public HandSwingEvent(PlayerEntity player, Hand hand) {
        this.player = player;
        this.hand = hand;
    }

    
    public PlayerEntity getPlayer() {
        return player;
    }

    
    public Hand getHand() {
        return hand;
    }
}
