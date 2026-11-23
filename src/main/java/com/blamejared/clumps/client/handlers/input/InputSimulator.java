package com.blamejared.clumps.client.handlers.input;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.lifecycle.FrameStartEvent;
import com.blamejared.clumps.client.event.lifecycle.RenderTickEvent;
import com.blamejared.clumps.client.event.lifecycle.ScreenChangeEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.window.WindowFocusEvent;
import com.blamejared.clumps.client.handlers.ClientHandler;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.keyboard.KeyStateOverrides;
import com.blamejared.clumps.client.keyboard.KeybindingCache;
import com.blamejared.clumps.client.keyboard.ModuleInputConfig;
import com.blamejared.clumps.client.mixin.accessor.KeyboardAccessor;
import com.blamejared.clumps.client.mixin.accessor.MouseAccessor;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.internals.InputSimulation;
import com.blamejared.clumps.client.render.bridge.MinecraftRenderState;
import com.blamejared.clumps.client.utils.MathRng;
import com.blamejared.clumps.client.utils.inventory.InventoryScreenSlots;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public final class InputSimulator extends ClientHandler {
    private static final long OS_REPLAY_TTL_NANOS = 250_000_000L;
    private static final long OS_CHAR_SUPPRESS_NANOS = 100_000_000L;
    private static final long MOVEMENT_OVERRIDE_TTL_NANOS = 70_000_000L;

    private record ResolvedInput(InputKey key, KeyBinding binding) { }

    private static final class ActivePulse {
        final InputKey key;
        final KeyBinding binding;
        final boolean viaOs;
        final boolean replayOnly;
        long releaseAtNanos;

        ActivePulse(InputKey key, KeyBinding binding, boolean viaOs, boolean replayOnly, long releaseAtNanos) {
            this.key = key;
            this.binding = binding;
            this.viaOs = viaOs;
            this.replayOnly = replayOnly;
            this.releaseAtNanos = releaseAtNanos;
        }
    }

    private record PendingOsReplay(boolean mouse, int code, int action, long expiresAtNanos) { }
    private static final class OsHeldTracker {
        int count;
        long expiresAtNanos;

        OsHeldTracker(int count, long expiresAtNanos) {
            this.count = count;
            this.expiresAtNanos = expiresAtNanos;
        }
    }

    private static final class CursorTask {
        final Module owner;
        final int syncId;
        final int slotId;
        final int button;
        final SlotActionType actionType;
        final double startX;
        final double startY;
        final double ctrlX;
        final double ctrlY;
        final double endX;
        final double endY;
        final long startedAtNanos;
        final long durationNanos;
        final Runnable onComplete;
        boolean readyToClick;

        CursorTask(Module owner, int syncId, int slotId, int button, SlotActionType actionType,
                   double startX, double startY, double ctrlX, double ctrlY, double endX, double endY,
                   long startedAtNanos, long durationNanos, Runnable onComplete) {
            this.owner = owner;
            this.syncId = syncId;
            this.slotId = slotId;
            this.button = button;
            this.actionType = actionType;
            this.startX = startX;
            this.startY = startY;
            this.ctrlX = ctrlX;
            this.ctrlY = ctrlY;
            this.endX = endX;
            this.endY = endY;
            this.startedAtNanos = startedAtNanos;
            this.durationNanos = durationNanos;
            this.onComplete = onComplete;
        }
    }

    private final NativeOsInput osInput = new NativeOsInput();
    private final Map<InputKey, ActivePulse> activePulses = new HashMap<>();
    private final Deque<PendingOsReplay> pendingOsReplays = new ArrayDeque<>();
    private final Deque<Long> pendingOsScrolls = new ArrayDeque<>();
    private final Map<Integer, OsHeldTracker> osSimulatedKeys = new HashMap<>();
    private final Map<Integer, OsHeldTracker> osSimulatedMouse = new HashMap<>();
    private final Map<KeyBinding, Boolean> activeMovementOsKeys = new HashMap<>();
    private long suppressOsCharUntilNanos;
    private CursorTask activeCursorTask;
    private static int simulatingDepth;

    public static InputSimulator get() {
        return ClumpsClient.handlerManager == null ? null : ClumpsClient.handlerManager.getInputSimulator();
    }

    public static boolean isSimulating() {
        return simulatingDepth > 0;
    }

    public boolean isCursorBusy() {
        return activeCursorTask != null;
    }

    public void reset() {
        releaseAll();
        cancelCursorTask();
        pendingOsReplays.clear();
        pendingOsScrolls.clear();
        osSimulatedKeys.clear();
        osSimulatedMouse.clear();
        suppressOsCharUntilNanos = 0L;
        KeyStateOverrides.clearAll();
    }

    // -------------------------------------------------------------------------
    // Public High-Level Simulation Entry Points (Called by ActionCoordinator / KeybindingEvents)
    // -------------------------------------------------------------------------

    public void simulateAttackTap(Module owner) {
        if (!InputSimulation.shouldSimulateMouseClicks() || !isAllowedForModule(owner)) {
            return;
        }
        if (CLIENT.options == null) {
            return;
        }
        ResolvedInput resolved = resolveAttack(owner);
        if (resolved != null) {
            tap(resolved, true);
        }
    }

    public void simulateUseTap(Module owner) {
        if (!InputSimulation.shouldSimulateMouseClicks() || !isAllowedForModule(owner)) {
            return;
        }
        if (CLIENT.options == null) {
            return;
        }
        ResolvedInput resolved = resolveUse(owner);
        if (resolved != null) {
            tap(resolved, true);
        }
    }

    public void simulateHotbarSelect(Module owner, int previousSlot, int targetSlot) {
        if (!InputSimulation.shouldSimulateHotbarSwaps() || !isAllowedForModule(owner)) {
            return;
        }
        if (CLIENT.options == null || targetSlot < 0 || targetSlot > 8 || previousSlot == targetSlot) {
            return;
        }
        ModuleInputConfig config = owner != null ? owner.inputConfig() : null;
        boolean hasCustomSlotBind = config != null && config.customSlotKey(targetSlot) != InputKey.NONE;
        if (!hasCustomSlotBind && InputSimulation.allowsScrollHotbarSwaps() && previousSlot >= 0 && previousSlot <= 8) {
            int delta = shortestHotbarDelta(previousSlot, targetSlot);
            if (Math.abs(delta) == 1) {
                dispatchScroll(-delta, true);
                return;
            }
        }
        ResolvedInput resolved = resolveHotbarSlot(owner, targetSlot);
        if (resolved != null) {
            tap(resolved, true);
        }
    }

    public void simulateOffhandSwap(Module owner) {
        if (!InputSimulation.shouldSimulateOffhandSwap() || !isAllowedForModule(owner)) {
            return;
        }
        if (CLIENT.options == null) {
            return;
        }
        ResolvedInput resolved = resolveOffhand(owner);
        if (resolved != null) {
            tap(resolved, true);
        }
    }

    public void simulateDropTap(Module owner) {
        if (!InputSimulation.isSimulationEnabled() || !isAllowedForModule(owner)) {
            return;
        }
        if (CLIENT.options == null) {
            return;
        }
        ResolvedInput resolved = resolveFromBinding(CLIENT.options.dropKey);
        if (resolved != null) {
            tap(resolved, true);
        }
    }

    public void simulateSlotClick(Module owner, int slotId, int button, SlotActionType actionType) {
        if (!InputSimulation.isSimulationEnabled() || !isAllowedForModule(owner) || CLIENT.options == null) {
            return;
        }
        if (actionType == SlotActionType.SWAP) {
            if (button == 40) {
                ResolvedInput offhand = resolveOffhand(owner);
                if (offhand != null) {
                    tap(offhand, true);
                }
            } else if (button >= 0 && button <= 8) {
                ResolvedInput slotBind = resolveHotbarSlot(owner, button);
                if (slotBind != null) {
                    tap(slotBind, true);
                }
            }
            return;
        }
        if (actionType == SlotActionType.QUICK_MOVE) {
            tapRaw(InputKey.LEFT_SHIFT, null, true);
            tapRaw(button == 1 ? InputKey.MOUSE_RIGHT : InputKey.MOUSE_LEFT, null, true);
            return;
        }
        if (actionType == SlotActionType.THROW) {
            ResolvedInput drop = resolveFromBinding(CLIENT.options.dropKey);
            if (drop != null) {
                tap(drop, true);
            }
            return;
        }
        if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.PICKUP_ALL) {
            tapRaw(button == 1 ? InputKey.MOUSE_RIGHT : InputKey.MOUSE_LEFT, null, true);
        }
    }

    public boolean scheduleCursorClick(Module owner, int syncId, int slotId, int button, SlotActionType actionType, Runnable onComplete) {
        if (!InputSimulation.isNativeMode() || !InputSimulation.shouldSimulateInventoryCursor()
                || !isAllowedForModule(owner)) {
            return false;
        }
        Screen screen = MinecraftRenderState.getScreen();
        if (!(screen instanceof HandledScreen<?>) || CLIENT.player == null || CLIENT.getWindow() == null) {
            return false;
        }
        if (activeCursorTask != null) {
            return true;
        }
        InventoryScreenSlots.ScreenLayout layout = InventoryScreenSlots.layoutOf(screen);
        if (layout == null || layout.handler().syncId != syncId || slotId < 0 || slotId >= layout.handler().slots.size()) {
            return false;
        }
        Window window = CLIENT.getWindow();
        int[] origin = InventoryScreenSlots.screenOrigin(layout, window);
        Slot slot = layout.handler().slots.get(slotId);
        double[] target = InventoryScreenSlots.jitteredPointInWindow(slot, origin[0], origin[1], window, 3.5);
        double startX = CLIENT.mouse.getX();
        double startY = CLIENT.mouse.getY();
        double dx = target[0] - startX;
        double dy = target[1] - startY;
        double dist = Math.hypot(dx, dy);
        double perpScale = dist > 20.0 ? MathRng.randomDouble(-0.12, 0.12) : 0.0;
        double ctrlX = (startX + target[0]) * 0.5 - dy * perpScale;
        double ctrlY = (startY + target[1]) * 0.5 + dx * perpScale;
        long durationNanos = InputSimulation.sampleCursorMillis() * 1_000_000L;
        activeCursorTask = new CursorTask(
                owner, syncId, slotId, button, actionType,
                startX, startY, ctrlX, ctrlY, target[0], target[1],
                System.nanoTime(), durationNanos, onComplete);
        return true;
    }

    public void syncMovementInput(PlayerInputEvent event) {
        if (CLIENT.options == null || event == null) {
            return;
        }
        if (!InputSimulation.shouldSimulateMovementKeys()) {
            releaseMovementOsKeys();
            return;
        }
        syncSingleMovementKey(CLIENT.options.forwardKey, event.forward, event.originalInput.forward());
        syncSingleMovementKey(CLIENT.options.backKey, event.backward, event.originalInput.backward());
        syncSingleMovementKey(CLIENT.options.leftKey, event.left, event.originalInput.left());
        syncSingleMovementKey(CLIENT.options.rightKey, event.right, event.originalInput.right());
        syncSingleMovementKey(CLIENT.options.jumpKey, event.jump, event.originalInput.jump());
        syncSingleMovementKey(CLIENT.options.sneakKey, event.sneak, event.originalInput.sneak());
        syncSingleMovementKey(CLIENT.options.sprintKey, event.sprint, event.originalInput.sprint());
    }

    public void clearMovementOverrides() {
        releaseMovementOsKeys();
    }

    // -------------------------------------------------------------------------
    // Mixin OS Replay Deduplication & State Isolation (MouseMixin / KeyboardMixin / InputKey)
    // -------------------------------------------------------------------------

    public boolean consumeOsReplayMouse(int button, int action) {
        boolean matched = consumeOsReplay(true, button, action);
        if (matched && action == GLFW.GLFW_RELEASE) {
            releaseOsSimulatedTracker(osSimulatedMouse, button);
        }
        return matched;
    }

    public boolean consumeOsReplayKey(int keyCode, int action) {
        if (action == GLFW.GLFW_REPEAT && isOsSimulatedKey(keyCode)) {
            suppressOsCharUntilNanos = Math.max(suppressOsCharUntilNanos, System.nanoTime() + OS_CHAR_SUPPRESS_NANOS);
            return true;
        }
        boolean matched = consumeOsReplay(false, keyCode, action);
        if (matched) {
            if (action == GLFW.GLFW_PRESS) {
                suppressOsCharUntilNanos = Math.max(suppressOsCharUntilNanos, System.nanoTime() + OS_CHAR_SUPPRESS_NANOS);
            } else if (action == GLFW.GLFW_RELEASE) {
                releaseOsSimulatedTracker(osSimulatedKeys, keyCode);
            }
        }
        return matched;
    }

    public boolean consumeOsReplayScroll(double vertical) {
        if (pendingOsScrolls.isEmpty() || vertical == 0.0) {
            return false;
        }
        long now = System.nanoTime();
        while (!pendingOsScrolls.isEmpty()) {
            long expires = pendingOsScrolls.pollFirst();
            if (expires >= now) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldSuppressOsChar() {
        return System.nanoTime() <= suppressOsCharUntilNanos;
    }

    public boolean isOsSimulatedKey(int keyCode) {
        return isOsSimulatedActive(osSimulatedKeys, keyCode);
    }

    public boolean isOsSimulatedMouse(int button) {
        return isOsSimulatedActive(osSimulatedMouse, button);
    }

    private boolean isOsSimulatedActive(Map<Integer, OsHeldTracker> map, int code) {
        OsHeldTracker tracker = map.get(code);
        if (tracker == null) {
            return false;
        }
        if (tracker.count <= 0 || System.nanoTime() > tracker.expiresAtNanos) {
            map.remove(code);
            return false;
        }
        return true;
    }

    private void markOsSimulatedPress(InputKey key, long holdDurationNanos) {
        if (key == null || key == InputKey.NONE) {
            return;
        }
        Map<Integer, OsHeldTracker> map = key.isMouse() ? osSimulatedMouse : osSimulatedKeys;
        long expires = System.nanoTime() + Math.max(holdDurationNanos, 0L) + OS_REPLAY_TTL_NANOS;
        OsHeldTracker tracker = map.get(key.code());
        if (tracker == null || System.nanoTime() > tracker.expiresAtNanos) {
            map.put(key.code(), new OsHeldTracker(1, expires));
        } else {
            tracker.count++;
            tracker.expiresAtNanos = Math.max(tracker.expiresAtNanos, expires);
        }
        if (!key.isMouse()) {
            suppressOsCharUntilNanos = Math.max(suppressOsCharUntilNanos, System.nanoTime() + OS_CHAR_SUPPRESS_NANOS);
        }
    }

    private static void releaseOsSimulatedTracker(Map<Integer, OsHeldTracker> map, int code) {
        OsHeldTracker tracker = map.get(code);
        if (tracker == null) {
            return;
        }
        tracker.count--;
        if (tracker.count <= 0) {
            map.remove(code);
        }
    }

    private boolean consumeOsReplay(boolean mouse, int code, int action) {
        if (pendingOsReplays.isEmpty()) {
            return false;
        }
        long now = System.nanoTime();
        Iterator<PendingOsReplay> iterator = pendingOsReplays.iterator();
        while (iterator.hasNext()) {
            PendingOsReplay entry = iterator.next();
            if (entry.expiresAtNanos < now) {
                iterator.remove();
                continue;
            }
            if (entry.mouse == mouse && entry.code == code && entry.action == action) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    private void recordOsReplay(InputKey key, int action) {
        if (key == null || key == InputKey.NONE) {
            return;
        }
        pendingOsReplays.addLast(new PendingOsReplay(key.isMouse(), key.code(), action, System.nanoTime() + OS_REPLAY_TTL_NANOS));
        while (pendingOsReplays.size() > 64) {
            pendingOsReplays.pollFirst();
        }
    }

    private void recordOsReplayScroll() {
        pendingOsScrolls.addLast(System.nanoTime() + OS_REPLAY_TTL_NANOS);
        while (pendingOsScrolls.size() > 32) {
            pendingOsScrolls.pollFirst();
        }
    }

    // -------------------------------------------------------------------------
    // Event Lifecycle (Main-Thread Pulse Drain, Smooth Cursor, Cleanup)
    // -------------------------------------------------------------------------

    @Subscribe(priority = EventPriority.HIGHEST)
    private void onFrameStart(FrameStartEvent event) {
        drainExpiredPulses();
        updateCursorTask();
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void onRenderTick(RenderTickEvent event) {
        drainExpiredPulses();
        updateCursorTask();
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void onClientTick(ClientTickEvent event) {
        drainExpiredPulses();
        completeReadyCursorTask();
        if (CLIENT.world == null || CLIENT.player == null) {
            reset();
        }
    }

    @Subscribe
    private void onWindowFocus(WindowFocusEvent event) {
        if (!event.focused) {
            releaseAll();
            cancelCursorTask();
        }
    }

    @Subscribe
    private void onScreenChange(ScreenChangeEvent event) {
        if (event.getScreen() == null || !(event.getScreen() instanceof HandledScreen<?>)) {
            cancelCursorTask();
        }
        if (event.getScreen() != null) {
            releaseMovementOsKeys();
        }
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        reset();
    }

    // -------------------------------------------------------------------------
    // Keybind Resolution (Per-Module Custom Keybind -> Live Minecraft Control)
    // -------------------------------------------------------------------------

    private boolean isAllowedForModule(Module owner) {
        if (owner == null) {
            return true;
        }
        ModuleInputConfig config = owner.inputConfig();
        return config == null || (config.isEnabled() && !config.isListeningForKey());
    }

    private ResolvedInput resolveAttack(Module owner) {
        KeyBinding mcBinding = CLIENT.options.attackKey;
        InputKey custom = owner != null && owner.inputConfig() != null ? owner.inputConfig().customAttackKey() : InputKey.NONE;
        return resolveWithOverride(custom, mcBinding);
    }

    private ResolvedInput resolveUse(Module owner) {
        KeyBinding mcBinding = CLIENT.options.useKey;
        InputKey custom = owner != null && owner.inputConfig() != null ? owner.inputConfig().customUseKey() : InputKey.NONE;
        return resolveWithOverride(custom, mcBinding);
    }

    private ResolvedInput resolveOffhand(Module owner) {
        KeyBinding mcBinding = CLIENT.options.swapHandsKey;
        InputKey custom = owner != null && owner.inputConfig() != null ? owner.inputConfig().customOffhandKey() : InputKey.NONE;
        return resolveWithOverride(custom, mcBinding);
    }

    private ResolvedInput resolveHotbarSlot(Module owner, int slot) {
        if (slot < 0 || CLIENT.options.hotbarKeys == null || slot >= CLIENT.options.hotbarKeys.length) {
            return null;
        }
        KeyBinding mcBinding = CLIENT.options.hotbarKeys[slot];
        InputKey custom = owner != null && owner.inputConfig() != null ? owner.inputConfig().customSlotKey(slot) : InputKey.NONE;
        return resolveWithOverride(custom, mcBinding);
    }

    private ResolvedInput resolveFromBinding(KeyBinding binding) {
        if (binding == null) {
            return null;
        }
        InputKey key = KeybindingCache.getKey(binding);
        return key == InputKey.NONE ? null : new ResolvedInput(key, binding);
    }

    private ResolvedInput resolveWithOverride(InputKey custom, KeyBinding mcBinding) {
        if (custom != null && custom != InputKey.NONE) {
            return new ResolvedInput(custom, mcBinding);
        }
        return resolveFromBinding(mcBinding);
    }

    // -------------------------------------------------------------------------
    // Low-Level Pulse Execution (OS + In-Game, Zero Double Execution)
    // -------------------------------------------------------------------------

    private void tap(ResolvedInput resolved, boolean replayOnly) {
        if (resolved == null || resolved.key == null || resolved.key == InputKey.NONE) {
            return;
        }
        tapRaw(resolved.key, resolved.binding, replayOnly);
    }

    private void tapRaw(InputKey key, KeyBinding binding, boolean replayOnly) {
        if (key == null || key == InputKey.NONE || CLIENT.getWindow() == null) {
            return;
        }
        long holdNanos = InputSimulation.sampleHoldMillis() * 1_000_000L;
        if (binding != null) {
            KeyStateOverrides.setPressed(binding, true, holdNanos);
        }
        if (isPhysicallyHeld(key)) {
            return;
        }

        ActivePulse existing = activePulses.get(key);
        if (existing != null) {
            releasePulse(existing);
            activePulses.remove(key);
        }

        boolean useOs = InputSimulation.usesOsOutput() && osInput.isAvailable() && osInput.isWindowFocused()
                && (!key.isMouse() || canSafelySendOsMouse());
        boolean dispatchedViaOs = false;
        if (useOs) {
            if (replayOnly) {
                recordOsReplay(key, GLFW.GLFW_PRESS);
            }
            dispatchedViaOs = key.isMouse()
                    ? osInput.sendMouseButton(key.code(), true)
                    : osInput.sendKey(key.code(), true);
            if (dispatchedViaOs && replayOnly) {
                markOsSimulatedPress(key, holdNanos);
            } else if (!dispatchedViaOs && replayOnly && !pendingOsReplays.isEmpty()) {
                pendingOsReplays.pollLast();
            }
        }

        if (!dispatchedViaOs) {
            dispatchInGame(key, binding, GLFW.GLFW_PRESS, replayOnly);
        }

        activePulses.put(key, new ActivePulse(key, binding, dispatchedViaOs, replayOnly, System.nanoTime() + holdNanos));
    }

    private boolean canSafelySendOsMouse() {
        if (MinecraftRenderState.getScreen() == null) {
            return true;
        }
        Window window = CLIENT.getWindow();
        if (window == null || CLIENT.mouse == null) {
            return false;
        }
        double x = CLIENT.mouse.getX();
        double y = CLIENT.mouse.getY();
        return x >= 2.0 && y >= 2.0 && x <= window.getWidth() - 2.0 && y <= window.getHeight() - 2.0;
    }

    private void dispatchScroll(int notches, boolean replayOnly) {
        if (notches == 0 || CLIENT.getWindow() == null) {
            return;
        }
        if (InputSimulation.usesOsOutput() && osInput.isAvailable() && osInput.isWindowFocused()
                && canSafelySendOsMouse()) {
            if (replayOnly) {
                recordOsReplayScroll();
            }
            if (osInput.sendMouseScroll(notches)) {
                return;
            }
            if (replayOnly && !pendingOsScrolls.isEmpty()) {
                pendingOsScrolls.pollLast();
            }
        }
        if (CLIENT.mouse == null) {
            return;
        }
        simulatingDepth++;
        try {
            ((MouseAccessor) CLIENT.mouse).invokeOnMouseScroll(CLIENT.getWindow().getHandle(), 0.0, notches);
        } finally {
            simulatingDepth = Math.max(0, simulatingDepth - 1);
        }
    }

    private void dispatchInGame(InputKey key, KeyBinding binding, int action, boolean replayOnly) {
        if (CLIENT.getWindow() == null) {
            return;
        }
        long handle = CLIENT.getWindow().getHandle();
        simulatingDepth++;
        try {
            if (key.isMouse()) {
                if (CLIENT.mouse != null) {
                    ((MouseAccessor) CLIENT.mouse).invokeOnMouseButton(handle, new MouseInput(key.code(), 0), action);
                }
            } else {
                if (CLIENT.keyboard != null) {
                    int scan = GLFW.glfwGetKeyScancode(key.code());
                    ((KeyboardAccessor) CLIENT.keyboard).invokeOnKey(handle, action, new KeyInput(key.code(), scan, 0));
                }
            }
        } finally {
            simulatingDepth = Math.max(0, simulatingDepth - 1);
        }
    }

    private void drainExpiredPulses() {
        if (activePulses.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        Iterator<Map.Entry<InputKey, ActivePulse>> iterator = activePulses.entrySet().iterator();
        while (iterator.hasNext()) {
            ActivePulse pulse = iterator.next().getValue();
            if (now >= pulse.releaseAtNanos) {
                iterator.remove();
                releasePulse(pulse);
            }
        }
    }

    private void releasePulse(ActivePulse pulse) {
        if (pulse == null) {
            return;
        }
        if (pulse.binding != null) {
            KeyStateOverrides.clear(pulse.binding);
        }
        if (isPhysicallyHeld(pulse.key)) {
            return;
        }
        if (pulse.viaOs) {
            if (pulse.replayOnly) {
                recordOsReplay(pulse.key, GLFW.GLFW_RELEASE);
            }
            if (pulse.key.isMouse()) {
                osInput.sendMouseButton(pulse.key.code(), false);
            } else {
                osInput.sendKey(pulse.key.code(), false);
            }
        } else {
            dispatchInGame(pulse.key, pulse.binding, GLFW.GLFW_RELEASE, pulse.replayOnly);
        }
    }

    public void releaseAll() {
        if (!activePulses.isEmpty()) {
            ActivePulse[] snapshot = activePulses.values().toArray(new ActivePulse[0]);
            activePulses.clear();
            for (ActivePulse pulse : snapshot) {
                releasePulse(pulse);
            }
        }
        releaseMovementOsKeys();
        osInput.releaseAll();
    }

    private void syncSingleMovementKey(KeyBinding binding, boolean desired, boolean physical) {
        if (binding == null) {
            return;
        }
        if (desired != physical) {
            KeyStateOverrides.setPressed(binding, desired, MOVEMENT_OVERRIDE_TTL_NANOS);
            if (InputSimulation.usesOsOutput() && osInput.isAvailable() && osInput.isWindowFocused()
                    && MinecraftRenderState.getScreen() == null) {
                InputKey key = KeybindingCache.getKey(binding);
                if (key != InputKey.NONE && !key.isMouse()) {
                    Boolean currentlyHeld = activeMovementOsKeys.get(binding);
                    if (desired && !Boolean.TRUE.equals(currentlyHeld)) {
                        recordOsReplay(key, GLFW.GLFW_PRESS);
                        if (osInput.sendKey(key.code(), true)) {
                            markOsSimulatedPress(key, 60_000_000_000L);
                            activeMovementOsKeys.put(binding, true);
                        }
                    } else if (desired && Boolean.TRUE.equals(currentlyHeld)) {
                        OsHeldTracker tracker = osSimulatedKeys.get(key.code());
                        if (tracker != null) {
                            tracker.expiresAtNanos = System.nanoTime() + 60_000_000_000L;
                        }
                    } else if (!desired && Boolean.TRUE.equals(currentlyHeld)) {
                        recordOsReplay(key, GLFW.GLFW_RELEASE);
                        osInput.sendKey(key.code(), false);
                        activeMovementOsKeys.remove(binding);
                    }
                }
            }
        } else {
            if (Boolean.TRUE.equals(activeMovementOsKeys.remove(binding))) {
                InputKey key = KeybindingCache.getKey(binding);
                if (key != InputKey.NONE && !key.isMouse() && !physical) {
                    recordOsReplay(key, GLFW.GLFW_RELEASE);
                    osInput.sendKey(key.code(), false);
                }
            }
        }
    }

    private void releaseMovementOsKeys() {
        if (activeMovementOsKeys.isEmpty()) {
            return;
        }
        KeyBinding[] keys = activeMovementOsKeys.keySet().toArray(new KeyBinding[0]);
        activeMovementOsKeys.clear();
        for (KeyBinding binding : keys) {
            KeyStateOverrides.clear(binding);
            InputKey key = KeybindingCache.getKey(binding);
            if (key != InputKey.NONE && !key.isMouse() && !isPhysicallyHeld(key)) {
                recordOsReplay(key, GLFW.GLFW_RELEASE);
                osInput.sendKey(key.code(), false);
            }
        }
    }

    private void updateCursorTask() {
        CursorTask task = activeCursorTask;
        if (task == null || task.readyToClick) {
            return;
        }
        Screen screen = MinecraftRenderState.getScreen();
        if (CLIENT.player == null || CLIENT.player.isDead()
                || !(screen instanceof HandledScreen<?>)
                || CLIENT.player.currentScreenHandler.syncId != task.syncId) {
            cancelCursorTask();
            return;
        }
        long elapsed = System.nanoTime() - task.startedAtNanos;
        double fraction = task.durationNanos <= 0L ? 1.0 : Math.max(0.0, Math.min(1.0, (double) elapsed / task.durationNanos));
        double t = MathRng.smoothStep(fraction, 0.0, 1.0);
        double inv = 1.0 - t;
        double x = inv * inv * task.startX + 2.0 * inv * t * task.ctrlX + t * t * task.endX;
        double y = inv * inv * task.startY + 2.0 * inv * t * task.ctrlY + t * t * task.endY;
        InputUtil.setCursorParameters(CLIENT.getWindow(), GLFW.GLFW_CURSOR_NORMAL, x, y);
        if (fraction >= 1.0) {
            task.readyToClick = true;
        }
    }

    private void completeReadyCursorTask() {
        CursorTask task = activeCursorTask;
        if (task == null || !task.readyToClick) {
            return;
        }
        activeCursorTask = null;
        Screen screen = MinecraftRenderState.getScreen();
        if (CLIENT.player == null || !(screen instanceof HandledScreen<?>)
                || CLIENT.player.currentScreenHandler.syncId != task.syncId) {
            return;
        }
        ActionCoordinator coordinator = ActionCoordinator.get();
        boolean succeeded;
        if (coordinator != null) {
            succeeded = coordinator.executeSlotClick(
                    task.owner, task.syncId, task.slotId, task.button, task.actionType, CLIENT.player).succeeded();
        } else if (CLIENT.interactionManager != null) {
            CLIENT.interactionManager.clickSlot(task.syncId, task.slotId, task.button, task.actionType, CLIENT.player);
            succeeded = true;
        } else {
            succeeded = false;
        }
        if (succeeded && task.onComplete != null) {
            try {
                task.onComplete.run();
            } catch (Throwable ignored) {
            }
        }
    }

    private void cancelCursorTask() {
        activeCursorTask = null;
    }

    private boolean isPhysicallyHeld(InputKey key) {
        if (key == null || key == InputKey.NONE || CLIENT.getWindow() == null) {
            return false;
        }
        if (osInput.isAvailable()) {
            return key.isMouse()
                    ? osInput.isPhysicallyHeldMouse(key.code())
                    : osInput.isPhysicallyHeldKey(key.code());
        }
        return key.isMouse() ? KeybindingCache.isMousePressed(key.code()) : key.isPressed();
    }

    private static int shortestHotbarDelta(int from, int to) {
        int forward = Math.floorMod(to - from, 9);
        int backward = Math.floorMod(from - to, 9);
        return forward <= backward ? forward : -backward;
    }
}
