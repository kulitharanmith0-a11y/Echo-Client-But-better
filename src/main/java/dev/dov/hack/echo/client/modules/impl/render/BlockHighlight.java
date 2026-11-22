package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.interaction.BlockInteractEvent;
import dev.dov.hack.echo.client.event.lifecycle.WorldTickContextEvent;
import dev.dov.hack.echo.client.event.render.WorldRenderEvent;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.world.WorldGeometry;
import dev.dov.hack.echo.client.render.world.WorldProjection;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public class BlockHighlight extends Module {
    public static BooleanSetting targetHighlight;
    public static BooleanSetting placementGhost;
    public static BooleanSetting throughWalls;
    public static FloatSetting smoothSpeed;
    public static FloatSetting ghostDuration;
    public static ColorSetting fillColor;
    public static ColorSetting outlineColor;
    public static ColorSetting ghostColor;

    private record PlacedGhost(Box box, long createdMs) {}

    private final List<PlacedGhost> ghosts = new ArrayList<>();
    private Box smoothedBox;
    private long lastFrameNanos;

    public BlockHighlight() {
        super(new ModuleInfo(
                CatalogText.literal("Block Highlight"),
                CatalogText.literal("Smoothly highlights targeted blocks and fades recently placed blocks"),
                ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        targetHighlight.description(CatalogText.literal("Smoothly interpolates a highlight box onto the block you are looking at"));
        placementGhost.description(CatalogText.literal("Draws a fading ghost outline on blocks you just placed"));
        smoothSpeed.setVisibilityPredicate(ignored -> targetHighlight.getValue());
        ghostDuration.setVisibilityPredicate(ignored -> placementGhost.getValue());
        ghostColor.setVisibilityPredicate(ignored -> placementGhost.getValue());

        smoothSpeed.tooltip("Animation");
        ghostDuration.tooltip("Animation");
        fillColor.tooltip("Appearance");
        outlineColor.tooltip("Appearance");
        ghostColor.tooltip("Appearance");
        throughWalls.tooltip("Appearance");
    }

    @Override
    public void onDisable() {
        ghosts.clear();
        smoothedBox = null;
        lastFrameNanos = 0L;
        super.onDisable();
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        ghosts.clear();
        smoothedBox = null;
        lastFrameNanos = 0L;
    }

    @Subscribe
    private void onBlockInteractPost(BlockInteractEvent.Post event) {
        if (isOutsideWorld() || !placementGhost.getValue() || event.getPlayer() != client.player) {
            return;
        }
        ItemStack stack = client.player.getStackInHand(event.getHand());
        if (!(stack.getItem() instanceof BlockItem)) {
            return;
        }
        BlockHitResult hit = event.getHit();
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos placedPos = hit.getBlockPos();
        BlockState existing = client.world.getBlockState(placedPos);
        if (!existing.isReplaceable()) {
            placedPos = placedPos.offset(hit.getSide());
        }
        Box box = new Box(placedPos).expand(0.002);
        long now = System.currentTimeMillis();
        synchronized (ghosts) {
            ghosts.removeIf(g -> g.box().equals(box));
            if (ghosts.size() > 48) {
                ghosts.remove(0);
            }
            ghosts.add(new PlacedGhost(box, now));
        }
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        long nowNanos = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.016F : Math.min(0.1F, (nowNanos - lastFrameNanos) / 1.0E9F);
        lastFrameNanos = nowNanos;

        WorldGeometry geometry = throughWalls.getValue()
                ? event.getRenderContext().getOverlayGeometry()
                : event.getRenderContext().getDepthTestedGeometry();

        if (targetHighlight.getValue()) {
            renderTargetBlock(geometry, dt);
        } else {
            smoothedBox = null;
        }

        if (placementGhost.getValue()) {
            renderPlacementGhosts(geometry);
        }
    }

    private void renderTargetBlock(WorldGeometry geometry, float dt) {
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            smoothedBox = null;
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        if (state.isAir()) {
            smoothedBox = null;
            return;
        }
        VoxelShape shape = state.getOutlineShape(client.world, pos);
        Box desired = shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos);
        desired = desired.expand(0.0025);

        if (smoothedBox == null || smoothedBox.getCenter().squaredDistanceTo(desired.getCenter()) > 16.0) {
            smoothedBox = desired;
        } else {
            double alpha = 1.0 - Math.exp(-Math.max(1.0, smoothSpeed.getValue()) * dt);
            smoothedBox = new Box(
                    lerp(smoothedBox.minX, desired.minX, alpha),
                    lerp(smoothedBox.minY, desired.minY, alpha),
                    lerp(smoothedBox.minZ, desired.minZ, alpha),
                    lerp(smoothedBox.maxX, desired.maxX, alpha),
                    lerp(smoothedBox.maxY, desired.maxY, alpha),
                    lerp(smoothedBox.maxZ, desired.maxZ, alpha));
        }

        int fill = fillColor.getArgb();
        int outline = outlineColor.getArgb();
        if (((fill >>> 24) & 0xFF) > 0) {
            geometry.drawBox(smoothedBox.minX, smoothedBox.minY, smoothedBox.minZ,
                    smoothedBox.getLengthX(), smoothedBox.getLengthY(), smoothedBox.getLengthZ(), fill);
        }
        WorldProjection.drawBoxOutline(geometry, smoothedBox, outline);
    }

    private void renderPlacementGhosts(WorldGeometry geometry) {
        long now = System.currentTimeMillis();
        long maxMs = Math.max(100L, Math.round(ghostDuration.getValue() * 1000.0F));
        int baseGhost = ghostColor.getArgb();
        int baseOutline = (baseGhost & 0x00FFFFFF) | 0xFF000000;

        synchronized (ghosts) {
            Iterator<PlacedGhost> it = ghosts.iterator();
            while (it.hasNext()) {
                PlacedGhost ghost = it.next();
                long elapsed = now - ghost.createdMs();
                if (elapsed >= maxMs) {
                    it.remove();
                    continue;
                }
                float t = (float) elapsed / (float) maxMs;
                float fade = (1.0F - t) * (1.0F - t);
                Box box = ghost.box();
                geometry.drawBox(box.minX, box.minY, box.minZ,
                        box.getLengthX(), box.getLengthY(), box.getLengthZ(),
                        scaleAlpha(baseGhost, fade));
                WorldProjection.drawBoxOutline(geometry, box, scaleAlpha(baseOutline, fade * 0.9F));
            }
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int scaleAlpha(int argb, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static void appendBootstrapSettings(BlockHighlight module) {
        CatalogSettings.append(module, targetHighlight);
        CatalogSettings.append(module, placementGhost);
        CatalogSettings.append(module, throughWalls);
        CatalogSettings.append(module, smoothSpeed);
        CatalogSettings.append(module, ghostDuration);
        CatalogSettings.append(module, fillColor);
        CatalogSettings.append(module, outlineColor);
        CatalogSettings.append(module, ghostColor);
    }

    public static void assignBootstrapSettings(
            BlockHighlight module,
            BooleanSetting preparedTargetHighlight,
            BooleanSetting preparedPlacementGhost,
            BooleanSetting preparedThroughWalls,
            FloatSetting preparedSmoothSpeed,
            FloatSetting preparedGhostDuration,
            ColorSetting preparedFillColor,
            ColorSetting preparedOutlineColor,
            ColorSetting preparedGhostColor) {
        targetHighlight = preparedTargetHighlight;
        placementGhost = preparedPlacementGhost;
        throughWalls = preparedThroughWalls;
        smoothSpeed = preparedSmoothSpeed;
        ghostDuration = preparedGhostDuration;
        fillColor = preparedFillColor;
        outlineColor = preparedOutlineColor;
        ghostColor = preparedGhostColor;
    }
}
