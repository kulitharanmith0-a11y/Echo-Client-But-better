package dev.dov.hack.echo.client.modules.impl.render;

import static dev.dov.hack.echo.client.init.CatalogText.literal;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.render.HudRenderEvent;
import dev.dov.hack.echo.client.event.render.TextTransformEvent;
import dev.dov.hack.echo.client.event.render.WorldRenderEvent;
import dev.dov.hack.echo.client.event.world.EntityHitboxEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.modules.internals.Targets;
import dev.dov.hack.echo.client.hud.ItemHudLayout;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.render.world.ProjectedBounds;
import dev.dov.hack.echo.client.render.ui.UiRenderer;
import dev.dov.hack.echo.client.render.world.WorldGeometry;
import dev.dov.hack.echo.client.render.world.WorldProjection;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.ColorSetting;
import dev.dov.hack.echo.client.settings.CustomWidgetSetting;
import dev.dov.hack.echo.client.settings.MultiChoiceSetting;
import dev.dov.hack.echo.client.ui.ColorMath;
import dev.dov.hack.echo.client.ui.UiBounds;
import dev.dov.hack.echo.client.utils.MathRng;
import dev.dov.hack.echo.client.utils.targeting.TargetSelection;
import dev.dov.hack.echo.client.render.font.FontAsset;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class PlayerESP extends Module {
    private static final CharSequence NONE = literal("None");
    private static final CharSequence DYNAMIC = literal("Dynamic");
    private static final CharSequence BOX = literal("Box");
    private static final CharSequence TWO_DIMENSIONAL = literal("2D");
    private static final CharSequence LEFT = literal("Left");
    private static final CharSequence RIGHT = literal("Right");
    private static final CharSequence BOTTOM = literal("Bottom");
    private static final CharSequence STATIC = literal("Static");
    private static MultiChoiceSetting modes;
    private static BooleanSetting fill;
    private static BooleanSetting renderSelf;
    private static ChoiceSetting healthBar;
    private static ChoiceSetting healthColor;
    private static ColorSetting fullHealthColor;
    private static ColorSetting lowHealthColor;
    private static ColorSetting healthBarColor;
    private static BooleanSetting showName;
    private static BooleanSetting showHeldItem;
    private static BooleanSetting tracers;
    private static ColorSetting color;
    private static BooleanSetting teamColor;
    private static CustomWidgetSetting playerEspWidget =
            new CustomWidgetSetting(literal("Preview"), 320.0f, PlayerESP::renderPreview);
    private static final float TEXT_SIZE = 7.0f;
    private static final float PREVIEW_HEALTH = 0.65f;
    private Queue<Runnable> overlayCalls;

    public PlayerESP() {
        super(new ModuleInfo(literal("Player ESP"), literal("Highlights players"), ModuleCategory.RENDER));
    }

    @Override
    public void postInitialize() {
        overlayCalls = new ArrayDeque<>();
        super.postInitialize();
        healthColor.setVisibilityPredicate(ignored -> !healthBar.isSelected(NONE));
        fullHealthColor.setVisibilityPredicate(ignored -> !healthBar.isSelected(NONE) && healthColor.isSelected(DYNAMIC));
        lowHealthColor.setVisibilityPredicate(ignored -> !healthBar.isSelected(NONE) && healthColor.isSelected(DYNAMIC));
        healthBarColor.setVisibilityPredicate(ignored -> !healthBar.isSelected(NONE) && healthColor.isSelected(STATIC));
        fill.tooltip("Appearance");
        renderSelf.tooltip("Appearance");
        healthColor.tooltip("Appearance");
        fullHealthColor.tooltip("Appearance");
        lowHealthColor.tooltip("Appearance");
        healthBarColor.tooltip("Appearance");
        tracers.tooltip("Appearance");
        color.tooltip("Appearance");
        teamColor.tooltip("Appearance");
        playerEspWidget.tooltip("Preview");
    }

    @Subscribe
    public void onWorldRender(WorldRenderEvent event) {
        if (client.world == null) {
            return;
        }
        WorldGeometry geometry = event.getRenderContext().getOverlayGeometry();
        UiRenderer renderer = EchoClient.uiRenderer;
        float tickDelta = event.getTickProgress();
        Vec3d camera = MinecraftRenderState.getCamera(client).getCameraPos();
        Frustum frustum = new Frustum(event.getViewMatrix(), event.getProjectionMatrix());
        frustum.setPosition(camera.x, camera.y, camera.z);
        boolean boxMode = modes.isSelected(BOX);
        boolean screenMode = modes.isSelected(TWO_DIMENSIONAL);
        boolean leftHealth = healthBar.isSelected(LEFT);
        boolean rightHealth = healthBar.isSelected(RIGHT);
        boolean bottomHealth = healthBar.isSelected(BOTTOM);
        boolean drawHealth = leftHealth || rightHealth || bottomHealth;
        int borderColor = 0xff000000;
        float stroke = 0.5f;
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) {
                continue;
            }
            boolean self = player == client.player;
            if (self && (!renderSelf.getValue() || client.options.getPerspective().isFirstPerson())) {
                continue;
            }
            if (!self && Targets.antiBot.getValue() && TargetSelection.isBot(player)) {
                continue;
            }
            int argb = playerColor(player);
            int opaqueColor = argb | 0xff000000;
            Vec3d position = player.getLerpedPos(tickDelta);
            EntityHitboxEvent hitbox = new EntityHitboxEvent(player, player.getBoundingBox().expand((double) player.getTargetingMargin()));
            hitbox.post();
            Box box = hitbox.getBoundingBox().offset(position.x - player.getX(), position.y - player.getY(), position.z - player.getZ());
            if (boxMode) {
                geometry.drawBox(box.minX, box.minY, box.minZ, box.getLengthX(), box.getLengthY(), box.getLengthZ(), argb);
                WorldProjection.drawBoxOutline(geometry, box, opaqueColor);
            }
            if (tracers.getValue() && !self) {
                geometry.drawTracer(position.x, position.y + (double) player.getStandingEyeHeight(), position.z, opaqueColor);
            }
            if (!screenMode && !drawHealth && !showName.getValue() && !showHeldItem.getValue()) {
                continue;
            }
            if (!self && !frustum.isVisible(box)) {
                continue;
            }
            ProjectedBounds projected = WorldProjection.projectBox(event, box);
            if (!projected.visible) {
                continue;
            }
            float x = UiRenderer.snapPixel((float) projected.minX);
            float y = UiRenderer.snapPixel((float) projected.minY);
            float width = UiRenderer.snapPixel((float) projected.maxX) - x;
            float height = UiRenderer.snapPixel((float) projected.maxY) - y;
            if (screenMode) {
                if (fill.getValue()) {
                    overlayCalls.add(() -> renderer.drawRoundedRect(x, y, width, height, 0.0f, argb));
                }
                overlayCalls.add(() -> {
                    WorldProjection.drawRectangleBorder(renderer, x - stroke, y - stroke, width + stroke * 2.0f, height + stroke * 2.0f, stroke * 3.0f, borderColor);
                    WorldProjection.drawRectangleBorder(renderer, x, y, width, height, stroke, opaqueColor);
                });
            }
            if (drawHealth) {
                float health = healthFraction(player);
                int healthArgb = healthColor(health);
                float gap = 1.5f;
                float thickness = MathRng.clamp(width * 0.06f, 0.75f, 2.0f);
                if (bottomHealth) {
                    float length = Math.max(width * health, thickness);
                    float barY = y + height + gap;
                    overlayCalls.add(() -> {
                        WorldProjection.drawRectangleBorder(renderer, x - stroke, barY - stroke, length + stroke * 2.0f, thickness + stroke * 2.0f, stroke, borderColor);
                        renderer.drawRoundedRect(x, barY, length, thickness, 0.0f, healthArgb);
                    });
                } else {
                    float length = Math.max(height * health, thickness);
                    float barX = rightHealth ? x + width + gap : x - thickness - gap;
                    float barY = y + height - length;
                    overlayCalls.add(() -> {
                        WorldProjection.drawRectangleBorder(renderer, barX - stroke, barY - stroke, thickness + stroke * 2.0f, length + stroke * 2.0f, stroke, borderColor);
                        renderer.drawRoundedRect(barX, barY, thickness, length, 0.0f, healthArgb);
                    });
                }
            }
            if (showName.getValue()) {
                String name = TextTransformEvent.transform(player.getName().getString());
                float textWidth = FontAsset.INTER.measureText(name, TEXT_SIZE);
                float textX = x + width / 2.0f - textWidth / 2.0f;
                float textY = y - TEXT_SIZE - 2.0f;
                overlayCalls.add(() -> renderer.drawTextWithShadow(FontAsset.INTER, name, textX, textY, TEXT_SIZE, -1));
            }
            if (showHeldItem.getValue()) {
                ItemStack held = player.getMainHandStack();
                if (held.isEmpty()) {
                    continue;
                }
                String name = held.getName().getString();
                float textWidth = FontAsset.INTER.measureText(name, TEXT_SIZE);
                float textX = x + width / 2.0f - textWidth / 2.0f;
                float textY = y + height + 2.0f + (bottomHealth ? 4.0f : 0.0f);
                overlayCalls.add(() -> renderer.drawTextWithShadow(FontAsset.INTER, name, textX, textY, TEXT_SIZE, -1));
            }
        }
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    public void onHudRender(HudRenderEvent event) {
        while (!overlayCalls.isEmpty()) {
            overlayCalls.poll().run();
        }
    }

    private static void renderPreview(ItemHudLayout context) {
        UiBounds bounds = context.getPlayerBounds();
        if (bounds == null) {
            context.drawPlayer();
            return;
        }
        boolean rightHealth = healthBar.isSelected(RIGHT);
        boolean bottomHealth = healthBar.isSelected(BOTTOM);
        boolean drawHealth = rightHealth || bottomHealth || healthBar.isSelected(LEFT);
        int argb = playerColor(client.player);
        float stroke = context.getLineWidth();
        if (tracers.getValue()) {
            context.drawLine(context.getWidth() * 0.12f, context.getHeight(), bounds.centerX(), context.getPlayerEyeY(), stroke, argb | 0xff000000);
        }
        context.drawPlayer();
        context.drawPlayerEsp(bounds, argb, modes.isSelected(BOX), modes.isSelected(TWO_DIMENSIONAL), fill.getValue());
        if (drawHealth) {
            drawPreviewHealth(context, bounds, rightHealth, bottomHealth, stroke);
        }
        if (showName.getValue()) {
            float top = Math.min(bounds.y(), context.getPlayerTop());
            context.drawCenteredText(FontAsset.INTER, previewName(), bounds.centerX(), top - 16.0f, 12.0f, -1);
        }
        if (showHeldItem.getValue()) {
            context.drawCenteredText(FontAsset.INTER, previewHeldItem(), bounds.centerX(), bounds.bottom() + (bottomHealth ? 14 : 6), 12.0f, -1);
        }
    }

    private static void drawPreviewHealth(ItemHudLayout context, UiBounds bounds, boolean right, boolean bottom, float stroke) {
        float scale = context.getAlignedLineWidth();
        float gap = scale * 3.0f;
        float thickness = MathRng.clamp(bounds.width() * 0.06f, scale * 1.5f, scale * 4.0f);
        float length = Math.max((bottom ? bounds.width() : bounds.height()) * PREVIEW_HEALTH, thickness);
        UiBounds healthBounds = bottom
                ? new UiBounds(bounds.x(), bounds.bottom() + gap, length, thickness)
                : new UiBounds(right ? bounds.right() + gap : bounds.x() - thickness - gap, bounds.bottom() - length, thickness, length);
        context.drawRectOutline(healthBounds, stroke, 0xff000000);
        context.drawRect(healthBounds, healthColor(PREVIEW_HEALTH));
    }

    private static int playerColor(PlayerEntity player) {
        int argb = color.getArgb();
        TextColor nameColor = teamColor.getValue() && player != null ? Targets.getNameColor(player) : null;
        return nameColor == null ? argb : argb & 0xff000000 | nameColor.getRgb() & 0xffffff;
    }

    private static String previewName() {
        String name = client.player == null ? "Player" : client.player.getName().getString();
        return TextTransformEvent.transform(name);
    }

    private static String previewHeldItem() {
        ItemStack held = client.player == null ? ItemStack.EMPTY : client.player.getMainHandStack();
        return held.isEmpty() ? "Diamond Sword" : held.getName().getString();
    }

    private static float healthFraction(PlayerEntity player) {
        float health = player.getHealth() + player.getAbsorptionAmount();
        return Math.clamp(health / player.getMaxHealth(), 0.0f, 1.0f);
    }

    private static int healthColor(float health) {
        if (healthColor.isSelected(STATIC)) {
            return healthBarColor.getArgb();
        }
        return ColorMath.interpolate(lowHealthColor.getArgb(), fullHealthColor.getArgb(), health);
    }

    public static void appendBootstrapSettings(PlayerESP Module) {
        CatalogSettings.append(Module, modes);
        CatalogSettings.append(Module, fill);
        CatalogSettings.append(Module, renderSelf);
        CatalogSettings.append(Module, healthBar);
        CatalogSettings.append(Module, healthColor);
        CatalogSettings.append(Module, fullHealthColor);
        CatalogSettings.append(Module, lowHealthColor);
        CatalogSettings.append(Module, healthBarColor);
        CatalogSettings.append(Module, showName);
        CatalogSettings.append(Module, showHeldItem);
        CatalogSettings.append(Module, tracers);
        CatalogSettings.append(Module, color);
        CatalogSettings.append(Module, teamColor);
        CatalogSettings.append(Module, playerEspWidget);
    }

    public static void assignBootstrapSettings(PlayerESP Module, ChoiceSetting preparedHealthColor, ColorSetting preparedColor, BooleanSetting preparedTeamColor, ColorSetting preparedFullHealthColor, BooleanSetting preparedRenderSelf, BooleanSetting preparedShowName, ColorSetting preparedHealthBarColor, BooleanSetting preparedShowHeldItem, BooleanSetting preparedFill, BooleanSetting preparedTracers, ColorSetting preparedLowHealthColor, MultiChoiceSetting preparedModes, ChoiceSetting preparedHealthBar) {
        healthColor = preparedHealthColor;
        color = preparedColor;
        teamColor = preparedTeamColor;
        fullHealthColor = preparedFullHealthColor;
        renderSelf = preparedRenderSelf;
        showName = preparedShowName;
        healthBarColor = preparedHealthBarColor;
        showHeldItem = preparedShowHeldItem;
        fill = preparedFill;
        tracers = preparedTracers;
        lowHealthColor = preparedLowHealthColor;
        modes = preparedModes;
        healthBar = preparedHealthBar;
    }
}
