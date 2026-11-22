package dev.dov.hack.echo.client.modules.impl.world;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.utils.strings.SegmentedText;

public class NoBreakDelay extends Module {
    public NoBreakDelay() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("No Break Delay"), dev.dov.hack.echo.client.init.CatalogText.literal("Removes block break delay"), dev.dov.hack.echo.client.modules.ModuleCategory.WORLD));
    }

}
