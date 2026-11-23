package com.blamejared.clumps.client.modules.impl.movement;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.network.PacketSendEvent;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.SprintOverrideHandler;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.ChoiceSetting;
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

import static com.blamejared.clumps.client.modules.ModuleLiterals.literal;

public class GUIWalk extends Module {
    private Queue<Packet<?>> queuedPackets;
    private boolean flushingPackets;
    private boolean packetDelayActive;
    private int packetDelayCounter;
    private KeyBinding[] movementKeys;
    private static ChoiceSetting mode;
    private static final String VANILLA_MODE = "Vanilla";

    public GUIWalk() {
        super(new ModuleInfo(com.blamejared.clumps.client.init.CatalogText.literal("GUI Walk"), com.blamejared.clumps.client.init.CatalogText.literal("Lets you walk inside a menu"), com.blamejared.clumps.client.modules.ModuleCategory.MOVEMENT));
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
        com.blamejared.clumps.client.init.CatalogSettings.append(Module, GUIWalk.mode);
    }

    
    public static void assignBootstrapSettings(GUIWalk Module, com.blamejared.clumps.client.settings.ChoiceSetting preparedMode) {
        GUIWalk.mode = preparedMode;
    }
}
