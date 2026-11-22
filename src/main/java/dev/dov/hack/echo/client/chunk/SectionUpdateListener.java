package dev.dov.hack.echo.client.chunk;

public interface SectionUpdateListener {
    void onSectionAdded(int index, SectionData section);
    void onSectionUpdated(int index, SectionData section);
    void onSectionRemoved(int lastIndex);
    void onCleared();
    void onPaletteUpdated(int[] colors);
}
