package com.blamejared.clumps.client.utils.inventory;

import com.blamejared.clumps.client.MinecraftAccess;
import com.blamejared.clumps.client.mixin.accessor.HandledScreenAccessor;
import com.blamejared.clumps.client.utils.MathRng;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.Window;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class InventoryScreenSlots implements MinecraftAccess {
    public record ScreenLayout(ScreenHandler handler, int backgroundWidth, int backgroundHeight) {
    }

    public record CursorTarget(int slotIndex, double windowX, double windowY, double distance) {
    }

    public InventoryScreenSlots() {
    }

    public static ScreenLayout layoutOf(Object screen) {
        if (screen instanceof InventoryScreen inventory) {
            return new ScreenLayout(inventory.getScreenHandler(), 176, 166);
        }
        if (screen instanceof CreativeInventoryScreen creative) {
            return new ScreenLayout(creative.getScreenHandler(), 195, 136);
        }
        if (screen instanceof HandledScreen<?> handled && screen instanceof HandledScreenAccessor accessor) {
            return new ScreenLayout(handled.getScreenHandler(), accessor.getBackgroundWidth(), accessor.getBackgroundHeight());
        }
        return null;
    }

    public static boolean isPlayerInventoryScreen(Object screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeInventoryScreen;
    }

    public static double[] centerInWindow(Slot slot, int left, int top, Window window) {
        return InventorySlotGeometry.centerInWindow(slot, left, top, window);
    }

    public static double[] jitteredPointInWindow(Slot slot, int left, int top, Window window, double maxJitterScaledPx) {
        double[] center = InventorySlotGeometry.centerInWindow(slot, left, top, window);
        if (maxJitterScaledPx <= 0.0 || window == null || window.getScaledWidth() <= 0 || window.getScaledHeight() <= 0) {
            return center;
        }
        double scaleX = (double) window.getWidth() / window.getScaledWidth();
        double scaleY = (double) window.getHeight() / window.getScaledHeight();
        double dx = MathRng.randomDouble(-maxJitterScaledPx, maxJitterScaledPx) * scaleX;
        double dy = MathRng.randomDouble(-maxJitterScaledPx, maxJitterScaledPx) * scaleY;
        return new double[] {center[0] + dx, center[1] + dy};
    }

    public static int[] screenOrigin(ScreenLayout layout, Window window) {
        int left = (window.getScaledWidth() - layout.backgroundWidth()) / 2;
        int top = (window.getScaledHeight() - layout.backgroundHeight()) / 2;
        return new int[] {left, top};
    }

    public static double farthestDistance(List<Slot> slots, boolean playerInventoryOnly, int left, int top, Window window, double cursorX, double cursorY) {
        return farthestDistance(slots, playerInventoryOnly, left, top, window, cursorX, cursorY, null);
    }

    public static double farthestDistance(List<Slot> slots, boolean playerInventoryOnly, int left, int top, Window window, double cursorX, double cursorY, Predicate<Slot> allowedSlot) {
        double farthest = 0.0;
        for (Slot slot : slots) {
            if (!isEligibleSlot(slot, playerInventoryOnly)) {
                continue;
            }
            if (allowedSlot != null && !allowedSlot.test(slot)) {
                continue;
            }
            double[] center = centerInWindow(slot, left, top, window);
            double distance = Math.hypot(center[0] - cursorX, center[1] - cursorY);
            if (distance > farthest) {
                farthest = distance;
            }
        }
        return farthest;
    }

    public static CursorTarget nearestItem(List<Slot> slots, Item item, boolean playerInventoryOnly, int left, int top, Window window, double cursorX, double cursorY) {
        return nearestItem(slots, item, playerInventoryOnly, left, top, window, cursorX, cursorY, null);
    }

    public static CursorTarget nearestItem(List<Slot> slots, Item item, boolean playerInventoryOnly, int left, int top, Window window, double cursorX, double cursorY, Predicate<Slot> allowedSlot) {
        double nearestDistance = Double.MAX_VALUE;
        CursorTarget nearest = null;
        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            Slot slot = slots.get(slotIndex);
            if (!isEligibleSlot(slot, playerInventoryOnly)) {
                continue;
            }
            if (allowedSlot != null && !allowedSlot.test(slot)) {
                continue;
            }
            if (!slot.getStack().isOf(item)) {
                continue;
            }
            double[] center = centerInWindow(slot, left, top, window);
            double distance = Math.hypot(center[0] - cursorX, center[1] - cursorY);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = new CursorTarget(slotIndex, center[0], center[1], distance);
            }
        }
        return nearest;
    }

    public static long cursorTravelDuration(double distance, double maximumDistance, float minimumDuration, float maximumDuration) {
        if (maximumDistance <= 0.0) {
            return Math.max(1L, (long) Math.round(minimumDuration));
        }
        double fraction = Math.max(0.0, Math.min(1.0, distance / maximumDistance));
        double duration = minimumDuration + fraction * (double) (maximumDuration - minimumDuration);
        return Math.max(1L, Math.round(duration));
    }

    private static boolean isEligibleSlot(Slot slot, boolean playerInventoryOnly) {
        if (playerInventoryOnly && !(slot.inventory instanceof PlayerInventory)) {
            return false;
        }
        return !(slot.inventory instanceof PlayerInventory) || slot.getIndex() != 40;
    }
}
