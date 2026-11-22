package dev.dov.hack.echo.client.modules.impl.internals;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.render.SkinTexturesEvent;
import dev.dov.hack.echo.client.event.render.TextTransformEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.gui.ScreenManager;
import dev.dov.hack.echo.client.settings.TextSetting;
import java.util.Optional;
import net.minecraft.component.type.ProfileComponent;

public class Protect extends Module {
    public static TextSetting nickname;
    public static TextSetting skin;

    public Protect() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Protect"), dev.dov.hack.echo.client.init.CatalogText.literal("Replaces your name and skin everywhere they are shown"), dev.dov.hack.echo.client.modules.ModuleCategory.INTERNALS));
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
