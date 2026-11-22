package dev.dov.hack.echo.client.handlers;

import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.interaction.EntityAttackEvent;
import dev.dov.hack.echo.client.event.network.PacketSendEvent;
import dev.dov.hack.echo.client.utils.targeting.EntityPredicates;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

public final class ServerFallDistanceTracker extends ClientHandler {
    private static final double MACE_SMASH_THRESHOLD = 1.5;
    private static ClientPlayerEntity trackedPlayer;
    private static double fallDistance;
    private static double lastSentY;
    private static boolean hasSentPosition;

    public static double fallDistance(PlayerEntity player) {
        if (player == null || player != trackedPlayer) {
            return 0.0;
        }
        return Math.min(fallDistance, player.fallDistance);
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void onPacketSent(PacketSendEvent event) {
        if (event.cancelled || CLIENT.player == null) {
            return;
        }
        if (CLIENT.player != trackedPlayer) {
            trackedPlayer = CLIENT.player;
            fallDistance = 0.0;
            hasSentPosition = false;
        }
        if (event.getPacket() instanceof TeleportConfirmC2SPacket) {
            fallDistance = 0.0;
            hasSentPosition = false;
            return;
        }
        if (!(event.getPacket() instanceof PlayerMoveC2SPacket movement)) {
            return;
        }
        double y = movement.getY(lastSentY);
        double deltaY = hasSentPosition ? y - lastSentY : 0.0;
        if (movement.changesPosition()) {
            lastSentY = y;
            hasSentPosition = true;
        }
        if (deltaY < 0.0 && !CLIENT.player.isTouchingWater()) {
            fallDistance -= deltaY;
        }
        if (movement.isOnGround() || deltaY > 0.0) {
            fallDistance = 0.0;
        }
    }

    @Subscribe
    private void onAttack(EntityAttackEvent.Post event) {
        PlayerEntity attacker = event.getPlayer();
        if (attacker == null || attacker != trackedPlayer || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!attacker.getMainHandStack().isOf(Items.MACE)
                || attacker.isGliding()
                || fallDistance(attacker) <= MACE_SMASH_THRESHOLD) {
            return;
        }
        if (target instanceof PlayerEntity player
                && EntityPredicates.isBlocking(player)
                && !EntityPredicates.isOutsideBlockingArc(player)) {
            return;
        }
        fallDistance = 0.0;
    }
}
