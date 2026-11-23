package com.blamejared.clumps.client.render.batch;

import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.command.RenderPass;

public interface RenderBatch {
    
    default void prepare(CommandEncoder encoder) {}

    
    default void reset() {}

    void draw(CommandEncoder encoder, RenderPass pass);

    void destroy();
}
