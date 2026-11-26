package com.blamejared.clumps.client.modules.impl.combat;

import static com.blamejared.clumps.client.init.CatalogText.literal;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.AttackKeyEvent;
import com.blamejared.clumps.client.event.input.HotbarKeyEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.DeferredActionQueue;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.FloatRangeSetting;
import com.blamejared.clumps.client.utils.items.InventoryItems;
import com.blamejared.clumps.client.utils.targeting.TargetSelection;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;

public class TotemHit extends Module {
    public static BooleanSetting silentSwap;
    public static FloatRangeSetting delay;
    boolean hotbarActionActive;
    private int knockbackSwordSlot;
    private boolean swordSwapPending;
    private long lastSwordSwapTick;
    private static final String TIMING_SECTION = "Timing";

    public TotemHit() {
        this(new ModuleInfo(literal("Totem Hit"), literal("Swaps to knockback sword when attacking with totem"), ModuleCategory.COMBAT));
    }

    public TotemHit(ModuleInfo info) {
        super(info);
    }

    @Override
    public void postInitialize() {
        hotbarActionActive = false;
        knockbackSwordSlot = -1;
        swordSwapPending = false;
        lastSwordSwapTick = -1L;
        delay.setVisibilityPredicate(ignored -> !silentSwap.getValue());
        delay.tooltip(TIMING_SECTION);
    }

    private int findKnockbackSword() {
        return InventoryItems.findItemWithEnchantmentInHotbar(
            stack -> InventoryItems.isLegacySword(stack.getItem()), Enchantments.KNOCKBACK, false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        hotbarActionActive = false;
        knockbackSwordSlot = -1;
        swordSwapPending = false;
        lastSwordSwapTick = -1L;
        DeferredActionQueue.finish(this, true);
    }

    @Subscribe
    private void onHotbarKey(HotbarKeyEvent event) {
        if (knockbackSwordSlot == -1 || event.slot != knockbackSwordSlot) return;
        if (client.player == null || (long) client.player.age - lastSwordSwapTick > 2L) {
            resetSwordSwap();
            return;
        }
        knockbackSwordSlot = -1;
        int delayTicks = silentSwap.getValue() ? 0 : (int) delay.sampleGaussian();
        if (!DeferredActionQueue.select(this, event.slot, delayTicks, true, () -> hotbarActionActive = false)) {
            resetSwordSwap();
            return;
        }
        event.addClick();
    }

    @Subscribe(priority = EventPriority.LOW)
    private void onAttackKey(AttackKeyEvent event) {
        if (isOutsideWorld() || client.isPaused()) return;
        if (swordSwapPending) {
            boolean recent = (long) client.player.age - lastSwordSwapTick <= 2L;
            resetSwordSwap();
            if (recent && InventoryItems.isLegacySword(client.player.getMainHandStack().getItem())) {
                if (!event.hasClicks()) event.addClick();
                hotbarActionActive = true;
            }
            return;
        }
        if (!event.hasClicks() || client.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) return;
        if (!(client.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)) return;
        if (!TargetSelection.isValidTarget(target)) return;
        int slot = findKnockbackSword();
        if (slot == -1 || DeferredActionQueue.contains(this)) return;
        if (silentSwap.getValue()) {
            if (performSilentSwapAttack(target, slot)) {
                event.setClickCount(0);
            }
            return;
        }
        event.setClickCount(0);
        knockbackSwordSlot = slot;
        swordSwapPending = true;
        lastSwordSwapTick = client.player.age;
    }

    private boolean performSilentSwapAttack(LivingEntity target, int swordSlot) {
        if (client.player == null || client.interactionManager == null) {
            return false;
        }
        ActionCoordinator coordinator = actions();
        if (coordinator == null || !coordinator.canSelectSlot(this, swordSlot, false, false)) {
            return false;
        }
        int totemSlot = client.player.getInventory().getSelectedSlot();
        if (totemSlot == swordSlot) {
            return false;
        }
        hotbarActionActive = true;
        try {
            if (!coordinator.executeSlotSelect(this, swordSlot, false).succeeded()) {
                return false;
            }
            return coordinator.executeEntityAttack(this, target, true).succeeded();
        } finally {
            if (client.player.getInventory().getSelectedSlot() != totemSlot) {
                client.player.getInventory().setSelectedSlot(totemSlot);
            }
            if (client.interactionManager != null) {
                ((ClientPlayerInteractionManagerAccessor) client.interactionManager).invokeSyncSelectedSlot();
            }
            hotbarActionActive = false;
        }
    }

    private void resetSwordSwap() {
        knockbackSwordSlot = -1;
        swordSwapPending = false;
        lastSwordSwapTick = -1L;
    }

    public static void appendBootstrapSettings(TotemHit Module) {
        CatalogSettings.append(Module, silentSwap);
        CatalogSettings.append(Module, delay);
    }

    public static void assignBootstrapSettings(TotemHit Module, FloatRangeSetting preparedDelay, BooleanSetting preparedSilentSwap) {
        delay = preparedDelay;
        silentSwap = preparedSilentSwap;
    }
}
