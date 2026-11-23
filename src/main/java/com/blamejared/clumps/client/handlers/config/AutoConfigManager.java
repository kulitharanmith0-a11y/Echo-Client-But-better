package com.blamejared.clumps.client.handlers.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.config.ProfileService;
import com.blamejared.clumps.client.config.StoreMeta;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.handlers.ClientHandler;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.utils.chat.ChatHudMessages;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class AutoConfigManager extends ClientHandler {
    private static final String PREF_KEY = "profile_presets_v1";
    private static final String FALLBACK_CANDIDATE_ID = "__default__";
    private static final String KIT_ID_PREFIX = "k:";
    private static final String CUSTOM_ID_PREFIX = "c:";
    private static final String AUTOSAVE = "_autosave";
    private static final int CUSTOM_SLOT_COUNT = 4;
    private static final int[] DELAY_PRESETS = {0, 200, 350, 500, 750, 1000};

    public static final class KitBinding {
        private boolean enabled;
        private String configName;

        public KitBinding(boolean enabled, String configName) {
            this.enabled = enabled;
            this.configName = configName == null ? "" : configName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getConfigName() {
            return configName == null ? "" : configName;
        }

        public void setConfigName(String configName) {
            this.configName = configName == null ? "" : configName.trim();
        }
    }

    private record DetectedKit(String id, String label, GamemodeKit builtInKit, String configName, boolean switchable) { }

    private final Gson gson = new Gson();
    private final Map<GamemodeKit, KitBinding> builtInBindings = new EnumMap<>(GamemodeKit.class);
    private final List<CustomKitRule> customRules = new ArrayList<>();
    private final Map<String, String> serverOverrides = new LinkedHashMap<>();

    private boolean enabled;
    private boolean notifyOnSwitch = false;
    private int stabilityDelayMs = 350;
    private String fallbackConfig = "";

    private String lockedKitId;
    private String lockedKitLabel;
    private GamemodeKit lockedBuiltInKit;
    private String lockedConfigName;
    private boolean serverOverrideActive;
    private boolean forceImmediateRescan;
    private boolean fallbackAppliedSinceUnlock;
    private String pendingCandidateId;
    private long pendingSinceMillis;
    private boolean wasDead;

    public AutoConfigManager() {
        for (GamemodeKit kit : GamemodeKit.values()) {
            builtInBindings.put(kit, new KitBinding(true, ""));
        }
        for (int i = 0; i < CUSTOM_SLOT_COUNT; i++) {
            customRules.add(new CustomKitRule("custom_" + (i + 1), "Custom " + (i + 1)));
        }
        loadPreferences();
    }

    public static AutoConfigManager get() {
        return ClumpsClient.handlerManager == null ? null : ClumpsClient.handlerManager.getAutoConfigManager();
    }

    // -------------------------------------------------------------------------
    // Public Configuration API (Used by Dashboard & Panels Config Views)
    // -------------------------------------------------------------------------

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        unlockCurrentKit();
        savePreferences();
    }

    public boolean isNotifyOnSwitch() {
        return notifyOnSwitch;
    }

    public void setNotifyOnSwitch(boolean notifyOnSwitch) {
        this.notifyOnSwitch = notifyOnSwitch;
        savePreferences();
    }

    public int getStabilityDelayMs() {
        return stabilityDelayMs;
    }

    public void cycleStabilityDelay() {
        int next = DELAY_PRESETS[0];
        for (int i = 0; i < DELAY_PRESETS.length; i++) {
            if (DELAY_PRESETS[i] == stabilityDelayMs) {
                next = DELAY_PRESETS[(i + 1) % DELAY_PRESETS.length];
                break;
            }
        }
        stabilityDelayMs = next;
        savePreferences();
    }

    public String getFallbackConfig() {
        return fallbackConfig == null ? "" : fallbackConfig;
    }

    public void setFallbackConfig(String configName) {
        this.fallbackConfig = configName == null ? "" : configName.trim();
        savePreferences();
    }

    public void cycleFallbackConfig() {
        setFallbackConfig(nextConfigName(getFallbackConfig()));
    }

    public KitBinding getKitBinding(GamemodeKit kit) {
        return builtInBindings.get(kit);
    }

    public boolean isKitEnabled(GamemodeKit kit) {
        KitBinding binding = builtInBindings.get(kit);
        return binding != null && binding.isEnabled();
    }

    public void setKitEnabled(GamemodeKit kit, boolean value) {
        KitBinding binding = builtInBindings.get(kit);
        if (binding != null) {
            binding.setEnabled(value);
            savePreferences();
        }
    }

    public String getKitConfig(GamemodeKit kit) {
        KitBinding binding = builtInBindings.get(kit);
        return binding == null ? "" : binding.getConfigName();
    }

    public void setKitConfig(GamemodeKit kit, String configName) {
        KitBinding binding = builtInBindings.get(kit);
        if (binding != null) {
            binding.setConfigName(configName);
            savePreferences();
        }
    }

    public void cycleKitConfig(GamemodeKit kit) {
        setKitConfig(kit, nextConfigName(getKitConfig(kit)));
    }

    public List<CustomKitRule> getCustomRules() {
        return customRules;
    }

    public CustomKitRule getCustomRule(int index) {
        if (index < 0 || index >= customRules.size()) {
            return null;
        }
        return customRules.get(index);
    }

    public void setCustomRuleEnabled(int index, boolean value) {
        CustomKitRule rule = getCustomRule(index);
        if (rule != null) {
            rule.setEnabled(value);
            savePreferences();
        }
    }

    public void setCustomRuleConfig(int index, String configName) {
        CustomKitRule rule = getCustomRule(index);
        if (rule != null) {
            rule.setConfigName(configName);
            savePreferences();
        }
    }

    public void cycleCustomRuleConfig(int index) {
        CustomKitRule rule = getCustomRule(index);
        if (rule != null) {
            rule.setConfigName(nextConfigName(rule.getConfigName()));
            savePreferences();
        }
    }

    public void toggleCustomRuleMatchMode(int index) {
        CustomKitRule rule = getCustomRule(index);
        if (rule != null) {
            rule.setMatchAll(!rule.isMatchAll());
            savePreferences();
        }
    }

    public void toggleCustomRuleItem(int index, String itemId) {
        CustomKitRule rule = getCustomRule(index);
        if (rule != null) {
            rule.toggleItem(itemId);
            savePreferences();
        }
    }

    public void clearCustomRuleItems(int index) {
        CustomKitRule rule = getCustomRule(index);
        if (rule != null) {
            rule.clearItems();
            savePreferences();
        }
    }

    public void syncCustomRuleFromSetting(int index) {
        CustomKitRule rule = getCustomRule(index);
        if (rule != null) {
            rule.syncFromSetting();
            savePreferences();
        }
    }

    public int captureHotbarToCustomRule(int index) {
        CustomKitRule rule = getCustomRule(index);
        if (rule == null || CLIENT.player == null) {
            return 0;
        }
        int count = rule.captureHotbar(CLIENT.player);
        if (count > 0 && !rule.isEnabled()) {
            rule.setEnabled(true);
        }
        savePreferences();
        return count;
    }

    public List<String> availableConfigNames() {
        if (ClumpsClient.ModuleConfig == null) {
            return List.of();
        }
        List<StoreMeta> metas = ClumpsClient.ModuleConfig.list();
        if (metas == null || metas.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (StoreMeta meta : metas) {
            if (meta == null || meta.name() == null || meta.name().isBlank() || AUTOSAVE.equals(meta.name())) {
                continue;
            }
            if (!names.contains(meta.name())) {
                names.add(meta.name());
            }
        }
        names.sort(Comparator.naturalOrder());
        return names;
    }

    public String getStatusLabel() {
        if (!enabled) {
            return "Disabled";
        }
        if (serverOverrideActive && lockedConfigName != null && !lockedConfigName.isBlank()) {
            return "Server: " + (lockedKitLabel == null ? "" : lockedKitLabel) + " -> " + lockedConfigName;
        }
        if (lockedKitLabel != null && !lockedKitLabel.isBlank()) {
            if (lockedConfigName != null && !lockedConfigName.isBlank()) {
                return "Locked: " + lockedKitLabel + " -> " + lockedConfigName;
            }
            return "Locked: " + lockedKitLabel + " (unmapped)";
        }
        return "Waiting for kit...";
    }

    public String getLockedKitLabel() {
        return lockedKitLabel == null ? "" : lockedKitLabel;
    }

    public String getActiveConfigName() {
        return lockedConfigName == null ? "" : lockedConfigName;
    }

    public boolean isServerOverrideActive() {
        return serverOverrideActive;
    }

    public String getCurrentServerKey() {
        if (CLIENT.world == null) {
            return "";
        }
        ServerInfo entry = CLIENT.getCurrentServerEntry();
        if (entry != null && entry.address != null && !entry.address.isBlank()) {
            return normalizeServerKey(entry.address);
        }
        return CLIENT.isInSingleplayer() ? "singleplayer" : "";
    }

    public static String normalizeServerKey(String rawAddress) {
        if (rawAddress == null) {
            return "";
        }
        String trimmed = rawAddress.trim().toLowerCase(Locale.ROOT);
        while (trimmed.endsWith(".")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public String getServerOverride(String host) {
        String key = normalizeServerKey(host);
        if (key.isEmpty()) {
            return "";
        }
        return serverOverrides.getOrDefault(key, "");
    }

    public void setServerOverride(String host, String configName) {
        String key = normalizeServerKey(host);
        if (key.isEmpty()) {
            return;
        }
        String trimmed = configName == null ? "" : configName.trim();
        if (trimmed.isEmpty()) {
            serverOverrides.remove(key);
        } else {
            serverOverrides.put(key, trimmed);
        }
        unlockCurrentKit();
        savePreferences();
    }

    public void cycleCurrentServerOverride() {
        String host = getCurrentServerKey();
        if (host.isEmpty()) {
            return;
        }
        setServerOverride(host, nextConfigName(getServerOverride(host)));
    }

    public boolean isKitLocked() {
        return lockedKitId != null;
    }

    public void unlockCurrentKit() {
        lockedKitId = null;
        lockedKitLabel = null;
        lockedBuiltInKit = null;
        lockedConfigName = null;
        serverOverrideActive = false;
        fallbackAppliedSinceUnlock = false;
        pendingCandidateId = null;
        pendingSinceMillis = 0L;
    }

    public void triggerUnlockRescan() {
        unlockCurrentKit();
        forceImmediateRescan = true;
    }

    public void reset() {
        unlockCurrentKit();
        forceImmediateRescan = false;
        wasDead = false;
    }

    private String nextConfigName(String current) {
        List<String> names = availableConfigNames();
        if (names.isEmpty()) {
            return "";
        }
        if (current == null || current.isBlank()) {
            return names.get(0);
        }
        int idx = names.indexOf(current);
        if (idx < 0 || idx + 1 >= names.size()) {
            return "";
        }
        return names.get(idx + 1);
    }

    // -------------------------------------------------------------------------
    // Lifecycle Events & Hotbar Detection
    // -------------------------------------------------------------------------

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        unlockCurrentKit();
        wasDead = false;
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof PlayerRespawnS2CPacket) {
            CLIENT.execute(this::unlockCurrentKit);
        }
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        if (!enabled || CLIENT.player == null || CLIENT.world == null || ClumpsClient.ModuleConfig == null) {
            return;
        }
        if (ClumpsClient.ModuleConfig.loading) {
            return;
        }
        if (CLIENT.player.isDead()) {
            wasDead = true;
            unlockCurrentKit();
            return;
        }
        if (wasDead) {
            wasDead = false;
            unlockCurrentKit();
        }
        if (MinecraftRenderState.getScreen() != null) {
            pendingCandidateId = null;
            pendingSinceMillis = 0L;
            return;
        }

        // 0. Per-server IP override takes top priority when configured for the current host.
        String serverKey = getCurrentServerKey();
        String serverCfg = getServerOverride(serverKey);
        if (!serverCfg.isBlank()) {
            String overrideId = "s:" + serverKey;
            if (!overrideId.equals(lockedKitId) || !serverCfg.equals(lockedConfigName)) {
                lockedKitId = overrideId;
                lockedKitLabel = serverKey;
                lockedBuiltInKit = null;
                serverOverrideActive = true;
                fallbackAppliedSinceUnlock = false;
                pendingCandidateId = null;
                pendingSinceMillis = 0L;
                forceImmediateRescan = false;
                if (loadConfigWithNotify("Server (" + serverKey + ")", serverCfg)) {
                    lockedConfigName = serverCfg;
                }
            }
            return;
        }
        if (serverOverrideActive) {
            unlockCurrentKit();
        }

        DetectedKit detected = detectKit();
        long now = System.currentTimeMillis();
        boolean immediate = forceImmediateRescan;
        forceImmediateRescan = false;

        // Mid-fight lock behavior: stay locked if items run out or only a depletion subset remains.
        if (lockedKitId != null) {
            if (detected == null || detected.id.equals(lockedKitId)) {
                pendingCandidateId = null;
                pendingSinceMillis = 0L;
                return;
            }
            if (lockedBuiltInKit != null && detected.builtInKit != null
                    && detected.builtInKit.isDepletionSubsetOf(lockedBuiltInKit)) {
                pendingCandidateId = null;
                pendingSinceMillis = 0L;
                return;
            }
            // A genuinely different kit appeared in the hotbar.
            if (!immediate && !isCandidateStable(detected.id, now)) {
                return;
            }
            applyDetectedKit(detected);
            return;
        }

        // Currently unlocked (fresh world join, respawn, or manual unlock).
        if (detected != null) {
            if (!immediate && !isCandidateStable(detected.id, now)) {
                return;
            }
            applyDetectedKit(detected);
            return;
        }

        String fallback = getFallbackConfig();
        if (!fallback.isBlank() && !fallbackAppliedSinceUnlock) {
            if (!isCandidateStable(FALLBACK_CANDIDATE_ID, now)) {
                return;
            }
            fallbackAppliedSinceUnlock = true;
            pendingCandidateId = null;
            pendingSinceMillis = 0L;
            loadConfigWithNotify("Fallback", fallback);
        } else {
            pendingCandidateId = null;
            pendingSinceMillis = 0L;
        }
    }

    private boolean isCandidateStable(String candidateId, long now) {
        if (!candidateId.equals(pendingCandidateId)) {
            pendingCandidateId = candidateId;
            pendingSinceMillis = now;
            return stabilityDelayMs <= 0;
        }
        return now - pendingSinceMillis >= stabilityDelayMs;
    }

    private void applyDetectedKit(DetectedKit detected) {
        pendingCandidateId = null;
        pendingSinceMillis = 0L;
        lockedKitId = detected.id;
        lockedKitLabel = detected.label;
        lockedBuiltInKit = detected.builtInKit;
        fallbackAppliedSinceUnlock = false;

        if (!detected.switchable || detected.configName == null || detected.configName.isBlank()) {
            lockedConfigName = "";
            return;
        }
        if (detected.configName.equals(lockedConfigName)) {
            return;
        }
        if (loadConfigWithNotify(detected.label, detected.configName)) {
            lockedConfigName = detected.configName;
        }
    }

    private boolean loadConfigWithNotify(String kitLabel, String configName) {
        if (ClumpsClient.ModuleConfig == null || configName == null || configName.isBlank()) {
            return false;
        }
        return ClumpsClient.ModuleConfig.loadForAutoConfig(configName);
    }

    private DetectedKit detectKit() {
        if (CLIENT.player == null) {
            return null;
        }
        Set<String> hotbarIds = new HashSet<>();
        boolean hasCrystal = false;
        boolean hasObsidian = false;
        boolean hasAnchor = false;
        boolean hasGlowstone = false;
        boolean hasTntCart = false;
        boolean hasRail = false;
        boolean hasBowOrCrossbow = false;
        boolean hasFlintAndSteel = false;
        boolean hasMace = false;
        boolean hasSpear = false;
        boolean hasSplashPotion = false;
        boolean hasSword = false;
        boolean hasNetheriteSword = false;
        boolean hasDiamondSword = false;
        boolean hasAxe = false;
        boolean hasNetheriteAxe = false;
        boolean hasDiamondAxe = false;
        boolean hasShield = false;
        boolean hasGoldenApple = false;
        boolean hasLavaBucket = false;
        boolean hasWaterBucket = false;
        boolean hasCobweb = false;
        boolean hasTotem = false;
        boolean hasEnderPearl = false;
        boolean hasXpBottle = false;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = CLIENT.player.getInventory().getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            Identifier id = Registries.ITEM.getId(item);
            if (id != null) {
                hotbarIds.add(id.toString());
            }
            if (item == Items.END_CRYSTAL) hasCrystal = true;
            else if (item == Items.OBSIDIAN) hasObsidian = true;
            else if (item == Items.RESPAWN_ANCHOR) hasAnchor = true;
            else if (item == Items.GLOWSTONE) hasGlowstone = true;
            else if (item == Items.TNT_MINECART) hasTntCart = true;
            else if (item == Items.BOW || item == Items.CROSSBOW) hasBowOrCrossbow = true;
            else if (item == Items.FLINT_AND_STEEL) hasFlintAndSteel = true;
            else if (item == Items.MACE || item instanceof MaceItem) hasMace = true;
            else if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) hasSplashPotion = true;
            else if (item == Items.SHIELD) hasShield = true;
            else if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) hasGoldenApple = true;
            else if (item == Items.LAVA_BUCKET) hasLavaBucket = true;
            else if (item == Items.WATER_BUCKET) hasWaterBucket = true;
            else if (item == Items.COBWEB) hasCobweb = true;
            else if (item == Items.TOTEM_OF_UNDYING) hasTotem = true;
            else if (item == Items.ENDER_PEARL) hasEnderPearl = true;
            else if (item == Items.EXPERIENCE_BOTTLE) hasXpBottle = true;

            if (InventoryItems.isRail(stack)) hasRail = true;
            if (InventoryItems.isSpear(item)) hasSpear = true;
            if (InventoryItems.isAxe(stack)) {
                hasAxe = true;
                if (item == Items.NETHERITE_AXE) {
                    hasNetheriteAxe = true;
                } else if (item == Items.DIAMOND_AXE) {
                    hasDiamondAxe = true;
                }
            }
            if (InventoryItems.isLegacySword(item) || item == Items.COPPER_SWORD) {
                hasSword = true;
                if (item == Items.NETHERITE_SWORD) {
                    hasNetheriteSword = true;
                } else if (item == Items.DIAMOND_SWORD) {
                    hasDiamondSword = true;
                }
            }
        }

        ItemStack offhand = CLIENT.player.getOffHandStack();
        if (!offhand.isEmpty()) {
            Item offItem = offhand.getItem();
            Identifier id = Registries.ITEM.getId(offItem);
            if (id != null) {
                hotbarIds.add(id.toString());
            }
            if (offItem == Items.TOTEM_OF_UNDYING) hasTotem = true;
            else if (offItem == Items.SHIELD) hasShield = true;
            else if (offItem == Items.END_CRYSTAL) hasCrystal = true;
            else if (offItem == Items.GOLDEN_APPLE || offItem == Items.ENCHANTED_GOLDEN_APPLE) hasGoldenApple = true;
        }

        // 1. Custom kit rules take top priority so custom server kits override generic presets.
        for (CustomKitRule rule : customRules) {
            if (rule.isEnabled() && !rule.getItemIds().isEmpty() && !rule.getConfigName().isBlank()
                    && rule.matches(hotbarIds)) {
                return new DetectedKit(CUSTOM_ID_PREFIX + rule.getId(), rule.getName(), null, rule.getConfigName(), true);
            }
        }

        // 2. Built-in gamemodes in priority order (most specific -> least specific).
        boolean hasSmpCore = (hasSword || hasAxe) && hasGoldenApple
                && ((hasTotem && (hasEnderPearl || hasShield || hasXpBottle))
                    || (hasEnderPearl && (hasShield || hasXpBottle)));
        boolean hasNetheriteGear = hasNetheriteSword || hasNetheriteAxe || wearsNetheriteArmor();
        boolean hasDiamondGear = hasDiamondSword || hasDiamondAxe || wearsDiamondArmor();

        GamemodeKit matched = null;
        if ((hasCrystal && (hasObsidian || hasAnchor || hasGlowstone)) || (hasAnchor && hasGlowstone)) {
            matched = GamemodeKit.CRYSTAL;
        } else if (hasTntCart && hasRail && (hasBowOrCrossbow || hasFlintAndSteel)) {
            matched = GamemodeKit.CART;
        } else if (hasMace) {
            matched = GamemodeKit.MACE;
        } else if (hasSpear) {
            matched = GamemodeKit.SPEAR;
        } else if (hasSmpCore) {
            matched = (!hasNetheriteGear && hasDiamondGear) ? GamemodeKit.DIA_SMP : GamemodeKit.SMP;
        } else if (hasSplashPotion && hasSword) {
            matched = hasNetheriteGear ? GamemodeKit.NETHPOT : GamemodeKit.DIAPOT;
        } else if (hasGoldenApple && (hasLavaBucket || hasWaterBucket) && (hasBowOrCrossbow || hasCobweb || hasShield)) {
            matched = GamemodeKit.UHC;
        } else if (hasAxe && hasShield) {
            matched = GamemodeKit.AXE;
        } else if (hasSword && !hasShield && !hasLavaBucket && !hasSplashPotion && !hasCrystal && !hasMace && !hasAnchor) {
            matched = GamemodeKit.SWORD;
        }

        if (matched == null) {
            return null;
        }
        KitBinding binding = builtInBindings.get(matched);
        boolean canSwitch = binding != null && binding.isEnabled() && !binding.getConfigName().isBlank();
        String cfg = binding == null ? "" : binding.getConfigName();
        return new DetectedKit(KIT_ID_PREFIX + matched.name(), matched.displayName(), matched, cfg, canSwitch);
    }

    private boolean wearsNetheriteArmor() {
        if (CLIENT.player == null) {
            return false;
        }
        return CLIENT.player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.NETHERITE_HELMET)
                || CLIENT.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.NETHERITE_CHESTPLATE)
                || CLIENT.player.getEquippedStack(EquipmentSlot.LEGS).isOf(Items.NETHERITE_LEGGINGS)
                || CLIENT.player.getEquippedStack(EquipmentSlot.FEET).isOf(Items.NETHERITE_BOOTS);
    }

    private boolean wearsDiamondArmor() {
        if (CLIENT.player == null) {
            return false;
        }
        return CLIENT.player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.DIAMOND_HELMET)
                || CLIENT.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.DIAMOND_CHESTPLATE)
                || CLIENT.player.getEquippedStack(EquipmentSlot.LEGS).isOf(Items.DIAMOND_LEGGINGS)
                || CLIENT.player.getEquippedStack(EquipmentSlot.FEET).isOf(Items.DIAMOND_BOOTS);
    }

    // -------------------------------------------------------------------------
    // Preferences Persistence (Independent of Individual Gamemode Configs)
    // -------------------------------------------------------------------------

    public void savePreferences() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            root.addProperty("notify", notifyOnSwitch);
            root.addProperty("delayMs", stabilityDelayMs);
            root.addProperty("fallback", getFallbackConfig());

            JsonObject kits = new JsonObject();
            for (GamemodeKit kit : GamemodeKit.values()) {
                KitBinding binding = builtInBindings.get(kit);
                JsonObject obj = new JsonObject();
                obj.addProperty("enabled", binding != null && binding.isEnabled());
                obj.addProperty("config", binding == null ? "" : binding.getConfigName());
                kits.add(kit.name(), obj);
            }
            root.add("kits", kits);

            JsonArray custom = new JsonArray();
            for (CustomKitRule rule : customRules) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", rule.getId());
                obj.addProperty("name", rule.getName());
                obj.addProperty("enabled", rule.isEnabled());
                obj.addProperty("matchAll", rule.isMatchAll());
                obj.addProperty("config", rule.getConfigName());
                JsonArray items = new JsonArray();
                for (String itemId : rule.getItemIds()) {
                    items.add(itemId);
                }
                obj.add("items", items);
                custom.add(obj);
            }
            root.add("custom", custom);

            JsonObject servers = new JsonObject();
            for (Map.Entry<String, String> entry : serverOverrides.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank()
                        && entry.getValue() != null && !entry.getValue().isBlank()) {
                    servers.addProperty(entry.getKey(), entry.getValue());
                }
            }
            root.add("serverOverrides", servers);

            Preferences prefs = Preferences.userRoot().node(ProfileService.PREFERENCES_NODE);
            prefs.put(PREF_KEY, gson.toJson(root));
            prefs.flush();
        } catch (Throwable ignored) {
        }
    }

    private void loadPreferences() {
        try {
            Preferences prefs = Preferences.userRoot().node(ProfileService.PREFERENCES_NODE);
            String raw = prefs.get(PREF_KEY, "");
            if (raw == null || raw.isBlank()) {
                return;
            }
            JsonElement parsed = JsonParser.parseString(raw);
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject root = parsed.getAsJsonObject();
            if (root.has("enabled")) enabled = root.get("enabled").getAsBoolean();
            if (root.has("notify")) notifyOnSwitch = root.get("notify").getAsBoolean();
            if (root.has("delayMs")) stabilityDelayMs = Math.max(0, root.get("delayMs").getAsInt());
            if (root.has("fallback")) fallbackConfig = root.get("fallback").getAsString();

            if (root.has("kits") && root.get("kits").isJsonObject()) {
                JsonObject kits = root.getAsJsonObject("kits");
                for (GamemodeKit kit : GamemodeKit.values()) {
                    if (!kits.has(kit.name()) || !kits.get(kit.name()).isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = kits.getAsJsonObject(kit.name());
                    KitBinding binding = builtInBindings.get(kit);
                    if (obj.has("enabled")) binding.setEnabled(obj.get("enabled").getAsBoolean());
                    if (obj.has("config")) binding.setConfigName(obj.get("config").getAsString());
                }
            }

            if (root.has("custom") && root.get("custom").isJsonArray()) {
                JsonArray custom = root.getAsJsonArray("custom");
                for (int i = 0; i < Math.min(custom.size(), customRules.size()); i++) {
                    if (!custom.get(i).isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = custom.get(i).getAsJsonObject();
                    CustomKitRule rule = customRules.get(i);
                    if (obj.has("name")) rule.setName(obj.get("name").getAsString());
                    if (obj.has("enabled")) rule.setEnabled(obj.get("enabled").getAsBoolean());
                    if (obj.has("matchAll")) rule.setMatchAll(obj.get("matchAll").getAsBoolean());
                    if (obj.has("config")) rule.setConfigName(obj.get("config").getAsString());
                    if (obj.has("items") && obj.get("items").isJsonArray()) {
                        List<String> ids = new ArrayList<>();
                        for (JsonElement el : obj.getAsJsonArray("items")) {
                            if (el.isJsonPrimitive()) {
                                ids.add(el.getAsString());
                            }
                        }
                        rule.setItemIds(ids);
                    }
                }
            }

            serverOverrides.clear();
            if (root.has("serverOverrides") && root.get("serverOverrides").isJsonObject()) {
                JsonObject servers = root.getAsJsonObject("serverOverrides");
                for (Map.Entry<String, JsonElement> entry : servers.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        String key = normalizeServerKey(entry.getKey());
                        String cfg = entry.getValue().getAsString().trim();
                        if (!key.isEmpty() && !cfg.isEmpty()) {
                            serverOverrides.put(key, cfg);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
