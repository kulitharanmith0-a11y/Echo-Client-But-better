package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.UsingItemQueryEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;

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
