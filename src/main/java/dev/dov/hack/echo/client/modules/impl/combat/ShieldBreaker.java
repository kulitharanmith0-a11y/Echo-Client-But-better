package dev.dov.hack.echo.client.modules.impl.combat;

import static dev.dov.hack.echo.client.init.CatalogText.literal;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.AttackKeyEvent;
import dev.dov.hack.echo.client.event.input.HotbarKeyEvent;
import dev.dov.hack.echo.client.event.input.UseKeyEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.modules.internals.Targets;
import dev.dov.hack.echo.client.handlers.AttackCooldownTracker;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.keyboard.KeybindingCache;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatRangeSetting;
import dev.dov.hack.echo.client.settings.HotbarSlotsSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.utils.CooldownTimer;
import dev.dov.hack.echo.client.utils.interaction.EntityInteractions;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import dev.dov.hack.echo.client.utils.targeting.EntityPredicates;
import dev.dov.hack.echo.client.utils.targeting.TargetSelection;
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
    public static ItemListSetting itemWhitelist;
    public static HotbarSlotsSetting hotbarWhitelist;
    private CooldownTimer attackTimer;
    private CooldownTimer switchTimer;
    private int selectedAxeSlot;
    private PlayerEntity shieldTarget;
    private PlayerEntity previousShieldTarget;
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

    @Subscribe(priority = EventPriority.LOWEST)
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || MinecraftRenderState.getScreen() != null) return;
        if (unblockTickPending) {
            unblockTickPending = false;
            Targets.shieldBreakActive = false;
            event.setClickCount(0);
            return;
        }
        if (previousShieldTarget != null) {
            previousShieldTarget = null;
            if (!Targets.shieldBreakActive && !event.hasClicks()) event.addClick();
            Targets.shieldBreakActive = false;
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
        if (stun.getValue()) previousShieldTarget = target;
        attackDelayActive = true;
        switchTimer.reset();
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
        initializePlayerEntityClass();
        previousShieldTarget = null;
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
        CatalogSettings.append(Module, itemWhitelist);
        CatalogSettings.append(Module, hotbarWhitelist);
    }

    public static CharSequence bootstrapReturnToOption3() { return returnToOption3; }
    public static CharSequence bootstrapReturnToOption2() { return returnToOption2; }
    public static CharSequence bootstrapReturnToOption() { return returnToOption; }
    public static CharSequence bootstrapModeOption2() { return modeOption2; }
    public static CharSequence bootstrapModeOption() { return modeOption; }

    public static void assignBootstrapSettings(ShieldBreaker Module, BooleanSetting preparedUnblockOwnShield, HotbarSlotsSetting preparedHotbarWhitelist, HotbarSlotsSetting preparedReturnSlots, BooleanSetting preparedSwapBack, ChoiceSetting preparedReturnTo, ChoiceSetting preparedMode, FloatRangeSetting preparedAttackDelay, BooleanSetting preparedAutoSwap, FloatRangeSetting preparedReactionDelay, ItemListSetting preparedReturnItems, BooleanSetting preparedStun, FloatRangeSetting preparedSwapBackDelay, ItemListSetting preparedItemWhitelist) {
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
        swapBackDelay = preparedSwapBackDelay;
        itemWhitelist = preparedItemWhitelist;
    }
}
