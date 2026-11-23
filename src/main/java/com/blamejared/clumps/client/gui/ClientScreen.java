package com.blamejared.clumps.client.gui;

import com.blamejared.clumps.client.render.ui.GuiDimensions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class ClientScreen extends Screen {
    
    protected ClientScreen(Text title) {
        super(title);
    }

    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float tickDelta) {
    }

    
    @Override
    public void renderInGameBackground(DrawContext context) {
    }

    
    public float toUiX(float x) {
        return x * GuiDimensions.width()
                / MinecraftClient.getInstance().getWindow().getScaledWidth();
    }

    
    public float toUiY(float y) {
        return y * GuiDimensions.height()
                / MinecraftClient.getInstance().getWindow().getScaledHeight();
    }
}
