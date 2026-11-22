package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.event.lifecycle.WorldTickContextEvent;
import dev.dov.hack.echo.client.event.render.WorldRenderEvent;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.world.WorldGeometry;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;

public class JumpCircles extends Module {
    private static final int SEGMENTS = 48;

    public static BooleanSetting onJump;
    public static BooleanSetting onLand;
    public static BooleanSetting otherPlayers;
    public static FloatSetting maxRadius;
    public static FloatSetting duration;
    public static ColorSetting color;

    private record Ring(double x, double y, double z, long createdMs) {}

    private final List<Ring> rings = new ArrayList<>();
    private final Map<Integer, Boolean> wasOnGround = new HashMap<>();

    public JumpCircles() {
        super(new ModuleInfo(
                CatalogText.literal("Jump Circles"),
                CatalogText.literal("Expanding glowing rings on the ground when jumping or landing"),
                ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        onJump.description(CatalogText.literal("Spawn a ring when jumping off the ground"));
        onLand.description(CatalogText.literal("Spawn a ring when landing back on the ground"));
        otherPlayers.description(CatalogText.literal("Also render jump and land rings for nearby players"));
        maxRadius.tooltip("Animation");
        duration.tooltip("Animation");
        color.tooltip("Appearance");
    }

    @Override
    public void onDisable() {
        rings.clear();
        wasOnGround.clear();
        super.onDisable();
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        rings.clear();
        wasOnGround.clear();
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player != client.player && !otherPlayers.getValue()) {
                continue;
            }
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            int id = player.getId();
            boolean ground = player.isOnGround();
            Boolean prev = wasOnGround.put(id, ground);
            if (prev == null) {
                continue;
            }
            if (prev && !ground && onJump.getValue() && player.getVelocity().y > 0.08) {
                addRing(player.getX(), player.getY() + 0.02, player.getZ(), now);
            } else if (!prev && ground && onLand.getValue()) {
                addRing(player.getX(), player.getY() + 0.02, player.getZ(), now);
            }
        }
        wasOnGround.keySet().removeIf(id -> client.world.getEntityById(id) == null);
    }

    private void addRing(double x, double y, double z, long now) {
        if (rings.size() > 40) {
            rings.remove(0);
        }
        rings.add(new Ring(x, y, z, now));
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld() || rings.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        long maxMs = Math.max(150L, Math.round(duration.getValue() * 1000.0F));
        double maxR = maxRadius.getValue();
        int baseColor = color.getArgb();
        WorldGeometry geometry = event.getRenderContext().getDepthTestedGeometry();

        Iterator<Ring> it = rings.iterator();
        while (it.hasNext()) {
            Ring ring = it.next();
            long elapsed = now - ring.createdMs();
            if (elapsed >= maxMs) {
                it.remove();
                continue;
            }
            float t = (float) elapsed / (float) maxMs;
            float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            float alpha = (1.0F - t) * (1.0F - t);
            double outerR = 0.2 + eased * maxR;
            double innerR = Math.max(0.05, outerR - (0.18 + 0.14 * (1.0F - t)));

            int outerColor = scaleAlpha(baseColor, alpha * 0.85F);
            int innerColor = scaleAlpha(baseColor, 0.0F);
            int lineColor = scaleAlpha(baseColor, alpha);

            double prevCos = 1.0;
            double prevSin = 0.0;
            for (int i = 1; i <= SEGMENTS; i++) {
                double angle = (Math.PI * 2.0 * i) / SEGMENTS;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);

                double ox0 = ring.x() + prevCos * outerR;
                double oz0 = ring.z() + prevSin * outerR;
                double ox1 = ring.x() + cos * outerR;
                double oz1 = ring.z() + sin * outerR;

                double ix0 = ring.x() + prevCos * innerR;
                double iz0 = ring.z() + prevSin * innerR;
                double ix1 = ring.x() + cos * innerR;
                double iz1 = ring.z() + sin * innerR;

                geometry.drawTriangle(
                        ox0, ring.y(), oz0, outerColor,
                        ox1, ring.y(), oz1, outerColor,
                        ix1, ring.y(), iz1, innerColor);
                geometry.drawTriangle(
                        ox0, ring.y(), oz0, outerColor,
                        ix1, ring.y(), iz1, innerColor,
                        ix0, ring.y(), iz0, innerColor);
                geometry.drawLine(ox0, ring.y() + 0.005, oz0, ox1, ring.y() + 0.005, oz1, lineColor);

                prevCos = cos;
                prevSin = sin;
            }
        }
    }

    private static int scaleAlpha(int argb, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * factor)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static void appendBootstrapSettings(JumpCircles module) {
        CatalogSettings.append(module, onJump);
        CatalogSettings.append(module, onLand);
        CatalogSettings.append(module, otherPlayers);
        CatalogSettings.append(module, maxRadius);
        CatalogSettings.append(module, duration);
        CatalogSettings.append(module, color);
    }

    public static void assignBootstrapSettings(
            JumpCircles module,
            BooleanSetting preparedOnJump,
            BooleanSetting preparedOnLand,
            BooleanSetting preparedOtherPlayers,
            FloatSetting preparedMaxRadius,
            FloatSetting preparedDuration,
            ColorSetting preparedColor) {
        onJump = preparedOnJump;
        onLand = preparedOnLand;
        otherPlayers = preparedOtherPlayers;
        maxRadius = preparedMaxRadius;
        duration = preparedDuration;
        color = preparedColor;
    }
}
