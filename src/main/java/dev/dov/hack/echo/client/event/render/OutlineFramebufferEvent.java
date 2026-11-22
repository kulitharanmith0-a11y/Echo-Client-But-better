package dev.dov.hack.echo.client.event.render;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.event.EventPhase;
import net.minecraft.client.gl.Framebuffer;

public class OutlineFramebufferEvent extends Event {
    private final Framebuffer framebuffer;
    private final boolean canDrawEntityOutlines;

    private OutlineFramebufferEvent(EventPhase phase, Framebuffer framebuffer, boolean canDrawEntityOutlines) {
        super(phase);
        this.framebuffer = framebuffer;
        this.canDrawEntityOutlines = canDrawEntityOutlines;
    }

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public boolean canDrawEntityOutlines() {
        return canDrawEntityOutlines;
    }

    
    public static class Prepare extends OutlineFramebufferEvent {
        public Prepare(Framebuffer framebuffer, boolean canDrawEntityOutlines) {
            super(EventPhase.POST, framebuffer, canDrawEntityOutlines);
        }
    }

    
    public static class Render extends OutlineFramebufferEvent {
        public Render() {
            super(EventPhase.PRE, null, false);
        }
    }
}
