package com.blamejared.clumps.client.runtime.api;

import com.blamejared.clumps.client.event.interaction.AttackAttemptEvent;
import net.minecraft.entity.Entity;

public class AttackEventView extends EventView {
    private final AttackAttemptEvent attackEvent;

    
    public AttackEventView(AttackAttemptEvent event) {
        super(event);
        attackEvent = event;
    }

    
    public PlayerView getPlayer() { return new PlayerView(attackEvent.getPlayer()); }

    
    public EntityView getTarget() {
        Entity target = attackEvent.getTarget();
        return target == null ? null : new EntityView(target);
    }
}
