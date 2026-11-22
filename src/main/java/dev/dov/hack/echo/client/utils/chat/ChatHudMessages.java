package dev.dov.hack.echo.client.utils.chat;

import dev.dov.hack.echo.client.MinecraftAccess;
import net.minecraft.text.Text;

public final class ChatHudMessages implements MinecraftAccess {
    private ChatHudMessages() {
    }

    public static void addMessage(Text message) {
        if (CLIENT.inGameHud == null) {
            return;
        }
        CLIENT.inGameHud.getChatHud().addMessage(message);
    }
}
