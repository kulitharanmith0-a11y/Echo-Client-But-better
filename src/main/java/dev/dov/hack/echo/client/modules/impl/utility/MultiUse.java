package dev.dov.hack.echo.client.modules.impl.utility;

import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.UsingItemQueryEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;

public class MultiUse extends Module {
    public MultiUse() {
        super(new ModuleInfo(CatalogText.literal("Multi Use"), CatalogText.literal("Attack and use items in the same tick"), ModuleCategory.UTILITY, false));
    }

    @Subscribe
    private void onUsingItemQuery(UsingItemQueryEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        event.setUsingItem(false);
    }
}
