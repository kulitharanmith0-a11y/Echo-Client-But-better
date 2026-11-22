package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.PlayerInputEvent;
import dev.dov.hack.echo.client.event.network.PacketReceiveEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.mixin.accessor.LivingEntityAccessor;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.MultiChoiceSetting;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;

public class JumpReset extends Module {
    private static final CharSequence PLAYER_SOURCE = CatalogText.literal("Player");
    private static final CharSequence MOB_SOURCE = CatalogText.literal("Mob");
    private static final CharSequence PROJECTILE_SOURCE = CatalogText.literal("Projectile");
    private static final CharSequence EXPLOSION_SOURCE = CatalogText.literal("Explosion");
    private static final CharSequence FALL_SOURCE = CatalogText.literal("Fall");
    private static final CharSequence FIRE_SOURCE = CatalogText.literal("Fire");
    private static final CharSequence MAGIC_SOURCE = CatalogText.literal("Magic");
    private static final CharSequence OTHER_SOURCE = CatalogText.literal("Other");
    private static final String WEB_TRANSLATION_FRAGMENT = "web";

    private static FloatSetting chance;
    private static MultiChoiceSetting damageSources;
    private static BooleanSetting liquidCheck;
    private static BooleanSetting webCheck;
    private RegistryEntry<DamageType> lastDamageType;

    public JumpReset() {
        super(new ModuleInfo(CatalogText.literal("Jump Reset"), CatalogText.literal("Automatically jumps on knockback to reduce incoming velocity"), ModuleCategory.MOVEMENT));
    }

    public static void appendBootstrapSettings(JumpReset Module) {
        CatalogSettings.append(Module, chance);
        CatalogSettings.append(Module, damageSources);
        CatalogSettings.append(Module, liquidCheck);
        CatalogSettings.append(Module, webCheck);
    }

    
    public static void assignBootstrapSettings(JumpReset Module, MultiChoiceSetting preparedDamageSources, FloatSetting preparedChance, BooleanSetting preparedWebCheck, BooleanSetting preparedLiquidCheck) {
        damageSources = preparedDamageSources;
        chance = preparedChance;
        webCheck = preparedWebCheck;
        liquidCheck = preparedLiquidCheck;
    }

    @Override
    public void postInitialize() {
        damageSources.setSelected(CatalogText.literal("Player"), true);
        damageSources.setSelected(CatalogText.literal("Mob"), true);
        damageSources.setSelected(CatalogText.literal("Projectile"), false);
        damageSources.setSelected(CatalogText.literal("Explosion"), false);
    }

    @Subscribe
    public void onDamagePacket(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof EntityDamageS2CPacket damage)) return;
        if (client.player == null || damage.entityId() != client.player.getId()) return;
        lastDamageType = damage.sourceType();
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || client.player == null) return;
        if (client.player.hurtTime != ((LivingEntityAccessor) client.player).getMaxHurtTime() - 1) return;
        if (!client.player.isOnGround()) return;
        if (!isDamageSourceAllowed()) return;
        if (!passesJumpChecks()) return;
        event.jump = true;
    }

    private boolean isDamageSourceAllowed() {
        if (lastDamageType == null || !damageSources.getOptions().values().stream().anyMatch(selected -> selected)) {
            return true;
        }
        if (lastDamageType.matchesKey(DamageTypes.PLAYER_ATTACK)) return damageSources.isSelected(PLAYER_SOURCE);
        if (lastDamageType.matchesKey(DamageTypes.MOB_ATTACK) || lastDamageType.matchesKey(DamageTypes.MOB_ATTACK_NO_AGGRO)) {
            return damageSources.isSelected(MOB_SOURCE);
        }
        if (lastDamageType.matchesKey(DamageTypes.ARROW)
                || lastDamageType.matchesKey(DamageTypes.TRIDENT)
                || lastDamageType.matchesKey(DamageTypes.MOB_PROJECTILE)
                || lastDamageType.matchesKey(DamageTypes.FIREBALL)
                || lastDamageType.matchesKey(DamageTypes.WITHER_SKULL)
                || lastDamageType.matchesKey(DamageTypes.WIND_CHARGE)
                || lastDamageType.matchesKey(DamageTypes.THROWN)
                || lastDamageType.matchesKey(DamageTypes.SPIT)
                || lastDamageType.matchesKey(DamageTypes.FIREWORKS)
                || lastDamageType.matchesKey(DamageTypes.UNATTRIBUTED_FIREBALL)
                || lastDamageType.matchesKey(DamageTypes.SPEAR)) {
            return damageSources.isSelected(PROJECTILE_SOURCE);
        }
        if (lastDamageType.matchesKey(DamageTypes.EXPLOSION) || lastDamageType.matchesKey(DamageTypes.PLAYER_EXPLOSION)) {
            return damageSources.isSelected(EXPLOSION_SOURCE);
        }
        if (lastDamageType.matchesKey(DamageTypes.FALL) || lastDamageType.matchesKey(DamageTypes.ENDER_PEARL)) {
            return damageSources.isSelected(FALL_SOURCE);
        }
        if (lastDamageType.matchesKey(DamageTypes.IN_FIRE)
                || lastDamageType.matchesKey(DamageTypes.ON_FIRE)
                || lastDamageType.matchesKey(DamageTypes.LAVA)
                || lastDamageType.matchesKey(DamageTypes.HOT_FLOOR)
                || lastDamageType.matchesKey(DamageTypes.CAMPFIRE)) {
            return damageSources.isSelected(FIRE_SOURCE);
        }
        if (lastDamageType.matchesKey(DamageTypes.MAGIC)
                || lastDamageType.matchesKey(DamageTypes.INDIRECT_MAGIC)
                || lastDamageType.matchesKey(DamageTypes.WITHER)) {
            return damageSources.isSelected(MAGIC_SOURCE);
        }
        return damageSources.isSelected(OTHER_SOURCE);
    }

    private boolean passesJumpChecks() {
        if (!(Math.random() * 100.0 <= chance.getValue())) return false;
        if (liquidCheck.getValue() && (client.player.isTouchingWater() || client.player.isInLava())) return false;
        return !webCheck.getValue() || !client.world.getBlockState(client.player.getBlockPos()).getBlock()
                .getTranslationKey().contains(WEB_TRANSLATION_FRAGMENT);
    }
}
