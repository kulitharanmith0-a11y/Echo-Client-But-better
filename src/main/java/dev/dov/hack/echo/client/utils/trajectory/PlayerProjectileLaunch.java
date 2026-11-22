package dev.dov.hack.echo.client.utils.trajectory;

import dev.dov.hack.echo.client.EchoClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class PlayerProjectileLaunch {
    private PlayerProjectileLaunch() {}

    public static Vec3d direction(PlayerEntity player, float pitchOffset) {
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (player == MinecraftClient.getInstance().player
                && EchoClient.handlerManager.getRotationManager().isSilentRotationActive()) {
            yaw = EchoClient.handlerManager.getRotationManager().effectiveRotation().yaw();
            pitch = EchoClient.handlerManager.getRotationManager().effectiveRotation().pitch();
        }
        return Vec3d.fromPolar(pitch + pitchOffset, yaw).normalize();
    }

    public static Vec3d inheritedMotion(PlayerEntity player) {
        Vec3d displacement = player.getEntityPos().subtract(player.lastX, player.lastY, player.lastZ);
        if (displacement.lengthSquared() - 4.0E-8 <= 0.0) {
            return Vec3d.ZERO;
        }
        return player.isOnGround() ? new Vec3d(displacement.x, 0.0, displacement.z) : displacement;
    }
}
