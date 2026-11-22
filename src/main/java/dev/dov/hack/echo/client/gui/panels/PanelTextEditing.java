package dev.dov.hack.echo.client.gui.panels;

public final class PanelTextEditing {
    private PanelTextEditing() {
    }

    
    public static String insertCharacter(String text, int cursor, char character) {
        int insertionIndex = Math.clamp(cursor, 0, text.length());
        return text.substring(0, insertionIndex) + character + text.substring(insertionIndex);
    }
}
