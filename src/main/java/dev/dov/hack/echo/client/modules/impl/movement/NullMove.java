package dev.dov.hack.echo.client.modules.impl.movement;

import dev.dov.hack.echo.client.event.Subscribe;
import dev.dov.hack.echo.client.event.input.PlayerInputEvent;
import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.modules.Module;
import dev.dov.hack.echo.client.modules.ModuleInfo;
import net.minecraft.util.PlayerInput;

public class NullMove extends Module {
    private boolean previousLeft;
    private boolean previousRight;
    private boolean previousForward;
    private boolean previousBackward;
    private boolean preferRight;
    private boolean preferForward;

    public NullMove() {
        super(new ModuleInfo(dev.dov.hack.echo.client.init.CatalogText.literal("Null Move"), dev.dov.hack.echo.client.init.CatalogText.literal("Instant direction changes"), dev.dov.hack.echo.client.modules.ModuleCategory.MOVEMENT, false));
    }

    @Override
    public void postInitialize() {
        previousLeft = false;
        previousRight = false;
        previousForward = false;
        previousBackward = false;
        preferRight = false;
        preferForward = false;
        super.postInitialize();
    }

    @Subscribe
    public void onMovementInput(PlayerInputEvent event) {
        if (isOutsideWorld() || client.currentScreen != null) {
            return;
        }

        boolean pressedLeft = event.left && !previousLeft;
        boolean pressedRight = event.right && !previousRight;
        if (pressedLeft && !pressedRight) {
            preferRight = false;
        } else if (pressedRight && !pressedLeft) {
            preferRight = true;
        }
        if (event.left && event.right) {
            if (preferRight) {
                event.left = false;
            } else {
                event.right = false;
            }
        }

        boolean pressedForward = event.forward && !previousForward;
        boolean pressedBackward = event.backward && !previousBackward;
        if (pressedForward && !pressedBackward) {
            preferForward = true;
        } else if (pressedBackward && !pressedForward) {
            preferForward = false;
        }
        if (event.forward && event.backward) {
            if (preferForward) {
                event.backward = false;
            } else {
                event.forward = false;
            }
        }
        PlayerInput physicalInput = event.originalInput;
        previousLeft = physicalInput.left();
        previousRight = physicalInput.right();
        previousForward = physicalInput.forward();
        previousBackward = physicalInput.backward();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        previousLeft = false;
        previousRight = false;
        previousForward = false;
        previousBackward = false;
    }

}
