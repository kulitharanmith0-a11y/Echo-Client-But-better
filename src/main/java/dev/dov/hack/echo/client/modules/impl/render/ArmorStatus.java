package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.modules.hud.HudElement;
import dev.dov.hack.echo.client.hud.HudElementLayout;
import dev.dov.hack.echo.client.hud.HudElementType;
import dev.dov.hack.echo.client.hud.HudLayout;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.init.CatalogText;
import net.minecraft.item.ItemStack;

public class ArmorStatus extends HudElement {
    public ArmorStatus() {
        super(new ModuleInfo(CatalogText.literal("Armor Status"), CatalogText.literal("Equipped helmet, chest, legs and boots"), ModuleCategory.RENDER), HudElementType.ARMOR_STATUS);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.ARMOR_STATUS.defaultX();
        float y = HudElementType.ARMOR_STATUS.defaultY();
        hud.drawPanel(layout, x, y, HudElementType.ARMOR_STATUS.defaultWidth(), HudElementType.ARMOR_STATUS.defaultHeight(), 12.0f, 1.0f, 14.0f);
        for (int separator = 0; separator < 3; separator++) {
            float separatorX = x + 43.0f + 39.0f * HudLayout.slotIndex(separator, 3);
            layout.drawRoundedRect(separatorX, y + 17.0f, 1.0f, 10.0f, 0.0f, 788529151, 1.0f);
        }
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = hud.getEquippedArmor(slot);
            float slotX = x + 8.0f + 39.0f * HudLayout.slotIndex(slot, 4);
            if (stack == null || stack.isEmpty()) {
                float glyphWidth = layout.textWidth(FontAsset.INTER_MULTIPLY, "×", 11.0f);
                float glyphHeight = layout.fontHeight(FontAsset.INTER_MULTIPLY, 11.0f);
                layout.drawText(FontAsset.INTER_MULTIPLY, "×", slotX + (32.0f - glyphWidth) * 0.5f, y + (44.0f - glyphHeight) * 0.5f, 11.0f, 1509949439, 1.0f);
            } else {
                layout.drawItem(stack, slotX + 8.0f, HudLayout.armorItemY(y), 16.0f, 20 + slot);
            }
        }
    }
}
