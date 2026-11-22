package dev.dov.hack.echo.client.event.render;

import dev.dov.hack.echo.client.event.Event;
import net.minecraft.client.option.Perspective;

public class PerspectiveEvent extends Event {
    private final Perspective perspective;

    public PerspectiveEvent(Perspective perspective) {
        this.perspective = perspective;
    }

    public Perspective getPerspective() {
        return perspective;
    }
}
