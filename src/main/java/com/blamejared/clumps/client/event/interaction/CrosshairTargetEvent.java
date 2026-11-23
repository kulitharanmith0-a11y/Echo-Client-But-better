package com.blamejared.clumps.client.event.interaction;

import com.blamejared.clumps.client.event.Event;
import net.minecraft.util.hit.HitResult;

public class CrosshairTargetEvent extends Event {
    
    public HitResult target;

    
    public CrosshairTargetEvent(HitResult target) {
        this.target = target;
    }
}
