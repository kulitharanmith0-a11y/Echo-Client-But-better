package dev.dov.hack.echo.client.modules.impl.combat;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.interaction.AttackAttemptEvent;
import dev.dov.hack.echo.client.event.interaction.HandSwingEvent;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.handlers.DeferredActionQueue;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.HotbarSlotsSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.utils.CooldownTimer;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import dev.dov.hack.echo.client.utils.items.ItemProperties;
import dev.dov.hack.echo.client.utils.targeting.EntityRaycasts;
import dev.dov.hack.echo.client.utils.targeting.WeaponAttributes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.hit.EntityHitResult;

public class SpearReach extends Module {
    private static BooleanSetting paperMCSupport;
    private static ItemListSetting itemWhitelist;
    private static HotbarSlotsSetting hotbarWhitelist;
    private static final int SWAP_DURATION_TICKS = 1;
    private CooldownTimer swapTimer;
    private boolean awaitingRestore;
    private static String[] sharedControlState;

    static {
        writeSharedControlState(null);
    }

    public SpearReach() {
        super(new dev.dov.hack.echo.client.modules.ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Spear Reach"), dev.dov.hack.echo.client.init.CatalogText.literal("Swaps to spears for increased reach"), dev.dov.hack.echo.client.modules.ModuleCategory.COMBAT));
    }

    @Override
    public void postInitialize() {
        swapTimer = new CooldownTimer();
        awaitingRestore = false;
        itemWhitelist.tooltip("Conditions");
        hotbarWhitelist.tooltip("Conditions");
    }

    private int findSpearSlot() {
        try {
            Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        if (client.player == null) return -1;
        return InventoryItems.findStackInHotbar(SpearReach::isSelectableSpear);
    }

    private static boolean isSelectableSpear(ItemStack stack) {
        String[] controlState = readSharedControlState();
        if (controlState == null && stack.isEmpty()) return false;
        try {
            Class.forName(Item.class.getName(), true, Item.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        if (!InventoryItems.isSpear(stack.getItem())) return false;
        try {
            Class.forName(ItemStack.class.getName(), true, ItemStack.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        try {
            Class.forName(RegistryKey.class.getName(), true, RegistryKey.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        boolean hasLunge = ItemProperties.hasEnchantment(stack, Enchantments.LUNGE);
        return controlState != null ? hasLunge : !hasLunge;
    }

    private boolean isHoldingSelectableSpear() {
        String[] controlState = readSharedControlState();
        if (controlState == null && client.player == null) return false;
        ItemStack stack = client.player.getMainHandStack();
        if (controlState == null && stack.isEmpty()) return false;
        if (!InventoryItems.isSpear(stack.getItem())) return false;
        boolean hasLunge = ItemProperties.hasEnchantment(stack, Enchantments.LUNGE);
        return controlState != null ? hasLunge : !hasLunge;
    }

    @Subscribe
    public final void onAttackAttempt(AttackAttemptEvent event) {
        String[] controlState = readSharedControlState();
        if (controlState == null && findExtendedReachTarget() == null) return;
        int spearSlot = findSpearSlot();
        if (controlState == null) {
            if (spearSlot == -1) return;
            int selectedSlot = client.player.getInventory().getSelectedSlot();
            if (selectedSlot == spearSlot) return;
            if (!DeferredActionQueue.select(this, spearSlot, SWAP_DURATION_TICKS, true, () -> awaitingRestore = false)) return;
        } else if (spearSlot == 0) {
            return;
        }
        swapTimer.reset();
        awaitingRestore = true;
        event.cancel();
        WeaponAttributes.performPiercingAttack();
    }

    @Subscribe
    public void onHandSwing(HandSwingEvent event) {
        if (paperMCSupport.getValue() && event.getPlayer() == client.player) {
            WeaponAttributes.recordSwingTick(client.player);
        }
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        String[] controlState = readSharedControlState();
        if (controlState == null) {
            if (isOutsideWorld()) return;
            if (!awaitingRestore) return;
            if (!swapTimer.hasElapsedTicks(SWAP_DURATION_TICKS)) return;
            DeferredActionQueue.finish(this);
        }
        awaitingRestore = false;
    }

    
    public ItemStack getReachStack() {
        String[] controlState = readSharedControlState();
        boolean gate = isEnabled();
        if (controlState == null) {
            if (!gate) return ItemStack.EMPTY;
            gate = paperMCSupport.getValue();
            if (!gate) return ItemStack.EMPTY;
            gate = isOutsideWorld();
        }
        if (gate) return ItemStack.EMPTY;
        int slot = findSpearSlot();
        return slot == -1 ? ItemStack.EMPTY : client.player.getInventory().getStack(slot);
    }

    @Override
    public void onDisable() {
        DeferredActionQueue.finish(this);
        awaitingRestore = false;
    }

    public EntityHitResult findExtendedReachTarget() {
        String[] controlState = readSharedControlState();
        if (controlState == null && !isEnabled()) return null;
        if (isOutsideWorld()) return null;
        if (MinecraftClient.getInstance().currentScreen != null) return null;
        boolean gate = client.isPaused();
        int slot;
        if (controlState == null) {
            if (gate) return null;
            if (isHoldingSelectableSpear()) return null;
            if (!itemWhitelist.acceptsHeldItem()) return null;
            if (!hotbarWhitelist.permitsCurrentSlot()) return null;
            if (EntityRaycasts.hasNormalReachTarget(client.player)) return null;
            slot = findSpearSlot();
        } else {
            slot = gate ? 1 : 0;
        }
        if (slot == -1) return null;
        ItemStack spear = client.player.getInventory().getStack(slot);
        EntityHitResult hit = EntityRaycasts.raycastWithStackRange(client.player, spear);
        if (hit == null) return null;
        boolean paperSupport = paperMCSupport.getValue();
        if (controlState != null) {
            if (paperSupport) return null;
        } else if (paperSupport
                && WeaponAttributes.remainingCooldownSeconds(client.player, spear, 1.0f) > 0.0) {
            return null;
        }
        return hit;
    }

    public static boolean hasPendingSwap() {
        try {
            Class.forName(Class.class.getName(), true, Class.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        return DeferredActionQueue.contains(SpearReach.class);
    }

    
    public static String[] readSharedControlState() { return sharedControlState; }
    public static void writeSharedControlState(String[] value) { sharedControlState = value; }

    
    public static void appendBootstrapSettings(SpearReach Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearReach.paperMCSupport);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearReach.itemWhitelist);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, SpearReach.hotbarWhitelist);
    }

    
    public static void assignBootstrapSettings(SpearReach Module, dev.dov.hack.echo.client.settings.BooleanSetting preparedPaperMCSupport, dev.dov.hack.echo.client.settings.HotbarSlotsSetting preparedHotbarWhitelist, dev.dov.hack.echo.client.settings.ItemListSetting preparedItemWhitelist) {
        SpearReach.paperMCSupport = preparedPaperMCSupport;
        SpearReach.hotbarWhitelist = preparedHotbarWhitelist;
        SpearReach.itemWhitelist = preparedItemWhitelist;
    }
}
