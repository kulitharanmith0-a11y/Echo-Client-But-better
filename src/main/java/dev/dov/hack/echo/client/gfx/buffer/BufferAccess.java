package dev.dov.hack.echo.client.gfx.buffer;

public enum BufferAccess {
    UNDEFINED, VERTEX_READ, INDEX_READ, UNIFORM_READ, INDIRECT_READ, STORAGE_READ, STORAGE_WRITE, TRANSFER_READ,
    TRANSFER_WRITE
}
