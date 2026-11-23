package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.internals.Targets;
import com.blamejared.clumps.client.render.world.WorldGeometry;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

public class TargetHalo extends Module {
    private static final CharSequence STYLE_ORBIT = CatalogText.literal("Orbit");
    private static final CharSequence STYLE_DOUBLE = CatalogText.literal("Double");
    private static final CharSequence STYLE_GROUND = CatalogText.literal("Ground");
    private static final int SEGMENTS = 48;
    private static final long ATTACK_LOCK_MS = 5000L;

    public static ChoiceSetting style;
    public static FloatSetting speed;
    public static FloatSetting radius;
    public static FloatSetting range;
    public static BooleanSetting throughWalls;
    public static ColorSetting primaryColor;
    public static ColorSetting secondaryColor;

    private int lastAttackedId = -1;
    private long lastAttackedMs;

    public TargetHalo() {
        super(new ModuleInfo(
                CatalogText.literal("Target Halo"),
                CatalogText.literal("Minimalist orbiting 3D halo ring around your active combat target"),
                ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        style.description(CatalogText.literal("Orbit rises/falls around the target; Double adds a counter-phase halo; Ground locks to feet"));
        speed.tooltip("Animation");
        radius.tooltip("Animation");
        range.tooltip("Targeting");
        primaryColor.tooltip("Appearance");
        secondaryColor.tooltip("Appearance");
        throughWalls.tooltip("Appearance");
    }

    @Override
    public CharSequence getSuffix() {
        return style.getValue();
    }

    @Override
    public void onDisable() {
        lastAttackedId = -1;
        lastAttackedMs = 0L;
        super.onDisable();
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        lastAttackedId = -1;
        lastAttackedMs = 0L;
    }

    @Subscribe
    private void onAttackPost(EntityAttackEvent.Post event) {
        if (isOutsideWorld() || event.getPlayer() != client.player) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity target && target != client.player) {
            lastAttackedId = target.getId();
            lastAttackedMs = System.currentTimeMillis();
        }
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        LivingEntity target = resolveTarget();
        if (target == null) {
            return;
        }
        float tickDelta = event.getTickProgress();
        Vec3d pos = target.getLerpedPos(tickDelta);
        double h = Math.max(0.8, target.getHeight());
        double r = Math.max(0.35, target.getWidth() * 0.5 + radius.getValue());
        double phase = (System.currentTimeMillis() * 0.0025) * speed.getValue();

        WorldGeometry geometry = throughWalls.getValue()
                ? event.getRenderContext().getOverlayGeometry()
                : event.getRenderContext().getDepthTestedGeometry();

        int colA = primaryColor.getArgb();
        int colB = secondaryColor.getArgb();

        if (style.isSelected(STYLE_GROUND)) {
            drawHaloRing(geometry, pos.x, pos.y + 0.04, pos.z, r, 0.24, phase, colA, colB, true);
        } else {
            double wave1 = 0.5 + 0.5 * Math.sin(phase);
            double y1 = pos.y + 0.1 + wave1 * (h - 0.15);
            double ribbon = 0.26 * Math.cos(phase);
            drawHaloRing(geometry, pos.x, y1, pos.z, r, ribbon, phase, colA, colB, false);

            if (style.isSelected(STYLE_DOUBLE)) {
                double phase2 = phase + Math.PI;
                double wave2 = 0.5 + 0.5 * Math.sin(phase2);
                double y2 = pos.y + 0.1 + wave2 * (h - 0.15);
                double ribbon2 = 0.22 * Math.cos(phase2);
                drawHaloRing(geometry, pos.x, y2, pos.z, r * 0.96, ribbon2, -phase2, colB, colA, false);
            }
        }
    }

    private void drawHaloRing(
            WorldGeometry geometry, double cx, double y, double cz,
            double r, double ribbonHeight, double angleOffset,
            int colorA, int colorB, boolean flatGround) {
        double prevCos = Math.cos(angleOffset);
        double prevSin = Math.sin(angleOffset);
        int prevLine = blendColor(colorA, colorB, 0.0F, 1.0F);
        int prevRibbon = blendColor(colorA, colorB, 0.0F, 0.42F);

        for (int i = 1; i <= SEGMENTS; i++) {
            float frac = (float) i / (float) SEGMENTS;
            double angle = angleOffset + Math.PI * 2.0 * frac;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            float blend = 0.5F + 0.5F * (float) Math.sin(frac * Math.PI * 2.0);
            int curLine = blendColor(colorA, colorB, blend, 1.0F);
            int curRibbon = blendColor(colorA, colorB, blend, 0.42F);
            int tailColor = blendColor(colorA, colorB, blend, 0.0F);

            double x0 = cx + prevCos * r;
            double z0 = cz + prevSin * r;
            double x1 = cx + cos * r;
            double z1 = cz + sin * r;

            geometry.drawLine(x0, y, z0, x1, y, z1, curLine);

            if (flatGround) {
                double innerR = Math.max(0.1, r - ribbonHeight);
                double ix0 = cx + prevCos * innerR;
                double iz0 = cz + prevSin * innerR;
                double ix1 = cx + cos * innerR;
                double iz1 = cz + sin * innerR;
                geometry.drawTriangle(x0, y, z0, prevRibbon, x1, y, z1, curRibbon, ix1, y, iz1, tailColor);
                geometry.drawTriangle(x0, y, z0, prevRibbon, ix1, y, iz1, tailColor, ix0, y, iz0, tailColor);
            } else {
                double yTail = y - ribbonHeight;
                geometry.drawTriangle(x0, y, z0, prevRibbon, x1, y, z1, curRibbon, x1, yTail, z1, tailColor);
                geometry.drawTriangle(x0, y, z0, prevRibbon, x1, yTail, z1, tailColor, x0, yTail, z0, tailColor);
            }

            prevCos = cos;
            prevSin = sin;
            prevLine = curLine;
            prevRibbon = curRibbon;
        }
    }

    private LivingEntity resolveTarget() {
        double maxRangeSq = (double) range.getValue() * (double) range.getValue();
        if (Targets.currentTarget != null && isValid(Targets.currentTarget, maxRangeSq)) {
            return Targets.currentTarget;
        }
        if (lastAttackedId >= 0 && System.currentTimeMillis() - lastAttackedMs <= ATTACK_LOCK_MS) {
            Entity attacked = client.world.getEntityById(lastAttackedId);
            if (attacked instanceof LivingEntity living && isValid(living, maxRangeSq)) {
                return living;
            }
        }
        if (client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living && isValid(living, maxRangeSq)) {
            return living;
        }
        PlayerEntity nearest = null;
        double bestDistSq = maxRangeSq;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (!isValid(player, maxRangeSq)) {
                continue;
            }
            double distSq = client.player.squaredDistanceTo(player);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = player;
            }
        }
        return nearest;
    }

    private boolean isValid(LivingEntity entity, double maxRangeSq) {
        return entity != null && entity != client.player
                && entity.isAlive() && !entity.isRemoved() && !entity.isSpectator()
                && TargetSelection.isValidTarget(entity)
                && client.player.squaredDistanceTo(entity) <= maxRangeSq;
    }

    private static int blendColor(int a, int b, float t, float alphaScale) {
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;
        int outA = Math.max(0, Math.min(255, Math.round((aa + (ba - aa) * t) * alphaScale)));
        int outR = Math.round(ar + (br - ar) * t);
        int outG = Math.round(ag + (bg - ag) * t);
        int outB = Math.round(ab + (bb - ab) * t);
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    public static CharSequence bootstrapOrbitStyle() { return STYLE_ORBIT; }
    public static CharSequence bootstrapDoubleStyle() { return STYLE_DOUBLE; }
    public static CharSequence bootstrapGroundStyle() { return STYLE_GROUND; }

    public static void appendBootstrapSettings(TargetHalo module) {
        CatalogSettings.append(module, style);
        CatalogSettings.append(module, speed);
        CatalogSettings.append(module, radius);
        CatalogSettings.append(module, range);
        CatalogSettings.append(module, throughWalls);
        CatalogSettings.append(module, primaryColor);
        CatalogSettings.append(module, secondaryColor);
    }

    public static void assignBootstrapSettings(
            TargetHalo module,
            ChoiceSetting preparedStyle,
            FloatSetting preparedSpeed,
            FloatSetting preparedRadius,
            FloatSetting preparedRange,
            BooleanSetting preparedThroughWalls,
            ColorSetting preparedPrimaryColor,
            ColorSetting preparedSecondaryColor) {
        style = preparedStyle;
        speed = preparedSpeed;
        radius = preparedRadius;
        range = preparedRange;
        throughWalls = preparedThroughWalls;
        primaryColor = preparedPrimaryColor;
        secondaryColor = preparedSecondaryColor;
    }
}
