package dev.dov.hack.echo.client.modules.internals;

import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.settings.FloatRangeSetting;

public class InputSimulation extends Module {
    private static final CharSequence MODE_OFF = CatalogText.literal("Off");
    private static final CharSequence MODE_REPLAY = CatalogText.literal("Replay");
    private static final CharSequence MODE_NATIVE = CatalogText.literal("Native");

    private static final CharSequence TARGET_OS_AND_GAME = CatalogText.literal("OS + In-Game");
    private static final CharSequence TARGET_GAME_ONLY = CatalogText.literal("In-Game Only");

    private static final CharSequence HOTBAR_OFF = CatalogText.literal("Off");
    private static final CharSequence HOTBAR_KEYS = CatalogText.literal("Keys");
    private static final CharSequence HOTBAR_KEYS_SCROLL = CatalogText.literal("Keys + Scroll");

    private static ChoiceSetting mode;
    private static ChoiceSetting outputTarget;
    private static BooleanSetting simulateMouseClicks;
    private static ChoiceSetting hotbarSwaps;
    private static BooleanSetting simulateMovementKeys;
    private static BooleanSetting simulateOffhandSwap;
    private static BooleanSetting simulateInventoryCursor;
    private static FloatRangeSetting holdDuration;
    private static FloatRangeSetting cursorDuration;

    public InputSimulation() {
        super(new ModuleInfo(
                CatalogText.literal("Input Simulation"),
                CatalogText.literal("Simulates your in-game or custom module keybinds across Minecraft, mods, and external apps"),
                ModuleCategory.INTERNALS));
    }

    @Override
    public void postInitialize() {
        if (mode != null) {
            mode.description(CatalogText.literal("Replay: zero-delay realistic input replay. Native: input-first with smooth inventory cursor travel"));
        }
        if (outputTarget != null) {
            outputTarget.description(CatalogText.literal("OS + In-Game sends Windows OS input while focused so external overlays and mods all detect it"));
            outputTarget.setVisibilityPredicate(ignored -> isSimulationEnabled());
        }
        if (simulateMouseClicks != null) {
            simulateMouseClicks.setVisibilityPredicate(ignored -> isSimulationEnabled());
        }
        if (hotbarSwaps != null) {
            hotbarSwaps.setVisibilityPredicate(ignored -> isSimulationEnabled());
        }
        if (simulateMovementKeys != null) {
            simulateMovementKeys.setVisibilityPredicate(ignored -> isSimulationEnabled());
        }
        if (simulateOffhandSwap != null) {
            simulateOffhandSwap.setVisibilityPredicate(ignored -> isSimulationEnabled());
        }
        if (simulateInventoryCursor != null) {
            simulateInventoryCursor.setVisibilityPredicate(ignored -> isSimulationEnabled());
        }
        if (holdDuration != null) {
            holdDuration.setVisibilityPredicate(ignored -> isSimulationEnabled());
        }
        if (cursorDuration != null) {
            cursorDuration.setVisibilityPredicate(ignored -> isSimulationEnabled() && simulateInventoryCursor.getValue());
        }
    }

    @Override
    public void toggle() {
    }

    @Override
    public void setEnabled(boolean requested) {
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isKeybindable() {
        return false;
    }

    public static boolean isSimulationEnabled() {
        return mode != null && !mode.isSelected(MODE_OFF);
    }

    public static boolean isNativeMode() {
        return mode != null && mode.isSelected(MODE_NATIVE);
    }

    public static boolean isReplayMode() {
        return mode != null && mode.isSelected(MODE_REPLAY);
    }

    public static boolean usesOsOutput() {
        return outputTarget != null && outputTarget.isSelected(TARGET_OS_AND_GAME);
    }

    public static boolean shouldSimulateMouseClicks() {
        return isSimulationEnabled() && simulateMouseClicks != null && simulateMouseClicks.getValue();
    }

    public static boolean shouldSimulateHotbarSwaps() {
        return isSimulationEnabled() && hotbarSwaps != null && !hotbarSwaps.isSelected(HOTBAR_OFF);
    }

    public static boolean allowsScrollHotbarSwaps() {
        return isSimulationEnabled() && hotbarSwaps != null && hotbarSwaps.isSelected(HOTBAR_KEYS_SCROLL);
    }

    public static boolean shouldSimulateMovementKeys() {
        return isSimulationEnabled() && simulateMovementKeys != null && simulateMovementKeys.getValue();
    }

    public static boolean shouldSimulateOffhandSwap() {
        return isSimulationEnabled() && simulateOffhandSwap != null && simulateOffhandSwap.getValue();
    }

    public static boolean shouldSimulateInventoryCursor() {
        return isSimulationEnabled() && simulateInventoryCursor != null && simulateInventoryCursor.getValue();
    }

    public static long sampleHoldMillis() {
        if (holdDuration == null) {
            return 45L;
        }
        return Math.max(10L, Math.min(200L, Math.round(holdDuration.sampleGaussian())));
    }

    public static long sampleCursorMillis() {
        if (cursorDuration == null) {
            return 110L;
        }
        return Math.max(20L, Math.min(400L, Math.round(cursorDuration.sampleGaussian())));
    }

    public static CharSequence bootstrapModeOff() { return MODE_OFF; }
    public static CharSequence bootstrapModeReplay() { return MODE_REPLAY; }
    public static CharSequence bootstrapModeNative() { return MODE_NATIVE; }
    public static CharSequence bootstrapTargetOsAndGame() { return TARGET_OS_AND_GAME; }
    public static CharSequence bootstrapTargetGameOnly() { return TARGET_GAME_ONLY; }
    public static CharSequence bootstrapHotbarOff() { return HOTBAR_OFF; }
    public static CharSequence bootstrapHotbarKeys() { return HOTBAR_KEYS; }
    public static CharSequence bootstrapHotbarKeysScroll() { return HOTBAR_KEYS_SCROLL; }

    public static void assignBootstrapSettings(
            InputSimulation module,
            ChoiceSetting preparedMode,
            ChoiceSetting preparedOutputTarget,
            BooleanSetting preparedMouseClicks,
            ChoiceSetting preparedHotbarSwaps,
            BooleanSetting preparedMovementKeys,
            BooleanSetting preparedOffhandSwap,
            BooleanSetting preparedInventoryCursor,
            FloatRangeSetting preparedHoldDuration,
            FloatRangeSetting preparedCursorDuration) {
        mode = preparedMode;
        outputTarget = preparedOutputTarget;
        simulateMouseClicks = preparedMouseClicks;
        hotbarSwaps = preparedHotbarSwaps;
        simulateMovementKeys = preparedMovementKeys;
        simulateOffhandSwap = preparedOffhandSwap;
        simulateInventoryCursor = preparedInventoryCursor;
        holdDuration = preparedHoldDuration;
        cursorDuration = preparedCursorDuration;
    }

    public static void appendBootstrapSettings(InputSimulation module) {
        CatalogSettings.append(module, mode);
        CatalogSettings.append(module, outputTarget);
        CatalogSettings.append(module, simulateMouseClicks);
        CatalogSettings.append(module, hotbarSwaps);
        CatalogSettings.append(module, simulateMovementKeys);
        CatalogSettings.append(module, simulateOffhandSwap);
        CatalogSettings.append(module, simulateInventoryCursor);
        CatalogSettings.append(module, holdDuration);
        CatalogSettings.append(module, cursorDuration);
    }
}
