package com.blamejared.clumps.client.gfx.opengl;

import com.blamejared.clumps.client.gfx.shader.PrimitiveKind;
import com.blamejared.clumps.client.gfx.shader.ScalarKind;

public record PushConstantUniform(int location, PrimitiveKind kind, ScalarKind scalarKind, int components, int columns, int offset) {}
