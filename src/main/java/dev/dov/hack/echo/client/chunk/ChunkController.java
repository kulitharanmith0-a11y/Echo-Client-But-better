package dev.dov.hack.echo.client.chunk;

public final class ChunkController {
    private SectionUpdateListener listener;

    public ChunkController() {}

    public void setListener(SectionUpdateListener listener) {
        this.listener = listener;
    }

    public SectionUpdateListener getListener() {
        return listener;
    }

    public void shutdown() {}
}
