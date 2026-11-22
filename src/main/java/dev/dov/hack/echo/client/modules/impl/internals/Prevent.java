package dev.dov.hack.echo.client.modules.impl.internals;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.InputPacketEvent;
import dev.dov.hack.echo.client.event.interaction.AttackAttemptEvent;
import dev.dov.hack.echo.client.event.interaction.BlockBreakingEvent;
import dev.dov.hack.echo.client.event.interaction.HandInteractEvent;
import dev.dov.hack.echo.client.event.interaction.ItemUseEvent;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.impl.utility.AutoTotem;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.settings.list.RegistryEntryGroup;
import dev.dov.hack.echo.client.utils.items.InventoryItems;
import dev.dov.hack.echo.client.utils.items.ItemGroups;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class Prevent extends Module {
    private static BooleanSetting noAirHits;
    private static ItemListSetting preventWithItemList;
    private static BooleanSetting noMining;
    private static ItemListSetting preventWithItemList2;
    private static BooleanSetting anchorCharge;
    private static BooleanSetting eChestOpen;
    private static BooleanSetting onObsidian;
    public static BooleanSetting untotem;
    public static BooleanSetting inInventory;
    public static BooleanSetting inHotbar;
    private static BooleanSetting noShieldWithBlocks;
    private static String noAirHitsTooltip;
    private static String airItemsTooltip;
    private static String noMiningTooltip;
    private static String miningItemsTooltip;
    private static String anchorTooltip;
    private static String interactionTooltip;
    private static String untotemTooltip;
    private static String totemLocationsTooltip;

    public Prevent() {
        super(new dev.dov.hack.echo.client.modules.ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Prevent"), dev.dov.hack.echo.client.init.CatalogText.literal("Prevents certain actions"), dev.dov.hack.echo.client.modules.ModuleCategory.INTERNALS));
    }

    @Override
    public void postInitialize() {
        preventWithItemList.setVisibilityPredicate(ignored -> noAirHits.getValue());
        preventWithItemList2.setVisibilityPredicate(ignored -> noMining.getValue());
        onObsidian.setVisibilityPredicate(ignored -> eChestOpen.getValue());
        inInventory.setVisibilityPredicate(ignored -> untotem.getValue());
        inHotbar.setVisibilityPredicate(ignored -> untotem.getValue());
        if (noAirHitsTooltip == null) noAirHitsTooltip = "Attacking";
        noAirHits.tooltip(noAirHitsTooltip);
        if (airItemsTooltip == null) airItemsTooltip = "Attacking";
        preventWithItemList.tooltip(airItemsTooltip);
        if (noMiningTooltip == null) noMiningTooltip = "Mining";
        noMining.tooltip(noMiningTooltip);
        if (miningItemsTooltip == null) miningItemsTooltip = "Mining";
        preventWithItemList2.tooltip(miningItemsTooltip);
        if (anchorTooltip == null) anchorTooltip = "Interact";
        anchorCharge.tooltip(anchorTooltip);
        if (interactionTooltip == null) interactionTooltip = "Interact";
        eChestOpen.tooltip(interactionTooltip);
        onObsidian.tooltip(interactionTooltip);
        noShieldWithBlocks.tooltip(interactionTooltip);
        if (untotemTooltip == null) untotemTooltip = "Totem";
        untotem.tooltip(untotemTooltip);
        if (totemLocationsTooltip == null) totemLocationsTooltip = "Totem";
        inInventory.tooltip(totemLocationsTooltip);
        inHotbar.tooltip(totemLocationsTooltip);
    }

    @Subscribe
    private void preventAirAttack(AttackAttemptEvent event) {
        if (isOutsideWorld() || client.crosshairTarget == null) return;
        if (client.crosshairTarget.getType() != HitResult.Type.MISS) return;
        if (!noAirHits.getValue() || isAllowedItemContext(preventWithItemList)) return;
        event.cancel();
    }

    @Subscribe
    private void preventBlockAttack(AttackAttemptEvent event) {
        if (isOutsideWorld() || client.crosshairTarget == null) return;
        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (!noMining.getValue() || isAllowedItemContext(preventWithItemList2)) return;
        event.cancel();
    }

    @Subscribe
    private void preventBlockBreaking(BlockBreakingEvent.Pre event) {
        if (isOutsideWorld() || !noMining.getValue()) return;
        if (isAllowedItemContext(preventWithItemList2)) return;
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        event.cancel();
    }

    @Subscribe
    private void preventExtraAnchorCharge(ItemUseEvent.Pre event) {
        if (isOutsideWorld() || !anchorCharge.getValue()) return;
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos position = hit.getBlockPos();
            if (client.world.getBlockState(position).getBlock() == Blocks.RESPAWN_ANCHOR
                    && client.world.getBlockState(position).get(RespawnAnchorBlock.CHARGES) != 0
                    && InventoryItems.holdsInMainHand(Items.GLOWSTONE)) {
                event.cancel();
            }
        }
    }

    @Subscribe
    private void preventEnderChestOpen(ItemUseEvent.Pre event) {
        if (isOutsideWorld() || !eChestOpen.getValue()) return;
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos position = hit.getBlockPos();
            if (client.world.getBlockState(position).getBlock() != Blocks.ENDER_CHEST) return;
            if (onObsidian.getValue()) {
                if (client.world.getBlockState(position.down()).getBlock() == Blocks.OBSIDIAN) event.cancel();
            } else {
                event.cancel();
            }
        }
    }

    @Subscribe
    private void preventShieldUseWithBlocks(HandInteractEvent.Pre event) {
        if (isOutsideWorld() || !noShieldWithBlocks.getValue()) return;
        if (event.getHand() != Hand.OFF_HAND) return;
        if (!client.player.getOffHandStack().isOf(Items.SHIELD)) return;
        if (!(client.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        event.cancel();
    }

    @Subscribe
    private void preventTotemHotbarRemoval(InputPacketEvent event) {
        if (isOutsideWorld() || !untotem.getValue() || !inHotbar.getValue()) return;
        AutoTotem autoTotem = (AutoTotem) EchoClient.ModuleManager.findFeature(AutoTotem.class);
        if (autoTotem != null && autoTotem.shouldAllowTotemRemoval()) return;
        if (client.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) event.cancel();
    }

    private boolean isAllowedItemContext(ItemListSetting preventedItems) {
        if (preventedItems.getSelectedCount() == 0) return false;
        ItemStack stack = InventoryItems.mainHandStack();
        Item item = stack.isEmpty() ? Items.AIR : stack.getItem();
        if (preventedItems.isEntrySelected(item)) return false;
        if (ItemGroups.swords().matches(item) && hasSelectedItemInGroup(preventedItems, ItemGroups.swords())) return false;
        if (ItemGroups.axes().matches(item) && hasSelectedItemInGroup(preventedItems, ItemGroups.axes())) return false;
        if (ItemGroups.mace().matches(item) && hasSelectedItemInGroup(preventedItems, ItemGroups.mace())) return false;
        if (item == Items.AIR && hasSelectedItemInGroup(preventedItems, ItemGroups.fists())) return false;
        return true;
    }

    private boolean hasSelectedItemInGroup(ItemListSetting selectedItems, RegistryEntryGroup<Item> group) {
        for (Item item : Registries.ITEM) {
            if (group.matches(item) && selectedItems.isEntrySelected(item)) return true;
        }
        return false;
    }

    
    public static void appendBootstrapSettings(Prevent Module) {
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.noAirHits);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.preventWithItemList);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.noMining);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.preventWithItemList2);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.anchorCharge);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.eChestOpen);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.onObsidian);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.untotem);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.inInventory);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.inHotbar);
        dev.dov.hack.echo.client.init.CatalogSettings.append(Module, Prevent.noShieldWithBlocks);
    }

    
    public static void assignBootstrapSettings(Prevent Module, dev.dov.hack.echo.client.settings.ItemListSetting preparedPreventWithItemList, dev.dov.hack.echo.client.settings.BooleanSetting preparedNoAirHits, dev.dov.hack.echo.client.settings.BooleanSetting preparedNoShieldWithBlocks, dev.dov.hack.echo.client.settings.BooleanSetting preparedInHotbar, dev.dov.hack.echo.client.settings.BooleanSetting preparedUntotem, dev.dov.hack.echo.client.settings.BooleanSetting preparedOnObsidian, dev.dov.hack.echo.client.settings.ItemListSetting preparedPreventWithItemList2, dev.dov.hack.echo.client.settings.BooleanSetting preparedNoMining, dev.dov.hack.echo.client.settings.BooleanSetting preparedEChestOpen, dev.dov.hack.echo.client.settings.BooleanSetting preparedInInventory, dev.dov.hack.echo.client.settings.BooleanSetting preparedAnchorCharge) {
        Prevent.preventWithItemList = preparedPreventWithItemList;
        Prevent.noAirHits = preparedNoAirHits;
        Prevent.noShieldWithBlocks = preparedNoShieldWithBlocks;
        Prevent.inHotbar = preparedInHotbar;
        Prevent.untotem = preparedUntotem;
        Prevent.onObsidian = preparedOnObsidian;
        Prevent.preventWithItemList2 = preparedPreventWithItemList2;
        Prevent.noMining = preparedNoMining;
        Prevent.eChestOpen = preparedEChestOpen;
        Prevent.inInventory = preparedInInventory;
        Prevent.anchorCharge = preparedAnchorCharge;
    }
}
