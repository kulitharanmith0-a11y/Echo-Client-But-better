package com.blamejared.clumps.client.modules.impl.combat;

import static com.blamejared.clumps.client.init.CatalogText.literal;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.event.input.UseKeyEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.modules.internals.Targets;
import com.blamejared.clumps.client.handlers.AttackCooldownTracker;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.settings.HotbarSlotsSetting;
import com.blamejared.clumps.client.settings.IntSetting;
import com.blamejared.clumps.client.settings.ItemListSetting;
import com.blamejared.clumps.client.utils.CooldownTimer;
import com.blamejared.clumps.client.utils.MathRng;
import com.blamejared.clumps.client.utils.interaction.EntityInteractions;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.EntityPredicates;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

public class ShieldBreaker extends Module {
    private static final CharSequence modeOption = literal("Click");
    private static final CharSequence modeOption2 = literal("Normal");
    private static final CharSequence returnToOption = literal("Original");
    private static final CharSequence returnToOption2 = literal("Slot");
    private static final CharSequence returnToOption3 = literal("Item");
    public static ChoiceSetting mode;
    public static FloatRangeSetting reactionDelay;
    public static FloatRangeSetting attackDelay;
    public static BooleanSetting autoSwap;
    public static BooleanSetting swapBack;
    public static FloatRangeSetting swapBackDelay;
    public static ChoiceSetting returnTo;
    public static HotbarSlotsSetting returnSlots;
    public static ItemListSetting returnItems;
    public static BooleanSetting unblockOwnShield;
    public static BooleanSetting stun;
    public static IntSetting stunChance;
    public static ItemListSetting itemWhitelist;
    public static HotbarSlotsSetting hotbarWhitelist;
    private static final long STUN_FOLLOWUP_WINDOW_TICKS = 8L;
    private static volatile ShieldBreaker activeInstance;
    private CooldownTimer attackTimer;
    private CooldownTimer switchTimer;
    private int selectedAxeSlot;
    private PlayerEntity shieldTarget;
    private int stunFollowupTargetId = -1;
    private long stunReadyAtTick;
    private long stunExpiresAtTick;
    private boolean stunWaitingForSwapBack;
    private PlayerEntity timedAttackTarget;
    private boolean attackDelayActive;
    private boolean switchDelayActive;
    private boolean ownShieldReleased;
    private boolean restoreShieldRequested;
    private boolean unblockTickPending;
    private static String reactionSection;
    private static String attackSection;
    private static String returnChoiceSection;
    private static String returnSelectionSection;
    private static String itemConditionsSection;
    private static String hotbarConditionsSection;

    public ShieldBreaker() {
        this(new ModuleInfo(literal("Shield Breaker"), literal("Breaks enemy shields"), ModuleCategory.COMBAT));
    }

    public ShieldBreaker(ModuleInfo info) { super(info); }

    @Override
    public void postInitialize() {
        attackTimer = new CooldownTimer();
        switchTimer = new CooldownTimer();
        selectedAxeSlot = -1;
        reactionDelay.setVisibilityPredicate(ignored -> mode.isSelected(modeOption2));
        attackDelay.setVisibilityPredicate(ignored -> mode.isSelected(modeOption2));
        autoSwap.setVisibilityPredicate(ignored -> mode.isSelected(modeOption2));
        unblockOwnShield.setVisibilityPredicate(ignored -> mode.isSelected(modeOption2));
        swapBack.setVisibilityPredicate(ignored -> mode.isSelected(modeOption)
            || mode.isSelected(modeOption2) && autoSwap.getValue());
        swapBackDelay.setVisibilityPredicate(ignored -> swapBack.getValue()
            && (mode.isSelected(modeOption) || mode.isSelected(modeOption2) && autoSwap.getValue()));
        returnTo.setVisibilityPredicate(ignored -> usesReturnSelection());
        returnSlots.setVisibilityPredicate(ignored -> usesReturnSelection() && returnTo.isSelected(returnToOption2));
        returnItems.setVisibilityPredicate(ignored -> usesReturnSelection() && returnTo.isSelected(returnToOption3));
        stunChance.setVisibilityPredicate(ignored -> stun.getValue());
        stunChance.description(literal("Percentage chance to follow up with a stun hit after breaking a shield"));
        stunChance.tooltip("Timing");
        if (reactionSection == null) reactionSection = "Timing";
        reactionDelay.tooltip(reactionSection);
        if (attackSection == null) attackSection = "Timing";
        attackDelay.tooltip(attackSection);
        swapBackDelay.tooltip(attackSection);
        if (returnChoiceSection == null) returnChoiceSection = "Return";
        returnTo.tooltip(returnChoiceSection);
        if (returnSelectionSection == null) returnSelectionSection = "Return";
        returnSlots.tooltip(returnSelectionSection);
        returnItems.tooltip(returnSelectionSection);
        if (itemConditionsSection == null) itemConditionsSection = "Conditions";
        itemWhitelist.tooltip(itemConditionsSection);
        if (hotbarConditionsSection == null) hotbarConditionsSection = "Conditions";
        hotbarWhitelist.tooltip(hotbarConditionsSection);
    }

    private static boolean usesReturnSelection() {
        return swapBack.getValue() && (mode.isSelected(modeOption)
            || mode.isSelected(modeOption2) && autoSwap.getValue());
    }

    @Override
    public void onEnable() {
        resetShieldBreakState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this, true);
        resetShieldBreakState();
        super.onDisable();
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (event.slot == 0) {
            releaseOwnShield();
            prepareAxeSwap();
        }
        if (unblockTickPending) {
            event.setClickCount(0);
            return;
        }
        if (event.slot != selectedAxeSlot) return;
        selectedAxeSlot = -1;
        int ticks = Math.max(0, Math.round(swapBackDelay.sampleGaussian()));
        boolean restoreAll = swapBack.getValue() && returnTo.isSelected(returnToOption);
        if (!DeferredActionQueue.select(this, event.slot, ticks, restoreAll, this::onSwapEnded)) {
            shieldTarget = null;
            return;
        }
        event.addClick();
    }

    public static boolean isShieldBreakPending() {
        ShieldBreaker inst = activeInstance;
        return inst != null && inst.isEnabled()
                && (inst.stunFollowupTargetId != -1 || inst.shieldTarget != null || inst.unblockTickPending);
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void onAttackKey(AttackKeyEvent event) {
        activeInstance = this;
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (unblockTickPending) {
            unblockTickPending = false;
            Targets.shieldBreakActive = false;
            event.setClickCount(0);
            return;
        }
        if (stunFollowupTargetId != -1) {
            if (AutoMaceSwap.hasPendingSwap() || AutoMaceSwap.isStunSequenceRunning()) {
                clearStunFollowup();
                Targets.shieldBreakActive = false;
                return;
            }
            long age = client.player.age;
            if (age > stunExpiresAtTick) {
                clearStunFollowup();
                Targets.shieldBreakActive = false;
                return;
            }
            if (age < stunReadyAtTick) {
                Targets.shieldBreakActive = true;
                return;
            }
            if (stunWaitingForSwapBack && DeferredActionQueue.contains(this)) {
                Targets.shieldBreakActive = true;
                return;
            }
            stunWaitingForSwapBack = false;
            if (EntityInteractions.isBlocking() || client.attackCooldown > 0) {
                Targets.shieldBreakActive = true;
                return;
            }
            if (client.crosshairTarget instanceof EntityHitResult hit
                    && hit.getEntity() instanceof PlayerEntity target
                    && target.getId() == stunFollowupTargetId
                    && target.isAlive() && !target.isRemoved()) {
                if (event.hasClicks() || actions().requestAttackClick(this, event)) {
                    clearStunFollowup();
                    Targets.shieldBreakActive = false;
                }
                return;
            }
            Targets.shieldBreakActive = true;
            return;
        }
        Targets.shieldBreakActive = false;
        if (shieldTarget != null) {
            PlayerEntity target = shieldTarget;
            shieldTarget = null;
            if (InventoryItems.holdsAxe()) addShieldAttack(event, target);
            return;
        }
        if (mode.isSelected(modeOption)) handleClickAttack(event);
        else handleNormalAttack(event);
    }

    @Subscribe
    private void onUseKey(UseKeyEvent event) {
        if (!ownShieldReleased) return;
        if (restoreShieldRequested) {
            restoreShieldRequested = false;
            ownShieldReleased = false;
            if (KeybindingCache.isPressed(client.options.useKey) && EntityInteractions.holdsShield()) {
                client.options.useKey.setPressed(true);
                if (!event.hasClicks()) event.addClick();
            }
            return;
        }
        if (KeybindingCache.isPressed(client.options.useKey)) {
            client.options.useKey.setPressed(false);
            event.setClickCount(0);
        } else {
            ownShieldReleased = false;
        }
    }

    private void handleClickAttack(AttackKeyEvent event) {
        if (AutoMaceSwap.isStunSequenceRunning()) return;
        if (!event.hasClicks() || !passesItemConditions()) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return;
        if (EntityInteractions.isUsingOffhand()) return;
        PlayerEntity target = findShieldTarget();
        if (target == null || client.attackCooldown > 0 || AttackCooldownTracker.hasNonFireHurtWindow(target)) return;
        if (InventoryItems.holdsAxe()) {
            addShieldAttack(event, target);
            return;
        }
        int slot = findAxeSlot();
        if (slot == -1 || DeferredActionQueue.contains(this)) return;
        int ticks = Math.max(0, Math.round(swapBackDelay.sampleGaussian()));
        boolean restoreAll = swapBack.getValue() && returnTo.isSelected(returnToOption);
        if (!DeferredActionQueue.select(this, slot, ticks, restoreAll, this::onSwapEnded)) return;
        addShieldAttack(event, target);
    }

    private void handleNormalAttack(AttackKeyEvent event) {
        if (AutoMaceSwap.isStunSequenceRunning()) return;
        if (!passesItemConditions()) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return;
        PlayerEntity target = findShieldTarget();
        if (target == null) { resetShieldAttackTiming(); return; }
        if (EntityInteractions.isBlocking() || client.attackCooldown > 0
                || AttackCooldownTracker.hasNonFireHurtWindow(target)) return;
        if (!InventoryItems.holdsAxe() || attackDelayActive) return;
        if (!switchTimer.hasElapsedMillis((double) attackDelay.sampleGaussian())) return;
        addShieldAttack(event, target);
    }

    private void prepareAxeSwap() {
        if (!mode.isSelected(modeOption2) || isOutsideWorld()) return;
        if (MinecraftRenderState.getScreen() != null || !passesItemConditions()) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return;
        PlayerEntity target = findShieldTarget();
        if (target == null) { resetShieldAttackTiming(); return; }
        if (target != timedAttackTarget) {
            timedAttackTarget = target;
            attackTimer.reset();
            switchTimer.reset();
        }
        if (unblockTickPending || EntityInteractions.isBlocking() || InventoryItems.holdsAxe()) return;
        if (client.attackCooldown > 0 || AttackCooldownTracker.hasNonFireHurtWindow(target)) return;
        if (!autoSwap.getValue() || switchDelayActive) return;
        if (!attackTimer.hasElapsedMillis((double) reactionDelay.sampleGaussian())) return;
        int slot = findAxeSlot();
        if (slot == -1 || DeferredActionQueue.contains(this)) return;
        selectedAxeSlot = slot;
        shieldTarget = target;
        switchDelayActive = true;
        switchTimer.reset();
    }

    private void addShieldAttack(AttackKeyEvent event, PlayerEntity target) {
        if (!event.hasClicks() && !actions().requestAttackClick(this, event)) return;
        Targets.currentTarget = target;
        if (stun.getValue() && passesStunChance() && client.player != null) {
            stunFollowupTargetId = target.getId();
            stunReadyAtTick = client.player.age + 1L;
            stunExpiresAtTick = client.player.age + STUN_FOLLOWUP_WINDOW_TICKS;
            stunWaitingForSwapBack = swapBack.getValue() && DeferredActionQueue.contains(this);
            Targets.shieldBreakActive = true;
        }
        attackDelayActive = true;
        switchTimer.reset();
    }

    private void clearStunFollowup() {
        stunFollowupTargetId = -1;
        stunReadyAtTick = 0L;
        stunExpiresAtTick = 0L;
        stunWaitingForSwapBack = false;
    }

    private boolean passesStunChance() {
        int chance = stunChance != null ? stunChance.getValue() : 100;
        if (chance >= 100) return true;
        if (chance <= 0) return false;
        return MathRng.randomInt(1, 101) <= chance;
    }

    private PlayerEntity findShieldTarget() {
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof PlayerEntity target)) return null;
        if (!EntityPredicates.isBlocking(target) || EntityPredicates.isOutsideBlockingArc(target)) return null;
        if (!TargetSelection.isValidTarget(target) || TargetSelection.resolveTarget(target) != target) return null;
        return target;
    }

    private boolean passesItemConditions() {
        return ownShieldReleased || DeferredActionQueue.contains(this)
            || itemWhitelist.acceptsHeldItem() && hotbarWhitelist.permitsCurrentSlot();
    }

    private int findAxeSlot() {
        return InventoryItems.findStackInHotbar(InventoryItems::isAxe);
    }

    private void releaseOwnShield() {
        if (!mode.isSelected(modeOption2) || !unblockOwnShield.getValue() || ownShieldReleased) return;
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null || !passesItemConditions()) return;
        if (DeferredActionQueue.hasPendingActions() && !DeferredActionQueue.contains(this)) return;
        PlayerEntity target = findShieldTarget();
        if (target == null || !EntityInteractions.isBlocking() || AttackCooldownTracker.hasNonFireHurtWindow(target)) return;
        client.options.useKey.setPressed(false);
        EntityInteractions.stopUsingItem();
        ownShieldReleased = true;
        unblockTickPending = true;
    }

    private void onSwapEnded() {
        if (swapBack.getValue() && !returnTo.isSelected(returnToOption)) {
            int slot = returnTo.isSelected(returnToOption2) ? returnSlots.chooseSelectedSlot()
                : InventoryItems.findStackInHotbar(stack -> returnItems.isEntrySelected(stack.getItem()));
            if (slot != -1) InventoryItems.selectHotbarSlot(slot);
        }
        if (!mode.isSelected(modeOption2) || !ownShieldReleased) return;
        if (swapBack.getValue()) restoreShieldRequested = true;
        else ownShieldReleased = false;
    }

    private void resetShieldAttackTiming() {
        initializePlayerEntityClass();
        timedAttackTarget = null;
        attackDelayActive = false;
        switchDelayActive = false;
        initializeActionTimerClass();
        CooldownTimer firstTimer = attackTimer;
        initializeActionTimerClass();
        firstTimer.reset();
        initializeActionTimerClass();
        CooldownTimer secondTimer = switchTimer;
        initializeActionTimerClass();
        secondTimer.reset();
    }

    private void resetShieldBreakState() {
        selectedAxeSlot = -1;
        initializePlayerEntityClass();
        shieldTarget = null;
        clearStunFollowup();
        ownShieldReleased = false;
        restoreShieldRequested = false;
        unblockTickPending = false;
        Targets.shieldBreakActive = false;
        resetShieldAttackTiming();
    }

    private static void initializePlayerEntityClass() {
        try {
            Class.forName(PlayerEntity.class.getName(), true, PlayerEntity.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    private static void initializeActionTimerClass() {
        try {
            Class.forName(CooldownTimer.class.getName(), true, CooldownTimer.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

    public static void appendBootstrapSettings(ShieldBreaker Module) {
        CatalogSettings.append(Module, mode);
        CatalogSettings.append(Module, reactionDelay);
        CatalogSettings.append(Module, attackDelay);
        CatalogSettings.append(Module, autoSwap);
        CatalogSettings.append(Module, swapBack);
        CatalogSettings.append(Module, swapBackDelay);
        CatalogSettings.append(Module, returnTo);
        CatalogSettings.append(Module, returnSlots);
        CatalogSettings.append(Module, returnItems);
        CatalogSettings.append(Module, unblockOwnShield);
        CatalogSettings.append(Module, stun);
        CatalogSettings.append(Module, stunChance);
        CatalogSettings.append(Module, itemWhitelist);
        CatalogSettings.append(Module, hotbarWhitelist);
    }

    public static CharSequence bootstrapReturnToOption3() { return returnToOption3; }
    public static CharSequence bootstrapReturnToOption2() { return returnToOption2; }
    public static CharSequence bootstrapReturnToOption() { return returnToOption; }
    public static CharSequence bootstrapModeOption2() { return modeOption2; }
    public static CharSequence bootstrapModeOption() { return modeOption; }

    public static void assignBootstrapSettings(ShieldBreaker Module, BooleanSetting preparedUnblockOwnShield, HotbarSlotsSetting preparedHotbarWhitelist, HotbarSlotsSetting preparedReturnSlots, BooleanSetting preparedSwapBack, ChoiceSetting preparedReturnTo, ChoiceSetting preparedMode, FloatRangeSetting preparedAttackDelay, BooleanSetting preparedAutoSwap, FloatRangeSetting preparedReactionDelay, ItemListSetting preparedReturnItems, BooleanSetting preparedStun, IntSetting preparedStunChance, FloatRangeSetting preparedSwapBackDelay, ItemListSetting preparedItemWhitelist) {
        unblockOwnShield = preparedUnblockOwnShield;
        hotbarWhitelist = preparedHotbarWhitelist;
        returnSlots = preparedReturnSlots;
        swapBack = preparedSwapBack;
        returnTo = preparedReturnTo;
        mode = preparedMode;
        attackDelay = preparedAttackDelay;
        autoSwap = preparedAutoSwap;
        reactionDelay = preparedReactionDelay;
        returnItems = preparedReturnItems;
        stun = preparedStun;
        stunChance = preparedStunChance;
        swapBackDelay = preparedSwapBackDelay;
        itemWhitelist = preparedItemWhitelist;
    }
}
