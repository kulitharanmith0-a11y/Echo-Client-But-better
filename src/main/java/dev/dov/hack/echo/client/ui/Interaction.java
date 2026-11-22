package dev.dov.hack.echo.client.ui;

public record Interaction(boolean hovered, boolean held, boolean pressed, boolean released, boolean clicked) {
    public static final Interaction NONE = new Interaction(false, false, false, false, false);
}
