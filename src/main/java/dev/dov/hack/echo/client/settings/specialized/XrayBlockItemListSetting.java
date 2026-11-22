package dev.dov.hack.echo.client.settings.specialized;

import dev.dov.hack.echo.client.modules.impl.render.Xray;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import dev.dov.hack.echo.client.settings.list.RegistryEntryGroup;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;

public class XrayBlockItemListSetting extends ItemListSetting {
    public XrayBlockItemListSetting(CharSequence label) {
        super(label);
        for (Item item : Xray.getDefaultItems()) {
            setEntrySelected(item, true);
        }
    }

    @Override
    public Predicate<Item> getEntryFilter() {
        return item -> item instanceof BlockItem;
    }

    @Override
    public List<RegistryEntryGroup<Item>> getGroups() {
        return Xray.getOreGroups();
    }
}
