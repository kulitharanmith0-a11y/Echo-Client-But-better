package com.blamejared.clumps.client.modules.impl.utility;

import com.blamejared.clumps.client.settings.RegistryListSetting;
import com.blamejared.clumps.client.settings.list.RegistryEntryGroup;
import com.blamejared.clumps.client.utils.blocks.BlockGroups;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;

final class AntiTrapBlockSelectionSetting extends RegistryListSetting<Block> {
    AntiTrapBlockSelectionSetting(CharSequence label, Supplier<Registry<Block>> registry) {
        super(label, registry);
    }

    @Override
    public List<RegistryEntryGroup<Block>> getGroups() {
        return BlockGroups.copyGroups();
    }

    @Override
    public Function<Block, String> getEntryLabel() {
        return block -> new ItemStack(block.asItem()).getName().getString();
    }
}
