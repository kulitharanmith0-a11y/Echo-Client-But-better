package com.blamejared.clumps.client.event.render;

import com.blamejared.clumps.client.event.Event;
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
