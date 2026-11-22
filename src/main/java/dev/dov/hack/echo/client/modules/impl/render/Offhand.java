package dev.dov.hack.echo.client.modules.impl.render;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.modules.ModuleManager;
import dev.dov.hack.echo.client.modules.hud.HudElement;
import dev.dov.hack.echo.client.hud.HudElementLayout;
import dev.dov.hack.echo.client.hud.HudElementType;
import dev.dov.hack.echo.client.hud.HudRenderer;
import dev.dov.hack.echo.client.init.CatalogText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

public class Offhand extends HudElement {
    public Offhand() {
        super(new ModuleInfo(CatalogText.literal("Offhand"), CatalogText.literal("Live offhand item beside the hotbar"), ModuleCategory.RENDER), HudElementType.OFFHAND);
    }

    @Override
    protected boolean shouldRender() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && !player.getOffHandStack().isEmpty()
                && ModuleManager.find(Hotbar.class)
                        .map(Module -> ((Hotbar) Module).canRenderHotbar()).orElse(false);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.OFFHAND.defaultX();
        float y = HudElementType.OFFHAND.defaultY();
        hud.drawPanel(layout, x, y, HudElementType.OFFHAND.defaultWidth(), HudElementType.OFFHAND.defaultHeight(), 12.0f, 1.0f, 28.0f);
        ItemStack stack = hud.getOffhandStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        layout.drawItem(stack, x + 14.0f, y + 13.5f, 16.0f, 10);
        if (stack.getCount() > 1) {
            layout.drawItemCount("x" + stack.getCount(), x + 25.0f, y + 25.5f, 9.5f, -419430401);
        }
    }
}
