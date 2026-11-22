package dev.dov.hack.echo.client.event.interaction;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.event.EventPhase;
import dev.dov.hack.echo.client.event.input.KeybindingEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public abstract class ItemUseEvent extends Event {
    private final PlayerEntity player;
    private final Hand hand;
    private final ItemStack stack;
    private final HitResult hit;

    public ItemUseEvent(EventPhase phase, PlayerEntity player, Hand hand, ItemStack stack, HitResult hit) {
        super(phase);
        this.player = player;
        this.hand = hand;
        this.stack = stack;
        this.hit = hit;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public ItemStack getStack() {
        return stack;
    }

    public HitResult getHit() {
        return hit;
    }

    public boolean isBlockHit() {
        return hit instanceof BlockHitResult;
    }

    public boolean isEntityHit() {
        return hit instanceof EntityHitResult;
    }

    
    public BlockHitResult getBlockHit() {
        return KeybindingEvent.getInputSentinel() != null || isBlockHit()
                ? (BlockHitResult) hit : null;
    }

    
    public EntityHitResult getEntityHit() {
        return KeybindingEvent.getInputSentinel() != null || isEntityHit()
                ? (EntityHitResult) hit : null;
    }

    
    public static class Pre extends ItemUseEvent {
        public Pre(PlayerEntity player, Hand hand, ItemStack stack, HitResult hit) {
            super(EventPhase.PRE, player, hand, stack, hit);
        }
    }

    
    public static class Post extends ItemUseEvent {
        public Post(PlayerEntity player, Hand hand, ItemStack stack, HitResult hit) {
            super(EventPhase.POST, player, hand, stack, hit);
        }
    }
}
