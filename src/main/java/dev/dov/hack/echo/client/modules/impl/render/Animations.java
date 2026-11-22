package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.interaction.BlockInteractEvent;
import dev.dov.hack.echo.client.event.interaction.EntityAttackEvent;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class Animations extends Module {
    private static final CharSequence OPT_NONE = CatalogText.literal("None");
    private static final CharSequence ATTACK_SLASH = CatalogText.literal("Crisp Slash");
    private static final CharSequence ATTACK_BLOCKHIT = CatalogText.literal("1.7 BlockHit");
    private static final CharSequence ATTACK_ARC = CatalogText.literal("Smooth Arc");
    private static final CharSequence ATTACK_THRUST = CatalogText.literal("Quick Thrust");
    private static final CharSequence ATTACK_FLICK = CatalogText.literal("Tilt Flick");

    private static final CharSequence USE_SIP = CatalogText.literal("Smooth Sip");
    private static final CharSequence USE_LOW_GUARD = CatalogText.literal("Low Guard");
    private static final CharSequence USE_PULSE = CatalogText.literal("Pulse");

    private static final CharSequence PLACE_POP = CatalogText.literal("Pop");
    private static final CharSequence PLACE_SLIDE = CatalogText.literal("Slide");
    private static final CharSequence PLACE_SNAP = CatalogText.literal("Snap");

    private static final long PLACE_ANIM_DURATION_MS = 180L;
    private static final long ATTACK_BURST_MS = 220L;

    public static ChoiceSetting attackStyle;
    public static ChoiceSetting useStyle;
    public static ChoiceSetting placeStyle;
    public static BooleanSetting smoothEquip;
    public static FloatSetting intensity;

    private long lastPlaceMs;
    private Hand lastPlaceHand = Hand.MAIN_HAND;
    private long lastAttackMs;

    public Animations() {
        super(new ModuleInfo(
                CatalogText.literal("Animations"),
                CatalogText.literal("Clean hand animations for attacking, using items, placing blocks, and swapping"),
                ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        attackStyle.description(CatalogText.literal("Hand swing curve when attacking entities"));
        useStyle.description(CatalogText.literal("Clean item-use posture when eating, drinking, or blocking"));
        placeStyle.description(CatalogText.literal("Tactile wrist animation when placing blocks, crystals, or webs"));
        smoothEquip.description(CatalogText.literal("Reduces the vertical equip dip when rapidly hotbar swapping"));
        intensity.description(CatalogText.literal("Overall strength multiplier for attack, use, and place animations"));

        attackStyle.tooltip("Styles");
        useStyle.tooltip("Styles");
        placeStyle.tooltip("Styles");
        smoothEquip.tooltip("Tuning");
        intensity.tooltip("Tuning");
    }

    @Override
    public CharSequence getSuffix() {
        return attackStyle.isSelected(OPT_NONE) ? placeStyle.getValue() : attackStyle.getValue();
    }

    @Subscribe
    private void onBlockPlace(BlockInteractEvent.Post event) {
        if (isOutsideWorld() || event.getPlayer() != client.player) {
            return;
        }
        lastPlaceMs = System.currentTimeMillis();
        lastPlaceHand = event.getHand() == null ? Hand.MAIN_HAND : event.getHand();
    }

    @Subscribe
    private void onAttackPost(EntityAttackEvent.Post event) {
        if (isOutsideWorld() || event.getPlayer() != client.player) {
            return;
        }
        lastAttackMs = System.currentTimeMillis();
    }

    public boolean overridesSwing() {
        return isEnabled() && attackStyle != null && !attackStyle.isSelected(OPT_NONE);
    }

    public void applySwing(float swingProgress, MatrixStack matrices, int armSign, Arm arm) {
        float k = MathHelper.clamp(intensity.getValue(), 0.2F, 2.0F);
        float p = MathHelper.clamp(swingProgress, 0.0F, 1.0F);
        float s = MathHelper.sin(p * (float) Math.PI);
        float sq = MathHelper.sin(MathHelper.sqrt(p) * (float) Math.PI);

        if (attackStyle.isSelected(ATTACK_SLASH)) {
            matrices.translate(armSign * (-0.28F * sq) * k, (0.08F * s) * k, (-0.22F * sq) * k);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * (32.0F * sq) * k));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-68.0F * sq) * k));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * (-18.0F * s) * k));
        } else if (attackStyle.isSelected(ATTACK_BLOCKHIT)) {
            matrices.translate(armSign * (-0.16F * sq) * k, (0.04F * s) * k, (-0.12F * sq) * k);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * (45.0F - 18.0F * sq) * k));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-38.0F * sq - 12.0F) * k));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * (-22.0F * sq) * k));
        } else if (attackStyle.isSelected(ATTACK_ARC)) {
            float ease = (float) Math.sin(Math.pow(p, 0.72) * Math.PI);
            matrices.translate(armSign * (-0.34F * ease) * k, (0.14F * s - 0.06F * ease) * k, (-0.18F * ease) * k);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * (42.0F * ease) * k));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-52.0F * ease) * k));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * (14.0F * s) * k));
        } else if (attackStyle.isSelected(ATTACK_THRUST)) {
            matrices.translate(armSign * (-0.10F * sq) * k, (0.02F * s) * k, (-0.42F * sq) * k);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-28.0F * sq) * k));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * (12.0F * sq) * k));
        } else if (attackStyle.isSelected(ATTACK_FLICK)) {
            matrices.translate(armSign * (-0.18F * sq) * k, (-0.04F * s) * k, (-0.14F * sq) * k);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-48.0F * sq) * k));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * (-14.0F * sq) * k));
        }
    }

    public void applyItemOverlayTransform(
            PlayerEntity player, Hand hand, float equipProgress, float swingProgress, MatrixStack matrices) {
        if (!isEnabled()) {
            return;
        }
        float k = MathHelper.clamp(intensity.getValue(), 0.2F, 2.0F);
        int armSign = hand == Hand.MAIN_HAND ? 1 : -1;

        // Smooth equip dip compensation
        if (smoothEquip.getValue() && equipProgress > 0.001F) {
            matrices.translate(0.0F, equipProgress * 0.35F, 0.0F);
        }

        // Tactile block-place animation
        if (!placeStyle.isSelected(OPT_NONE) && hand == lastPlaceHand) {
            long elapsed = System.currentTimeMillis() - lastPlaceMs;
            if (elapsed >= 0L && elapsed < PLACE_ANIM_DURATION_MS) {
                float t = (float) elapsed / (float) PLACE_ANIM_DURATION_MS;
                float pulse = MathHelper.sin(t * (float) Math.PI);
                float easeOut = 1.0F - (1.0F - t) * (1.0F - t);
                if (placeStyle.isSelected(PLACE_POP)) {
                    float scale = 1.0F + 0.09F * pulse * k;
                    matrices.translate(0.0F, 0.04F * pulse * k, -0.10F * pulse * k);
                    matrices.scale(scale, scale, scale);
                } else if (placeStyle.isSelected(PLACE_SLIDE)) {
                    matrices.translate(armSign * (-0.07F * pulse) * k, -0.04F * pulse * k, -0.12F * pulse * k);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-24.0F * pulse * k));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * 10.0F * (1.0F - easeOut) * k));
                } else if (placeStyle.isSelected(PLACE_SNAP)) {
                    float sharp = (1.0F - t) * (1.0F - t);
                    matrices.translate(0.0F, -0.03F * sharp * k, -0.15F * sharp * k);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-18.0F * sharp * k));
                }
            }
        }

        // Subtle attack follow-through impulse
        if (hand == Hand.MAIN_HAND && overridesSwing()) {
            long atkElapsed = System.currentTimeMillis() - lastAttackMs;
            if (atkElapsed >= 0L && atkElapsed < ATTACK_BURST_MS && swingProgress > 0.0F) {
                float pulse = MathHelper.sin(((float) atkElapsed / (float) ATTACK_BURST_MS) * (float) Math.PI);
                float scale = 1.0F + 0.035F * pulse * k;
                matrices.scale(scale, scale, scale);
            }
        }

        // Active item-use styling (eating, drinking, shield, bow)
        if (!useStyle.isSelected(OPT_NONE) && player != null && player.isUsingItem() && player.getActiveHand() == hand) {
            ItemStack active = player.getActiveItem();
            int maxUse = Math.max(1, active.getMaxUseTime(player));
            float usedTicks = Math.max(0.0F, maxUse - player.getItemUseTimeLeft());
            float wave = MathHelper.sin(usedTicks * 0.45F);
            if (useStyle.isSelected(USE_SIP)) {
                matrices.translate(armSign * -0.06F * k, -0.05F * k + 0.02F * wave * k, -0.05F * k);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(8.0F * k + 4.0F * wave * k));
            } else if (useStyle.isSelected(USE_LOW_GUARD)) {
                matrices.translate(armSign * -0.04F * k, -0.14F * k, -0.06F * k);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * k));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * 14.0F * k));
            } else if (useStyle.isSelected(USE_PULSE)) {
                float scale = 1.0F + 0.04F * wave * k;
                matrices.translate(0.0F, -0.04F * k, -0.04F * k);
                matrices.scale(scale, scale, scale);
            }
        }
    }

    public static CharSequence bootstrapNone() { return OPT_NONE; }
    public static CharSequence bootstrapAttackSlash() { return ATTACK_SLASH; }
    public static CharSequence bootstrapAttackBlockHit() { return ATTACK_BLOCKHIT; }
    public static CharSequence bootstrapAttackArc() { return ATTACK_ARC; }
    public static CharSequence bootstrapAttackThrust() { return ATTACK_THRUST; }
    public static CharSequence bootstrapAttackFlick() { return ATTACK_FLICK; }
    public static CharSequence bootstrapUseSip() { return USE_SIP; }
    public static CharSequence bootstrapUseLowGuard() { return USE_LOW_GUARD; }
    public static CharSequence bootstrapUsePulse() { return USE_PULSE; }
    public static CharSequence bootstrapPlacePop() { return PLACE_POP; }
    public static CharSequence bootstrapPlaceSlide() { return PLACE_SLIDE; }
    public static CharSequence bootstrapPlaceSnap() { return PLACE_SNAP; }

    public static void appendBootstrapSettings(Animations module) {
        CatalogSettings.append(module, attackStyle);
        CatalogSettings.append(module, useStyle);
        CatalogSettings.append(module, placeStyle);
        CatalogSettings.append(module, smoothEquip);
        CatalogSettings.append(module, intensity);
    }

    public static void assignBootstrapSettings(
            Animations module,
            ChoiceSetting preparedAttackStyle,
            ChoiceSetting preparedUseStyle,
            ChoiceSetting preparedPlaceStyle,
            BooleanSetting preparedSmoothEquip,
            FloatSetting preparedIntensity) {
        attackStyle = preparedAttackStyle;
        useStyle = preparedUseStyle;
        placeStyle = preparedPlaceStyle;
        smoothEquip = preparedSmoothEquip;
        intensity = preparedIntensity;
    }
}
