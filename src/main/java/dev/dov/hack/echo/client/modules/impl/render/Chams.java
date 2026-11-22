package dev.dov.hack.echo.client.modules.impl.render;

import static dev.dov.hack.echo.client.init.CatalogText.literal;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.render.EntityOutlineEvent;
import dev.dov.hack.echo.client.event.render.OutlineFramebufferEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.modules.internals.Targets;
import dev.dov.hack.echo.client.hud.ItemHudLayout;
import dev.dov.hack.echo.client.render.player.EntityMaterialSettings;
import dev.dov.hack.echo.client.render.texture.TexturePasses;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.CustomWidgetSetting;
import dev.dov.hack.echo.client.settings.EntityTypeListSetting;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.utils.targeting.TargetSelection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TextColor;

public class Chams extends Module implements EntityMaterialSettings {
    private static ColorSetting color1;
    private static ColorSetting color2;
    private static FloatSetting lineWidth;
    private static FloatSetting outlineOpacity;
    private static FloatSetting fillOpacity;
    private static FloatSetting glowRadius;
    private static FloatSetting glowIntensity;
    private static FloatSetting speed;
    private static FloatSetting gradientScale;
    private static EntityTypeListSetting entityTypes = new EntityTypeListSetting(literal("Targets"));
    private static BooleanSetting renderSelf;
    private static BooleanSetting teamColors;
    private static CustomWidgetSetting chamsWidget =
            new CustomWidgetSetting(literal("Preview"), 320.0f, Chams::renderPreview);
    private boolean outlinedEntityThisFrame;
    private float animationPhase;
    private long previousAnimationNanos;

    public Chams() {
        super(new ModuleInfo(literal("Chams"), literal("Gradient outline shader on entities"), ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        color1.setVisibilityPredicate(ignored -> !teamColors.getValue());
        color2.setVisibilityPredicate(ignored -> !teamColors.getValue());
        color1.tooltip("Appearance");
        color2.tooltip("Appearance");
        lineWidth.tooltip("Appearance");
        outlineOpacity.tooltip("Appearance");
        fillOpacity.tooltip("Appearance");
        glowRadius.tooltip("Appearance");
        glowIntensity.tooltip("Appearance");
        speed.tooltip("Timing");
        gradientScale.tooltip("Appearance");
        teamColors.tooltip("Appearance");
        chamsWidget.tooltip("Preview");
    }

    @Override public ColorSetting getColor1Setting() { return color1; }
    @Override public ColorSetting getColor2Setting() { return color2; }
    @Override public FloatSetting getLineWidthSetting() { return lineWidth; }
    @Override public FloatSetting getOutlineOpacitySetting() { return outlineOpacity; }
    @Override public FloatSetting getFillOpacitySetting() { return fillOpacity; }
    @Override public FloatSetting getGlowRadiusSetting() { return glowRadius; }
    @Override public FloatSetting getGlowIntensitySetting() { return glowIntensity; }
    @Override public FloatSetting getGradientScaleSetting() { return gradientScale; }
    @Override public boolean usesTeamColors() { return teamColors.getValue(); }

    @Override
    public float getAnimationPhase() {
        long now = System.nanoTime();
        if (previousAnimationNanos != 0L) {
            float elapsedSeconds = (float) (now - previousAnimationNanos) / 1.0E9f;
            animationPhase = (animationPhase + elapsedSeconds * speed.getValue()) % 1.0f;
        }
        previousAnimationNanos = now;
        return animationPhase;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        previousAnimationNanos = 0L;
        outlinedEntityThisFrame = false;
        TexturePasses.releaseResources();
    }

    @Subscribe
    private void onEntityOutline(EntityOutlineEvent event) {
        if (shouldOutline(event.getEntity())) {
            event.setOutlined(true);
        }
        if (event.isOutlined()) {
            outlinedEntityThisFrame = true;
        }
    }

    @Subscribe
    private void onOutlineRender(OutlineFramebufferEvent.Render event) {
        event.cancel();
    }

    @Subscribe
    private void onOutlinePrepare(OutlineFramebufferEvent.Prepare event) {
        boolean hadOutlinedEntity = outlinedEntityThisFrame;
        outlinedEntityThisFrame = false;
        if (!hadOutlinedEntity || !event.canDrawEntityOutlines() || event.getFramebuffer() == null) {
            return;
        }
        TexturePasses.applyMaterial(this, event.getFramebuffer());
        if (EchoClient.externalRendererActive && EchoClient.externalRenderer != null && EchoClient.worldRenderer != null) {
            EchoClient.worldRenderer.queueExternalOverlay(event.getFramebuffer().getColorAttachmentView());
        } else {
            TexturePasses.blitToMainFramebuffer(event.getFramebuffer());
        }
        event.cancel();
    }

    private boolean shouldOutline(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity == client.player) {
            return renderSelf.getValue();
        }
        if (entity instanceof PlayerEntity player && Targets.antiBot.getValue()
                && TargetSelection.isBot(player)) {
            return false;
        }
        return entityTypes.accepts(entity);
    }

    private static void renderPreview(ItemHudLayout context) {
        Chams Module = EchoClient.ModuleManager == null ? null
                : (Chams) EchoClient.ModuleManager.findFeature(Chams.class);
        if (Module == null) {
            return;
        }
        context.drawEntityMaterial(Module, previewTeamColor());
    }

    private static int previewTeamColor() {
        TextColor nameColor = client.player == null ? null : Targets.getNameColor(client.player);
        return nameColor == null ? -1 : 0xff000000 | nameColor.getRgb();
    }

    public static void appendBootstrapSettings(Chams Module) {
        CatalogSettings.append(Module, color1);
        CatalogSettings.append(Module, color2);
        CatalogSettings.append(Module, lineWidth);
        CatalogSettings.append(Module, outlineOpacity);
        CatalogSettings.append(Module, fillOpacity);
        CatalogSettings.append(Module, glowRadius);
        CatalogSettings.append(Module, glowIntensity);
        CatalogSettings.append(Module, speed);
        CatalogSettings.append(Module, gradientScale);
        CatalogSettings.append(Module, entityTypes);
        CatalogSettings.append(Module, renderSelf);
        CatalogSettings.append(Module, teamColors);
        CatalogSettings.append(Module, chamsWidget);
    }

    public static void assignBootstrapSettings(Chams Module, FloatSetting preparedSpeed, ColorSetting preparedColor1, BooleanSetting preparedRenderSelf, FloatSetting preparedGradientScale, FloatSetting preparedGlowRadius, FloatSetting preparedLineWidth, ColorSetting preparedColor2, FloatSetting preparedOutlineOpacity, BooleanSetting preparedTeamColors, FloatSetting preparedFillOpacity, FloatSetting preparedGlowIntensity) {
        speed = preparedSpeed;
        color1 = preparedColor1;
        renderSelf = preparedRenderSelf;
        gradientScale = preparedGradientScale;
        glowRadius = preparedGlowRadius;
        lineWidth = preparedLineWidth;
        color2 = preparedColor2;
        outlineOpacity = preparedOutlineOpacity;
        teamColors = preparedTeamColors;
        fillOpacity = preparedFillOpacity;
        glowIntensity = preparedGlowIntensity;
    }
}
