package com.blamejared.clumps.client.utils.inventory;

import net.minecraft.client.util.Window;
import net.minecraft.screen.slot.Slot;

public final class InventorySlotGeometry {
    private InventorySlotGeometry() {
    }

    public static double[] centerInWindow(Slot slot, int screenLeft, int screenTop, Window window) {
        double centerX = screenLeft + slot.x + 8.0;
        double centerY = screenTop + slot.y + 8.0;
        double windowX = centerX * window.getWidth() / window.getScaledWidth();
        double windowY = centerY * window.getHeight() / window.getScaledHeight();
        return new double[] {windowX, windowY};
    }
}
