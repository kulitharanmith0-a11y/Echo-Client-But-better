package com.blamejared.clumps.client.modules.impl.internals;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.render.SkinTexturesEvent;
import com.blamejared.clumps.client.event.render.TextTransformEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.gui.ScreenManager;
import com.blamejared.clumps.client.settings.TextSetting;
import java.util.Optional;
import net.minecraft.component.type.ProfileComponent;

public class Protect extends Module {
    public static TextSetting nickname;
    public static TextSetting skin;

    public Protect() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Protect"), com.blamejared.clumps.client.init.CatalogText.literal("Replaces your name and skin everywhere they are shown"), com.blamejared.clumps.client.modules.ModuleCategory.INTERNALS));
    }

    @Subscribe
    private void onTextTransform(TextTransformEvent event) {
        String replacement = nickname.getText();
        if (replacement.isEmpty()) {
            return;
        }
        String username = client.getSession().getUsername();
        if (username.isEmpty() || username.equals(replacement)) {
            return;
        }
        event.text = event.text.replace(username, replacement);
    }

    @Subscribe
    private void onSkinTextures(SkinTexturesEvent event) {
        String profileName = skin.getText();
        if (profileName.isEmpty() || ScreenManager.focusedSetting == skin) {
            return;
        }
        if (!client.uuidEquals(event.playerId)) {
            return;
        }
        client.getPlayerSkinCache().getFuture(ProfileComponent.ofDynamic(profileName))
                .getNow(Optional.empty())
                .ifPresent(entry -> event.textures = entry.getTextures());
    }

}
