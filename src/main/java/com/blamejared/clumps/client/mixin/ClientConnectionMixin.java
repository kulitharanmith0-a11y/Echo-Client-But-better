package com.blamejared.clumps.client.mixin;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.event.network.PacketSendEvent;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Unique private boolean clumps$resendingWithoutFlush;

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void beforeHandlePacket(
            Packet<T> packet, PacketListener listener, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null) {
            coordinator.onServerPacketReceived(packet);
        }
        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void beforeSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo callback) {
        if (ClumpsClient.eventsDisabled) {
            return;
        }
        if (clumps$resendingWithoutFlush) {
            return;
        }
        PacketSendEvent event = new PacketSendEvent(packet);
        event.post();
        if (event.cancelled) {
            callback.cancel();
            return;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        if (coordinator != null && coordinator.filterOutgoingPacket(packet)) {
            callback.cancel();
            return;
        }
        if (flush && !event.shouldFlush()) {
            callback.cancel();
            clumps$resendingWithoutFlush = true;
            try {
                ((ClientConnection) (Object) this).send(packet, listener, false);
            } finally {
                clumps$resendingWithoutFlush = false;
            }
        }
    }
}
