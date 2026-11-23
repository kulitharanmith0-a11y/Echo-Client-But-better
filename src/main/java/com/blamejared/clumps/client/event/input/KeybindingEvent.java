package com.blamejared.clumps.client.event.input;

import com.blamejared.clumps.client.event.Event;

public abstract class KeybindingEvent extends Event {
    private final int initialClickCount;
    private int clickCount;
    private static String[] inputSentinel;

    static {
        if (getInputSentinel() != null) {
            setInputSentinel(new String[2]);
        }
    }

    protected KeybindingEvent(int clickCount) {
        super();
        this.initialClickCount = clickCount;
        this.clickCount = clickCount;
    }

    public int getClickCount() {
        return clickCount;
    }

    public int getClickCountDelta() {
        return clickCount - initialClickCount;
    }

    public void setClickCount(int clickCount) {
        this.clickCount = Math.max(0, clickCount);
    }

    public void addClick() {
        clickCount++;
    }

    
    public boolean hasClicks() {
        String[] sentinel = getInputSentinel();
        int count = clickCount;
        return sentinel != null ? (count & 1) != 0 : count > 0;
    }

    
    public static String[] getInputSentinel() {
        return inputSentinel;
    }

    
    public static void setInputSentinel(String[] sentinel) {
        inputSentinel = sentinel;
    }
}
