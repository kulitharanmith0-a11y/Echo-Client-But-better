package dev.dov.hack.echo.client.event.interaction;

import dev.dov.hack.echo.client.event.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class AttackAttemptEvent extends Event {
    private final PlayerEntity player;
    private final Entity target;

    
    public AttackAttemptEvent(PlayerEntity player, Entity target) {
        this.player = player;
        this.target = target;
    }

    
    public PlayerEntity getPlayer() { return player; }

    
    public Entity getTarget() { return target; }
}
