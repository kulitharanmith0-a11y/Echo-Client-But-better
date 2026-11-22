package dev.dov.hack.echo.client.gfx.shader.glsl;

import dev.dov.hack.echo.client.gfx.shader.PrimitiveKind;
import dev.dov.hack.echo.client.gfx.shader.ScalarKind;

public final class PushConstantItem {
    public String accessPath;
    public int offset;
    public PrimitiveKind kind;
    public ScalarKind scalarKind;
    public int components;
    public int columns;
}
