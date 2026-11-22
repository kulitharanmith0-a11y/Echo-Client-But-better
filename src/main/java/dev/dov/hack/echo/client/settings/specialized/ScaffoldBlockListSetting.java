package dev.dov.hack.echo.client.settings.specialized;

import dev.dov.hack.echo.client.modules.impl.world.Scaffold;
import dev.dov.hack.echo.client.settings.RegistryListSetting;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;

public class ScaffoldBlockListSetting extends RegistryListSetting<Block> {
    public ScaffoldBlockListSetting(CharSequence label, Supplier<Registry<Block>> registry) {
        super(label, registry);
    }

    @Override public Function<Block, String> getEntryLabel() {
        return block -> new ItemStack(block.asItem()).getName().getString();
    }

    @Override public Predicate<Block> getEntryFilter() {
        return new Predicate<>() {
            @Override public boolean test(Block block) {
                return block.asItem() instanceof BlockItem && Scaffold.isSuitableBlock(block);
            }
        };
    }
}
