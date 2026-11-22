package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.network.PacketReceiveEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.MultiChoiceSetting;
import dev.dov.hack.echo.client.utils.interaction.EntityInteractions;
import dev.dov.hack.echo.client.utils.network.VelocityPackets;
import java.util.Optional;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;

public class Velocity extends Module {
    private static final CharSequence CANCEL_MODE = CatalogText.literal("Cancel");
    private static final CharSequence EXPLOSION_SOURCE = CatalogText.literal("Explosions");
    private static final CharSequence HIT_SOURCE = CatalogText.literal("Hits");
    private static final CharSequence MINEMEN_CLUB_MODE = CatalogText.literal("MinemenClub");
    private static ChoiceSetting mode;
    private static FloatSetting amount;
    private static MultiChoiceSetting sources;
    private boolean explosionKnockbackPending;

    public Velocity() {
        super(new ModuleInfo(CatalogText.literal("Velocity"), CatalogText.literal("Modifies knockback received"), ModuleCategory.MOVEMENT, false));
    }

    public static void appendBootstrapSettings(Velocity Module) {
        CatalogSettings.append(Module, mode);
        CatalogSettings.append(Module, amount);
        CatalogSettings.append(Module, sources);
    }

    public static void assignBootstrapSettings(Velocity Module, FloatSetting preparedAmount, ChoiceSetting preparedMode, MultiChoiceSetting preparedSources) {
        amount = preparedAmount;
        mode = preparedMode;
        sources = preparedSources;
    }

    @Override
    public void postInitialize() {
        explosionKnockbackPending = false;
        amount.setVisibilityPredicate(ignored -> mode.isSelected(CANCEL_MODE));
    }

    @Subscribe
    public void onVelocityPacket(PacketReceiveEvent event) {
        if (isOutsideWorld()) return;
        if (!(event.getPacket() instanceof EntityVelocityUpdateS2CPacket velocity)) return;
        if (client.player == null || VelocityPackets.entityId(velocity) != client.player.getId()) return;

        boolean followsExplosion = explosionKnockbackPending && sources.isSelected(EXPLOSION_SOURCE);
        explosionKnockbackPending = false;
        if (followsExplosion && !sources.isSelected(HIT_SOURCE)) {
            if (mode.isSelected(CANCEL_MODE)) event.cancel();
            return;
        }
        if (!sources.isSelected(HIT_SOURCE)) return;
        if (mode.isSelected(CANCEL_MODE)) {
            if (amount.getValue() == 1.0F) return;
            float scale = amount.getValue();
            if (scale == 0.0F) {
                event.cancel();
            } else {
                Vec3d motion = VelocityPackets.velocity(velocity);
                event.cancel();
                client.player.setVelocity(motion.x * scale, motion.y * scale, motion.z * scale);
            }
        } else if (mode.isSelected(MINEMEN_CLUB_MODE) && client.player.hurtTime == 7) {
            EntityInteractions.setVelocityX(client.player.getVelocity().getX() * 0.25);
            EntityInteractions.setVelocityY(-0.05350000038743019);
            EntityInteractions.setVelocityZ(client.player.getVelocity().getZ() * 0.25);
        }
    }

    @Subscribe
    public void onExplosionPacket(PacketReceiveEvent event) {
        if (isOutsideWorld()) return;
        if (!(event.getPacket() instanceof ExplosionS2CPacket explosion)) return;
        if (client.player == null) return;
        if (!sources.isSelected(EXPLOSION_SOURCE)) return;
        if (!mode.isSelected(CANCEL_MODE)) return;
        if (amount.getValue() == 1.0F) return;
        Optional<Vec3d> knockback = explosion.playerKnockback();
        if (knockback.isEmpty()) return;
        float scale = amount.getValue();
        event.cancel();
        if (scale == 0.0F) {
            explosionKnockbackPending = true;
            return;
        }
        Vec3d motion = knockback.get();
        Vec3d scaledMotion = new Vec3d(motion.x * scale, motion.y * scale, motion.z * scale);
        client.player.addVelocityInternal(scaledMotion);
        explosionKnockbackPending = true;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        explosionKnockbackPending = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        explosionKnockbackPending = false;
    }
}
