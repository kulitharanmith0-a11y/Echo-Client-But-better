package com.blamejared.clumps.client.modules.impl.render;

import static com.blamejared.clumps.client.init.CatalogText.literal;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.render.HudRenderEvent;
import com.blamejared.clumps.client.event.render.TextTransformEvent;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.internals.Targets;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.render.font.FontAsset;
import com.blamejared.clumps.client.render.ui.UiRenderer;
import com.blamejared.clumps.client.render.world.ProjectedBounds;
import com.blamejared.clumps.client.render.world.WorldProjection;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.utils.MathRng;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Nametags extends Module {
    private static final FloatSetting scale = new FloatSetting(literal("Scale"), 1.0F, 0.6F, 2.0F, 0.05F);
    private static final FloatSetting maxRange = new FloatSetting(literal("Max Range"), 128.0F, 16.0F, 256.0F, 8.0F, literal("m"));
    private static final BooleanSetting showHealth = new BooleanSetting(literal("Health"), true);
    private static final BooleanSetting showPing = new BooleanSetting(literal("Ping"), true);
    private static final BooleanSetting showDistance = new BooleanSetting(literal("Distance"), true);
    private static final BooleanSetting showArmor = new BooleanSetting(literal("Armor"), true);
    private static final BooleanSetting showHeldItems = new BooleanSetting(literal("Held Items"), true);
    private static final BooleanSetting showEnchants = new BooleanSetting(literal("Enchants"), true);
    private static final BooleanSetting showDurability = new BooleanSetting(literal("Durability"), true);
    private static final BooleanSetting background = new BooleanSetting(literal("Background"), true);
    private static final BooleanSetting renderSelf = new BooleanSetting(literal("Render Self"), false);
    private static final BooleanSetting ignoreInvisible = new BooleanSetting(literal("Ignore Invisible"), false);

    private static final List<EnchantBadge> TRACKED_ENCHANTS = List.of(
            new EnchantBadge(Enchantments.PROTECTION, "P"),
            new EnchantBadge(Enchantments.BLAST_PROTECTION, "BP"),
            new EnchantBadge(Enchantments.FIRE_PROTECTION, "FP"),
            new EnchantBadge(Enchantments.FEATHER_FALLING, "FF"),
            new EnchantBadge(Enchantments.SHARPNESS, "S"),
            new EnchantBadge(Enchantments.BREACH, "B"),
            new EnchantBadge(Enchantments.DENSITY, "D"),
            new EnchantBadge(Enchantments.WIND_BURST, "WB"),
            new EnchantBadge(Enchantments.FIRE_ASPECT, "FA"),
            new EnchantBadge(Enchantments.KNOCKBACK, "KB"),
            new EnchantBadge(Enchantments.POWER, "PW"),
            new EnchantBadge(Enchantments.UNBREAKING, "U"),
            new EnchantBadge(Enchantments.MENDING, "M")
    );

    private final List<TagEntry> pendingTags = new ArrayList<>();

    public Nametags() {
        super(new ModuleInfo(
                literal("Nametags"),
                literal("Renders detailed custom nametags with health, ping, armor, and enchants above players"),
                ModuleCategory.RENDER));
    }

    public void initializeDefaults() {
        settings.clear();
        CatalogSettings.append(this, scale);
        CatalogSettings.append(this, maxRange);
        CatalogSettings.append(this, showHealth);
        CatalogSettings.append(this, showPing);
        CatalogSettings.append(this, showDistance);
        CatalogSettings.append(this, showArmor);
        CatalogSettings.append(this, showHeldItems);
        CatalogSettings.append(this, showEnchants);
        CatalogSettings.append(this, showDurability);
        CatalogSettings.append(this, background);
        CatalogSettings.append(this, renderSelf);
        CatalogSettings.append(this, ignoreInvisible);
        postInitialize();
    }

    @Override
    public void postInitialize() {
        showEnchants.setVisibilityPredicate(ignored -> showArmor.getValue() || showHeldItems.getValue());
        showDurability.setVisibilityPredicate(ignored -> showArmor.getValue() || showHeldItems.getValue());

        scale.tooltip("Appearance");
        background.tooltip("Appearance");
        maxRange.tooltip("Filter");
        renderSelf.tooltip("Filter");
        ignoreInvisible.tooltip("Filter");
        showHealth.tooltip("Info");
        showPing.tooltip("Info");
        showDistance.tooltip("Info");
        showArmor.tooltip("Equipment");
        showHeldItems.tooltip("Equipment");
        showEnchants.tooltip("Equipment");
        showDurability.tooltip("Equipment");
    }

    @Override
    public void onDisable() {
        pendingTags.clear();
        super.onDisable();
    }

    public boolean shouldHideVanillaLabel(Entity entity) {
        if (!isEnabled() || !(entity instanceof PlayerEntity player) || client.player == null) {
            return false;
        }
        if (player == client.player) {
            return renderSelf.getValue();
        }
        if (ignoreInvisible.getValue() && player.isInvisible()) {
            return false;
        }
        if (Targets.antiBot != null && Targets.antiBot.getValue() && TargetSelection.isBot(player)) {
            return false;
        }
        double range = maxRange.getValue();
        return client.player.squaredDistanceTo(player) <= range * range;
    }

    @Subscribe
    public void onWorldRender(WorldRenderEvent event) {
        pendingTags.clear();
        if (client.world == null || client.player == null) {
            return;
        }
        float tickDelta = event.getTickProgress();
        Vec3d camera = MinecraftRenderState.getCamera(client).getCameraPos();
        double rangeSq = (double) maxRange.getValue() * maxRange.getValue();

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) {
                continue;
            }
            boolean self = player == client.player;
            if (self && (!renderSelf.getValue() || client.options.getPerspective().isFirstPerson())) {
                continue;
            }
            if (!self && ignoreInvisible.getValue() && player.isInvisible()) {
                continue;
            }
            if (!self && Targets.antiBot != null && Targets.antiBot.getValue() && TargetSelection.isBot(player)) {
                continue;
            }
            Vec3d pos = player.getLerpedPos(tickDelta);
            double distSq = camera.squaredDistanceTo(pos);
            if (!self && distSq > rangeSq) {
                continue;
            }
            double anchorY = pos.y + player.getHeight() + (player.isSneaking() ? 0.22 : 0.34);
            Box anchor = new Box(pos.x - 0.02, anchorY - 0.02, pos.z - 0.02, pos.x + 0.02, anchorY + 0.02, pos.z + 0.02);
            ProjectedBounds projected = WorldProjection.projectBox(event, anchor);
            if (projected == null || !projected.visible) {
                continue;
            }
            float screenX = (float) ((projected.minX + projected.maxX) * 0.5);
            float screenY = (float) projected.minY;
            double distance = Math.sqrt(distSq);

            String rawName = player.getName().getString();
            String displayName = TextTransformEvent.transform(rawName);
            boolean friend = ClumpsClient.friendManager != null && ClumpsClient.friendManager.contains(rawName);
            int ping = lookupPing(player);
            float health = Math.max(0.0F, player.getHealth());
            float absorption = Math.max(0.0F, player.getAbsorptionAmount());
            float maxHealth = Math.max(1.0F, player.getMaxHealth());

            List<ItemStack> equipment = collectEquipment(player);
            pendingTags.add(new TagEntry(
                    player, displayName, friend, player.isSneaking(),
                    screenX, screenY, distance, ping, health, absorption, maxHealth, equipment));
        }

        pendingTags.sort(Comparator.comparingDouble(TagEntry::distance).reversed());
    }

    @Subscribe(priority = EventPriority.HIGH)
    public void onHudRender(HudRenderEvent event) {
        if (pendingTags.isEmpty()) {
            return;
        }
        UiRenderer renderer = ClumpsClient.uiRenderer;
        if (renderer == null) {
            pendingTags.clear();
            return;
        }
        for (TagEntry entry : pendingTags) {
            renderTag(renderer, entry);
        }
        pendingTags.clear();
    }

    private void renderTag(UiRenderer renderer, TagEntry entry) {
        float distFactor = MathRng.clamp(1.12F - (float) entry.distance() * 0.0055F, 0.72F, 1.05F);
        float tagScale = scale.getValue() * distFactor;
        float fontSize = 7.5F * tagScale;
        float padX = 4.5F * tagScale;
        float padY = 2.6F * tagScale;
        float gap = 3.5F * tagScale;

        List<Segment> segments = new ArrayList<>(4);
        if (showDistance.getValue()) {
            segments.add(new Segment((int) Math.round(entry.distance()) + "m", 0xFFB5BDC8));
        }
        int nameColor = entry.friend() ? 0xFF4BE3FF : (entry.sneaking() ? 0xFFFFB74D : 0xFFFFFFFF);
        segments.add(new Segment(entry.displayName(), nameColor));
        if (showHealth.getValue()) {
            float total = entry.health() + entry.absorption();
            String hpText = entry.absorption() >= 0.5F
                    ? String.format("%.1f+%.0f", entry.health(), entry.absorption())
                    : String.format("%.1f", total);
            segments.add(new Segment(hpText, healthColor(entry.health() / entry.maxHealth(), entry.absorption() >= 0.5F)));
        }
        if (showPing.getValue() && entry.ping() >= 0) {
            segments.add(new Segment(entry.ping() + "ms", pingColor(entry.ping())));
        }

        float totalTextWidth = 0.0F;
        float[] segWidths = new float[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            float w = FontAsset.INTER.measureText(segments.get(i).text(), fontSize);
            segWidths[i] = w;
            totalTextWidth += w;
            if (i > 0) {
                totalTextWidth += gap;
            }
        }

        float pillW = totalTextWidth + padX * 2.0F;
        float pillH = fontSize + padY * 2.0F;
        float pillX = UiRenderer.snapPixel(entry.screenX() - pillW * 0.5F);
        float pillY = UiRenderer.snapPixel(entry.screenY() - pillH);

        if (background.getValue()) {
            float radius = 3.0F * tagScale;
            int bgColor = entry.friend() ? 0xC40B1F2A : 0xC410131A;
            int borderColor = entry.friend() ? 0x994BE3FF : 0x45FFFFFF;
            renderer.drawRoundedRect(pillX, pillY, pillW, pillH, radius, bgColor);
            WorldProjection.drawRectangleBorder(renderer, pillX, pillY, pillW, pillH, Math.max(0.6F, 0.7F * tagScale), borderColor);
        }

        float cursorX = pillX + padX;
        float textY = pillY + padY - 0.3F * tagScale;
        for (int i = 0; i < segments.size(); i++) {
            Segment seg = segments.get(i);
            renderer.drawTextWithShadow(FontAsset.INTER, seg.text(), cursorX, textY, fontSize, seg.color());
            cursorX += segWidths[i] + gap;
        }

        List<ItemStack> gear = entry.equipment();
        if (!gear.isEmpty()) {
            float iconSize = 11.5F * tagScale;
            float iconGap = 2.0F * tagScale;
            float rowW = gear.size() * iconSize + (gear.size() - 1) * iconGap;
            float startX = UiRenderer.snapPixel(entry.screenX() - rowW * 0.5F);
            float rowY = UiRenderer.snapPixel(pillY - iconSize - 3.0F * tagScale);
            float enchantSize = 5.2F * tagScale;

            for (int i = 0; i < gear.size(); i++) {
                ItemStack stack = gear.get(i);
                float ix = startX + i * (iconSize + iconGap);
                renderer.drawItem(stack, entry.player(), i + 1, ix, rowY, iconSize, 1.0F);

                if (showDurability.getValue() && stack.isDamageable() && stack.getDamage() > 0) {
                    float frac = MathRng.clamp(1.0F - (float) stack.getDamage() / Math.max(1, stack.getMaxDamage()), 0.0F, 1.0F);
                    float barH = Math.max(1.2F, 1.5F * tagScale);
                    float barY = rowY + iconSize - barH;
                    renderer.drawRoundedRect(ix, barY, iconSize, barH, 0.0F, 0xCC000000);
                    renderer.drawRoundedRect(ix, barY, iconSize * frac, barH, 0.0F, healthColor(frac, false));
                }

                if (showEnchants.getValue()) {
                    List<String> badges = collectEnchantLabels(stack);
                    float ey = rowY - enchantSize - 1.0F * tagScale;
                    for (int b = 0; b < badges.size() && b < 3; b++) {
                        String label = badges.get(b);
                        float lw = FontAsset.INTER.measureText(label, enchantSize);
                        renderer.drawTextWithShadow(FontAsset.INTER, label, ix + (iconSize - lw) * 0.5F, ey, enchantSize, 0xFF7CE8FF);
                        ey -= enchantSize + 0.6F * tagScale;
                    }
                }
            }
        }
    }

    private List<ItemStack> collectEquipment(PlayerEntity player) {
        List<ItemStack> list = new ArrayList<>(6);
        if (showHeldItems.getValue()) {
            ItemStack main = player.getMainHandStack();
            if (!main.isEmpty()) {
                list.add(main);
            }
        }
        if (showArmor.getValue()) {
            EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (EquipmentSlot slot : armorSlots) {
                ItemStack piece = player.getEquippedStack(slot);
                if (!piece.isEmpty()) {
                    list.add(piece);
                }
            }
        }
        if (showHeldItems.getValue()) {
            ItemStack off = player.getOffHandStack();
            if (!off.isEmpty()) {
                list.add(off);
            }
        }
        return list;
    }

    private static List<String> collectEnchantLabels(ItemStack stack) {
        List<String> labels = new ArrayList<>(3);
        for (EnchantBadge badge : TRACKED_ENCHANTS) {
            int level = InventoryItems.getEnchantmentLevel(stack, badge.key());
            if (level > 0) {
                labels.add(badge.key() == Enchantments.MENDING ? badge.code() : badge.code() + level);
                if (labels.size() >= 3) {
                    break;
                }
            }
        }
        return labels;
    }

    private int lookupPing(PlayerEntity player) {
        if (client.getNetworkHandler() == null) {
            return -1;
        }
        PlayerListEntry info = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return info == null ? -1 : Math.max(0, info.getLatency());
    }

    private static int healthColor(float fraction, boolean hasAbsorption) {
        if (hasAbsorption && fraction > 0.75F) {
            return 0xFFFFD54F;
        }
        float clamped = MathRng.clamp(fraction, 0.0F, 1.0F);
        if (clamped >= 0.65F) {
            return 0xFF4ADE80;
        }
        if (clamped >= 0.35F) {
            return 0xFFFACC15;
        }
        return 0xFFF87171;
    }

    private static int pingColor(int ping) {
        if (ping <= 60) {
            return 0xFF4ADE80;
        }
        if (ping <= 140) {
            return 0xFFFACC15;
        }
        return 0xFFF87171;
    }

    private record EnchantBadge(RegistryKey<Enchantment> key, String code) {}
    private record Segment(String text, int color) {}
    private record TagEntry(
            PlayerEntity player,
            String displayName,
            boolean friend,
            boolean sneaking,
            float screenX,
            float screenY,
            double distance,
            int ping,
            float health,
            float absorption,
            float maxHealth,
            List<ItemStack> equipment) {}
}
