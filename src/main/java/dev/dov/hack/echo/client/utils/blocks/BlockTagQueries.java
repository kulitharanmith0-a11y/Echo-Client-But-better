package dev.dov.hack.echo.client.utils.blocks;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class BlockTagQueries {
    private BlockTagQueries() {}
    public static TagKey<Block> minecraftTag(String path) {
        return TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla(path));
    }
    public static boolean contains(TagKey<Block> tag, Block block) {
        return block.getDefaultState().isIn(tag);
    }
}
