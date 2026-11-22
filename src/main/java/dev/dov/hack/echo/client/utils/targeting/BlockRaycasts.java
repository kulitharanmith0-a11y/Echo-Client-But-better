package dev.dov.hack.echo.client.utils.targeting;

import static dev.dov.hack.echo.client.MinecraftAccess.CLIENT;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.MinecraftAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class BlockRaycasts implements MinecraftAccess {
    private BlockRaycasts() {
    }

    
    public static BlockHitResult raycast(float yaw, float pitch) {
        if (CLIENT.player == null || CLIENT.world == null) {
            return null;
        }
        return raycast(CLIENT.player.getCameraPosVec(1.0F), yaw, pitch);
    }

    
    public static BlockHitResult raycast(Vec3d origin, float yaw, float pitch) {
        if (CLIENT.player == null || CLIENT.world == null) {
            return null;
        }
        Vec3d end = origin.add(CLIENT.player.getRotationVector(pitch, yaw)
                .multiply(CLIENT.player.getBlockInteractionRange()));
        return CLIENT.world.raycast(new RaycastContext(origin, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, CLIENT.player));
    }

    
    public static HitResult raycastTo(Vec3d end, float tickDelta) {
        if (CLIENT.player == null || CLIENT.world == null) {
            return null;
        }
        Vec3d origin = CLIENT.player.getCameraPosVec(tickDelta);
        return CLIENT.world.raycast(new RaycastContext(origin, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, CLIENT.player));
    }

    
    public static boolean matchesHit(BlockHitResult expected, boolean requireSameSide) {
        BlockHitResult actual = raycast(
                EchoClient.handlerManager.getRotationManager().effectiveRotation().yaw(), EchoClient.handlerManager.getRotationManager().effectiveRotation().pitch());
        if (actual == null || actual.getType() != HitResult.Type.BLOCK
                || !actual.getBlockPos().equals(expected.getBlockPos())) {
            return false;
        }
        if (requireSameSide && actual.getSide() != expected.getSide()) {
            return false;
        }
        Vec3d origin = CLIENT.player.getCameraPosVec(1.0F);
        return ProjectileUtil.raycast(CLIENT.player, origin, actual.getPos(), new Box(origin, actual.getPos()).expand(1.0), Entity::canHit, origin.squaredDistanceTo(actual.getPos())) == null;
    }

    
    public static boolean intersectsBlock(float yaw, float pitch, BlockPos position) {
        if (CLIENT.player == null || position == null) {
            return false;
        }
        Vec3d origin = CLIENT.player.getCameraPosVec(1.0F);
        Vec3d direction = CLIENT.player.getRotationVector(pitch, yaw);
        Vec3d end = origin.add(direction.multiply(CLIENT.player.getBlockInteractionRange()));
        return new Box(position).expand(1.0E-4).raycast(origin, end).isPresent();
    }
}
