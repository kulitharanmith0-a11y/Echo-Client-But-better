package com.blamejared.clumps.client.modules.impl.render;

import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.interaction.BlockInteractEvent;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
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
    private static final CharSequence ATTACK_EXHIBITION = CatalogText.literal("Exhibition");
    private static final CharSequence ATTACK_SIGMA = CatalogText.literal("Sigma");
    private static final CharSequence ATTACK_KATANA = CatalogText.literal("Katana");
    private static final CharSequence ATTACK_ARC = CatalogText.literal("Smooth Arc");
    private static final CharSequence ATTACK_THRUST = CatalogText.literal("Quick Thrust");
    private static final CharSequence ATTACK_FLICK = CatalogText.literal("Tilt Flick");
    private static final CharSequence ATTACK_SPIN = CatalogText.literal("Spin");
    private static final CharSequence ATTACK_BREEZE = CatalogText.literal("Breeze");

    private static final CharSequence USE_SIP = CatalogText.literal("Smooth Sip");
    private static final CharSequence USE_LOW_GUARD = CatalogText.literal("Low Guard");
    private static final CharSequence USE_PULSE = CatalogText.literal("Pulse");
    private static final CharSequence USE_CLEAN_TILT = CatalogText.literal("Clean Tilt");
    private static final CharSequence USE_FLOATING = CatalogText.literal("Floating");

    private static final CharSequence PLACE_POP = CatalogText.literal("Pop");
    private static final CharSequence PLACE_SLIDE = CatalogText.literal("Slide");
    private static final CharSequence PLACE_SNAP = CatalogText.literal("Snap");
    private static final CharSequence PLACE_FLIP = CatalogText.literal("Flip");
    private static final CharSequence PLACE_BOUNCE = CatalogText.literal("Bounce");

    private static final CharSequence IDLE_BREATHE = CatalogText.literal("Breathe");
    private static final CharSequence IDLE_FLOAT = CatalogText.literal("Float");
    private static final CharSequence IDLE_SWAY = CatalogText.literal("Sway");

    private static final long PLACE_ANIM_DURATION_MS = 180L;
    private static final long ATTACK_BURST_MS = 220L;

    public static ChoiceSetting attackStyle;
    public static ChoiceSetting useStyle;
    public static ChoiceSetting placeStyle;
    public static ChoiceSetting idleStyle;
    public static BooleanSetting smoothEquip;
    public static FloatSetting intensity;

    private long lastPlaceMs;
    private Hand lastPlaceHand = Hand.MAIN_HAND;
    private long lastAttackMs;

    public Animations() {
        super(new ModuleInfo(
                CatalogText.literal("Animations"),
                CatalogText.literal("Clean hand animations for attacking, using items, placing blocks, idle sway, and swapping"),
                ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        attackStyle.description(CatalogText.literal("Hand swing curve when attacking entities"));
        useStyle.description(CatalogText.literal("Clean item-use posture when eating, drinking, or blocking"));
        placeStyle.description(CatalogText.literal("Tactile wrist animation when placing blocks, crystals, or webs"));
        idleStyle.description(CatalogText.literal("Subtle ambient breathing or floating motion while holding items"));
        smoothEquip.description(CatalogText.literal("Reduces the vertical equip dip when rapidly hotbar swapping"));
        intensity.description(CatalogText.literal("Overall strength multiplier for attack, use, place, and idle animations"));

        attackStyle.tooltip("Styles");
        useStyle.tooltip("Styles");
        placeStyle.tooltip("Styles");
        idleStyle.tooltip("Styles");
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
        } else if (attackStyle.isSelected(ATTACK_EXHIBITION)) {
            float chop = MathHelper.sin(MathHelper.sqrt(p) * (float) Math.PI);
            matrices.translate(armSign * (-0.18F * chop) * k, (-0.06F * s) * k, (-0.16F * chop) * k);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * (44.0F * chop) * k));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-54.0F * chop) * k));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * (-28.0F * chop) * k));
        } else if (attackStyle.isSelected(ATTACK_SIGMA)) {
            float twist = MathHelper.sin(p * p * (float) Math.PI);
            matrices.translate(armSign * (-0.22F * sq) * k, (0.06F * twist) * k, (-0.18F * sq) * k);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * (38.0F * sq - 12.0F * twist) * k));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-46.0F * sq) * k));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * (-30.0F * twist) * k));
        } else if (attackStyle.isSelected(ATTACK_KATANA)) {
            float whip = (float) Math.sin(Math.pow(p, 0.58) * Math.PI);
            matrices.translate(armSign * (-0.42F * whip) * k, (-0.02F * s) * k, (-0.24F * whip) * k);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * (68.0F * whip) * k));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-26.0F * whip) * k));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * (-42.0F * whip) * k));
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
        } else if (attackStyle.isSelected(ATTACK_SPIN)) {
            matrices.translate(armSign * (-0.16F * s) * k, (0.05F * s) * k, (-0.20F * s) * k);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-360.0F * p) * Math.min(k, 1.25F)));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * (18.0F * s) * k));
        } else if (attackStyle.isSelected(ATTACK_BREEZE)) {
            float glide = MathHelper.sin(p * (float) Math.PI);
            matrices.translate(armSign * (-0.24F * glide) * k, (0.10F * glide - 0.04F * sq) * k, (-0.16F * glide) * k);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * (24.0F * glide) * k));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((-34.0F * glide) * k));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * (12.0F * glide) * k));
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

        // Subtle idle ambient motion
        if (idleStyle != null && !idleStyle.isSelected(OPT_NONE) && swingProgress <= 0.001F
                && (player == null || !player.isUsingItem())) {
            double sec = System.currentTimeMillis() * 0.001;
            if (idleStyle.isSelected(IDLE_BREATHE)) {
                float b = (float) Math.sin(sec * 2.2 + (hand == Hand.OFF_HAND ? 1.1 : 0.0));
                matrices.translate(0.0F, 0.014F * b * k, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(1.8F * b * k));
            } else if (idleStyle.isSelected(IDLE_FLOAT)) {
                float f1 = (float) Math.sin(sec * 1.9 + (hand == Hand.OFF_HAND ? 1.5 : 0.0));
                float f2 = (float) Math.cos(sec * 1.3);
                matrices.translate(armSign * 0.012F * f2 * k, 0.022F * f1 * k, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * 2.6F * f2 * k));
            } else if (idleStyle.isSelected(IDLE_SWAY)) {
                float sw = (float) Math.sin(sec * 2.6);
                matrices.translate(armSign * 0.018F * sw * k, -Math.abs(sw) * 0.010F * k, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * 3.5F * sw * k));
            }
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
                } else if (placeStyle.isSelected(PLACE_FLIP)) {
                    matrices.translate(0.0F, 0.03F * pulse * k, -0.12F * pulse * k);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-42.0F * pulse * k));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * 22.0F * pulse * k));
                } else if (placeStyle.isSelected(PLACE_BOUNCE)) {
                    float bounce = MathHelper.sin(t * (float) Math.PI * 1.5F) * (1.0F - t);
                    matrices.translate(0.0F, 0.06F * bounce * k, -0.08F * pulse * k);
                    float scale = 1.0F + 0.07F * bounce * k;
                    matrices.scale(scale, scale, scale);
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
            } else if (useStyle.isSelected(USE_CLEAN_TILT)) {
                matrices.translate(armSign * -0.08F * k, -0.08F * k, -0.06F * k);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(armSign * 22.0F * k));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armSign * -12.0F * k));
            } else if (useStyle.isSelected(USE_FLOATING)) {
                float smooth = MathHelper.sin(usedTicks * 0.25F);
                matrices.translate(armSign * -0.03F * k, -0.06F * k + 0.03F * smooth * k, -0.04F * k);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F * smooth * k));
            }
        }
    }

    public static CharSequence bootstrapNone() { return OPT_NONE; }
    public static CharSequence bootstrapAttackSlash() { return ATTACK_SLASH; }
    public static CharSequence bootstrapAttackBlockHit() { return ATTACK_BLOCKHIT; }
    public static CharSequence bootstrapAttackExhibition() { return ATTACK_EXHIBITION; }
    public static CharSequence bootstrapAttackSigma() { return ATTACK_SIGMA; }
    public static CharSequence bootstrapAttackKatana() { return ATTACK_KATANA; }
    public static CharSequence bootstrapAttackArc() { return ATTACK_ARC; }
    public static CharSequence bootstrapAttackThrust() { return ATTACK_THRUST; }
    public static CharSequence bootstrapAttackFlick() { return ATTACK_FLICK; }
    public static CharSequence bootstrapAttackSpin() { return ATTACK_SPIN; }
    public static CharSequence bootstrapAttackBreeze() { return ATTACK_BREEZE; }
    public static CharSequence bootstrapUseSip() { return USE_SIP; }
    public static CharSequence bootstrapUseLowGuard() { return USE_LOW_GUARD; }
    public static CharSequence bootstrapUsePulse() { return USE_PULSE; }
    public static CharSequence bootstrapUseCleanTilt() { return USE_CLEAN_TILT; }
    public static CharSequence bootstrapUseFloating() { return USE_FLOATING; }
    public static CharSequence bootstrapPlacePop() { return PLACE_POP; }
    public static CharSequence bootstrapPlaceSlide() { return PLACE_SLIDE; }
    public static CharSequence bootstrapPlaceSnap() { return PLACE_SNAP; }
    public static CharSequence bootstrapPlaceFlip() { return PLACE_FLIP; }
    public static CharSequence bootstrapPlaceBounce() { return PLACE_BOUNCE; }
    public static CharSequence bootstrapIdleBreathe() { return IDLE_BREATHE; }
    public static CharSequence bootstrapIdleFloat() { return IDLE_FLOAT; }
    public static CharSequence bootstrapIdleSway() { return IDLE_SWAY; }

    public static void appendBootstrapSettings(Animations module) {
        CatalogSettings.append(module, attackStyle);
        CatalogSettings.append(module, useStyle);
        CatalogSettings.append(module, placeStyle);
        CatalogSettings.append(module, idleStyle);
        CatalogSettings.append(module, smoothEquip);
        CatalogSettings.append(module, intensity);
    }

    public static void assignBootstrapSettings(
            Animations module,
            ChoiceSetting preparedAttackStyle,
            ChoiceSetting preparedUseStyle,
            ChoiceSetting preparedPlaceStyle,
            ChoiceSetting preparedIdleStyle,
            BooleanSetting preparedSmoothEquip,
            FloatSetting preparedIntensity) {
        attackStyle = preparedAttackStyle;
        useStyle = preparedUseStyle;
        placeStyle = preparedPlaceStyle;
        idleStyle = preparedIdleStyle;
        smoothEquip = preparedSmoothEquip;
        intensity = preparedIntensity;
    }
}
