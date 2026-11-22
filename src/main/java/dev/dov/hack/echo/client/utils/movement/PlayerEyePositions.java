package dev.dov.hack.echo.client.utils.movement;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class PlayerEyePositions {
    private PlayerEyePositions() {}
    public static Vec3d loweredEyePosition(PlayerEntity player) {
        return new Vec3d(player.getX(), player.getEyeY() - 0.1, player.getZ());
    }
}
