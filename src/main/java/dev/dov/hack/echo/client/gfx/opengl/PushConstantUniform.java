package dev.dov.hack.echo.client.gfx.opengl;

import dev.dov.hack.echo.client.gfx.shader.PrimitiveKind;
import dev.dov.hack.echo.client.gfx.shader.ScalarKind;

public record PushConstantUniform(int location, PrimitiveKind kind, ScalarKind scalarKind, int components, int columns, int offset) {}
