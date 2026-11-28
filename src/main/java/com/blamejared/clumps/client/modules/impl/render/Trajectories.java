package com.blamejared.clumps.client.modules.impl.render;

import static com.blamejared.clumps.client.init.CatalogText.literal;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.mixin.accessor.PersistentProjectileEntityAccessor;
import com.blamejared.clumps.client.render.world.WorldGeometry;
import com.blamejared.clumps.client.render.world.WorldProjection;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.MultiChoiceSetting;
import com.blamejared.clumps.client.utils.trajectory.BlockImpactProjectileTrajectories;
import com.blamejared.clumps.client.utils.trajectory.BowTrajectories;
import com.blamejared.clumps.client.utils.trajectory.CrossbowTrajectories;
import com.blamejared.clumps.client.utils.trajectory.FluidAwareProjectileTrajectories;
import com.blamejared.clumps.client.utils.trajectory.ThrownProjectileTrajectories;
import com.blamejared.clumps.client.utils.trajectory.WindChargeTrajectories;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class Trajectories extends Module {
    private static final int IMPACT_CIRCLE_SEGMENTS = 24;
    private static final double IMPACT_CIRCLE_RADIUS = 0.4;
    private static final double IMPACT_SURFACE_OFFSET = 0.02;
    private static final CharSequence PEARL = literal("Pearl");
    private static final CharSequence POTION = literal("Potion");
    private static final CharSequence WIND_CHARGE = literal("Wind Charge");
    private static final CharSequence BOW = literal("Bow");
    private static final CharSequence CROSSBOW = literal("Crossbow");
    private static final CharSequence FIREWORK = literal("Firework");
    private static final CharSequence ARROW = literal("Arrow");
    private static MultiChoiceSetting trajectories;
    private static ColorSetting pearlColor;
    private static ColorSetting potionColor;
    private static ColorSetting windChargeColor;
    private static ColorSetting bowColor;
    private static ColorSetting crossbowColor;
    private static ColorSetting fireworkColor;
    private static ColorSetting arrowColor;
    private static final BooleanSetting players = new BooleanSetting(literal("Players"), true);
    private static final BooleanSetting includeSelf = new BooleanSetting(literal("Include Self"), false);
    private static final ColorSetting playerColor = new ColorSetting(literal("Player Color"), 80, 220, 255, 220);

    public Trajectories() {
        super(new ModuleInfo(literal("Trajectories"), literal("Renders projectile trajectories"), ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        trajectories.setSelected(literal("Pearl"), true);
        trajectories.setSelected(literal("Potion"), true);
        trajectories.setSelected(literal("Wind Charge"), true);
        trajectories.setSelected(literal("Bow"), true);
        trajectories.setSelected(literal("Crossbow"), true);
        trajectories.setSelected(literal("Firework"), true);
        trajectories.setSelected(literal("Arrow"), true);
        pearlColor.setVisibilityPredicate(ignored -> showsPearls());
        potionColor.setVisibilityPredicate(ignored -> showsPotions());
        windChargeColor.setVisibilityPredicate(ignored -> showsWindCharges());
        bowColor.setVisibilityPredicate(ignored -> showsBow());
        crossbowColor.setVisibilityPredicate(ignored -> showsCrossbow());
        fireworkColor.setVisibilityPredicate(ignored -> showsFireworks());
        arrowColor.setVisibilityPredicate(ignored -> showsArrows());
        pearlColor.tooltip("Appearance");
        potionColor.tooltip("Appearance");
        windChargeColor.tooltip("Appearance");
        bowColor.tooltip("Appearance");
        crossbowColor.tooltip("Appearance");
        fireworkColor.tooltip("Appearance");
        arrowColor.tooltip("Appearance");
        players.description(literal("Predicts and renders where airborne players will land"));
        includeSelf.setVisibilityPredicate(ignored -> players.getValue());
        playerColor.setVisibilityPredicate(ignored -> players.getValue());
        playerColor.tooltip("Appearance");
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        WorldGeometry geometry = event.getRenderContext().getDepthTestedGeometry();
        float tickDelta = event.getTickProgress();
        if (showsPearls() && holdsItem(Items.ENDER_PEARL)) {
            drawTrajectory(geometry, FluidAwareProjectileTrajectories.fromPlayer(client.player, tickDelta), trajectoryColor(pearlColor));
        }
        if (showsPearls()) {
            int argb = trajectoryColor(pearlColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity.getType() == EntityType.ENDER_PEARL) {
                    drawTrajectory(geometry, FluidAwareProjectileTrajectories.fromEntity(entity), argb);
                }
            }
        }
        if (showsPotions() && holdsPotion()) {
            drawTrajectory(geometry, ThrownProjectileTrajectories.fromPlayer(client.player, tickDelta), trajectoryColor(potionColor));
        }
        if (showsPotions()) {
            int argb = trajectoryColor(potionColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof PotionEntity) {
                    drawTrajectory(geometry, ThrownProjectileTrajectories.fromEntity(entity), argb);
                }
            }
        }
        if (showsWindCharges() && holdsItem(Items.WIND_CHARGE)) {
            drawTrajectory(geometry, WindChargeTrajectories.fromPlayer(client.player, tickDelta), trajectoryColor(windChargeColor));
        }
        if (showsWindCharges()) {
            int argb = trajectoryColor(windChargeColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity.getType() == EntityType.WIND_CHARGE) {
                    drawTrajectory(geometry, WindChargeTrajectories.fromEntity(entity), argb);
                }
            }
        }
        if (showsBow() && holdsItem(Items.BOW)) {
            drawTrajectory(geometry, BowTrajectories.fromPlayer(client.player, bowChargeTicks(), tickDelta), trajectoryColor(bowColor));
        }
        if (showsCrossbow() && holdsItem(Items.CROSSBOW)) {
            ItemStack crossbow = heldCrossbow();
            if (!crossbow.isEmpty()) {
                int argb = trajectoryColor(crossbowColor);
                for (List<Vec3d> path : CrossbowTrajectories.fromPlayer(client.player, crossbow, tickDelta)) {
                    drawTrajectory(geometry, path, argb);
                }
            }
        }
        if (showsFireworks()) {
            int argb = trajectoryColor(fireworkColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof FireworkRocketEntity firework) {
                    drawTrajectory(geometry, CrossbowTrajectories.fromFirework(firework), argb);
                }
            }
        }
        if (showsArrows()) {
            int argb = trajectoryColor(arrowColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof PersistentProjectileEntity projectile
                        && !((PersistentProjectileEntityAccessor) projectile).invokeIsInGround()) {
                    drawTrajectory(geometry, BlockImpactProjectileTrajectories.fromEntity(projectile), argb);
                }
            }
        }
        if (players.getValue()) {
            int argb = trajectoryColor(playerColor);
            for (PlayerEntity player : client.world.getPlayers()) {
                if (!includeSelf.getValue() && player == client.player) continue;
                if (!player.isAlive() || player.isSpectator() || player.isOnGround()
                        || player.isTouchingWater() || player.isClimbing() || player.isGliding()) continue;
                List<Vec3d> arc = predictPlayerLandingArc(player, tickDelta);
                if (arc.size() >= 2) {
                    drawTrajectory(geometry, arc, argb);
                }
            }
        }
    }

    private List<Vec3d> predictPlayerLandingArc(PlayerEntity player, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, player.lastX, player.getX());
        double y = MathHelper.lerp(tickDelta, player.lastY, player.getY());
        double z = MathHelper.lerp(tickDelta, player.lastZ, player.getZ());
        Vec3d vel = player.getVelocity();
        double vx = vel.x;
        double vy = vel.y;
        double vz = vel.z;
        if (player != client.player && (vx * vx + vy * vy + vz * vz) < 1.0E-4) {
            vx = player.getX() - player.lastX;
            vy = player.getY() - player.lastY;
            vz = player.getZ() - player.lastZ;
        }
        if (Math.abs(vy) < 0.02 && (vx * vx + vz * vz) < 0.002) {
            return List.of();
        }
        List<Vec3d> points = new ArrayList<>(32);
        points.add(new Vec3d(x, y, z));
        for (int tick = 0; tick < 80; tick++) {
            Vec3d start = new Vec3d(x, y, z);
            double nx = x + vx;
            double ny = y + vy;
            double nz = z + vz;
            Vec3d end = new Vec3d(nx, ny, nz);
            BlockHitResult hit = client.world.raycast(new RaycastContext(
                    start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getPos());
                break;
            }
            points.add(end);
            x = nx;
            y = ny;
            z = nz;
            vx *= 0.91;
            vy = (vy - 0.08) * 0.98;
            vz *= 0.91;
            if (y < client.world.getBottomY() - 16) {
                break;
            }
        }
        return points;
    }

    private int bowChargeTicks() {
        if (!client.player.isUsingItem()) {
            return 20;
        }
        ItemStack active = client.player.getActiveItem();
        if (!(active.getItem() instanceof BowItem)) {
            return 20;
        }
        int ticks = active.getMaxUseTime(client.player) - client.player.getItemUseTimeLeft();
        return MathHelper.clamp(ticks, 0, 20);
    }

    private static boolean showsPearls() { return trajectories.isSelected(PEARL); }
    private static boolean showsWindCharges() { return trajectories.isSelected(WIND_CHARGE); }
    private static boolean showsPotions() { return trajectories.isSelected(POTION); }
    private static boolean showsBow() { return trajectories.isSelected(BOW); }
    private static boolean showsCrossbow() { return trajectories.isSelected(CROSSBOW); }
    private static boolean showsFireworks() { return trajectories.isSelected(FIREWORK); }
    private static boolean showsArrows() { return trajectories.isSelected(ARROW); }

    private ItemStack heldCrossbow() {
        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.getItem() instanceof CrossbowItem) {
            return mainHand;
        }
        ItemStack offHand = client.player.getOffHandStack();
        return offHand.getItem() instanceof CrossbowItem ? offHand : ItemStack.EMPTY;
    }

    private boolean holdsPotion() {
        return holdsItem(Items.SPLASH_POTION) || holdsItem(Items.LINGERING_POTION);
    }

    private static boolean holdsItem(Item item) {
        return client.player.getMainHandStack().getItem() == item
                || client.player.getOffHandStack().getItem() == item;
    }

    private void drawTrajectory(WorldGeometry geometry, List<Vec3d> path, int argb) {
        if (path.size() < 2) {
            return;
        }
        for (int index = 0; index < path.size() - 1; index++) {
            Vec3d start = path.get(index);
            Vec3d end = path.get(index + 1);
            geometry.drawLine(start.x, start.y, start.z, end.x, end.y, end.z, argb);
        }
        Vec3d penultimate = path.get(path.size() - 2);
        Vec3d impact = path.get(path.size() - 1);
        Vec3d normal = new Vec3d(0.0, 1.0, 0.0);
        if (client.world != null) {
            BlockHitResult hit = client.world.raycast(new RaycastContext(penultimate, impact, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
            if (hit.getType() != HitResult.Type.MISS) {
                impact = hit.getPos();
                Direction side = hit.getSide();
                normal = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
                Vec3d towardPrevious = penultimate.subtract(impact);
                if (towardPrevious.lengthSquared() > 1.0E-6 && normal.dotProduct(towardPrevious) < 0.0) {
                    normal = normal.multiply(-1.0);
                }
            }
        }
        Vec3d center = impact.add(normal.normalize().multiply(IMPACT_SURFACE_OFFSET));
        WorldProjection.drawCircle(geometry, center, normal, IMPACT_CIRCLE_RADIUS, IMPACT_CIRCLE_SEGMENTS, argb);
    }

    private int trajectoryColor(ColorSetting setting) {
        return ColorHelper.fromFloats(setting.getAlphaFraction(), setting.getRedFraction(), setting.getGreenFraction(), setting.getBlueFraction());
    }

    public static void appendBootstrapSettings(Trajectories Module) {
        CatalogSettings.append(Module, trajectories);
        CatalogSettings.append(Module, pearlColor);
        CatalogSettings.append(Module, potionColor);
        CatalogSettings.append(Module, windChargeColor);
        CatalogSettings.append(Module, bowColor);
        CatalogSettings.append(Module, crossbowColor);
        CatalogSettings.append(Module, fireworkColor);
        CatalogSettings.append(Module, arrowColor);
        CatalogSettings.append(Module, players);
        CatalogSettings.append(Module, includeSelf);
        CatalogSettings.append(Module, playerColor);
    }

    public static CharSequence bootstrapTrajectoriesOption2() { return PEARL; }
    public static CharSequence bootstrapTrajectoriesOption5() { return POTION; }
    public static CharSequence bootstrapTrajectoriesOption() { return WIND_CHARGE; }
    public static CharSequence bootstrapTrajectoriesOption3() { return BOW; }
    public static CharSequence bootstrapTrajectoriesOption4() { return CROSSBOW; }
    public static CharSequence bootstrapTrajectoriesOption7() { return FIREWORK; }
    public static CharSequence bootstrapTrajectoriesOption6() { return ARROW; }

    public static void assignBootstrapSettings(Trajectories Module, ColorSetting preparedArrowColor, ColorSetting preparedPearlColor, ColorSetting preparedWindChargeColor, ColorSetting preparedPotionColor, ColorSetting preparedFireworkColor, ColorSetting preparedBowColor, ColorSetting preparedCrossbowColor, MultiChoiceSetting preparedTrajectories) {
        arrowColor = preparedArrowColor;
        pearlColor = preparedPearlColor;
        windChargeColor = preparedWindChargeColor;
        potionColor = preparedPotionColor;
        fireworkColor = preparedFireworkColor;
        bowColor = preparedBowColor;
        crossbowColor = preparedCrossbowColor;
        trajectories = preparedTrajectories;
    }
}
