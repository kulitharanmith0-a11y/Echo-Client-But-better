package com.blamejared.clumps.client.gfx.vertex;

public enum VertexFormat {
    Float(1, 4, VertexScalarKind.Float), Float2(2, 8, VertexScalarKind.Float), Float3(3, 12, VertexScalarKind.Float), Float4(4, 16, VertexScalarKind.Float), Int(1, 4, VertexScalarKind.Int), Int2(2, 8, VertexScalarKind.Int), Int3(3, 12, VertexScalarKind.Int), Int4(4, 16, VertexScalarKind.Int), Mat2(4, 16, VertexScalarKind.Float), Mat3(9, 36, VertexScalarKind.Float), Mat4(16, 64, VertexScalarKind.Float);

    public final int components;
    public final int bytes;
    public final VertexScalarKind kind;

    VertexFormat(int components, int bytes, VertexScalarKind kind) {
        this.components = components;
        this.bytes = bytes;
        this.kind = kind;
    }

    public enum VertexScalarKind { Float, Int }
    public enum VertexStepMode { VERTEX, INSTANCE } public record VertexAttribute(int location, VertexFormat format, int offset) {}
}