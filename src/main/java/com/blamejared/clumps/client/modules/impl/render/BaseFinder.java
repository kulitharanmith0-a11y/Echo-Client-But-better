package com.blamejared.clumps.client.modules.impl.render;

import static com.blamejared.clumps.client.init.CatalogText.literal;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.config.NameRegistry;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.render.HudRenderEvent;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.render.world.ProjectedBounds;
import com.blamejared.clumps.client.render.world.WorldGeometry;
import com.blamejared.clumps.client.render.world.WorldProjection;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

public class BaseFinder extends Module {
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final double CLUSTER_RADIUS_SQ = 24.0 * 24.0;

    private static final IntSetting range = new IntSetting(literal("Range"), 160, 32, 256, literal("m"));
    private static final IntSetting minBlocks = new IntSetting(literal("Min Blocks"), 3, 1, 20);
    private static final BooleanSetting ignoreNatural = new BooleanSetting(literal("Filter Natural"), true);
    private static final BooleanSetting includeSpawners = new BooleanSetting(literal("Include Spawners"), true);
    private static final BooleanSetting includeWorkstations = new BooleanSetting(literal("Include Workstations"), true);
    private static final BooleanSetting showBoxes = new BooleanSetting(literal("Block Boxes"), true);
    private static final BooleanSetting showLabels = new BooleanSetting(literal("Cluster Labels"), true);
    private static final BooleanSetting tracers = new BooleanSetting(literal("Cluster Tracers"), true);
    private static final BooleanSetting notifyOnFind = new BooleanSetting(literal("Notify On Find"), true);

    private final List<BaseCluster> clusters = new ArrayList<>();
    private final List<ClusterLabel> pendingLabels = new ArrayList<>();
    private final Set<Long> notifiedRegions = new HashSet<>();
    private int scanCooldownTicks;

    public BaseFinder() {
        super(new ModuleInfo(
                literal("Base Finder"),
                literal("Detects player bases and stashes by clustering storage and player-placed blocks"),
                ModuleCategory.RENDER));
    }

    public void initializeDefaults() {
        settings.clear();
        CatalogSettings.append(this, range);
        CatalogSettings.append(this, minBlocks);
        CatalogSettings.append(this, ignoreNatural);
        CatalogSettings.append(this, includeSpawners);
        CatalogSettings.append(this, includeWorkstations);
        CatalogSettings.append(this, showBoxes);
        CatalogSettings.append(this, showLabels);
        CatalogSettings.append(this, tracers);
        CatalogSettings.append(this, notifyOnFind);
        postInitialize();
    }

    @Override
    public void postInitialize() {
        range.tooltip("Detection");
        minBlocks.tooltip("Detection");
        ignoreNatural.tooltip("Detection");
        includeSpawners.tooltip("Detection");
        includeWorkstations.tooltip("Detection");
        showBoxes.tooltip("Visuals");
        showLabels.tooltip("Visuals");
        tracers.tooltip("Visuals");
        notifyOnFind.tooltip("Alerts");

        range.description(literal("Horizontal radius in blocks to scan loaded chunks"));
        minBlocks.description(literal("Minimum clustered base blocks required (Shulkers, Beacons, and Ender Chests always qualify)"));
        ignoreNatural.description(literal("Filters lone dungeon/mineshaft chests unless high-value or workstation blocks are nearby"));
        includeSpawners.description(literal("Detect mob spawners and trial spawners"));
        includeWorkstations.description(literal("Include furnaces, brewing stands, signs, beds, banners, and crafting stations"));
        showBoxes.description(literal("Highlight individual containers and base blocks in 3D"));
        showLabels.description(literal("Display 2D base summary tags with distance and count"));
        tracers.description(literal("Draw tracer lines to each detected base cluster center"));
        notifyOnFind.description(literal("Send a client notification with coordinates when a new base or stash is discovered"));
    }

    @Override
    public void onEnable() {
        clusters.clear();
        pendingLabels.clear();
        notifiedRegions.clear();
        scanCooldownTicks = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        clusters.clear();
        pendingLabels.clear();
        notifiedRegions.clear();
        super.onDisable();
    }

    @Subscribe
    public void onWorldContext(WorldTickContextEvent event) {
        clusters.clear();
        pendingLabels.clear();
        notifiedRegions.clear();
        scanCooldownTicks = 0;
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (client.world == null || client.player == null) {
            clusters.clear();
            return;
        }
        if (scanCooldownTicks > 0) {
            scanCooldownTicks--;
            return;
        }
        scanCooldownTicks = SCAN_INTERVAL_TICKS;
        scanLoadedChunks(client.world, client.player.getBlockPos());
    }

    @Subscribe
    public void onWorldRender(WorldRenderEvent event) {
        pendingLabels.clear();
        if (clusters.isEmpty() || client.world == null || client.player == null) {
            return;
        }
        WorldGeometry geometry = event.getRenderContext().getOverlayGeometry();
        Vec3d camera = MinecraftRenderState.getCamera(client).getCameraPos();
        boolean drawBoxes = showBoxes.getValue();
        boolean drawTracers = tracers.getValue();
        boolean drawLabels = showLabels.getValue();

        for (BaseCluster cluster : clusters) {
            int accent = cluster.accentColor();
            if (drawBoxes) {
                for (DetectedBlock block : cluster.blocks()) {
                    BlockPos p = block.pos();
                    int c = block.color();
                    int fill = (c & 0x00FFFFFF) | 0x36000000;
                    Box box = new Box(p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0);
                    geometry.drawBox(box.minX, box.minY, box.minZ, 1.0, 1.0, 1.0, fill);
                    WorldProjection.drawBoxOutline(geometry, box, c);
                }
            }
            Vec3d center = cluster.center();
            if (drawTracers) {
                geometry.drawTracer(center.x, center.y, center.z, accent);
            }
            if (drawLabels) {
                Box anchor = new Box(center.x - 0.1, center.y + 0.8, center.z - 0.1, center.x + 0.1, center.y + 1.0, center.z + 0.1);
                ProjectedBounds projected = WorldProjection.projectBox(event, anchor);
                if (projected != null && projected.visible) {
                    double dist = camera.distanceTo(center);
                    float sx = (float) ((projected.minX + projected.maxX) * 0.5);
                    float sy = (float) projected.minY;
                    pendingLabels.add(new ClusterLabel(cluster.labelText(dist), sx, sy, accent));
                }
            }
        }
    }

    @Subscribe(priority = EventPriority.HIGH)
    public void onHudRender(HudRenderEvent event) {
        if (pendingLabels.isEmpty()) {
            return;
        }
        UiRenderer renderer = ClumpsClient.uiRenderer;
        if (renderer == null) {
            pendingLabels.clear();
            return;
        }
        float fontSize = 7.5F;
        for (ClusterLabel label : pendingLabels) {
            float textW = FontAsset.INTER.measureText(label.text(), fontSize);
            float pillW = textW + 10.0F;
            float pillH = fontSize + 5.5F;
            float px = UiRenderer.snapPixel(label.x() - pillW * 0.5F);
            float py = UiRenderer.snapPixel(label.y() - pillH);
            renderer.drawRoundedRect(px, py, pillW, pillH, 3.0F, 0xCC11141C);
            WorldProjection.drawRectangleBorder(renderer, px, py, pillW, pillH, 0.75F, label.color());
            renderer.drawTextWithShadow(FontAsset.INTER, label.text(), px + 5.0F, py + 2.5F, fontSize, 0xFFFFFFFF);
        }
        pendingLabels.clear();
    }

    private void scanLoadedChunks(ClientWorld world, BlockPos origin) {
        List<DetectedBlock> detected = new ArrayList<>();
        int maxDist = range.getValue();
        double maxDistSq = (double) maxDist * maxDist;
        int minCx = (origin.getX() - maxDist) >> 4;
        int maxCx = (origin.getX() + maxDist) >> 4;
        int minCz = (origin.getZ() - maxDist) >> 4;
        int maxCz = (origin.getZ() + maxDist) >> 4;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!(world.getChunk(cx, cz, ChunkStatus.FULL, false) instanceof WorldChunk chunk)) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    DetectedBlock item = classify(entity);
                    if (item == null) {
                        continue;
                    }
                    double dx = item.pos().getX() - origin.getX();
                    double dz = item.pos().getZ() - origin.getZ();
                    if (dx * dx + dz * dz <= maxDistSq) {
                        detected.add(item);
                    }
                }
            }
        }

        List<BaseCluster> built = buildClusters(detected);
        clusters.clear();
        clusters.addAll(built);

        if (notifyOnFind.getValue()) {
            for (BaseCluster cluster : built) {
                BlockPos cPos = BlockPos.ofFloored(cluster.center());
                long regionKey = (((long) (cPos.getX() >> 5)) << 32) ^ ((cPos.getZ() >> 5) & 0xFFFFFFFFL);
                if (notifiedRegions.add(regionKey)) {
                    String msg = cluster.kind() + " found at " + cPos.getX() + ", " + cPos.getY() + ", " + cPos.getZ()
                            + " (" + cluster.blocks().size() + " blocks)";
                    NameRegistry.postNotification(this, literal(msg));
                }
            }
        }
    }

    private DetectedBlock classify(BlockEntity entity) {
        if (entity == null || entity.isRemoved()) {
            return null;
        }
        BlockPos pos = entity.getPos();
        if (entity instanceof ShulkerBoxBlockEntity) {
            return new DetectedBlock(pos, BlockKind.SHULKER, 0xFFE879F9, true);
        }
        if (entity instanceof EnderChestBlockEntity || entity instanceof BeaconBlockEntity) {
            return new DetectedBlock(pos, BlockKind.HIGH_VALUE, 0xFFA855F7, true);
        }
        if (entity instanceof ChestBlockEntity || entity instanceof BarrelBlockEntity
                || entity instanceof HopperBlockEntity || entity instanceof DispenserBlockEntity) {
            return new DetectedBlock(pos, BlockKind.STORAGE, 0xFFF59E0B, false);
        }
        if (entity instanceof MobSpawnerBlockEntity || entity instanceof TrialSpawnerBlockEntity) {
            if (!includeSpawners.getValue()) {
                return null;
            }
            return new DetectedBlock(pos, BlockKind.SPAWNER, 0xFF38BDF8, false);
        }
        if (includeWorkstations.getValue()) {
            if (entity instanceof EnchantingTableBlockEntity || entity instanceof BrewingStandBlockEntity
                    || entity instanceof BedBlockEntity || entity instanceof CrafterBlockEntity
                    || entity instanceof JukeboxBlockEntity) {
                return new DetectedBlock(pos, BlockKind.PLAYER_PROOF, 0xFF22C55E, true);
            }
            if (entity instanceof AbstractFurnaceBlockEntity || entity instanceof SignBlockEntity
                    || entity instanceof BannerBlockEntity || entity instanceof LecternBlockEntity
                    || entity instanceof DecoratedPotBlockEntity || entity instanceof ChiseledBookshelfBlockEntity) {
                return new DetectedBlock(pos, BlockKind.WORKSTATION, 0xFFFB923C, false);
            }
        }
        return null;
    }

    private List<BaseCluster> buildClusters(List<DetectedBlock> items) {
        List<BaseCluster> result = new ArrayList<>();
        if (items.isEmpty()) {
            return result;
        }
        boolean[] visited = new boolean[items.size()];
        int required = minBlocks.getValue();
        boolean filterNatural = ignoreNatural.getValue();

        for (int i = 0; i < items.size(); i++) {
            if (visited[i]) {
                continue;
            }
            List<DetectedBlock> group = new ArrayList<>();
            List<Integer> queue = new ArrayList<>();
            visited[i] = true;
            queue.add(i);
            for (int q = 0; q < queue.size(); q++) {
                DetectedBlock current = items.get(queue.get(q));
                group.add(current);
                for (int j = 0; j < items.size(); j++) {
                    if (visited[j]) {
                        continue;
                    }
                    if (current.pos().getSquaredDistance(items.get(j).pos()) <= CLUSTER_RADIUS_SQ) {
                        visited[j] = true;
                        queue.add(j);
                    }
                }
            }

            int shulkers = 0;
            int storage = 0;
            int spawners = 0;
            int playerProof = 0;
            double sumX = 0.0;
            double sumY = 0.0;
            double sumZ = 0.0;
            for (DetectedBlock b : group) {
                sumX += b.pos().getX() + 0.5;
                sumY += b.pos().getY() + 0.5;
                sumZ += b.pos().getZ() + 0.5;
                if (b.kind() == BlockKind.SHULKER) shulkers++;
                if (b.kind() == BlockKind.STORAGE) storage++;
                if (b.kind() == BlockKind.SPAWNER) spawners++;
                if (b.highValue() || b.kind() == BlockKind.WORKSTATION) playerProof++;
            }

            boolean hasHighValue = shulkers > 0 || group.stream().anyMatch(DetectedBlock::highValue);
            if (!hasHighValue && group.size() < required) {
                continue;
            }
            if (filterNatural && !hasHighValue && playerProof == 0 && spawners == 0 && storage <= 2) {
                continue;
            }

            Vec3d center = new Vec3d(sumX / group.size(), sumY / group.size(), sumZ / group.size());
            String kind = shulkers > 0 ? "STASH" : (spawners > 0 && storage <= 2 && playerProof == 0 ? "SPAWNER" : "BASE");
            int accent = shulkers > 0 ? 0xFFE879F9 : (hasHighValue ? 0xFF22C55E : (spawners > 0 && playerProof == 0 ? 0xFF38BDF8 : 0xFFF59E0B));
            result.add(new BaseCluster(kind, center, group, shulkers, accent));
        }
        return result;
    }

    private enum BlockKind { SHULKER, HIGH_VALUE, PLAYER_PROOF, STORAGE, WORKSTATION, SPAWNER }
    private record DetectedBlock(BlockPos pos, BlockKind kind, int color, boolean highValue) {}
    private record ClusterLabel(String text, float x, float y, int color) {}
    private record BaseCluster(String kind, Vec3d center, List<DetectedBlock> blocks, int shulkers, int accentColor) {
        String labelText(double distance) {
            String detail = shulkers > 0
                    ? shulkers + " shulker" + (shulkers == 1 ? "" : "s") + " / " + blocks.size() + " total"
                    : blocks.size() + " block" + (blocks.size() == 1 ? "" : "s");
            return kind + " • " + detail + " • " + (int) Math.round(distance) + "m";
        }
    }
}
