package com.blamejared.clumps.client.event.interaction;

import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.event.EventPhase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public class BlockInteractEvent extends Event {
    private final PlayerEntity player;
    private final Hand hand;
    private final BlockHitResult hit;

    public BlockInteractEvent(EventPhase phase, PlayerEntity player, Hand hand, BlockHitResult hit) {
        super(phase);
        this.player = player;
        this.hand = hand;
        this.hit = hit;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public BlockHitResult getHit() {
        return hit;
    }

    
    public static class Pre extends BlockInteractEvent {
        public Pre(PlayerEntity player, Hand hand, BlockHitResult hit) {
            super(EventPhase.PRE, player, hand, hit);
        }
    }

    
    public static class Post extends BlockInteractEvent {
        public Post(PlayerEntity player, Hand hand, BlockHitResult hit) {
            super(EventPhase.POST, player, hand, hit);
        }
    }
}
