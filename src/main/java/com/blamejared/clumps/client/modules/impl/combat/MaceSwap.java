package com.blamejared.clumps.client.modules.impl.combat;

import static com.blamejared.clumps.client.init.CatalogText.literal;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.interaction.EntityAttackEvent;
import com.blamejared.clumps.client.event.interaction.ItemUsageTickEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.internals.Targets;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.ServerFallDistanceTracker;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.mixin.accessor.KeyBindingAccessor;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.FloatSetting;
import com.blamejared.clumps.client.settings.HotbarSlotsSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.settings.NamedRangeGroupSetting;
import com.blamejared.clumps.client.settings.range.NamedRange;
import com.blamejared.clumps.client.utils.interaction.EntityInteractions;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.items.ItemGroups;
import com.blamejared.clumps.client.utils.targeting.AttackEvaluation;
import com.blamejared.clumps.client.utils.targeting.EntityPredicates;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import net.minecraft.client.util.InputUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.hit.EntityHitResult;

public class MaceSwap extends Module {
    private static final CharSequence RETURN_SLOT = literal("Slot");
    private static final CharSequence RETURN_ITEM = literal("Item");
    private static final CharSequence RETURN_ORIGINAL = literal("Original");
    public static FloatRangeSetting delay;
    public static ItemListSetting itemWhitelist;
    public static HotbarSlotsSetting hotbarWhitelist;
    private static final float SMASH_THRESHOLD = 1.5f;
    private static final float BREACH_UPPER_BOUND = 7.0f;
    private static final float MAXIMUM_FALL_DISTANCE = 20.0f;
    private static final NamedRange breachRange = new NamedRange(literal("Breach"), SMASH_THRESHOLD, BREACH_UPPER_BOUND);
    private static final NamedRange windBurstRange = new NamedRange(literal("Wind Burst"), SMASH_THRESHOLD, MAXIMUM_FALL_DISTANCE);
    private static final NamedRange densityRange = new NamedRange(literal("Density"), SMASH_THRESHOLD, MAXIMUM_FALL_DISTANCE);
    private static final NamedRange fallbackRange = new NamedRange(literal("Fallback")).disabled();
    public static NamedRangeGroupSetting maceSwapRanges = new NamedRangeGroupSetting(literal("Priority Order"), breachRange, windBurstRange, densityRange, fallbackRange)
        .bounds(literal("Fall Distance"), 0.0f, MAXIMUM_FALL_DISTANCE, 0.1f, literal(" Blocks"))
        .band(literal("Any"), 0.0f, MAXIMUM_FALL_DISTANCE)
        .band(literal("Smash"), SMASH_THRESHOLD, MAXIMUM_FALL_DISTANCE)
        .band(literal("Ground"), 0.0f, SMASH_THRESHOLD);
    public static BooleanSetting stunSlam;
    public static FloatRangeSetting stunSwitchDelay;
    public static BooleanSetting spearDTap;
    public static FloatSetting minimumDamage;
    public static ChoiceSetting returnTo;
    public static HotbarSlotsSetting returnSlots;
    public static ItemListSetting returnItems;
    private static boolean restoreUseKey = false;
    private static int scheduledMaceSlot = -1;
    private static int scheduledTargetId = -1;
    private static long scheduledSwapTick = -1L;
    private int pendingMaceSlot;
    private int pendingTargetId;
    private boolean requireSameTarget;
    private boolean attackPending;
    private long lastSwapTick;
    private static String delaySection;
    private static String itemConditionsSection;
    private static String hotbarConditionsSection;
    private static String prioritySection;
    private static String stunSection;
    private static String stunDelaySection;
    private static String spearSection;
    private static String damageSection;
    private static String returnChoiceSection;
    private static String returnSelectionSection;

    public MaceSwap() {
        this(new ModuleInfo(literal("Mace Swap"), literal("Swaps to mace for attacks"), ModuleCategory.COMBAT));
    }

    public MaceSwap(ModuleInfo info) { super(info); }

    @Override
    public void postInitialize() {
        resetPendingAttack();
        stunSwitchDelay.setVisibilityPredicate(ignored -> stunSlam.getValue());
        stunSwitchDelay.description(literal("Total ticks from the shield disable to attack with the mace"));
        minimumDamage.setVisibilityPredicate(ignored -> spearDTap.getValue());
        returnSlots.setVisibilityPredicate(ignored -> returnTo.isSelected(RETURN_SLOT));
        returnItems.setVisibilityPredicate(ignored -> returnTo.isSelected(RETURN_ITEM));
        if (delaySection == null) delaySection = "Timing";
        delay.tooltip(delaySection);
        if (itemConditionsSection == null) itemConditionsSection = "Conditions";
        itemWhitelist.tooltip(itemConditionsSection);
        if (hotbarConditionsSection == null) hotbarConditionsSection = "Conditions";
        hotbarWhitelist.tooltip(hotbarConditionsSection);
        if (prioritySection == null) prioritySection = "Mace Priority";
        maceSwapRanges.tooltip(prioritySection);
        maceSwapRanges.description(literal("Top to bottom, the first entry with a usable mace inside its fall distance wins. Disabled entries block maces carrying them"));
        if (stunSection == null) stunSection = "Stun Slam";
        stunSlam.tooltip(stunSection);
        if (stunDelaySection == null) stunDelaySection = "Stun Slam";
        stunSwitchDelay.tooltip(stunDelaySection);
        if (spearSection == null) spearSection = "Spear D-tap";
        spearDTap.tooltip(spearSection);
        if (damageSection == null) damageSection = "Spear D-tap";
        minimumDamage.tooltip(damageSection);
        if (returnChoiceSection == null) returnChoiceSection = "Return";
        returnTo.tooltip(returnChoiceSection);
        if (returnSelectionSection == null) returnSelectionSection = "Return";
        returnSlots.tooltip(returnSelectionSection);
        returnItems.tooltip(returnSelectionSection);
    }

    @Override
    public void onEnable() {
        DeferredActionQueue.finish(this, true);
        resetSharedState();
        resetPendingAttack();
        initializeLivingEntityClass();
        Targets.currentTarget = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, true);
        resetSharedState();
        resetPendingAttack();
        initializeLivingEntityClass();
        Targets.currentTarget = null;
        super.onDisable();
    }

    private static void initializeLivingEntityClass() {
        try {
            Class.forName(LivingEntity.class.getName(), true, LivingEntity.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (pendingMaceSlot == -1 || event.slot != pendingMaceSlot) return;
        if (client.player == null || (long) client.player.age - lastSwapTick > 2L) {
            resetPendingAttack();
            return;
        }
        pendingMaceSlot = -1;
        if (client.player.getInventory().getSelectedSlot() != event.slot) {
            int ticks = Math.max(0, (int) delay.sampleGaussian());
            if (!DeferredActionQueue.select(this, event.slot, ticks, returnTo.isSelected(RETURN_ORIGINAL), this::onSwapEnded)) {
                resetPendingAttack();
                return;
            }
        }
        event.addClick();
    }

    @Subscribe(priority = EventPriority.LOW)
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (attackPending) {
            boolean recent = (long) client.player.age - lastSwapTick <= 2L;
            int targetId = pendingTargetId;
            boolean checkTarget = requireSameTarget;
            resetPendingAttack();
            boolean holdingMace = InventoryItems.mainHandStack().getItem() == Items.MACE;
            boolean validTarget = !checkTarget || isCrosshairTargetValid(targetId);
            if (recent && holdingMace && validTarget && !event.hasClicks()) event.addClick();
            return;
        }
        if (!event.hasClicks()) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)) return;
        int slot = findEligibleMace(target);
        if (slot == -1) return;
        suspendItemUse();
        DeferredActionQueue.select(this, slot, Math.max(0, (int) delay.sampleGaussian()), returnTo.isSelected(RETURN_ORIGINAL), this::onSwapEnded);
    }

    @Subscribe
    private void beforeInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (client.options.attackKey == null || ((KeyBindingAccessor) client.options.attackKey).getTimesPressed() <= 0) return;
        if (client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
            requestMaceSwap(target);
        }
    }

    
    public boolean requestMaceSwap(LivingEntity target) {
        int slot = findEligibleMace(target);
        if (slot == -1) return false;
        suspendItemUse();
        pendingMaceSlot = slot;
        pendingTargetId = target.getId();
        requireSameTarget = true;
        attackPending = true;
        lastSwapTick = client.player.age;
        return true;
    }

    private int findEligibleMace(LivingEntity target) {
        if (isOutsideWorld() || attackPending || pendingMaceSlot != -1) return -1;
        if (EntityInteractions.isUsingOffhand() || !itemWhitelist.acceptsHeldItem()
                || !hotbarWhitelist.permitsCurrentSlot()) return -1;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return -1;
        if (!TargetSelection.isValidTarget(target)) return -1;
        if (target instanceof PlayerEntity player && EntityPredicates.isBlocking(player)
                && !EntityPredicates.isOutsideBlockingArc(player)) return -1;
        int slot = findPriorityMace();
        if (slot == -1 || DeferredActionQueue.contains(this)
                || client.player.getInventory().getSelectedSlot() == slot) return -1;
        return slot;
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void processStunSwap(HandleInputEvent.Pre event) {
        if (isOutsideWorld()) return;
        if (MinecraftRenderState.getScreen() != null || !stunSlam.getValue()) {
            Targets.currentTarget = null;
            clearScheduledSwap();
            return;
        }
        Targets.currentTarget = null;
        if (scheduledMaceSlot == -1 || (long) client.player.age < scheduledSwapTick) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)
                && !DeferredActionQueue.contains(ShieldBreaker.class)) {
            clearScheduledSwap();
            return;
        }
        int slot = scheduledMaceSlot;
        int targetId = scheduledTargetId;
        clearScheduledSwap();
        prepareStunAttack(slot, targetId);
    }

    @Subscribe(priority = EventPriority.HIGH)
    private void afterShieldAttack(EntityAttackEvent.Post event) {
        if (isOutsideWorld() || !stunSlam.getValue() || event.getPlayer() != client.player) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        LivingEntity tracked = Targets.currentTarget;
        Targets.currentTarget = null;
        if (tracked == null || tracked.getId() != target.getId()) return;
        if (!InventoryItems.holdsAxe() || !TargetSelection.isValidTarget(target)) return;
        int slot = findPriorityMace();
        if (slot == -1) return;
        int ticks = Math.max(1, Math.round(stunSwitchDelay.sampleGaussian()));
        scheduledMaceSlot = slot;
        scheduledTargetId = target.getId();
        scheduledSwapTick = client.player.age + ticks;
        Targets.shieldBreakActive = true;
    }

    private void prepareStunAttack(int slot, int targetId) {
        suspendItemUse();
        pendingMaceSlot = client.player.getInventory().getSelectedSlot() == slot ? -1 : slot;
        pendingTargetId = targetId;
        requireSameTarget = false;
        attackPending = true;
        lastSwapTick = client.player.age;
        Targets.shieldBreakActive = true;
    }

    int findPriorityMace() {
        return findPriorityMace(maceSwapRanges, densityRange, breachRange, windBurstRange);
    }

    static int findPriorityMace(
            NamedRangeGroupSetting ranges,
            NamedRange density,
            NamedRange breach,
            NamedRange windBurst) {
        float fallDistance = Math.max(
                (float) client.player.fallDistance,
                (float) ServerFallDistanceTracker.fallDistance(client.player));
        for (NamedRange range : ranges.getRanges()) {
            if (!range.isEnabled() || !ranges.contains(range, fallDistance)) continue;
            int slot = findBestMace(enchantmentFor(range, density, breach, windBurst), ranges, density, breach, windBurst);
            if (slot != -1) return slot;
        }
        return -1;
    }

    private static int findBestMace(
            RegistryKey<Enchantment> enchantment,
            NamedRangeGroupSetting ranges,
            NamedRange density,
            NamedRange breach,
            NamedRange windBurst) {
        int bestSlot = -1;
        int bestLevel = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (!(stack.getItem() instanceof MaceItem) || hasDisabledEnchantment(stack, ranges, density, breach, windBurst)) continue;
            int level = enchantment == null ? (hasNoConfiguredEnchantment(stack, ranges, density, breach, windBurst) ? 1 : 0)
                : InventoryItems.getEnchantmentLevel(stack, enchantment);
            if (level > bestLevel) { bestSlot = slot; bestLevel = level; }
        }
        return bestSlot;
    }

    private static boolean hasDisabledEnchantment(
            ItemStack stack,
            NamedRangeGroupSetting ranges,
            NamedRange density,
            NamedRange breach,
            NamedRange windBurst) {
        for (NamedRange range : ranges.getRanges()) {
            RegistryKey<Enchantment> enchantment = enchantmentFor(range, density, breach, windBurst);
            if (enchantment != null && !range.isEnabled()
                    && InventoryItems.getEnchantmentLevel(stack, enchantment) > 0) return true;
        }
        return false;
    }

    private static boolean hasNoConfiguredEnchantment(
            ItemStack stack,
            NamedRangeGroupSetting ranges,
            NamedRange density,
            NamedRange breach,
            NamedRange windBurst) {
        for (NamedRange range : ranges.getRanges()) {
            RegistryKey<Enchantment> enchantment = enchantmentFor(range, density, breach, windBurst);
            if (enchantment != null && InventoryItems.getEnchantmentLevel(stack, enchantment) > 0) return false;
        }
        return true;
    }

    private static RegistryKey<Enchantment> enchantmentFor(
            NamedRange range,
            NamedRange density,
            NamedRange breach,
            NamedRange windBurst) {
        if (range == density) return Enchantments.DENSITY;
        if (range == breach) return Enchantments.BREACH;
        if (range == windBurst) return Enchantments.WIND_BURST;
        return null;
    }

    public int getStunDelayTicks(LivingEntity target) {
        if (!isEnabled() || !stunSlam.getValue() || !(target instanceof PlayerEntity player)) return 0;
        if (!EntityPredicates.isBlocking(player) || EntityPredicates.isOutsideBlockingArc(player)) return 0;
        return Math.max(1, Math.round(stunSwitchDelay.getUpper()));
    }

    public static boolean allowsAxes() {
        if (itemWhitelist.getSelectedCount() == 0) return true;
        for (Item item : Registries.ITEM) {
            if (ItemGroups.axes().matches(item) && itemWhitelist.isEntrySelected(item)) return true;
        }
        return false;
    }

    public static boolean hasPendingSwap() { return DeferredActionQueue.contains(MaceSwap.class); }

    @Subscribe
    private void onItemUsageTick(ItemUsageTickEvent event) {
        if (isOutsideWorld()) return;
        boolean selected = false;
        for (AttackEvaluation evaluation : event.getAttackEvaluations()) {
            if (!evaluation.isAvailable()) continue;
            float damage = evaluation.damage() ? evaluation.damageAmount() : 0.0f;
            if (!selected && spearDTap.getValue() && !(damage < minimumDamage.getValue())
                    && evaluation.target() instanceof LivingEntity target) selected = requestMaceSwap(target);
        }
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        if (isOutsideWorld()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerRespawnS2CPacket || packet instanceof PlayerPositionLookS2CPacket) {
            resetSharedState();
            resetPendingAttack();
            Targets.currentTarget = null;
        }
    }

    private static void resetSharedState() { restoreUseKey = false; clearScheduledSwap(); }
    private static void clearScheduledSwap() { scheduledMaceSlot = -1; scheduledTargetId = -1; scheduledSwapTick = -1L; }
    private void resetPendingAttack() {
        pendingMaceSlot = -1;
        pendingTargetId = -1;
        requireSameTarget = true;
        attackPending = false;
        lastSwapTick = -1L;
    }

    private boolean isCrosshairTargetValid(int targetId) {
        if (targetId < 0 || !(client.crosshairTarget instanceof EntityHitResult hit)) return false;
        return hit.getEntity().getId() == targetId && hit.getEntity() instanceof LivingEntity target
            && target.isAlive() && !target.isRemoved() && TargetSelection.isValidTarget(target);
    }

    private void suspendItemUse() {
        if (client.player != null && (client.player.isUsingItem() || client.options.useKey.isPressed())) {
            client.options.useKey.setPressed(false);
            EntityInteractions.stopUsingItem();
        }
        if (!restoreUseKey && KeybindingCache.isPressed(client.options.useKey)) restoreUseKey = true;
    }

    private void onSwapEnded() {
        if (!returnTo.isSelected(RETURN_ORIGINAL)) {
            int slot = returnTo.isSelected(RETURN_SLOT) ? returnSlots.chooseSelectedSlot()
                : InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
            if (slot != -1) InventoryItems.selectHotbarSlot(slot);
        }
        restoreItemUse();
    }

    private void restoreItemUse() {
        if (!restoreUseKey) return;
        restoreUseKey = false;
        if (client.options.useKey == null) return;
        InputUtil.Key key = ((KeyBindingAccessor) client.options.useKey).getBoundKey();
        if (key == null) return;
        InputKey input = key.getCategory() == InputUtil.Type.MOUSE
            ? InputKey.fromMouseCode(key.getCode()) : InputKey.fromCode(key.getCode());
        if (input.isPressed()) client.options.useKey.setPressed(true);
    }

    public static void appendBootstrapSettings(MaceSwap Module) {
        CatalogSettings.append(Module, delay);
        CatalogSettings.append(Module, itemWhitelist);
        CatalogSettings.append(Module, hotbarWhitelist);
        CatalogSettings.append(Module, maceSwapRanges);
        CatalogSettings.append(Module, stunSlam);
        CatalogSettings.append(Module, stunSwitchDelay);
        CatalogSettings.append(Module, spearDTap);
        CatalogSettings.append(Module, minimumDamage);
        CatalogSettings.append(Module, returnTo);
        CatalogSettings.append(Module, returnSlots);
        CatalogSettings.append(Module, returnItems);
    }

    public static void assignBootstrapSettings(MaceSwap Module, BooleanSetting preparedSpearDTap, HotbarSlotsSetting preparedHotbarWhitelist, FloatRangeSetting preparedStunSwitchDelay, ItemListSetting preparedReturnItems, FloatSetting preparedMinimumDamage, ChoiceSetting preparedReturnTo, HotbarSlotsSetting preparedReturnSlots, ItemListSetting preparedItemWhitelist, BooleanSetting preparedStunSlam, FloatRangeSetting preparedDelay) {
        spearDTap = preparedSpearDTap;
        hotbarWhitelist = preparedHotbarWhitelist;
        stunSwitchDelay = preparedStunSwitchDelay;
        returnItems = preparedReturnItems;
        minimumDamage = preparedMinimumDamage;
        returnTo = preparedReturnTo;
        returnSlots = preparedReturnSlots;
        itemWhitelist = preparedItemWhitelist;
        stunSlam = preparedStunSlam;
        delay = preparedDelay;
    }
}
