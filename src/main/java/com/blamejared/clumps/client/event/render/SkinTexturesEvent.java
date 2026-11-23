package com.blamejared.clumps.client.event.render;

import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.event.EventBus;
import java.util.UUID;
import net.minecraft.entity.player.SkinTextures;

public class SkinTexturesEvent extends Event {
    
    public final UUID playerId;
    
    public SkinTextures textures;

    private SkinTexturesEvent(UUID playerId, SkinTextures textures) {
        this.playerId = playerId;
        this.textures = textures;
    }

    
    public static SkinTextures resolve(UUID playerId, SkinTextures textures) {
        if (!EventBus.hasSubscribers(SkinTexturesEvent.class)) {
            return textures;
        }
        SkinTexturesEvent event = new SkinTexturesEvent(playerId, textures);
        event.post();
        return event.textures;
    }
}
