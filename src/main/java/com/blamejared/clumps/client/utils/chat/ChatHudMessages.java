package com.blamejared.clumps.client.utils.chat;

import com.blamejared.clumps.client.MinecraftAccess;
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
