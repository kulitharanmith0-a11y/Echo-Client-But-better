package com.blamejared.clumps.client.event.input;

import com.blamejared.clumps.client.event.Event;

public class KeyPressedQueryEvent extends Event {
    private boolean pressed;
    private boolean overridden;

    
    public KeyPressedQueryEvent(boolean pressed) {
        this.pressed = pressed;
        this.overridden = false;
    }

    
    public boolean isPressed() {
        return pressed;
    }

    
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
        this.overridden = true;
    }

    
    public boolean isOverridden() {
        return overridden;
    }
}
