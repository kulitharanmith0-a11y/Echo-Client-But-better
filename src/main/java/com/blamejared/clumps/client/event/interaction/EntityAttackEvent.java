package com.blamejared.clumps.client.event.interaction;

import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.event.EventPhase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class EntityAttackEvent extends Event {
    private final PlayerEntity player;
    private final Entity target;
    private static String[] attackSentinel;

    static {
        if (getAttackSentinel() != null) {
            setAttackSentinel(new String[5]);
        }
    }

    
    public EntityAttackEvent(EventPhase phase, PlayerEntity player, Entity target) {
        super(phase);
        this.player = player;
        this.target = target;
    }

    
    public PlayerEntity getPlayer() {
        return player;
    }

    
    public Entity getTarget() {
        return target;
    }

    
    public static void setAttackSentinel(String[] sentinel) {
        attackSentinel = sentinel;
    }

    
    public static String[] getAttackSentinel() {
        return attackSentinel;
    }

    
    public static class Pre extends EntityAttackEvent {
        
        public Pre(PlayerEntity player, Entity target) {
            super(EventPhase.PRE, player, target);
        }
    }

    
    public static class Post extends EntityAttackEvent {
        
        public Post(PlayerEntity player, Entity target) {
            super(EventPhase.POST, player, target);
        }
    }
}
