package dev.dov.hack.echo.client.event.interaction;

import dev.dov.hack.echo.client.event.Event;
import net.minecraft.util.hit.HitResult;

public class CrosshairTargetEvent extends Event {
    
    public HitResult target;

    
    public CrosshairTargetEvent(HitResult target) {
        this.target = target;
    }
}
