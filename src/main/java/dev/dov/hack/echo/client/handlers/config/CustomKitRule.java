package dev.dov.hack.echo.client.handlers.config;

import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.settings.ItemListSetting;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class CustomKitRule {
    private final String id;
    private String name;
    private boolean enabled;
    private boolean matchAll;
    private String configName;
    private final List<String> itemIds;
    private final ItemListSetting itemListSetting;

    public CustomKitRule(String id, String name) {
        this.id = id;
        this.name = name;
        this.enabled = false;
        this.matchAll = true;
        this.configName = "";
        this.itemIds = new ArrayList<>();
        this.itemListSetting = new ItemListSetting(CatalogText.literal(name + " Items"));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    public void setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
    }

    public String getConfigName() {
        return configName == null ? "" : configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName == null ? "" : configName.trim();
    }

    public ItemListSetting getItemListSetting() {
        return itemListSetting;
    }

    public List<String> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<String> ids) {
        itemIds.clear();
        if (ids != null) {
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (String entry : ids) {
                if (entry != null && !entry.isBlank()) {
                    unique.add(entry.trim());
                }
            }
            itemIds.addAll(unique);
        }
        syncToSetting();
    }

    public void syncToSetting() {
        List<Identifier> parsed = new ArrayList<>(itemIds.size());
        for (String raw : itemIds) {
            Identifier identifier = Identifier.tryParse(raw);
            if (identifier != null) {
                parsed.add(identifier);
            }
        }
        itemListSetting.replaceSelectedIds(parsed);
    }

    public void syncFromSetting() {
        itemIds.clear();
        for (Identifier identifier : itemListSetting.getSelectedIds()) {
            if (identifier != null) {
                itemIds.add(identifier.toString());
            }
        }
    }

    public boolean containsItem(String itemId) {
        return itemId != null && itemIds.contains(itemId);
    }

    public void toggleItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        if (itemIds.contains(itemId)) {
            itemIds.remove(itemId);
        } else {
            itemIds.add(itemId);
        }
        syncToSetting();
    }

    public void clearItems() {
        itemIds.clear();
        itemListSetting.replaceSelectedIds(List.of());
    }

    public int captureHotbar(ClientPlayerEntity player) {
        if (player == null) {
            return 0;
        }
        LinkedHashSet<String> captured = new LinkedHashSet<>();
        for (int slot = 0; slot < 9; slot++) {
            addStackId(captured, player.getInventory().getStack(slot));
        }
        addStackId(captured, player.getOffHandStack());
        itemIds.clear();
        itemIds.addAll(captured);
        syncToSetting();
        return itemIds.size();
    }

    private static void addStackId(Set<String> target, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.isOf(Items.AIR)) {
            return;
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id != null) {
            target.add(id.toString());
        }
    }

    public boolean matches(Set<String> hotbarIds) {
        if (itemIds.isEmpty() || hotbarIds == null || hotbarIds.isEmpty()) {
            return false;
        }
        if (matchAll) {
            for (String required : itemIds) {
                if (!hotbarIds.contains(required)) {
                    return false;
                }
            }
            return true;
        }
        for (String required : itemIds) {
            if (hotbarIds.contains(required)) {
                return true;
            }
        }
        return false;
    }

    public String itemsSummary(int maxShown) {
        if (itemIds.isEmpty()) {
            return "No items selected";
        }
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(maxShown, itemIds.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(displayNameForId(itemIds.get(i)));
        }
        if (itemIds.size() > shown) {
            sb.append(" +").append(itemIds.size() - shown);
        }
        return sb.toString();
    }

    public static String displayNameForId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return "";
        }
        Identifier parsed = Identifier.tryParse(rawId);
        if (parsed != null && Registries.ITEM.containsId(parsed)) {
            Item item = Registries.ITEM.get(parsed);
            if (item != Items.AIR) {
                return new ItemStack(item).getName().getString();
            }
        }
        int colon = rawId.indexOf(':');
        String base = colon >= 0 ? rawId.substring(colon + 1) : rawId;
        return base.replace('_', ' ');
    }
}
