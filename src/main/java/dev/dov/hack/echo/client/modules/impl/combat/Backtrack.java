package dev.dov.hack.echo.client.modules.impl.combat;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.interaction.EntityAttackEvent;
import dev.dov.hack.echo.client.event.lifecycle.RenderTickEvent;
import dev.dov.hack.echo.client.event.lifecycle.WorldTickContextEvent;
import dev.dov.hack.echo.client.event.network.PacketReceiveEvent;
import dev.dov.hack.echo.client.event.render.WorldRenderEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.handlers.AttackCooldownTracker;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.render.world.OverlayWorldGeometry;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.FloatRangeSetting;
import dev.dov.hack.echo.client.settings.HotbarSlotsSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.utils.CooldownTimer;
import dev.dov.hack.echo.client.utils.MathRng;
import dev.dov.hack.echo.client.utils.network.Packets;
import dev.dov.hack.echo.client.utils.network.VelocityPackets;
import dev.dov.hack.echo.client.utils.strings.Concat;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Backtrack extends Module {
    private static final CharSequence rangeSeparator = CatalogText.literal("-");
    private static final CharSequence millisecondsSuffix = CatalogText.literal("ms");
    private static FloatRangeSetting holdTime;
    private static IntSetting maximumHurtTime;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static ColorSetting fillColor;
    private static ColorSetting outlineColor;
    private ArrayDeque<DelayedPacket> delayedPackets;
    private CooldownTimer trackingTimer;
    private LivingEntity trackedTarget;
    private long holdMillis;
    private Vec3d serverTargetPosition;
    private Vec3d previousRenderPosition;
    private Vec3d renderPosition;
    private static String timingTooltip;
    private static String itemTooltip;
    private static String hotbarTooltip;
    private static String fillTooltip;
    private static String outlineTooltip;

    public Backtrack() {
        super(new ModuleInfo(CatalogText.literal("Backtrack"), CatalogText.literal("Attacks targets at their previous positions"), ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        delayedPackets = new ArrayDeque<>();
        trackingTimer = new CooldownTimer();
        holdTime.setAllowOutOfRange(true);
        maximumHurtTime.setAllowOutOfRange(true);
        maximumHurtTime.description(CatalogText.literal("Activates when the target has at most this many hurt ticks remaining."));
        itemWhitelist.description(CatalogText.literal("Activates only with selected items. Empty allows every item."));
        if (timingTooltip == null) timingTooltip = "Timing";
        holdTime.tooltip(timingTooltip);
        if (itemTooltip == null) itemTooltip = "Conditions";
        itemWhitelist.tooltip(itemTooltip);
        if (hotbarTooltip == null) hotbarTooltip = "Conditions";
        hotbarWhitelist.tooltip(hotbarTooltip);
        if (fillTooltip == null) fillTooltip = "Appearance";
        fillColor.tooltip(fillTooltip);
        if (outlineTooltip == null) outlineTooltip = "Appearance";
        outlineColor.tooltip(outlineTooltip);
    }

    @Override
    public CharSequence getSuffix() {
        int lower = (int) holdTime.getLower();
        int upper = (int) holdTime.getUpper();
        return lower == upper ? Concat.sequences(Concat.integer(lower), millisecondsSuffix)
                : Concat.sequences(Concat.integer(lower), rangeSeparator, Concat.integer(upper), millisecondsSuffix);
    }

    @Override
    public synchronized void onEnable() {
        resetTracking(false);
        super.onEnable();
    }

    @Override
    public synchronized void onDisable() {
        resetTracking(true);
        super.onDisable();
    }

    @Subscribe
    private synchronized void onWorldContext(WorldTickContextEvent event) {
        resetTracking(false);
    }

    @Subscribe
    private synchronized void onAttack(EntityAttackEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.isConnectedToLocalServer(client)) return;
        if (!(event.getTarget() instanceof LivingEntity target) || !target.isAlive()) return;
        if (trackedTarget != null) {
            if (trackedTarget == target) return;
            clearTrackedTarget();
        }
        if (!itemWhitelist.acceptsHeldItem() || !hotbarWhitelist.permitsCurrentSlot()) return;
        if (AttackCooldownTracker.remainingAttackCooldown(target) > maximumHurtTime.getValue()) return;
        trackedTarget = target;
        trackingTimer.reset();
        holdMillis = Math.max(0L, Math.round(MathRng.randomDouble(holdTime.getLower(), holdTime.getUpper())));
        Vec3d position = target.getTrackedPosition().getPos();
        renderPosition = position;
        previousRenderPosition = position;
        serverTargetPosition = position;
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private synchronized void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.isConnectedToLocalServer(client)) return;
        Packet<?> packet = event.getPacket();
        if (packet == null) return;
        for (Packet<?> child : unpackBundle(packet)) {
            if (endsTracking(child) || (trackedTarget != null
                    && (!isTrackedTargetValid() || removesTrackedTarget(child)))) {
                clearTrackedTarget();
                return;
            }
            if (requiresImmediateReplay(child)) {
                replayThrough(Long.MAX_VALUE);
                return;
            }
        }
        if (trackedTarget == null) return;
        for (Packet<?> child : unpackBundle(packet)) updateServerPosition(child);
        if (isServerPositionFarther()) {
            delayedPackets.add(new DelayedPacket(packet, trackingTimer.currentMillis()));
            event.cancel();
        } else if (!delayedPackets.isEmpty()) {
            clearTrackedTarget();
        }
    }

    private static Iterable<? extends Packet<?>> unpackBundle(Packet<?> packet) {
        return packet instanceof BundleS2CPacket bundle ? bundle.getPackets() : List.of(packet);
    }

    private boolean removesTrackedTarget(Packet<?> packet) {
        return packet instanceof EntitiesDestroyS2CPacket removed
                && removed.getEntityIds().contains(trackedTarget.getId());
    }

    @Subscribe
    private synchronized void onRenderTick(RenderTickEvent event) {
        if (trackedTarget == null) return;
        if (!isTrackedTargetValid()) {
            clearTrackedTarget();
            return;
        }
        updateRenderPosition();
        if (!delayedPackets.isEmpty()) replayThrough(trackingTimer.currentMillis() - holdMillis);
    }

    @Subscribe
    private synchronized void onWorldRender(WorldRenderEvent event) {
        boolean fill = fillColor.getAlpha() > 0;
        boolean outline = outlineColor.getAlpha() > 0;
        if (trackedTarget == null || delayedPackets.isEmpty() || (!fill && !outline)) return;
        Vec3d position = previousRenderPosition.lerp(renderPosition, (double) event.getTickProgress());
        Box box = trackedTarget.getBoundingBox().offset(position.subtract(trackedTarget.getEntityPos()));
        OverlayWorldGeometry geometry = event.getRenderContext().getOverlayGeometry();
        if (fill) geometry.drawBox(box.minX, box.minY, box.minZ, box.getLengthX(), box.getLengthY(), box.getLengthZ(), fillColor.getArgb());
        if (outline) drawTargetOutline(geometry, box, outlineColor.getArgb());
    }

    private boolean isTrackedTargetValid() {
        return trackedTarget != null && trackedTarget.isAlive() && !trackedTarget.isRemoved()
                && itemWhitelist.acceptsHeldItem() && hotbarWhitelist.permitsCurrentSlot();
    }

    private boolean endsTracking(Packet<?> packet) {
        return packet instanceof DisconnectS2CPacket || packet instanceof PlayerRespawnS2CPacket
                || (packet instanceof HealthUpdateS2CPacket health && health.getHealth() <= 0.0f);
    }

    private boolean requiresImmediateReplay(Packet<?> packet) {
        if (packet instanceof PlayerPositionLookS2CPacket) return true;
        if (client.player == null) return false;
        if (packet instanceof EntityDamageS2CPacket damage && damage.entityId() == client.player.getId()) return true;
        return packet instanceof EntityVelocityUpdateS2CPacket velocity
                && VelocityPackets.entityId(velocity) == client.player.getId();
    }

    private void updateServerPosition(Packet<?> packet) {
        if (packet instanceof EntityS2CPacket relative && relative.isPositionChanged() && client.world != null) {
            Entity entity = relative.getEntity(client.world);
            if (entity != null && entity.getId() == trackedTarget.getId()) {
                serverTargetPosition = new Vec3d(
                        Packets.applyRelativeDelta(serverTargetPosition.x, relative.getDeltaX()), Packets.applyRelativeDelta(serverTargetPosition.y, relative.getDeltaY()), Packets.applyRelativeDelta(serverTargetPosition.z, relative.getDeltaZ()));
            }
        } else if (packet instanceof EntityPositionSyncS2CPacket position && position.id() == trackedTarget.getId()) {
            serverTargetPosition = position.values().position();
        } else if (packet instanceof EntityPositionS2CPacket teleport && teleport.entityId() == trackedTarget.getId()) {
            Vec3d position = teleport.change().position();
            serverTargetPosition = new Vec3d(
                    teleport.relatives().contains(PositionFlag.X) ? serverTargetPosition.x + position.x : position.x, teleport.relatives().contains(PositionFlag.Y) ? serverTargetPosition.y + position.y : position.y, teleport.relatives().contains(PositionFlag.Z) ? serverTargetPosition.z + position.z : position.z);
        }
    }

    private boolean isServerPositionFarther() {
        if (client.player == null) return false;
        Vec3d eye = client.player.getEyePos();
        Vec3d current = trackedTarget.getTrackedPosition().getPos();
        return squaredDistanceToTargetAt(eye, serverTargetPosition) > squaredDistanceToTargetAt(eye, current);
    }

    private double squaredDistanceToTargetAt(Vec3d observer, Vec3d targetPosition) {
        return trackedTarget.getBoundingBox().offset(targetPosition.subtract(trackedTarget.getEntityPos()))
                .squaredMagnitude(observer);
    }

    private void updateRenderPosition() {
        if (renderPosition.squaredDistanceTo(serverTargetPosition) > 4096.0) renderPosition = serverTargetPosition;
        previousRenderPosition = renderPosition;
        renderPosition = renderPosition.lerp(serverTargetPosition, 0.5);
    }

    private void replayThrough(long cutoffMillis) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        while (true) {
            DelayedPacket delayed = delayedPackets.peek();
            if (delayed == null || delayed.queuedAtMillis() > cutoffMillis) return;
            delayedPackets.poll();
            if (handler != null) {
                Packet<?> packet = delayed.packet();
                initializeResolvedClass(ClientPlayNetworkHandler.class);
                applyIgnoringFailure(handler, packet);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyIgnoringFailure(ClientPlayNetworkHandler handler, Packet<?> packet) {
        try {
            ((Packet) packet).apply(handler);
        } catch (Throwable ignored) {
        }
    }

    private synchronized void clearTrackedTarget() {
        replayThrough(Long.MAX_VALUE);
        initializeResolvedClass(LivingEntity.class);
        trackedTarget = null;
        initializeResolvedClass(Vec3d.class);
        renderPosition = null;
        initializeResolvedClass(Vec3d.class);
        previousRenderPosition = null;
        initializeResolvedClass(Vec3d.class);
        serverTargetPosition = null;
    }

    private static void initializeResolvedClass(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    private synchronized void resetTracking(boolean flushPending) {
        if (!flushPending) delayedPackets.clear();
        clearTrackedTarget();
    }

    
    private static void drawTargetOutline(OverlayWorldGeometry geometry, Box box, int color) {
        double x = box.minX, y = box.minY, z = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;
        geometry.drawLine(x, y, z, maxX, y, z, color);
        geometry.drawLine(maxX, y, z, maxX, y, maxZ, color);
        geometry.drawLine(maxX, y, maxZ, x, y, maxZ, color);
        geometry.drawLine(x, y, maxZ, x, y, z, color);
        geometry.drawLine(x, maxY, z, maxX, maxY, z, color);
        geometry.drawLine(maxX, maxY, z, maxX, maxY, maxZ, color);
        geometry.drawLine(maxX, maxY, maxZ, x, maxY, maxZ, color);
        geometry.drawLine(x, maxY, maxZ, x, maxY, z, color);
        geometry.drawLine(x, y, z, x, maxY, z, color);
        geometry.drawLine(maxX, y, z, maxX, maxY, z, color);
        geometry.drawLine(maxX, y, maxZ, maxX, maxY, maxZ, color);
        geometry.drawLine(x, y, maxZ, x, maxY, maxZ, color);
    }

    
    private record DelayedPacket(Packet<?> packet, long queuedAtMillis) {}

    public static void appendBootstrapSettings(Backtrack Module) {
        CatalogSettings.append(Module, holdTime);
        CatalogSettings.append(Module, maximumHurtTime);
        CatalogSettings.append(Module, itemWhitelist);
        CatalogSettings.append(Module, hotbarWhitelist);
        CatalogSettings.append(Module, fillColor);
        CatalogSettings.append(Module, outlineColor);
    }

    public static void assignBootstrapSettings(Backtrack Module, HotbarSlotsSetting preparedHotbarWhitelist, ColorSetting preparedOutlineColor, IntSetting preparedMaximumHurtTime, FloatRangeSetting preparedHoldTime, ItemListSetting preparedItemWhitelist, ColorSetting preparedFillColor) {
        hotbarWhitelist = preparedHotbarWhitelist;
        outlineColor = preparedOutlineColor;
        maximumHurtTime = preparedMaximumHurtTime;
        holdTime = preparedHoldTime;
        itemWhitelist = preparedItemWhitelist;
        fillColor = preparedFillColor;
    }
}
