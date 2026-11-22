package dev.dov.hack.echo.client.modules.impl.world;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.PlayerInputEvent;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.event.network.PacketReceiveEvent;
import dev.dov.hack.echo.client.event.render.WorldRenderEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.world.WorldGeometry;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;

public class BlockBruteforcer extends Module {
    private static final CharSequence START = CatalogText.literal("Start");
    private static final CharSequence ABORT = CatalogText.literal("Abort");
    private static IntSetting radius;
    private static IntSetting up;
    private static IntSetting down;
    private static IntSetting delay;
    private static IntSetting blocksPerTick;
    private static BooleanSetting freezeMovement;
    private static ChoiceSetting mode;
    private List<BlockPos> pendingProbePositions;
    private Set<BlockPos> probedPositions;
    private Set<BlockPos> confirmedPositions;
    private int delayTicks;
    private boolean probing;
    private BlockPos scanOrigin;

    public BlockBruteforcer() {
        super(new ModuleInfo(CatalogText.literal("Block Bruteforcer"), CatalogText.literal("Reveals obfuscated blocks by bruteforcing each block"), ModuleCategory.WORLD));
    }

    public static void appendBootstrapSettings(BlockBruteforcer Module) {
        CatalogSettings.append(Module, radius);
        CatalogSettings.append(Module, up);
        CatalogSettings.append(Module, down);
        CatalogSettings.append(Module, delay);
        CatalogSettings.append(Module, blocksPerTick);
        CatalogSettings.append(Module, freezeMovement);
        CatalogSettings.append(Module, mode);
    }

    public static void assignBootstrapSettings(BlockBruteforcer Module, IntSetting preparedRadius, IntSetting preparedUp, IntSetting preparedDelay, IntSetting preparedDown, IntSetting preparedBlocksPerTick, ChoiceSetting preparedMode, BooleanSetting preparedFreezeMovement) {
        radius = preparedRadius;
        up = preparedUp;
        delay = preparedDelay;
        down = preparedDown;
        blocksPerTick = preparedBlocksPerTick;
        mode = preparedMode;
        freezeMovement = preparedFreezeMovement;
    }

    @Override
    public void postInitialize() {
        pendingProbePositions = new ArrayList<>();
        probedPositions = new HashSet<>();
        confirmedPositions = new HashSet<>();
        delayTicks = 0;
        probing = false;
        delay.tooltip("Timing");
        blocksPerTick.tooltip("Timing");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Objects.requireNonNull(pendingProbePositions, "object reference is null").clear();
        Objects.requireNonNull(probedPositions, "object reference is null").clear();
        Objects.requireNonNull(confirmedPositions, "object reference is null").clear();
        rebuildProbeQueue();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Objects.requireNonNull(pendingProbePositions, "object reference is null").clear();
        Objects.requireNonNull(probedPositions, "object reference is null").clear();
        Objects.requireNonNull(confirmedPositions, "object reference is null").clear();
        probing = false;
        scanOrigin = null;
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld()) return;
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            setEnabled(false);
            return;
        }
        if (event.getPacket() instanceof BlockUpdateS2CPacket update) {
            BlockPos position = update.getPos();
            if (probedPositions.contains(position)) confirmedPositions.add(position);
        }
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            setEnabled(false);
            return;
        }
        if (probing) probeNextBatch();
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld()) return;
        if (!probing || !freezeMovement.getValue()) return;
        event.forward = false;
        event.backward = false;
        event.left = false;
        event.right = false;
    }

    private void rebuildProbeQueue() {
        probing = true;
        pendingProbePositions.clear();
        probedPositions.clear();
        BlockPos origin = client.player.getBlockPos();
        scanOrigin = origin;
        int horizontal = radius.getValue();
        int above = up.getValue();
        int below = down.getValue();
        for (int y = -below; y <= above; ++y) {
            for (int x = -horizontal; x <= horizontal; ++x) {
                for (int z = -horizontal; z <= horizontal; ++z) {
                    BlockPos position = new BlockPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!client.world.isAir(position) && !probedPositions.contains(position)) {
                        pendingProbePositions.add(position);
                    }
                }
            }
        }
    }

    private void probeNextBatch() {
        if (pendingProbePositions.isEmpty()) {
            probing = false;
            setEnabled(false);
            return;
        }
        if (client.getNetworkHandler() == null && client.interactionManager == null) {
            probing = false;
            return;
        }
        ++delayTicks;
        if (delayTicks < delay.getValue()) return;
        delayTicks = 0;
        int count = Math.min(blocksPerTick.getValue(), pendingProbePositions.size());
        for (int index = 0; index < count; ++index) {
            BlockPos position = pendingProbePositions.remove(0);
            probedPositions.add(position);
            if (mode.isSelected(START)) {
                client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, position, Direction.UP));
            } else if (mode.isSelected(ABORT)) {
                client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, position, Direction.UP));
            }
            client.player.swingHand(Hand.MAIN_HAND);
        }
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld() || scanOrigin == null) return;
        int horizontal = radius.getValue();
        int above = up.getValue();
        int below = down.getValue();
        WorldGeometry geometry = event.getRenderContext().getDepthTestedGeometry();
        double x0 = scanOrigin.getX() - horizontal;
        double y0 = scanOrigin.getY() - below;
        double z0 = scanOrigin.getZ() - horizontal;
        double x1 = scanOrigin.getX() + horizontal + 1;
        double y1 = scanOrigin.getY() + above + 1;
        double z1 = scanOrigin.getZ() + horizontal + 1;
        int color = ColorHelper.fromFloats(0.8F, 1.0F, 1.0F, 1.0F);
        geometry.drawLine(x0, y0, z0, x1, y0, z0, color);
        geometry.drawLine(x1, y0, z0, x1, y0, z1, color);
        geometry.drawLine(x1, y0, z1, x0, y0, z1, color);
        geometry.drawLine(x0, y0, z1, x0, y0, z0, color);
        geometry.drawLine(x0, y1, z0, x1, y1, z0, color);
        geometry.drawLine(x1, y1, z0, x1, y1, z1, color);
        geometry.drawLine(x1, y1, z1, x0, y1, z1, color);
        geometry.drawLine(x0, y1, z1, x0, y1, z0, color);
        geometry.drawLine(x0, y0, z0, x0, y1, z0, color);
        geometry.drawLine(x1, y0, z0, x1, y1, z0, color);
        geometry.drawLine(x1, y0, z1, x1, y1, z1, color);
        geometry.drawLine(x0, y0, z1, x0, y1, z1, color);
    }

    
    public Set<BlockPos> getConfirmedPositions() { return confirmedPositions; }
    public boolean isProbing() { return probing; }
    public int getProgressPercent() {
        int total = pendingProbePositions.size() + probedPositions.size();
        return total == 0 ? 100 : probedPositions.size() * 100 / total;
    }
}
