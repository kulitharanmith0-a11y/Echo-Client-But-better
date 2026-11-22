package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.render.BlockCullingEvent;
import dev.dov.hack.echo.client.event.render.BlockLightingEvent;
import dev.dov.hack.echo.client.event.render.BlockRenderSeedEvent;
import dev.dov.hack.echo.client.event.render.BlockRenderTypeEvent;
import dev.dov.hack.echo.client.event.render.FluidRenderEvent;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.settings.list.RegistryEntryGroup;
import dev.dov.hack.echo.client.settings.specialized.XrayBlockItemListSetting;
import dev.dov.hack.echo.client.utils.strings.Concat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Xray extends Module {
    private static final Direction[] NEIGHBORS = Direction.values();
    private volatile Set<Block> selectedBlocks;
    private ThreadLocal<Boolean> currentBlockExposed;
    private boolean savedChunkCullingEnabled;
    private static final List<RegistryEntryGroup<Item>> ORE_GROUPS = List.of(
            oreGroup("Diamond", ItemTags.DIAMOND_ORES), oreGroup("Gold", ItemTags.GOLD_ORES), oreGroup("Iron", ItemTags.IRON_ORES), oreGroup("Emerald", ItemTags.EMERALD_ORES), oreGroup("Redstone", ItemTags.REDSTONE_ORES), oreGroup("Lapis", ItemTags.LAPIS_ORES), oreGroup("Coal", ItemTags.COAL_ORES), oreGroup("Copper", ItemTags.COPPER_ORES));
    private static final Item[] DEFAULT_ITEMS = {
        Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE, Items.ANCIENT_DEBRIS, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.NETHER_GOLD_ORE, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE, Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE, Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.NETHER_QUARTZ_ORE
    };
    public static BooleanSetting throughLiquids;
    public static BooleanSetting exposedToAir;
    public static ItemListSetting blocks = new XrayBlockItemListSetting(CatalogText.literal("Blocks"));

    public Xray() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Xray"), dev.dov.hack.echo.client.init.CatalogText.literal("Renders blocks through walls"), dev.dov.hack.echo.client.modules.ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        selectedBlocks = Collections.emptySet();
        currentBlockExposed = ThreadLocal.withInitial(() -> false);
        savedChunkCullingEnabled = true;
        blocks.onChange(this::onSelectedBlocksChanged);
        throughLiquids.onChange(this::reloadIfEnabled);
        exposedToAir.onChange(this::reloadIfEnabled);
    }

    @Override
    public void onEnable() {
        rebuildSelectedBlocks();
        savedChunkCullingEnabled = Objects.requireNonNull(client, "object reference is null").chunkCullingEnabled;
        Objects.requireNonNull(client, "object reference is null").chunkCullingEnabled = false;
        super.onEnable();
        MinecraftRenderState.reloadWorldRenderer(client);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Objects.requireNonNull(client, "object reference is null").chunkCullingEnabled = savedChunkCullingEnabled;
        MinecraftRenderState.reloadWorldRenderer(client);
    }

    @Subscribe
    public void onBlockRenderType(BlockRenderTypeEvent event) {
        if (!isSelectedBlock(event.block)) {
            event.cancel();
        }
    }

    @Subscribe
    public void onBlockLighting(BlockLightingEvent event) {
        event.fullBrightness = true;
    }

    @Subscribe
    public void onBlockRenderSeed(BlockRenderSeedEvent event) {
        currentBlockExposed.set(false);
        if (!exposedToAir.getValue() || !isSelectedBlock(event.state.getBlock())) {
            return;
        }
        currentBlockExposed.set(hasExposedFace(event.state, event.position));
    }

    @Subscribe
    public void onBlockCulling(BlockCullingEvent event) {
        if (!exposedToAir.getValue() || currentBlockExposed.get()) {
            event.cancel();
        }
    }

    @Subscribe
    public void onFluidRender(FluidRenderEvent event) {
        if (throughLiquids.getValue()) {
            event.cancel();
        }
    }

    private static RegistryEntryGroup<Item> oreGroup(String label, TagKey<Item> tag) {
        return new RegistryEntryGroup<>(Concat.strings(label), item -> new ItemStack(item).isIn(tag));
    }

    private void rebuildSelectedBlocks() {
        initializeFreecamState();
        Set<Block> selected = new HashSet<>();
        for (Block block : Registries.BLOCK) {
            if (blocks.isEntrySelected(block.asItem())) {
                selected.add(block);
            }
        }
        selectedBlocks = Collections.unmodifiableSet(selected);
    }

    private boolean hasExposedFace(BlockState state, BlockPos position) {
        initializeFreecamState();
        ClientWorld world = client.world;
        if (world == null) {
            return false;
        }
        BlockPos.Mutable neighbor = new BlockPos.Mutable();
        for (Direction side : NEIGHBORS) {
            if (Block.shouldDrawSide(state, world.getBlockState(neighbor.set(position, side)), side)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSelectedBlock(Block block) {
        return selectedBlocks.contains(block);
    }

    private void onSelectedBlocksChanged() {
        rebuildSelectedBlocks();
        reloadIfEnabled();
    }

    private void reloadIfEnabled() {
        if (isEnabled()) {
            MinecraftRenderState.reloadWorldRenderer(client);
        }
    }

    
    private static void initializeFreecamState() {
        try {
            Class.forName(Freecam.class.getName(), true, Freecam.class.getClassLoader());
        } catch (ClassNotFoundException impossibleForClassLiteral) {
            throw new IllegalStateException("Resolved class initialization failed", impossibleForClassLiteral);
        }
    }

    
    public static Item[] getDefaultItems() {
        return DEFAULT_ITEMS;
    }

    
    public static List<RegistryEntryGroup<Item>> getOreGroups() {
        return ORE_GROUPS;
    }

}
