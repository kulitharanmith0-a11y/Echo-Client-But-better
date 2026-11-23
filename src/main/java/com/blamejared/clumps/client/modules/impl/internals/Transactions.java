package com.blamejared.clumps.client.modules.impl.internals;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.utils.chat.ChatHudMessages;
import com.blamejared.clumps.client.utils.strings.Concat;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.text.Text;

public class Transactions extends Module {
    private static final String TRANSACTION_PREFIX = "Transaction Packet: ";

    public Transactions() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("Transactions"), com.blamejared.clumps.client.init.CatalogText.literal("Transaction packet logging. Useful for figuring out the anticheat of a server"), com.blamejared.clumps.client.modules.ModuleCategory.INTERNALS, false));
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof CommonPingS2CPacket) {
            client.execute(() -> logTransaction(event));
        }
    }

    private static void logTransaction(PacketReceiveEvent event) {
        int parameter = ((CommonPingS2CPacket) event.getPacket()).getParameter();
        Text message = Text.of(Concat.sequences(TRANSACTION_PREFIX, Concat.integer(parameter)).toString());
        ChatHudMessages.addMessage(message);
    }

}
