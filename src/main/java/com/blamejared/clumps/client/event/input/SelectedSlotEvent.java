package com.blamejared.clumps.client.event.input;

import com.blamejared.clumps.client.event.Event;

public class SelectedSlotEvent extends Event {
    private int previousSlot;
    private int selectedSlot;

    
    public SelectedSlotEvent(int previousSlot, int selectedSlot) {
        this.previousSlot = previousSlot;
        this.selectedSlot = selectedSlot;
    }

    
    public int getPreviousSlot() {
        return previousSlot;
    }

    
    public int getSelectedSlot() {
        return selectedSlot;
    }

    
    public void setPreviousSlot(int previousSlot) {
        this.previousSlot = previousSlot;
    }

    
    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }
}
