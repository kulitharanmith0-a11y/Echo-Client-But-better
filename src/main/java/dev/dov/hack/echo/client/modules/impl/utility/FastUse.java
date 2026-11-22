package dev.dov.hack.echo.client.modules.impl.utility;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.interaction.ItemUseEvent;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.mixin.accessor.MinecraftClientAccessor;
import dev.dov.hack.echo.client.settings.FloatRangeSetting;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.settings.specialized.FastUseItemListSetting;
import net.minecraft.item.Item;

public class FastUse extends Module {
    public static final CharSequence BLOCKS_GROUP = CatalogText.literal("Blocks");
    public static final CharSequence FOOD_GROUP = CatalogText.literal("Food");
    public static final CharSequence POTIONS_GROUP = CatalogText.literal("Potions");
    public static final CharSequence ENDER_PEARLS_GROUP = CatalogText.literal("Ender Pearls");
    public static final CharSequence THROWABLES_GROUP = CatalogText.literal("Throwables");
    public static final CharSequence BUCKETS_GROUP = CatalogText.literal("Buckets");
    public static final CharSequence FIREWORKS_GROUP = CatalogText.literal("Fireworks");
    public static FloatRangeSetting delay;
    public static ItemListSetting items = new FastUseItemListSetting(CatalogText.literal("Items"));

    public FastUse() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Fast Use"), dev.dov.hack.echo.client.init.CatalogText.literal("Faster item use"), dev.dov.hack.echo.client.modules.ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        delay.tooltip(CatalogText.literal("Timing"));
    }

    @Subscribe
    private void onItemUsePost(ItemUseEvent.Post event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.interactionManager == null || client.interactionManager.isBreakingBlock()) {
            return;
        }
        if (items.getSelectedCount() > 0) {
            Item mainHandItem = client.player.getMainHandStack().getItem();
            Item offHandItem = client.player.getOffHandStack().getItem();
            if (!items.isEntrySelected(mainHandItem) && !items.isEntrySelected(offHandItem)) {
                return;
            }
        }
        ((MinecraftClientAccessor) client).setItemUseCooldown((int) delay.sampleUniform());
    }

}
