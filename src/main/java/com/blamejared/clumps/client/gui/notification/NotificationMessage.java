package com.blamejared.clumps.client.gui.notification;

import com.blamejared.clumps.client.hud.HudIcon;

public record NotificationMessage(long id, CharSequence text, HudIcon icon) {
}
