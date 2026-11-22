package dev.dov.hack.echo.client.gfx.effect;

import dev.dov.hack.echo.client.render.BackdropMode;
import dev.dov.hack.echo.client.gfx.command.CommandEncoder;
import dev.dov.hack.echo.client.gfx.texture.TextureView;

public interface BackdropPass extends AutoCloseable {
    default BackdropTextures render(CommandEncoder commands, TextureView input, boolean sourceYUp, float radius) {
        return render(commands, input, sourceYUp, radius, BackdropMode.FULL);
    }

    BackdropTextures render(CommandEncoder commands, TextureView input, boolean sourceYUp, float radius, BackdropMode mode);
    
    @Override
    void close();

    record BackdropTextures(TextureView halfResolution, TextureView fullResolution) {}
}