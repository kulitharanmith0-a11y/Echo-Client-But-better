package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.lifecycle.WorldTickContextEvent;
import dev.dov.hack.echo.client.event.render.HudRenderEvent;
import dev.dov.hack.echo.client.event.render.WorldRenderEvent;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.render.world.ProjectedBounds;
import dev.dov.hack.echo.client.render.world.WorldGeometry;
import dev.dov.hack.echo.client.render.world.WorldProjection;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class PearlMarkers extends Module {
    private static final int MAX_SIM_TICKS = 100;
    private static final int RING_SEGMENTS = 36;
    private static final Vec3d UP = new Vec3d(0.0, 1.0, 0.0);

    public static BooleanSetting showPearls;
    public static BooleanSetting showWindCharges;
    public static BooleanSetting showBeam;
    public static BooleanSetting showEtaTag;
    public static BooleanSetting throughWalls;
    public static ColorSetting pearlColor;
    public static ColorSetting windColor;

    private record ProjectedMarker(float screenX, float screenY, String label, int color) {}

    private final List<ProjectedMarker> hudMarkers = new ArrayList<>();

    public PearlMarkers() {
        super(new ModuleInfo(
                CatalogText.literal("Pearl Markers"),
                CatalogText.literal("Pulsing landing rings and ETA tags for airborne Ender Pearls and Wind Charges"),
                ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        showPearls.description(CatalogText.literal("Predict and mark landing points of airborne Ender Pearls"));
        showWindCharges.description(CatalogText.literal("Predict and mark impact points of airborne Wind Charges"));
        showBeam.description(CatalogText.literal("Draw a thin vertical beacon line at the predicted impact"));
        showEtaTag.description(CatalogText.literal("Show a small glass ETA tag over the landing ring"));
        pearlColor.tooltip("Appearance");
        windColor.tooltip("Appearance");
        throughWalls.tooltip("Appearance");
    }

    @Override
    public void onDisable() {
        hudMarkers.clear();
        super.onDisable();
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        hudMarkers.clear();
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        hudMarkers.clear();
        if (isOutsideWorld()) {
            return;
        }
        WorldGeometry geometry = throughWalls.getValue()
                ? event.getRenderContext().getOverlayGeometry()
                : event.getRenderContext().getDepthTestedGeometry();

        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.008);
        float tickDelta = event.getTickProgress();

        for (Entity entity : client.world.getEntities()) {
            boolean isPearl = showPearls.getValue() && entity instanceof EnderPearlEntity;
            boolean isWind = showWindCharges.getValue() && entity instanceof AbstractWindChargeEntity;
            if (!isPearl && !isWind) {
                continue;
            }
            if (entity.isRemoved()) {
                continue;
            }
            LandingPrediction prediction = simulateLanding(entity, tickDelta, isPearl);
            if (prediction == null) {
                continue;
            }
            int baseColor = isPearl ? pearlColor.getArgb() : windColor.getArgb();
            int solid = (baseColor & 0x00FFFFFF) | 0xFF000000;
            Vec3d landing = prediction.point();
            double y = landing.y + 0.03;

            double outerR = (isPearl ? 0.52 : 0.42) + pulse * 0.14;
            double innerR = Math.max(0.16, outerR - 0.14);
            WorldProjection.drawCircle(geometry, new Vec3d(landing.x, y, landing.z), UP, outerR, RING_SEGMENTS, solid);
            WorldProjection.drawCircle(geometry, new Vec3d(landing.x, y, landing.z), UP, innerR, RING_SEGMENTS, scaleAlpha(solid, 0.55F));

            // Glowing ground disc gradient
            int rim = scaleAlpha(baseColor, 0.55F);
            int centerCol = scaleAlpha(baseColor, 0.10F);
            double prevCos = 1.0;
            double prevSin = 0.0;
            for (int i = 1; i <= RING_SEGMENTS; i++) {
                double a = (Math.PI * 2.0 * i) / RING_SEGMENTS;
                double cos = Math.cos(a);
                double sin = Math.sin(a);
                geometry.drawTriangle(
                        landing.x, y, landing.z, centerCol,
                        landing.x + prevCos * outerR, y, landing.z + prevSin * outerR, rim,
                        landing.x + cos * outerR, y, landing.z + sin * outerR, rim);
                prevCos = cos;
                prevSin = sin;
            }

            if (showBeam.getValue()) {
                double beamHeight = isPearl ? 2.2 : 1.4;
                geometry.drawLine(landing.x, y, landing.z, landing.x, y + beamHeight, landing.z, scaleAlpha(solid, 0.75F));
            }

            if (showEtaTag.getValue()) {
                Box markerBox = new Box(landing.x - 0.05, y + 0.6, landing.z - 0.05,
                        landing.x + 0.05, y + 0.7, landing.z + 0.05);
                ProjectedBounds projected = WorldProjection.projectBox(event, markerBox);
                if (projected.visible) {
                    float seconds = prediction.ticks() * 0.05F;
                    String prefix = isPearl ? "Pearl" : "Wind";
                    String label = String.format(Locale.ROOT, "%s %.1fs", prefix, seconds);
                    hudMarkers.add(new ProjectedMarker(
                            (float) ((projected.minX + projected.maxX) * 0.5),
                            (float) projected.minY,
                            label,
                            solid));
                }
            }
        }
    }

    @Subscribe
    private void onHudRender(HudRenderEvent event) {
        if (isOutsideWorld() || hudMarkers.isEmpty() || !showEtaTag.getValue()) {
            return;
        }
        UiRenderer ui = event.getRenderer();
        if (ui == null || FontAsset.INTER_SEMI_BOLD == null) {
            return;
        }
        for (ProjectedMarker marker : hudMarkers) {
            float textW = FontAsset.INTER_SEMI_BOLD.measureText(marker.label(), 11.0F);
            float padX = 7.0F;
            float w = textW + padX * 2.0F;
            float h = 20.0F;
            float x = marker.screenX() - w * 0.5F;
            float y = marker.screenY() - h - 4.0F;
            ui.drawRoundedRect(x, y, w, h, 5.0F, 0xCC0D0D10, 1.0F, scaleAlpha(marker.color(), 0.7F));
            ui.drawText(FontAsset.INTER_SEMI_BOLD, marker.label(), x + padX, y + 4.5F, 11.0F, 0xFFFFFFFF);
        }
    }

    private LandingPrediction simulateLanding(Entity entity, float tickDelta, boolean isPearl) {
        Vec3d pos = entity.getLerpedPos(tickDelta);
        Vec3d vel = entity.getVelocity();
        double drag = isPearl ? 0.99 : 0.95;
        double gravity = isPearl ? 0.03 : 0.0;

        for (int tick = 1; tick <= MAX_SIM_TICKS; tick++) {
            Vec3d next = pos.add(vel);
            BlockHitResult hit = client.world.raycast(new RaycastContext(
                    pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                return new LandingPrediction(hit.getPos(), tick);
            }
            pos = next;
            vel = vel.multiply(drag).add(0.0, -gravity, 0.0);
            if (vel.lengthSquared() < 1.0E-5) {
                break;
            }
        }
        return null;
    }

    private record LandingPrediction(Vec3d point, int ticks) {}

    private static int scaleAlpha(int argb, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static void appendBootstrapSettings(PearlMarkers module) {
        CatalogSettings.append(module, showPearls);
        CatalogSettings.append(module, showWindCharges);
        CatalogSettings.append(module, showBeam);
        CatalogSettings.append(module, showEtaTag);
        CatalogSettings.append(module, throughWalls);
        CatalogSettings.append(module, pearlColor);
        CatalogSettings.append(module, windColor);
    }

    public static void assignBootstrapSettings(
            PearlMarkers module,
            BooleanSetting preparedShowPearls,
            BooleanSetting preparedShowWindCharges,
            BooleanSetting preparedShowBeam,
            BooleanSetting preparedShowEtaTag,
            BooleanSetting preparedThroughWalls,
            ColorSetting preparedPearlColor,
            ColorSetting preparedWindColor) {
        showPearls = preparedShowPearls;
        showWindCharges = preparedShowWindCharges;
        showBeam = preparedShowBeam;
        showEtaTag = preparedShowEtaTag;
        throughWalls = preparedThroughWalls;
        pearlColor = preparedPearlColor;
        windColor = preparedWindColor;
    }
}
