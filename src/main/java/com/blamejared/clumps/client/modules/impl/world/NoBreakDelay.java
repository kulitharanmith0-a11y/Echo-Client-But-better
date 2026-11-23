package com.blamejared.clumps.client.modules.impl.world;

import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.utils.strings.SegmentedText;

public class NoBreakDelay extends Module {
    public NoBreakDelay() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("No Break Delay"), com.blamejared.clumps.client.init.CatalogText.literal("Removes block break delay"), com.blamejared.clumps.client.modules.ModuleCategory.WORLD));
    }

}
