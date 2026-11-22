package dev.dov.hack.echo.client.gui.notification;

import dev.dov.hack.echo.client.hud.HudIcon;

public record NotificationMessage(long id, CharSequence text, HudIcon icon) {
}
