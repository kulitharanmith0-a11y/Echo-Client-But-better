package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.interaction.EntityAttackEvent;
import dev.dov.hack.echo.client.event.lifecycle.WorldTickContextEvent;
import dev.dov.hack.echo.client.event.network.PacketReceiveEvent;
import dev.dov.hack.echo.client.gfx.texture.TextureView;
import dev.dov.hack.echo.client.hud.HudElementLayout;
import dev.dov.hack.echo.client.hud.HudElementType;
import dev.dov.hack.echo.client.hud.HudLayout;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.modules.hud.HudElement;
import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.ui.UiSize;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import dev.dov.hack.echo.client.utils.targeting.EntityPredicates;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class Target extends HudElement {
    private static final byte STATUS_DEATH = 3;
    private static final byte STATUS_TOTEM_POP = 35;
    private static final long SHIELD_DISABLE_DURATION_MS = 5000L;
    private static final float PANEL_HEIGHT = 72.0f;

    private final Map<UUID, Integer> totemPops = new HashMap<>();
    private final Map<UUID, Long> shieldStunUntilMs = new HashMap<>();

    public Target() {
        super(new ModuleInfo(
                CatalogText.literal("Target"),
                CatalogText.literal("Target health, armor, offhand, totem pops, ping, and shield status"),
                ModuleCategory.RENDER), HudElementType.TARGET);
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        totemPops.clear();
        shieldStunUntilMs.clear();
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld() || !(event.getPacket() instanceof EntityStatusS2CPacket packet)) {
            return;
        }
        Entity entity = packet.getEntity(client.world);
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }
        byte status = packet.getStatus();
        if (status == STATUS_TOTEM_POP) {
            totemPops.merge(player.getUuid(), 1, Integer::sum);
        } else if (status == STATUS_DEATH) {
            totemPops.remove(player.getUuid());
            shieldStunUntilMs.remove(player.getUuid());
        }
    }

    @Subscribe
    private void onAttackPost(EntityAttackEvent.Post event) {
        if (isOutsideWorld() || event.getPlayer() != client.player) {
            return;
        }
        if (event.getTarget() instanceof PlayerEntity target
                && InventoryItems.holdsAxe()
                && (EntityPredicates.isBlocking(target)
                || EntityPredicates.isUsingBlockingItem(target)
                || target.isBlocking())) {
            shieldStunUntilMs.put(target.getUuid(), System.currentTimeMillis() + SHIELD_DISABLE_DURATION_MS);
        }
    }

    @Override
    protected boolean shouldRender() {
        return HudRenderer.INSTANCE.getTargetVisibility() > 0.002f;
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        boolean inGame = client.currentScreen == null;
        float visibility = inGame ? hud.getTargetVisibility() : 1.0f;
        float textOpacity = inGame ? hud.getTargetTextOpacity() : 1.0f;
        layout.setAnimation(0.92f + 0.08f * visibility, visibility);

        LivingEntity living = hud.getTarget();
        String name = hud.getTargetName();
        String healthText = HudRenderer.formatTargetHealth(hud.getTargetHealth());
        String metaLine = buildMetaLine(living, !inGame);

        float panelWidth = layout.getWidth();
        float contentWidth = Math.max(114.0f, panelWidth - 62.0f - 10.0f);
        float right = 932.0f + contentWidth;

        hud.drawPanel(layout, 870.0f, 511.0f, panelWidth, PANEL_HEIGHT, 12.0f, 1.0f, 14.0f);
        TextureView skin = hud.getTargetSkin();
        if (skin == null) {
            layout.drawTexture(hud.getFallbackTargetPortrait(), 877.0f, 520.0f, 44.0f, 44.0f, 8.0f, 1.0f);
        } else {
            layout.drawTextureRegion(skin, 877.0f, 520.0f, 44.0f, 44.0f, 8.0f, 1.0f, 0.125f, 0.125f, 0.25f, 0.25f);
            layout.drawTextureRegion(skin, 877.0f, 520.0f, 44.0f, 44.0f, 8.0f, 1.0f, 0.625f, 0.125f, 0.75f, 0.25f);
        }

        // Row 1: Name + Health
        float nameY = HudLayout.centeredLeft(526.0f, layout.fontHeight(FontAsset.INTER_SEMI_BOLD, 13.0f));
        float healthY = HudLayout.centeredLeft(526.0f, layout.fontHeight(FontAsset.INTER, 12.0f));
        layout.drawText(FontAsset.INTER_SEMI_BOLD, name, 932.0f, nameY, 13.0f, -1, textOpacity);
        layout.drawText(FontAsset.INTER, healthText,
                HudLayout.rightAlignedLeft(right, layout.textWidth(FontAsset.INTER, healthText, 12.0f)),
                healthY, 12.0f, -1929379841, textOpacity);

        // Row 2: Armor (4) + MainHand (1) + Offhand (1)
        for (int slot = 0; slot < 4; slot++) {
            drawArmorSlot(layout, slot, slotX(slot), 535.0f);
        }
        ItemStack mainHand = hud.getTargetMainHand();
        if (mainHand.isEmpty()) {
            drawEmptySlot(layout, slotX(4), 535.0f);
        } else {
            layout.drawItem(mainHand, slotX(4), 535.0f, 16.0f, 44);
        }
        ItemStack offHand = living == null ? ItemStack.EMPTY : living.getOffHandStack();
        if (offHand.isEmpty()) {
            drawEmptySlot(layout, slotX(5), 535.0f);
        } else {
            layout.drawItem(offHand, slotX(5), 535.0f, 16.0f, 45);
        }

        // Row 3: Ping · Pops · Shield/Stun · Buffs
        if (!metaLine.isEmpty()) {
            layout.drawText(FontAsset.INTER, metaLine, 932.0f, 554.5f, 9.5f, 0xB8FFFFFF, textOpacity);
        }

        // Bottom health progress bar
        layout.drawRoundedRect(932.0f, 571.0f, contentWidth, 2.5f, 1.25f, 0x26ffffff, 1.0f);
        float fraction = HudLayout.clampFraction(hud.getTargetHealth() / Math.max(1.0f, hud.getTargetMaximumHealth()));
        layout.drawAccentRect(932.0f, 571.0f, contentWidth * fraction, 2.5f, 1.25f, 1.0f);
    }

    private String buildMetaLine(LivingEntity living, boolean preview) {
        if (preview && living == null) {
            return "34ms · 2 pops · STUN 3.2s";
        }
        if (!(living instanceof PlayerEntity player)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int ping = resolvePing(player);
        if (ping >= 0) {
            sb.append(ping).append("ms");
        }
        int pops = totemPops.getOrDefault(player.getUuid(), 0);
        if (pops > 0) {
            appendSep(sb);
            sb.append(pops).append(pops == 1 ? " pop" : " pops");
        }
        Long stunUntil = shieldStunUntilMs.get(player.getUuid());
        long now = System.currentTimeMillis();
        if (stunUntil != null && stunUntil > now) {
            appendSep(sb);
            sb.append(String.format(Locale.ROOT, "STUN %.1fs", (stunUntil - now) / 1000.0));
        } else if (EntityPredicates.isBlocking(player) || player.isBlocking()) {
            appendSep(sb);
            sb.append("SHIELD");
        }
        if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
            appendSep(sb);
            sb.append("STR");
        }
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            appendSep(sb);
            sb.append("SPD");
        }
        if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
            appendSep(sb);
            sb.append("REG");
        }
        return sb.toString();
    }

    private int resolvePing(PlayerEntity player) {
        if (client.getNetworkHandler() == null) {
            return -1;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry == null ? -1 : Math.max(0, entry.getLatency());
    }

    private static void appendSep(StringBuilder sb) {
        if (!sb.isEmpty()) {
            sb.append(" · ");
        }
    }

    private static float slotX(int slot) {
        return 932.0f + 18.0f * HudLayout.slotIndex(slot, 6);
    }

    private static void drawArmorSlot(HudElementLayout layout, int slot, float x, float y) {
        ItemStack stack = HudRenderer.INSTANCE.getTargetArmor(slot);
        if (stack.isEmpty()) {
            drawEmptySlot(layout, x, y);
            return;
        }
        layout.drawItem(stack, x, y, 16.0f, 40 + slot);
    }

    private static void drawEmptySlot(HudElementLayout layout, float x, float y) {
        float width = layout.textWidth(FontAsset.INTER, "×", 9.0f);
        float height = layout.fontHeight(FontAsset.INTER, 9.0f);
        layout.drawText(FontAsset.INTER, "×", x + (16.0f - width) * 0.5f, y + (16.0f - height) * 0.5f, 9.0f, 0x59ffffff, 1.0f);
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        String health = HudRenderer.formatTargetHealth(hud.getTargetHealth());
        float nameWidth = layout.textWidth(FontAsset.INTER_SEMI_BOLD, hud.getTargetName(), 13.0f);
        float healthWidth = layout.textWidth(FontAsset.INTER, health, 12.0f);
        String meta = buildMetaLine(hud.getTarget(), client.currentScreen != null);
        float metaWidth = meta.isEmpty() ? 0.0f : layout.textWidth(FontAsset.INTER, meta, 9.5f);
        float baseContent = Math.max(112.0f, Math.max(HudLayout.targetContentWidth(nameWidth, healthWidth), metaWidth));
        return new UiSize(HudLayout.targetPanelWidth(baseContent), PANEL_HEIGHT);
    }
}
