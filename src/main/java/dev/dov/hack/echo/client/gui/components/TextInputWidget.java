package dev.dov.hack.echo.client.gui.components;

public final class TextInputWidget {
    public final String placeholder;
    private String text;
    private int cursorPos;
    private boolean focused;

    public TextInputWidget(String placeholder) {
        this.placeholder = placeholder;
        this.text = "";
    }

    public String getText() { return text; }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        this.cursorPos = this.text.length();
    }

    public void appendChar(char c) {
        text = text.substring(0, cursorPos) + c + text.substring(cursorPos);
        cursorPos++;
    }

    public void backspace() {
        if (cursorPos > 0) {
            text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
            cursorPos--;
        }
    }

    public int getCursorPos() { return cursorPos; }

    public boolean isFocused() { return focused; }

    public void setFocused(boolean focused) { this.focused = focused; }
}