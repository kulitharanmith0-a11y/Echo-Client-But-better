package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.render.WorldRenderEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.world.DepthTestedWorldGeometry;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.utils.Rotation;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AimVisualizer extends Module {
    private static FloatSetting size;
    private static ColorSetting serverColor;
    private static ColorSetting clientColor;

    public AimVisualizer() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Aim Visualizer"), dev.dov.hack.echo.client.init.CatalogText.literal("Shows where server-side rotations are aiming"), dev.dov.hack.echo.client.modules.ModuleCategory.RENDER, false));
    }

    @Override
    public void postInitialize() {
        size.tooltip("Appearance");
        serverColor.tooltip("Appearance");
        clientColor.tooltip("Appearance");
    }

    private Vec3d findEndpoint(Vec3d origin, float pitch, float yaw, AttackRangeComponent range) {
        Vec3d direction = client.player.getRotationVector(pitch, yaw);
        float hitboxMargin = range.hitboxMargin();
        Vec3d minimum = origin.add(direction.multiply(range.getEffectiveMinRange(client.player)));
        Vec3d maximum = origin.add(direction.multiply(range.getEffectiveMaxRange(client.player)
                + Math.max(0.0, client.player.getMovement().dotProduct(direction))));
        BlockHitResult blockHit = client.world.raycast(new RaycastContext(origin, maximum, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            maximum = blockHit.getPos();
            if (origin.squaredDistanceTo(maximum) < origin.squaredDistanceTo(minimum)) {
                return maximum;
            }
        }
        Box search = Box.of(minimum, hitboxMargin, hitboxMargin, hitboxMargin)
                .stretch(maximum.subtract(minimum)).expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityCollision(client.world, client.player, minimum, maximum, search, EntityPredicates.CAN_HIT, hitboxMargin);
        return entityHit != null ? entityHit.getPos() : maximum;
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.player == null || client.world == null) {
            return;
        }
        float tickProgress = event.getTickProgress();
        Vec3d origin = client.player.getCameraPosVec(tickProgress);
        ItemStack heldStack = client.player.getMainHandStack();
        AttackRangeComponent range = heldStack.get(DataComponentTypes.ATTACK_RANGE);
        if (range == null) {
            range = AttackRangeComponent.defaultForEntity(client.player);
        }
        boolean showServerAim = rotations().isSilentRotationActive();
        Vec3d serverEndpoint = null;
        if (showServerAim) {
            float[] angles = getServerAngles();
            float yaw = angles[0];
            float pitch = angles[1];
            serverEndpoint = findEndpoint(origin, pitch, yaw, range);
        }
        float clientYaw = client.player.getLerpedYaw(tickProgress);
        float clientPitch = client.player.getLerpedPitch(tickProgress);
        Vec3d clientEndpoint = findEndpoint(origin, clientPitch, clientYaw, range);
        int serverArgb = ColorHelper.fromFloats(serverColor.getAlphaFraction(), serverColor.getRedFraction(), serverColor.getGreenFraction(), serverColor.getBlueFraction());
        int clientArgb = ColorHelper.fromFloats(clientColor.getAlphaFraction(), clientColor.getRedFraction(), clientColor.getGreenFraction(), clientColor.getBlueFraction());
        float serverSize = size.getValue();
        double clientSize = (double) serverSize - 0.001;
        DepthTestedWorldGeometry geometry = event.getRenderContext().getDepthTestedGeometry();
        if (showServerAim) {
            geometry.drawBox(serverEndpoint.x - (double) serverSize * 0.5, serverEndpoint.y - (double) serverSize * 0.5, serverEndpoint.z - (double) serverSize * 0.5, serverSize, serverSize, serverSize, serverArgb);
        }
        geometry.drawBox(clientEndpoint.x - clientSize * 0.5, clientEndpoint.y - clientSize * 0.5, clientEndpoint.z - clientSize * 0.5, clientSize, clientSize, clientSize, clientArgb);
    }

    private float[] getServerAngles() {
        if (rotations().isSilentRotationActive()) {
            Rotation rotation = rotations().effectiveRotation();
            return new float[] {rotation.yaw(), rotation.pitch()};
        }
        Rotation rotation = rotations().effectiveRotation();
        return new float[] {rotation.yaw(), rotation.pitch()};
    }

    
    public static void appendBootstrapSettings(AimVisualizer Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AimVisualizer.size);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AimVisualizer.serverColor);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, AimVisualizer.clientColor);
    }

    
    public static void assignBootstrapSettings(AimVisualizer Module, dev.dov.hack.echo.client.settings.FloatSetting preparedSize, dev.dov.hack.echo.client.settings.ColorSetting preparedServerColor, dev.dov.hack.echo.client.settings.ColorSetting preparedClientColor) {
        AimVisualizer.size = preparedSize;
        AimVisualizer.serverColor = preparedServerColor;
        AimVisualizer.clientColor = preparedClientColor;
    }
}
