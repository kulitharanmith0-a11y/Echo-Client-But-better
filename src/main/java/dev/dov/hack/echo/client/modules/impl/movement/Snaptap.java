package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.EventPriority;
import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.KeyboardEvent;
import dev.dov.hack.echo.client.event.input.PlayerInputEvent;
import dev.dov.hack.echo.client.event.lifecycle.ScreenChangeEvent;
import dev.dov.hack.echo.client.event.window.WindowFocusEvent;
import dev.dov.hack.echo.client.init.CatalogSettings;
import dev.dov.hack.echo.client.init.CatalogText;
import dev.dov.hack.echo.client.mixin.accessor.KeyBindingAccessor;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import dev.dov.hack.echo.client.settings.BooleanSetting;
import dev.dov.hack.echo.client.settings.ChoiceSetting;
import dev.dov.hack.echo.client.utils.strings.Concat;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.PlayerInput;
import org.lwjgl.glfw.GLFW;

public class Snaptap extends Module {
    private static final CharSequence MODE_LAST_INPUT = Concat.strings("Last Input");
    private static final CharSequence MODE_FIRST_INPUT = Concat.strings("First Input");

    private static ChoiceSetting mode;
    private static BooleanSetting horizontal;
    private static BooleanSetting vertical;
    private static BooleanSetting pauseWhileSneaking;

    private long pressCounter;
    private long leftSeq;
    private long rightSeq;
    private long forwardSeq;
    private long backSeq;
    private boolean prevLeft;
    private boolean prevRight;
    private boolean prevForward;
    private boolean prevBack;

    public Snaptap() {
        super(new ModuleInfo(
                CatalogText.literal("Snaptap"),
                CatalogText.literal("Prioritizes the most recently pressed opposing movement key (SOCD)"),
                ModuleCategory.MOVEMENT));
    }

    @Override
    public boolean supportsInputSimulation() {
        return false;
    }

    @Override
    public CharSequence getSuffix() {
        return mode != null ? mode.getValue() : MODE_LAST_INPUT;
    }

    @Override
    public void onEnable() {
        resetState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    public void onKeyboard(KeyboardEvent event) {
        if (!isEnabled() || client == null || client.options == null || client.currentScreen != null) {
            return;
        }
        int action = event.getAction();
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) {
            return;
        }
        int keyCode = event.getKey();
        GameOptions options = client.options;
        if (matchesKey(options.leftKey, keyCode)) {
            leftSeq = action == GLFW.GLFW_PRESS ? ++pressCounter : 0L;
        } else if (matchesKey(options.rightKey, keyCode)) {
            rightSeq = action == GLFW.GLFW_PRESS ? ++pressCounter : 0L;
        } else if (matchesKey(options.forwardKey, keyCode)) {
            forwardSeq = action == GLFW.GLFW_PRESS ? ++pressCounter : 0L;
        } else if (matchesKey(options.backKey, keyCode)) {
            backSeq = action == GLFW.GLFW_PRESS ? ++pressCounter : 0L;
        }
    }

    @Subscribe(priority = EventPriority.HIGH)
    public void onPlayerInput(PlayerInputEvent event) {
        if (!isEnabled() || isOutsideWorld() || client.currentScreen != null) {
            resetState();
            return;
        }

        PlayerInput physical = event.originalInput;
        syncAxisSequences(physical.left(), physical.right(), physical.forward(), physical.backward());

        if (pauseWhileSneaking != null && pauseWhileSneaking.getValue() && event.sneak) {
            return;
        }

        boolean preferFirst = mode != null && MODE_FIRST_INPUT.equals(mode.getValue());

        if ((horizontal == null || horizontal.getValue()) && event.left && event.right) {
            boolean chooseRight = preferFirst
                    ? (rightSeq > 0L && (leftSeq == 0L || rightSeq < leftSeq))
                    : (rightSeq >= leftSeq);
            if (chooseRight) {
                event.left = false;
            } else {
                event.right = false;
            }
        }

        if ((vertical == null || vertical.getValue()) && event.forward && event.backward) {
            boolean chooseForward = preferFirst
                    ? (forwardSeq > 0L && (backSeq == 0L || forwardSeq < backSeq))
                    : (forwardSeq >= backSeq);
            if (chooseForward) {
                event.backward = false;
            } else {
                event.forward = false;
            }
        }
    }

    @Subscribe
    public void onScreenChange(ScreenChangeEvent event) {
        if (event.getScreen() != null) {
            resetState();
        }
    }

    @Subscribe
    public void onWindowFocus(WindowFocusEvent event) {
        if (!event.focused) {
            resetState();
        }
    }

    private void syncAxisSequences(boolean leftHeld, boolean rightHeld, boolean forwardHeld, boolean backHeld) {
        if (leftHeld && (!prevLeft || leftSeq == 0L)) {
            if (leftSeq == 0L) {
                leftSeq = ++pressCounter;
            }
        } else if (!leftHeld) {
            leftSeq = 0L;
        }

        if (rightHeld && (!prevRight || rightSeq == 0L)) {
            if (rightSeq == 0L) {
                rightSeq = ++pressCounter;
            }
        } else if (!rightHeld) {
            rightSeq = 0L;
        }

        if (forwardHeld && (!prevForward || forwardSeq == 0L)) {
            if (forwardSeq == 0L) {
                forwardSeq = ++pressCounter;
            }
        } else if (!forwardHeld) {
            forwardSeq = 0L;
        }

        if (backHeld && (!prevBack || backSeq == 0L)) {
            if (backSeq == 0L) {
                backSeq = ++pressCounter;
            }
        } else if (!backHeld) {
            backSeq = 0L;
        }

        prevLeft = leftHeld;
        prevRight = rightHeld;
        prevForward = forwardHeld;
        prevBack = backHeld;
    }

    private static boolean matchesKey(KeyBinding binding, int keyCode) {
        if (binding == null) {
            return false;
        }
        InputUtil.Key bound = ((KeyBindingAccessor) binding).getBoundKey();
        return bound != null && bound.getCategory() == InputUtil.Type.KEYSYM && bound.getCode() == keyCode;
    }

    private void resetState() {
        leftSeq = 0L;
        rightSeq = 0L;
        forwardSeq = 0L;
        backSeq = 0L;
        prevLeft = false;
        prevRight = false;
        prevForward = false;
        prevBack = false;
    }

    public static CharSequence bootstrapModeLastInput() {
        return MODE_LAST_INPUT;
    }

    public static CharSequence bootstrapModeFirstInput() {
        return MODE_FIRST_INPUT;
    }

    public static void assignBootstrapSettings(
            Snaptap Module,
            ChoiceSetting preparedMode,
            BooleanSetting preparedHorizontal,
            BooleanSetting preparedVertical,
            BooleanSetting preparedPauseWhileSneaking) {
        Snaptap.mode = preparedMode;
        Snaptap.horizontal = preparedHorizontal;
        Snaptap.vertical = preparedVertical;
        Snaptap.pauseWhileSneaking = preparedPauseWhileSneaking;
    }

    public static void appendBootstrapSettings(Snaptap Module) {
        CatalogSettings.append(Module, Snaptap.mode);
        CatalogSettings.append(Module, Snaptap.horizontal);
        CatalogSettings.append(Module, Snaptap.vertical);
        CatalogSettings.append(Module, Snaptap.pauseWhileSneaking);
    }
}
