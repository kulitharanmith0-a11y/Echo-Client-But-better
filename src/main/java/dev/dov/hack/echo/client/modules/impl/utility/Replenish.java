package dev.dov.hack.echo.client.modules.impl.utility;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.HandleInputEvent;
import dev.dov.hack.echo.client.event.input.MouseMovementEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.render.bridge.MinecraftRenderState;
import dev.dov.hack.echo.client.settings.FloatSetting;
import dev.dov.hack.echo.client.settings.IntSetting;
import dev.dov.hack.echo.client.utils.MathRng;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public class Replenish extends Module {
    private static final String TIMING_SECTION = "Timing";
    private static FloatSetting minPercent;
    private static IntSetting cursorSpeed;
    private boolean movingCursor;
    private long cursorMoveStartMillis;
    private long cursorMoveDurationMillis;
    private double cursorStartX;
    private double cursorStartY;
    private double cursorTargetX;
    private double cursorTargetY;
    private int pendingInventorySlot;

    public Replenish() {
        super(new ModuleInfo(CatalogText.literal("Replenish"), CatalogText.literal("Automatically replenishes items"), ModuleCategory.UTILITY));
    }

    @Override
    public void postInitialize() {
        movingCursor = false;
        cursorMoveStartMillis = 0L;
        cursorMoveDurationMillis = 200L;
        cursorStartX = 0.0;
        cursorStartY = 0.0;
        cursorTargetX = 0.0;
        cursorTargetY = 0.0;
        pendingInventorySlot = -1;
        cursorSpeed.tooltip(TIMING_SECTION);
    }

    @Subscribe
    private void onHandleInput(HandleInputEvent.Pre event) {
        if (isOutsideWorld() || !(MinecraftRenderState.getScreen() instanceof InventoryScreen)) {
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.isEmpty() || !stack.isStackable()) {
                continue;
            }
            float percent = (float) stack.getCount() / (float) stack.getMaxCount() * 100.0f;
            if (stack.getCount() == 1 || percent <= Math.max(minPercent.getValue(), 5.0f)) {
                replenish(stack);
            }
        }
    }

    private void replenish(ItemStack hotbarStack) {
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.isEmpty() || !sameReplenishmentItem(hotbarStack, stack)) {
                continue;
            }
            int inventoryIndex = inventoryIndexForSlot(slot);
            moveCursorToSlot(slot);
            quickMove(inventoryIndex);
            return;
        }
    }

    private boolean sameReplenishmentItem(ItemStack first, ItemStack second) {
        if (!first.getItem().equals(second.getItem())) {
            return false;
        }
        if (first.getItem() instanceof BlockItem firstBlock && second.getItem() instanceof BlockItem secondBlock) {
            return firstBlock.getBlock() == secondBlock.getBlock();
        }
        return true;
    }

    private int inventoryIndexForSlot(int slot) {
        return client.player.currentScreenHandler.slots.get(slot).getIndex();
    }

    private void quickMove(int slot) {
        int syncId = client.player.currentScreenHandler.syncId;
        PlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) {
            return;
        }
        var simulator = dev.dov.hack.echo.client.handlers.input.InputSimulator.get();
        if (simulator != null && simulator.scheduleCursorClick(this, syncId, slot, 0, SlotActionType.QUICK_MOVE, null)) {
            return;
        }
        actions().executeSlotClick(this, syncId, slot, 0, SlotActionType.QUICK_MOVE, player);
    }

    private void moveCursorToSlot(int slotIndex) {
        if (MinecraftRenderState.getScreen() == null) {
            return;
        }
        var handler = ((InventoryScreen) MinecraftRenderState.getScreen()).getScreenHandler();
        var window = client.getWindow();
        double mouseX = client.mouse.getX();
        double mouseY = client.mouse.getY();
        int left = (window.getScaledWidth() - 176) / 2;
        int top = (window.getScaledHeight() - 166) / 2;
        var slot = handler.slots.get(slotIndex);
        double targetX = (double) (left + slot.x) + 8.0;
        double targetY = (double) (top + slot.y) + 8.0;
        cursorStartX = mouseX;
        cursorStartY = mouseY;
        cursorMoveDurationMillis = 200L - (long) cursorSpeed.getValue() * 2L;
        cursorTargetX = targetX;
        cursorTargetY = targetY;
        cursorMoveStartMillis = System.currentTimeMillis();
        movingCursor = true;
        pendingInventorySlot = slotIndex;
    }

    @Subscribe
    private void onMouseMovement(MouseMovementEvent.Post event) {
        if (!movingCursor) {
            return;
        }
        if (client == null || MinecraftRenderState.getScreen() == null) {
            movingCursor = false;
            return;
        }
        if (!(MinecraftRenderState.getScreen() instanceof InventoryScreen)) {
            movingCursor = false;
            return;
        }
        long elapsed = System.currentTimeMillis() - cursorMoveStartMillis;
        double progress = Math.max(0.0, Math.min(1.0, (double) elapsed / (double) cursorMoveDurationMillis));
        double eased = MathRng.smoothStep(progress, 0.0, 1.0);
        double x = cursorStartX + (cursorTargetX - cursorStartX) * eased;
        double y = cursorStartY + (cursorTargetY - cursorStartY) * eased;
        var window = client.getWindow();
        double windowX = x * (double) window.getWidth() / (double) window.getScaledWidth();
        double windowY = y * (double) window.getHeight() / (double) window.getScaledHeight();
        InputUtil.setCursorParameters(window, GLFW.GLFW_CURSOR_NORMAL, windowX, windowY);
        if (progress >= 1.0) {
            movingCursor = false;
            if (pendingInventorySlot >= 0) {
                quickMove(pendingInventorySlot);
                pendingInventorySlot = -1;
            }
        }
    }

    public static void appendBootstrapSettings(Replenish Module) {
        CatalogSettings.append(Module, minPercent);
        CatalogSettings.append(Module, cursorSpeed);
    }

    public static void assignBootstrapSettings(Replenish Module, FloatSetting preparedMinPercent, IntSetting preparedCursorSpeed) {
        minPercent = preparedMinPercent;
        cursorSpeed = preparedCursorSpeed;
    }
}
