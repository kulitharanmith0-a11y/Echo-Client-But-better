package com.blamejared.clumps.client.gfx.shader.glsl;

import com.blamejared.clumps.client.gfx.shader.PrimitiveKind;
import com.blamejared.clumps.client.gfx.shader.ScalarKind;

public final class PushConstantItem {
    public String accessPath;
    public int offset;
    public PrimitiveKind kind;
    public ScalarKind scalarKind;
    public int components;
    public int columns;
}
