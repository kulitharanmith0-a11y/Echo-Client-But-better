package com.blamejared.clumps.client.gfx.effect;

import com.blamejared.clumps.client.render.BackdropMode;
import com.blamejared.clumps.client.gfx.command.CommandEncoder;
import com.blamejared.clumps.client.gfx.texture.TextureView;

public interface BackdropPass extends AutoCloseable {
    default BackdropTextures render(CommandEncoder commands, TextureView input, boolean sourceYUp, float radius) {
        return render(commands, input, sourceYUp, radius, BackdropMode.FULL);
    }

    BackdropTextures render(CommandEncoder commands, TextureView input, boolean sourceYUp, float radius, BackdropMode mode);
    
    @Override
    void close();

    record BackdropTextures(TextureView halfResolution, TextureView fullResolution) {}
}