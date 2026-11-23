package com.blamejared.clumps.client.event.movement;

import com.blamejared.clumps.client.event.Event;

public class RotationEvent extends Event {
    private final RotationContext context;
    private final float tickDelta;
    private float yaw;
    private float pitch;

    private RotationEvent(RotationContext context, float yaw, float pitch, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static RotationEvent dispatch(RotationContext context, float yaw, float pitch) {
        return dispatch(context, yaw, pitch, 1.0F);
    }

    public static RotationEvent dispatch(RotationContext context, float yaw, float pitch, float tickDelta) {
        RotationEvent event = new RotationEvent(context, yaw, pitch, tickDelta);
        event.post();
        return event;
    }

    public RotationContext getContext() {
        return context;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
