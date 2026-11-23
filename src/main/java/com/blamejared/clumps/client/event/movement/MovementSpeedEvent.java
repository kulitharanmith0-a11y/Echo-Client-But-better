package com.blamejared.clumps.client.event.movement;

import com.blamejared.clumps.client.event.Event;

public class MovementSpeedEvent extends Event {
    private float activeItemSpeedMultiplier;
    private boolean blockedFromSprinting;
    private boolean slowDown;
    private boolean hasVehicle;

    
    public MovementSpeedEvent(float activeItemSpeedMultiplier, boolean blockedFromSprinting, boolean slowDown, boolean hasVehicle) {
        this.activeItemSpeedMultiplier = activeItemSpeedMultiplier;
        this.blockedFromSprinting = blockedFromSprinting;
        this.slowDown = slowDown;
        this.hasVehicle = hasVehicle;
    }

    
    public float getActiveItemSpeedMultiplier() { return activeItemSpeedMultiplier; }

    
    public boolean isBlockedFromSprinting() { return blockedFromSprinting; }

    
    public boolean shouldSlowDown() { return slowDown; }

    
    public boolean hasVehicle() { return hasVehicle; }

    
    public void setActiveItemSpeedMultiplier(float multiplier) { activeItemSpeedMultiplier = multiplier; }

    
    public void setBlockedFromSprinting(boolean blocked) { blockedFromSprinting = blocked; }

    
    public void setSlowDown(boolean slowDown) { this.slowDown = slowDown; }

    
    public void setHasVehicle(boolean hasVehicle) { this.hasVehicle = hasVehicle; }
}
