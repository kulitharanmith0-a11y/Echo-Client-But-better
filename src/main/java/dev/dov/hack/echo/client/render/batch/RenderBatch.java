package dev.dov.hack.echo.client.render.batch;

import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.command.RenderPass;

public interface RenderBatch {
    
    default void prepare(CommandEncoder encoder) {}

    
    default void reset() {}

    void draw(CommandEncoder encoder, RenderPass pass);

    void destroy();
}
