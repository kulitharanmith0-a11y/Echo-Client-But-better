package dev.dov.hack.echo.client.event.interaction;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.event.EventPhase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class HandInteractEvent extends Event {
    private final PlayerEntity player;
    private final Hand hand;

    public HandInteractEvent(EventPhase phase, PlayerEntity player, Hand hand) {
        super(phase);
        this.player = player;
        this.hand = hand;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public static class Pre extends HandInteractEvent {
        public Pre(PlayerEntity player, Hand hand) {
            super(EventPhase.PRE, player, hand);
        }
    }
}
