package dev.dov.hack.echo.client.modules.impl.utility;

import dev.dov.hack.echo.client.settings.RegistryListSetting;
import dev.dov.hack.echo.client.settings.list.RegistryEntryGroup;
import dev.dov.hack.echo.client.utils.blocks.BlockGroups;
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
