package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.world.WorldGeometry;
import com.blamejared.clumps.client.render.world.WorldProjection;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ColorSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Box;

public class PopChams extends Module {
    private static final byte STATUS_DEATH = 3;
    private static final byte STATUS_TOTEM_POP = 35;

    public static BooleanSetting onTotemPop;
    public static BooleanSetting onHit;
    public static BooleanSetting onDeath;
    public static BooleanSetting throughWalls;
    public static FloatSetting duration;
    public static FloatSetting riseHeight;
    public static ColorSetting fillColor;
    public static ColorSetting outlineColor;

    private record Ghost(Box box, long createdMs, boolean isPop) {}

    private final List<Ghost> ghosts = new ArrayList<>();

    public PopChams() {
        super(new ModuleInfo(
                CatalogText.literal("Pop Chams"),
                CatalogText.literal("Leaves a fading ghost silhouette on totem pops, hits, and deaths"),
                ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        onTotemPop.description(CatalogText.literal("Spawn a fading ghost where an enemy pops a totem"));
        onHit.description(CatalogText.literal("Spawn a subtle fading ghost whenever you hit a player"));
        onDeath.description(CatalogText.literal("Spawn a fading ghost where a player dies"));
        duration.tooltip("Animation");
        riseHeight.tooltip("Animation");
        fillColor.tooltip("Appearance");
        outlineColor.tooltip("Appearance");
        throughWalls.tooltip("Appearance");
    }

    @Override
    public void onDisable() {
        ghosts.clear();
        super.onDisable();
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        ghosts.clear();
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld() || !(event.getPacket() instanceof EntityStatusS2CPacket packet)) {
            return;
        }
        byte status = packet.getStatus();
        boolean pop = status == STATUS_TOTEM_POP && onTotemPop.getValue();
        boolean death = status == STATUS_DEATH && onDeath.getValue();
        if (!pop && !death) {
            return;
        }
        Entity entity = packet.getEntity(client.world);
        if (entity instanceof PlayerEntity player && player != client.player) {
            spawnGhost(player, pop);
        }
    }

    @Subscribe
    private void onAttackPost(EntityAttackEvent.Post event) {
        if (isOutsideWorld() || !onHit.getValue() || event.getPlayer() != client.player) {
            return;
        }
        if (event.getTarget() instanceof PlayerEntity target && target != client.player) {
            spawnGhost(target, false);
        }
    }

    private void spawnGhost(LivingEntity entity, boolean isPop) {
        Box box = entity.getBoundingBox();
        if (box == null) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (ghosts) {
            if (ghosts.size() > 32) {
                ghosts.remove(0);
            }
            ghosts.add(new Ghost(box, now, isPop));
        }
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        long now = System.currentTimeMillis();
        long maxMs = Math.max(100L, Math.round(duration.getValue() * 1000.0F));
        double rise = riseHeight.getValue();
        WorldGeometry geometry = throughWalls.getValue()
                ? event.getRenderContext().getOverlayGeometry()
                : event.getRenderContext().getDepthTestedGeometry();

        int baseFill = fillColor.getArgb();
        int baseOutline = outlineColor.getArgb();

        synchronized (ghosts) {
            Iterator<Ghost> it = ghosts.iterator();
            while (it.hasNext()) {
                Ghost ghost = it.next();
                long elapsed = now - ghost.createdMs();
                if (elapsed >= maxMs) {
                    it.remove();
                    continue;
                }
                float t = (float) elapsed / (float) maxMs;
                float eased = 1.0F - (1.0F - t) * (1.0F - t);
                float alpha = (1.0F - t) * (1.0F - t);
                double yOffset = eased * rise;
                double expand = ghost.isPop() ? eased * 0.08 : eased * 0.03;
                Box box = ghost.box().offset(0.0, yOffset, 0.0).expand(expand, 0.0, expand);

                int fill = scaleAlpha(baseFill, alpha);
                int outline = scaleAlpha(baseOutline, alpha);

                // Draw silhouette layers: legs, torso, and head proportions for a clean humanoid ghost
                double h = box.getLengthY();
                double w = box.getLengthX();
                double d = box.getLengthZ();
                double cx = (box.minX + box.maxX) * 0.5;
                double cz = (box.minZ + box.maxZ) * 0.5;

                // Main body core
                geometry.drawBox(box.minX, box.minY, box.minZ, w, h, d, fill);
                WorldProjection.drawBoxOutline(geometry, box, outline);

                // Head highlight box
                double headSize = Math.min(w, d) * 0.72;
                double headMinY = box.maxY - Math.min(0.45, h * 0.25);
                Box headBox = new Box(
                        cx - headSize * 0.5, headMinY, cz - headSize * 0.5,
                        cx + headSize * 0.5, box.maxY, cz + headSize * 0.5);
                geometry.drawBox(headBox.minX, headBox.minY, headBox.minZ,
                        headBox.getLengthX(), headBox.getLengthY(), headBox.getLengthZ(),
                        scaleAlpha(baseFill, alpha * 0.65F));
                WorldProjection.drawBoxOutline(geometry, headBox, scaleAlpha(baseOutline, alpha * 0.85F));
            }
        }
    }

    private static int scaleAlpha(int argb, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static void appendBootstrapSettings(PopChams module) {
        CatalogSettings.append(module, onTotemPop);
        CatalogSettings.append(module, onHit);
        CatalogSettings.append(module, onDeath);
        CatalogSettings.append(module, throughWalls);
        CatalogSettings.append(module, duration);
        CatalogSettings.append(module, riseHeight);
        CatalogSettings.append(module, fillColor);
        CatalogSettings.append(module, outlineColor);
    }

    public static void assignBootstrapSettings(
            PopChams module,
            BooleanSetting preparedOnTotemPop,
            BooleanSetting preparedOnHit,
            BooleanSetting preparedOnDeath,
            BooleanSetting preparedThroughWalls,
            FloatSetting preparedDuration,
            FloatSetting preparedRiseHeight,
            ColorSetting preparedFillColor,
            ColorSetting preparedOutlineColor) {
        onTotemPop = preparedOnTotemPop;
        onHit = preparedOnHit;
        onDeath = preparedOnDeath;
        throughWalls = preparedThroughWalls;
        duration = preparedDuration;
        riseHeight = preparedRiseHeight;
        fillColor = preparedFillColor;
        outlineColor = preparedOutlineColor;
    }
}
