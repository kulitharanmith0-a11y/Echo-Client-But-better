package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.PlayerInputEvent;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.event.network.PacketSendEvent;
import dev.dov.hack.echo.client.event.render.WorldRenderEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.handlers.SprintOverrideHandler;
import dev.dov.hack.echo.client.keyboard.KeybindingCache;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import static dev.dov.hack.echo.client.modules.ModuleLiterals.literal;

public class GUIWalk extends Module {
    private Queue<Packet<?>> queuedPackets;
    private boolean flushingPackets;
    private boolean packetDelayActive;
    private int packetDelayCounter;
    private KeyBinding[] movementKeys;
    private static ChoiceSetting mode;
    private static final String VANILLA_MODE = "Vanilla";

    public GUIWalk() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("GUI Walk"), dev.dov.hack.echo.client.init.CatalogText.literal("Lets you walk inside a menu"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        queuedPackets = new ConcurrentLinkedDeque<>();
        flushingPackets = false;
        packetDelayActive = false;
        packetDelayCounter = 0;
        super.postInitialize();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        java.util.Objects.requireNonNull(queuedPackets, "object reference is null").clear();
        packetDelayActive = false;
        packetDelayCounter = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        flushQueuedPackets();
        packetDelayActive = false;
        packetDelayCounter = 0;
        SprintOverrideHandler.setSuppressSprint(false);
    }

    private KeyBinding[] getMovementKeys() {
        if (movementKeys == null) {
            movementKeys = new KeyBinding[]{client.options.forwardKey, client.options.rightKey, client.options.leftKey, client.options.jumpKey, client.options.backKey};
        }
        return movementKeys;
    }

    public static boolean isPhysicallyPressed(KeyBinding key) {
        return KeybindingCache.isPressed(key);
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) return;
        if (!(MinecraftRenderState.getScreen() instanceof ChatScreen)) {
            for (KeyBinding key : getMovementKeys()) {
                key.setPressed(isPhysicallyPressed(key));
            }
        }
    }

    @Subscribe
    private void onPacketSend(PacketSendEvent event) {
        if (isOutsideWorld() || flushingPackets || mode.isSelected(VANILLA_MODE)) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClickSlotC2SPacket || packet instanceof CloseHandledScreenC2SPacket
                || packet instanceof ChatMessageC2SPacket || packet instanceof CommandExecutionC2SPacket) {
            queuedPackets.add(packet);
            event.cancel();
            packetDelayActive = true;
            packetDelayCounter = 0;
            SprintOverrideHandler.setSuppressSprint(true);
            return;
        }
        if (packetDelayActive && packet instanceof PlayerMoveC2SPacket) {
            packetDelayCounter++;
            if (packetDelayCounter >= 1) {
                flushQueuedPackets();
                packetDelayActive = false;
                packetDelayCounter = 0;
                SprintOverrideHandler.setSuppressSprint(false);
            }
        }
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (isOutsideWorld()) return;
        if (!queuedPackets.isEmpty() && !packetDelayActive
                && !(MinecraftRenderState.getScreen() instanceof HandledScreen<?>)) {
            packetDelayActive = true;
            packetDelayCounter = 0;
            SprintOverrideHandler.setSuppressSprint(true);
        }
    }

    private void flushQueuedPackets() {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            queuedPackets.clear();
            return;
        }
        flushingPackets = true;
        while (!queuedPackets.isEmpty()) {
            Packet<?> packet = queuedPackets.poll();
            if (packet != null) handler.sendPacket(packet);
        }
        flushingPackets = false;
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || !packetDelayActive) return;
        event.forward = false;
        event.backward = false;
        event.left = false;
        event.right = false;
        event.jump = false;
        event.sneak = false;
    }

    
    public static void appendBootstrapSettings(GUIWalk Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, GUIWalk.mode);
    }

    
    public static void assignBootstrapSettings(GUIWalk Module, dev.dov.hack.echo.client.settings.ChoiceSetting preparedMode) {
        GUIWalk.mode = preparedMode;
    }
}
