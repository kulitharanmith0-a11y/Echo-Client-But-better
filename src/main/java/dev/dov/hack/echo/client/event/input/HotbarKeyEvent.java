package dev.dov.hack.echo.client.event.input;

public class HotbarKeyEvent extends KeybindingEvent {
    public final int slot;

    public HotbarKeyEvent(int slot, int clickCount) {
        super(clickCount);
        this.slot = slot;
    }
}
