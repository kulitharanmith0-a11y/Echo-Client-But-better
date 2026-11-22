package dev.dov.hack.echo.client.hud;

public record KeybindRow(String id, CharSequence name, CharSequence detail, String binding, HudIcon icon, boolean enabled) {
}
