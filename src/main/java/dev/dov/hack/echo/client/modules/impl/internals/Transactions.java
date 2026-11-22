package dev.dov.hack.echo.client.modules.impl.internals;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.network.PacketReceiveEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.utils.chat.ChatHudMessages;
import dev.dov.hack.echo.client.utils.strings.Concat;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.text.Text;

public class Transactions extends Module {
    private static final String TRANSACTION_PREFIX = "Transaction Packet: ";

    public Transactions() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Transactions"), dev.dov.hack.echo.client.init.CatalogText.literal("Transaction packet logging. Useful for figuring out the anticheat of a server"), dev.dov.hack.echo.client.modules.ModuleCategory.INTERNALS, false));
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
