package dev.dov.hack.echo.client.settings.specialized;

import dev.dov.hack.echo.client.modules.impl.utility.FastUse;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.settings.list.RegistryEntryGroup;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;

public class FastUseItemListSetting extends ItemListSetting {
    public FastUseItemListSetting(CharSequence label) {
        super(label);
    }

    @Override
    public List<RegistryEntryGroup<Item>> getGroups() {
        return List.of(
                new RegistryEntryGroup<>(FastUse.BLOCKS_GROUP, FastUseItemListSetting::isBlock), new RegistryEntryGroup<>(FastUse.FOOD_GROUP, FastUseItemListSetting::isFood), new RegistryEntryGroup<>(FastUse.POTIONS_GROUP, FastUseItemListSetting::isPotion), new RegistryEntryGroup<>(FastUse.ENDER_PEARLS_GROUP, FastUseItemListSetting::isEnderPearl), new RegistryEntryGroup<>(FastUse.THROWABLES_GROUP, FastUseItemListSetting::isThrowable), new RegistryEntryGroup<>(FastUse.BUCKETS_GROUP, FastUseItemListSetting::isFilledBucket), new RegistryEntryGroup<>(FastUse.FIREWORKS_GROUP, FastUseItemListSetting::isFirework));
    }

    private static boolean isBlock(Item item) {
        return item instanceof BlockItem;
    }

    private static boolean isFood(Item item) {
        return new ItemStack(item).contains(DataComponentTypes.FOOD);
    }

    private static boolean isPotion(Item item) {
        return item instanceof PotionItem;
    }

    private static boolean isEnderPearl(Item item) {
        return item instanceof EnderPearlItem;
    }

    private static boolean isThrowable(Item item) {
        return item == Items.SNOWBALL || item == Items.EGG
                || item == Items.WIND_CHARGE || item == Items.EXPERIENCE_BOTTLE;
    }

    private static boolean isFilledBucket(Item item) {
        return item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET
                || item == Items.MILK_BUCKET || item == Items.POWDER_SNOW_BUCKET;
    }

    private static boolean isFirework(Item item) {
        return item == Items.FIREWORK_ROCKET;
    }
}
