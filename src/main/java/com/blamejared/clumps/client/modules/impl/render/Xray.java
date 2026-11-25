package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.render.BlockCullingEvent;
import com.blamejared.clumps.client.event.render.BlockLightingEvent;
import com.blamejared.clumps.client.event.render.BlockRenderSeedEvent;
import com.blamejared.clumps.client.event.render.BlockRenderTypeEvent;
import com.blamejared.clumps.client.event.render.FluidRenderEvent;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.render.world.WorldGeometry;
import com.blamejared.clumps.client.render.world.WorldProjection;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.settings.list.RegistryEntryGroup;
import com.blamejared.clumps.client.settings.specialized.XrayBlockItemListSetting;
import com.blamejared.clumps.client.utils.strings.Concat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.math.Direction;

public class Xray extends Module {
    private static final Direction[] NEIGHBORS = Direction.values();
    private static final int SCAN_INTERVAL_TICKS = 15;
    private static final int MAX_HIGHLIGHT_BLOCKS = 512;
    private volatile Set<Block> selectedBlocks;
    private ThreadLocal<Boolean> currentBlockExposed;
    private boolean savedChunkCullingEnabled;
    private final List<HighlightedBlock> highlightedBlocks = new ArrayList<>();
    private int scanCooldownTicks;
    private BlockPos lastScanOrigin;
    private static final List<RegistryEntryGroup<Item>> ORE_GROUPS = List.of(
            oreGroup("Diamond", ItemTags.DIAMOND_ORES),
            oreGroup("Gold", ItemTags.GOLD_ORES),
            oreGroup("Iron", ItemTags.IRON_ORES),
            oreGroup("Emerald", ItemTags.EMERALD_ORES),
            oreGroup("Redstone", ItemTags.REDSTONE_ORES),
            oreGroup("Lapis", ItemTags.LAPIS_ORES),
            oreGroup("Coal", ItemTags.COAL_ORES),
            oreGroup("Copper", ItemTags.COPPER_ORES),
            customGroup("Ancient Debris", item -> item == Items.ANCIENT_DEBRIS || item == Items.NETHER_QUARTZ_ORE),
            customGroup("Storage", Xray::isStorageItem),
            customGroup("Spawners & Vaults", item -> item == Items.SPAWNER || item == Items.TRIAL_SPAWNER
                    || item == Items.VAULT || item == Items.BEACON),
            customGroup("Utility", item -> item == Items.END_PORTAL_FRAME || item == Items.ENCHANTING_TABLE
                    || item == Items.ANVIL || item == Items.RESPAWN_ANCHOR
                    || item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN || item == Items.TNT));
    private static final Item[] DEFAULT_ITEMS = {
        Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE, Items.ANCIENT_DEBRIS, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.NETHER_GOLD_ORE, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE, Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE, Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.NETHER_QUARTZ_ORE
    };
    public static BooleanSetting throughLiquids;
    public static BooleanSetting exposedToAir;
    public static ItemListSetting blocks = new XrayBlockItemListSetting(CatalogText.literal("Blocks"));
    private static final BooleanSetting highlightBlocks = new BooleanSetting(CatalogText.literal("Highlight Blocks"), false);
    private static final IntSetting highlightRange = new IntSetting(CatalogText.literal("Highlight Range"), 32, 8, 64, CatalogText.literal("m"));
    private static final BooleanSetting highlightTracers = new BooleanSetting(CatalogText.literal("Highlight Tracers"), false);

    public Xray() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Xray"), com.blamejared.clumps.client.init.CatalogText.literal("Renders blocks through walls with optional ESP highlights"), com.blamejared.clumps.client.modules.ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        selectedBlocks = Collections.emptySet();
        currentBlockExposed = ThreadLocal.withInitial(() -> false);
        savedChunkCullingEnabled = true;
        blocks.onChange(this::onSelectedBlocksChanged);
        throughLiquids.onChange(this::reloadIfEnabled);
        exposedToAir.onChange(this::onFilterChanged);
        highlightBlocks.onChange(this::invalidateScan);
        highlightRange.onChange(this::invalidateScan);

        highlightRange.setVisibilityPredicate(ignored -> highlightBlocks.getValue());
        highlightTracers.setVisibilityPredicate(ignored -> highlightBlocks.getValue());
        highlightBlocks.tooltip("Highlights");
        highlightRange.tooltip("Highlights");
        highlightTracers.tooltip("Highlights");
        highlightBlocks.description(CatalogText.literal("Draws color-coded 3D ESP outlines around nearby selected blocks (also works in StreamProof mode)"));
        highlightRange.description(CatalogText.literal("Scan radius in blocks for 3D block highlights"));
        highlightTracers.description(CatalogText.literal("Draws tracers to highlighted blocks"));

        if (!settings.contains(highlightBlocks)) {
            CatalogSettings.append(this, highlightBlocks);
            CatalogSettings.append(this, highlightRange);
            CatalogSettings.append(this, highlightTracers);
        }
    }

    @Override
    public void onEnable() {
        rebuildSelectedBlocks();
        invalidateScan();
        savedChunkCullingEnabled = Objects.requireNonNull(client, "object reference is null").chunkCullingEnabled;
        Objects.requireNonNull(client, "object reference is null").chunkCullingEnabled = false;
        super.onEnable();
        MinecraftRenderState.reloadWorldRenderer(client);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        highlightedBlocks.clear();
        lastScanOrigin = null;
        Objects.requireNonNull(client, "object reference is null").chunkCullingEnabled = savedChunkCullingEnabled;
        MinecraftRenderState.reloadWorldRenderer(client);
    }

    @Subscribe
    public void onWorldContext(WorldTickContextEvent event) {
        highlightedBlocks.clear();
        lastScanOrigin = null;
        scanCooldownTicks = 0;
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (!highlightBlocks.getValue()) {
            if (!highlightedBlocks.isEmpty()) {
                highlightedBlocks.clear();
            }
            return;
        }
        if (client.world == null || client.player == null) {
            highlightedBlocks.clear();
            return;
        }
        BlockPos playerPos = client.player.getBlockPos();
        boolean movedFar = lastScanOrigin == null || lastScanOrigin.getSquaredDistance(playerPos) >= 16.0;
        if (scanCooldownTicks > 0 && !movedFar) {
            scanCooldownTicks--;
            return;
        }
        scanCooldownTicks = SCAN_INTERVAL_TICKS;
        lastScanOrigin = playerPos;
        scanNearbySelectedBlocks(client.world, playerPos);
    }

    @Subscribe
    public void onWorldRender(WorldRenderEvent event) {
        if (!highlightBlocks.getValue() || highlightedBlocks.isEmpty() || client.world == null) {
            return;
        }
        WorldGeometry geometry = event.getRenderContext().getOverlayGeometry();
        boolean drawTracers = highlightTracers.getValue();
        for (HighlightedBlock entry : highlightedBlocks) {
            BlockPos pos = entry.pos();
            int outlineColor = entry.color();
            int fillColor = (outlineColor & 0x00FFFFFF) | 0x33000000;
            Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
            geometry.drawBox(box.minX, box.minY, box.minZ, 1.0, 1.0, 1.0, fillColor);
            WorldProjection.drawBoxOutline(geometry, box, outlineColor);
            if (drawTracers) {
                geometry.drawTracer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, outlineColor);
            }
        }
    }

    private void scanNearbySelectedBlocks(ClientWorld world, BlockPos origin) {
        highlightedBlocks.clear();
        Set<Block> targets = selectedBlocks;
        if (targets.isEmpty()) {
            return;
        }
        int radius = highlightRange.getValue();
        double maxDistSq = (double) radius * radius;
        int minChunkX = (origin.getX() - radius) >> 4;
        int maxChunkX = (origin.getX() + radius) >> 4;
        int minChunkZ = (origin.getZ() - radius) >> 4;
        int maxChunkZ = (origin.getZ() + radius) >> 4;
        int minY = Math.max(world.getBottomY(), origin.getY() - radius);
        int maxY = Math.min(world.getTopYInclusive(), origin.getY() + radius);
        boolean exposedOnly = exposedToAir.getValue();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!(world.getChunk(cx, cz, ChunkStatus.FULL, false) instanceof WorldChunk chunk)) {
                    continue;
                }
                ChunkSection[] sections = chunk.getSectionArray();
                int bottomY = chunk.getBottomY();
                int baseX = cx << 4;
                int baseZ = cz << 4;
                for (int sIdx = 0; sIdx < sections.length; sIdx++) {
                    ChunkSection section = sections[sIdx];
                    if (section == null || section.isEmpty()) {
                        continue;
                    }
                    int secMinY = bottomY + (sIdx << 4);
                    int secMaxY = secMinY + 15;
                    if (secMaxY < minY || secMinY > maxY) {
                        continue;
                    }
                    if (!section.hasAny(state -> targets.contains(state.getBlock()))) {
                        continue;
                    }
                    int yStart = Math.max(0, minY - secMinY);
                    int yEnd = Math.min(15, maxY - secMinY);
                    for (int ly = yStart; ly <= yEnd; ly++) {
                        int wy = secMinY + ly;
                        double dy = wy - origin.getY();
                        double dySq = dy * dy;
                        if (dySq > maxDistSq) {
                            continue;
                        }
                        for (int lx = 0; lx < 16; lx++) {
                            int wx = baseX + lx;
                            double dx = wx - origin.getX();
                            double dxySq = dx * dx + dySq;
                            if (dxySq > maxDistSq) {
                                continue;
                            }
                            for (int lz = 0; lz < 16; lz++) {
                                BlockState state = section.getBlockState(lx, ly, lz);
                                Block block = state.getBlock();
                                if (!targets.contains(block)) {
                                    continue;
                                }
                                int wz = baseZ + lz;
                                double dz = wz - origin.getZ();
                                double distSq = dxySq + dz * dz;
                                if (distSq > maxDistSq) {
                                    continue;
                                }
                                mutable.set(wx, wy, wz);
                                if (exposedOnly && !hasExposedFace(state, mutable)) {
                                    continue;
                                }
                                highlightedBlocks.add(new HighlightedBlock(mutable.toImmutable(), colorForBlock(block), distSq));
                            }
                        }
                    }
                }
            }
        }

        if (highlightedBlocks.size() > MAX_HIGHLIGHT_BLOCKS) {
            highlightedBlocks.sort(Comparator.comparingDouble(HighlightedBlock::distSq));
            highlightedBlocks.subList(MAX_HIGHLIGHT_BLOCKS, highlightedBlocks.size()).clear();
        }
    }

    private static int colorForBlock(Block block) {
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE || block == Blocks.DIAMOND_BLOCK) {
            return 0xFF38BDF8;
        }
        if (block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK) {
            return 0xFFFF5722;
        }
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE || block == Blocks.EMERALD_BLOCK) {
            return 0xFF22C55E;
        }
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE || block == Blocks.GOLD_BLOCK) {
            return 0xFFFACC15;
        }
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return 0xFFEF4444;
        }
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return 0xFF3B82F6;
        }
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return 0xFFE2E8F0;
        }
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return 0xFFFB923C;
        }
        if (block instanceof ShulkerBoxBlock || block == Blocks.SPAWNER || block == Blocks.TRIAL_SPAWNER || block == Blocks.VAULT) {
            return 0xFFE879F9;
        }
        if (block == Blocks.ENDER_CHEST || block == Blocks.BEACON) {
            return 0xFFA855F7;
        }
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL || block == Blocks.HOPPER) {
            return 0xFFF59E0B;
        }
        return 0xFFFFFFFF;
    }

    private static boolean isStorageItem(Item item) {
        if (item == Items.CHEST || item == Items.TRAPPED_CHEST || item == Items.BARREL
                || item == Items.ENDER_CHEST || item == Items.HOPPER) {
            return true;
        }
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static RegistryEntryGroup<Item> customGroup(String label, java.util.function.Predicate<Item> filter) {
        return new RegistryEntryGroup<>(Concat.strings(label), filter);
    }

    private void onFilterChanged() {
        invalidateScan();
        reloadIfEnabled();
    }

    private void invalidateScan() {
        scanCooldownTicks = 0;
        lastScanOrigin = null;
    }

    private record HighlightedBlock(BlockPos pos, int color, double distSq) {}

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
        invalidateScan();
        reloadIfEnabled();
    }

    private void reloadIfEnabled() {
        if (isEnabled()) {
            MinecraftRenderState.reloadWorldRenderer(client);
        }
    }

    
    private static void initializeFreecamState() {
    }

    
    public static Item[] getDefaultItems() {
        return DEFAULT_ITEMS;
    }

    
    public static List<RegistryEntryGroup<Item>> getOreGroups() {
        return ORE_GROUPS;
    }

}
