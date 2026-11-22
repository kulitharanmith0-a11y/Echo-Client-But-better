package dev.dov.hack.echo.client.modules.impl.utility;

import dev.dov.hack.echo.client.handlers.action.ActionCoordinator;
import dev.dov.hack.echo.client.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PearlCatchCalculations {
    private PearlCatchCalculations() {
    }

    
    public static float aimSpeed(float targetYaw, float targetPitch, float playerYaw, float playerPitch, int throwTick, int playerAge) {
        float yawDifference = Math.abs(MathHelper.wrapDegrees(targetYaw - playerYaw));
        float pitchDifference = Math.abs(targetPitch - playerPitch);
        float largestDifference = Math.max(yawDifference, pitchDifference);
        float availableSeconds = Math.max(1, throwTick - playerAge - 1) * 0.05F;
        return MathHelper.clamp(largestDifference / availableSeconds, 40.0F, 300.0F);
    }

    
    public static Vec3d origin(Vec3d capturedOrigin, Vec3d playerPosition) {
        return capturedOrigin != null ? capturedOrigin : playerPosition;
    }

    
    public static Vec3d restoredVelocity(Vec3d savedVelocity, boolean suppressVertical) {
        return new Vec3d(savedVelocity.x, suppressVertical ? 0.0 : savedVelocity.y, savedVelocity.z);
    }

    
    public static void usePendingOffhandWindCharge(MinecraftClient client) {
        usePendingOffhandWindCharge(client, null);
    }

    public static void usePendingOffhandWindCharge(MinecraftClient client, Module owner) {
        if (client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))
                && !client.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE))
                && client.player.getOffHandStack().isOf(Items.WIND_CHARGE)) {
            ActionCoordinator coordinator = ActionCoordinator.get();
            if (coordinator != null) {
                coordinator.executeItemUse(owner, Hand.OFF_HAND);
            } else {
                client.interactionManager.interactItem(client.player, Hand.OFF_HAND);
            }
        }
    }
}