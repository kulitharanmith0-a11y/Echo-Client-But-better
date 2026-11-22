package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.EchoClient;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.modules.ModuleManager;
import dev.dov.hack.echo.client.modules.hud.HudElement;
import dev.dov.hack.echo.client.hud.HudElementLayout;
import dev.dov.hack.echo.client.hud.HudElementType;
import dev.dov.hack.echo.client.hud.HudLayout;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.dov.hack.echo.client.render.font.FontAsset;
import dev.dov.hack.echo.client.init.CatalogText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class Hotbar extends HudElement {
    public Hotbar() {
        super(new ModuleInfo(CatalogText.literal("Hotbar"), CatalogText.literal("Nine live inventory slots and counts"), ModuleCategory.RENDER), HudElementType.HOTBAR);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.HOTBAR.defaultX();
        float y = HudElementType.HOTBAR.defaultY();
        hud.drawPanel(layout, x, y, HudElementType.HOTBAR.defaultWidth(), HudElementType.HOTBAR.defaultHeight(), 12.0f, 1.0f, 12.0f);
        float indicator = hud.getHotbarIndicatorX(x);
        float target = hud.getHotbarIndicatorTargetX(x);
        float stretch = Math.min(12.0f, Math.abs(indicator - target));
        float indicatorX = indicator >= target ? indicator - stretch : indicator;
        float indicatorWidth = 27.0f + stretch;
        layout.drawRoundedRect(indicatorX, y + 36.0f, indicatorWidth, 1.0f, 0.5f, 0x26ffffff, 1.0f);
        float visibleWidth = indicatorWidth * HudLayout.clampFraction(hud.getHotbarIndicatorProgress());
        if (visibleWidth > 0.001f) {
            layout.drawGlow(indicatorX, y + 36.0f, visibleWidth, 1.0f, 0.5f, 5.0f + stretch * 0.15f, 0x70ffffff, 1.0f);
            layout.drawRoundedRect(indicatorX, y + 36.0f, visibleWidth, 1.0f, 0.5f, -1275068417, 1.0f);
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = hud.getHotbarStack(slot);
            float slotX = x + 6.0f + 39.0f * HudLayout.slotIndex(slot, 9);
            if (stack == null || stack.isEmpty()) {
                String label = HudRenderer.HOTBAR_SLOT_LABELS[slot];
                float width = layout.textWidth(FontAsset.INTER_SEMI_BOLD, label, 9.5f);
                float height = layout.fontHeight(FontAsset.INTER_SEMI_BOLD, 9.5f);
                layout.drawText(FontAsset.INTER_SEMI_BOLD, label, slotX + (37.0f - width) * 0.5f, y + (44.0f - height) * 0.5f, 9.5f, 0x73ffffff, 1.0f);
                continue;
            }
            layout.drawItem(stack, slotX + 10.5f, HudLayout.hotbarItemY(y), 16.0f, slot + 1);
            if (stack.getCount() > 1) {
                layout.drawItemCount("x" + stack.getCount(), x + 26.0f + 39.0f * HudLayout.slotIndex(slot, 9), y + 26.0f, 9.5f, -419430401);
            }
        }
    }

    @Override
    protected boolean shouldRender() {
        return canRenderHotbar();
    }

    public static boolean replacesVanillaHotbar() {
        boolean enabled = ModuleManager.find(Hotbar.class)
                .map(Module -> Module.isEnabled() && ((Hotbar) Module).canRenderHotbar())
                .orElse(false);
        boolean replacing = enabled && EchoClient.uiRenderer != null && HudRenderer.INSTANCE.isInitialized();
        HudRenderer.INSTANCE.updateHotbarReplacement(replacing);
        return replacing;
    }

    boolean canRenderHotbar() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null && client.player != null && !client.player.isSpectator()
                && client.getCameraEntity() instanceof PlayerEntity
                && MinecraftClient.getInstance().currentScreen == null
                && !MinecraftClient.getInstance().options.hudHidden;
    }
}
