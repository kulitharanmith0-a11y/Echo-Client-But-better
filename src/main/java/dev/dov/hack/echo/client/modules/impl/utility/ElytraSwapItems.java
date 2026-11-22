package dev.dov.hack.echo.client.modules.impl.utility;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ElytraSwapItems {
    private ElytraSwapItems() {}

    public static boolean isElytra(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA;
    }
}
