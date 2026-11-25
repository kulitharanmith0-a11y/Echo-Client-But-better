package com.blamejared.clumps.client.modules.impl.combat;

import static com.blamejared.clumps.client.init.CatalogText.literal;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.interaction.ItemUsageTickEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.ServerFallDistanceTracker;
import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.mixin.accessor.KeyBindingAccessor;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.utils.interaction.EntityInteractions;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.EntityPredicates;
import com.blamejared.clumps.client.utils.targeting.EntityRaycasts;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;

public class BreachSwap extends Module {
    private static final CharSequence WEAPON_SWORD = literal("Sword Only");
    private static final CharSequence WEAPON_SWORD_AXE = literal("Sword & Axe");
    private static final CharSequence WEAPON_ANY = literal("Any Non-Mace");
    private static final float SMASH_FALL_THRESHOLD = 1.5F;

    private static final ChoiceSetting triggerWeapons = new ChoiceSetting(
            literal("Trigger Weapons"), WEAPON_SWORD, WEAPON_SWORD, WEAPON_SWORD_AXE, WEAPON_ANY);
    private static final IntSetting minBreachLevel = new IntSetting(
            literal("Min Breach"), 1, 1, 4);
    private static final FloatSetting minCooldown = new FloatSetting(
            literal("Min Cooldown"), 85.0F, 0.0F, 100.0F, 5.0F, literal("%"));
    private static final BooleanSetting onlyCrits = new BooleanSetting(
            literal("Only Crits"), false);
    private static final BooleanSetting onlyArmored = new BooleanSetting(
            literal("Only Armored"), true);
    private static final BooleanSetting ignoreShields = new BooleanSetting(
            literal("Ignore Shields"), true);
    private static final BooleanSetting skipWhenFalling = new BooleanSetting(
            literal("Skip Smash Falls"), true);
    private static final FloatRangeSetting returnDelay = new FloatRangeSetting(
            literal("Return Delay"), 0.0F, 1.0F, 0.0F, 5.0F, 1.0F, literal(" ticks"));

    private static volatile boolean swapActive;
    private boolean restoreUseKey;
    private int pendingMaceSlot = -1;
    private int pendingTargetId = -1;
    private boolean attackPending;
    private long lastSwapTick = -1L;

    public BreachSwap() {
        super(new ModuleInfo(
                literal("Breach Swap"),
                literal("Attribute-swaps from your weapon to a Breach mace on attack to shred armor at sword speed"),
                ModuleCategory.COMBAT));
    }

    public void initializeDefaults() {
        settings.clear();
        CatalogSettings.append(this, triggerWeapons);
        CatalogSettings.append(this, minBreachLevel);
        CatalogSettings.append(this, minCooldown);
        CatalogSettings.append(this, onlyCrits);
        CatalogSettings.append(this, onlyArmored);
        CatalogSettings.append(this, ignoreShields);
        CatalogSettings.append(this, skipWhenFalling);
        CatalogSettings.append(this, returnDelay);
        postInitialize();
    }

    @Override
    public void postInitialize() {
        triggerWeapons.tooltip("Conditions");
        minBreachLevel.tooltip("Conditions");
        minCooldown.tooltip("Conditions");
        onlyCrits.tooltip("Conditions");
        onlyArmored.tooltip("Conditions");
        ignoreShields.tooltip("Conditions");
        skipWhenFalling.tooltip("Conditions");
        returnDelay.tooltip("Timing");

        triggerWeapons.description(literal("Weapons that trigger a same-tick attribute swap to your Breach mace"));
        minBreachLevel.description(literal("Minimum Breach enchantment level required on the hotbar mace"));
        minCooldown.description(literal("Minimum attack cooldown progress required before swapping"));
        onlyCrits.description(literal("Only breach-swap when landing a falling critical hit"));
        onlyArmored.description(literal("Only swap against targets wearing armor"));
        ignoreShields.description(literal("Skip swapping while target is blocking with a shield"));
        skipWhenFalling.description(literal("Yield to Mace Swap / Auto Mace Swap during smash falls (>= 1.5 blocks)"));
        returnDelay.description(literal("Ticks to wait before switching back to your weapon"));
        resetPendingAttack();
    }

    @Override
    public void onEnable() {
        DeferredActionQueue.finish(this, true);
        restoreUseKey = false;
        resetPendingAttack();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, true);
        restoreUseKey = false;
        resetPendingAttack();
        super.onDisable();
    }

    public static boolean hasPendingSwap() {
        return swapActive || DeferredActionQueue.contains(BreachSwap.class);
    }

    @Subscribe
    private void beforeInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (client.options.attackKey == null || ((KeyBindingAccessor) client.options.attackKey).getTimesPressed() <= 0) {
            return;
        }
        if (client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
            int slot = findEligibleBreachMace(target);
            if (slot != -1) {
                suspendItemUse();
                pendingMaceSlot = slot;
                pendingTargetId = target.getId();
                attackPending = true;
                lastSwapTick = client.player.age;
                swapActive = true;
            }
        }
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (pendingMaceSlot == -1 || event.slot != pendingMaceSlot) {
            return;
        }
        if (client.player == null || (long) client.player.age - lastSwapTick > 2L) {
            resetPendingAttack();
            return;
        }
        pendingMaceSlot = -1;
        if (client.player.getInventory().getSelectedSlot() != event.slot) {
            int ticks = Math.max(0, Math.round(returnDelay.sampleGaussian()));
            if (!DeferredActionQueue.select(this, event.slot, ticks, true, this::onSwapEnded)) {
                if (com.blamejared.clumps.client.diagnostics.DevDiagnostics.ENABLED) {
                    com.blamejared.clumps.client.diagnostics.DevDiagnostics.recordDeny("Breach Swap", "SLOT", "slot=" + event.slot);
                }
                resetPendingAttack();
                return;
            }
            if (com.blamejared.clumps.client.diagnostics.DevDiagnostics.ENABLED) {
                com.blamejared.clumps.client.diagnostics.DevDiagnostics.metric("BreachSwap.swap_ok");
                com.blamejared.clumps.client.diagnostics.DevDiagnostics.log("Breach Swap", "SWAP_MACE",
                        "slot=" + event.slot + " retTicks=" + ticks + " target=" + pendingTargetId);
            }
        }
        event.addClick();
    }

    @Subscribe(priority = EventPriority.LOW)
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) {
            return;
        }
        if (attackPending) {
            boolean recent = (long) client.player.age - lastSwapTick <= 2L;
            int targetId = pendingTargetId;
            resetPendingAttack();
            boolean holdingMace = InventoryItems.mainHandStack().getItem() == Items.MACE;
            boolean validTarget = isCrosshairTargetValid(targetId);
            if (recent && holdingMace && validTarget && !event.hasClicks()) {
                actions().requestAttackClick(this, event);
            }
            return;
        }
        if (!event.hasClicks()) {
            return;
        }
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        int slot = findEligibleBreachMace(target);
        if (slot == -1) {
            return;
        }
        suspendItemUse();
        int ticks = Math.max(0, Math.round(returnDelay.sampleGaussian()));
        DeferredActionQueue.select(this, slot, ticks, true, this::onSwapEnded);
    }

    @Subscribe
    private void onItemUsageTick(ItemUsageTickEvent event) {
        if (restoreUseKey && client.player != null && !client.player.isUsingItem()) {
            event.cancel();
        }
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerPositionLookS2CPacket || packet instanceof PlayerRespawnS2CPacket) {
            resetPendingAttack();
        }
    }

    private int findEligibleBreachMace(LivingEntity target) {
        if (isOutsideWorld() || attackPending || pendingMaceSlot != -1) {
            return -1;
        }
        if (EntityInteractions.isUsingOffhand()) {
            return -1;
        }
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) {
            return -1;
        }
        if (AutoMaceSwap.hasPendingSwap() || MaceSwap.hasPendingSwap()) {
            return -1;
        }
        if (!matchesTriggerWeapon(client.player.getMainHandStack())) {
            return -1;
        }
        if (client.player.getAttackCooldownProgress(0.5F) * 100.0F < minCooldown.getValue()) {
            return -1;
        }
        if (onlyCrits.getValue() && !EntityRaycasts.isCriticalState()) {
            return -1;
        }
        if (skipWhenFalling.getValue() && shouldYieldForSmashFall()) {
            return -1;
        }
        if (!TargetSelection.isValidTarget(target)) {
            return -1;
        }
        if (onlyArmored.getValue() && target.getArmor() <= 0) {
            return -1;
        }
        if (ignoreShields.getValue()
                && target instanceof PlayerEntity player
                && EntityPredicates.isBlocking(player)
                && !EntityPredicates.isOutsideBlockingArc(player)) {
            return -1;
        }
        int slot = findBestBreachMaceSlot();
        if (slot == -1 || DeferredActionQueue.contains(this)
                || client.player.getInventory().getSelectedSlot() == slot) {
            return -1;
        }
        return slot;
    }

    private boolean matchesTriggerWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() instanceof MaceItem) {
            return false;
        }
        boolean sword = stack.isIn(ItemTags.SWORDS) || InventoryItems.isLegacySword(stack.getItem());
        if (triggerWeapons.isSelected(WEAPON_SWORD)) {
            return sword;
        }
        if (triggerWeapons.isSelected(WEAPON_SWORD_AXE)) {
            return sword || InventoryItems.isAxe(stack);
        }
        return true;
    }

    private boolean shouldYieldForSmashFall() {
        float fallDistance = Math.max(
                (float) client.player.fallDistance,
                (float) ServerFallDistanceTracker.fallDistance(client.player));
        if (fallDistance < SMASH_FALL_THRESHOLD) {
            return false;
        }
        if (ClumpsClient.ModuleManager == null) {
            return false;
        }
        Module maceSwap = ClumpsClient.ModuleManager.findFeature(MaceSwap.class);
        if (maceSwap != null && maceSwap.isEnabled()) {
            return true;
        }
        Module autoMaceSwap = ClumpsClient.ModuleManager.findFeature(AutoMaceSwap.class);
        return autoMaceSwap != null && autoMaceSwap.isEnabled();
    }

    private int findBestBreachMaceSlot() {
        int required = minBreachLevel.getValue();
        int bestSlot = -1;
        int bestScore = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (!(stack.getItem() instanceof MaceItem)) {
                continue;
            }
            int breach = InventoryItems.getEnchantmentLevel(stack, Enchantments.BREACH);
            if (breach < required) {
                continue;
            }
            // Prefer highest Breach level; break ties by preferring pure Breach over Density/Wind Burst maces
            int density = InventoryItems.getEnchantmentLevel(stack, Enchantments.DENSITY);
            int windBurst = InventoryItems.getEnchantmentLevel(stack, Enchantments.WIND_BURST);
            int score = breach * 100 - density * 5 - windBurst * 2;
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private boolean isCrosshairTargetValid(int expectedTargetId) {
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity entity)
                || !TargetSelection.isValidTarget(entity)) {
            return false;
        }
        return expectedTargetId == -1 || entity.getId() == expectedTargetId;
    }

    private void suspendItemUse() {
        if (client.player == null || !client.player.isUsingItem()) {
            return;
        }
        client.options.useKey.setPressed(false);
        EntityInteractions.stopUsingItem();
        restoreUseKey = true;
    }

    private void onSwapEnded() {
        if (!restoreUseKey) {
            return;
        }
        restoreUseKey = false;
        if (client.options != null && KeybindingCache.isPressed(client.options.useKey)) {
            client.options.useKey.setPressed(true);
        }
    }

    private void resetPendingAttack() {
        pendingMaceSlot = -1;
        pendingTargetId = -1;
        attackPending = false;
        lastSwapTick = -1L;
        swapActive = false;
    }
}
