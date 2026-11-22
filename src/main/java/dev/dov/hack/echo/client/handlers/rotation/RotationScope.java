package dev.dov.hack.echo.client.handlers.rotation;

public final class RotationScope implements AutoCloseable {
    private final RotationManager manager;
    private boolean active = true;

    RotationScope(RotationManager manager) {
        this.manager = manager;
    }

    
    public void commit() {
        if (active) manager.markScopeUsed(this);
    }

    
    @Override
    public void close() {
        if (!active) return;
        active = false;
        manager.closeScope(this);
    }

    boolean isActive() {
        return active;
    }

    void invalidate() {
        active = false;
    }
}
