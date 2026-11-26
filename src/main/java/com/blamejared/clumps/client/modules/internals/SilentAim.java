package com.blamejared.clumps.client.modules.internals;

import com.blamejared.clumps.client.init.CatalogSettings;
import com.blamejared.clumps.client.init.CatalogText;
import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.PlayerInputEvent;
import com.blamejared.clumps.client.event.movement.RotationContext;
import com.blamejared.clumps.client.event.movement.RotationEvent;
import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import com.blamejared.clumps.client.handlers.rotation.AimState;
import com.blamejared.clumps.client.settings.BooleanSetting;
import com.blamejared.clumps.client.settings.ChoiceSetting;
import com.blamejared.clumps.client.utils.Rotation;
import net.minecraft.util.math.MathHelper;

public class SilentAim extends Module {
    private static final CharSequence MODE_NONE = CatalogText.literal("None");
    private static final CharSequence MODE_SILENT = CatalogText.literal("Silent");
    private static final CharSequence MODE_STRICT = CatalogText.literal("Strict");
    private static final double INV_SQRT_2 = 0.7071067811865475;
    private static final int[][] DIRECTIONS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };
    private static ChoiceSetting mode;
    private static BooleanSetting serverSync;
    private static BooleanSetting renderRotationsSetting;

    public SilentAim() {
        super(new ModuleInfo(CatalogText.literal("Silent Aim"), CatalogText.literal("Silent movement correction for rotations"), ModuleCategory.INTERNALS));
    }

    
    @Override
    public void postInitialize() {
        if (serverSync != null) {
            serverSync.setVisibilityPredicate(ignored -> isCorrectionEnabled());
            serverSync.description(CatalogText.literal("Synchronize movement correction, sprint state, and packets with server rotations"));
        }
        EventBus.register(this);
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

    public boolean isCorrectionEnabled() {
        return mode != null && !mode.isSelected(MODE_NONE);
    }

    public static boolean isServerSyncEnabled() {
        return serverSync != null && serverSync.getValue();
    }

    public boolean shouldCorrectMovement() {
        if (mode == null) {
            return false;
        }
        if (mode.isSelected(MODE_SILENT)) {
            return true;
        }
        if (!mode.isSelected(MODE_STRICT)) {
            return false;
        }
        Module owner = rotations().snapshot().owner();
        return owner != null && owner.requiresMovementCorrection();
    }

    public static boolean renderRotations() {
        return renderRotationsSetting.getValue();
    }

    @Subscribe
    public void onRotationQuery(RotationEvent event) {
        if (event.getContext() != RotationContext.MOVEMENT) {
            return;
        }
        if (isServerSyncEnabled() && rotations().isMovementSyncActive()) {
            Rotation synced = rotations().movementRotation();
            event.setYaw(synced.yaw());
            event.setPitch(synced.pitch());
            return;
        }
        if (!shouldCorrectMovement() || !rotations().isSilentRotationActive()) {
            return;
        }
        Rotation effective = rotations().effectiveRotation();
        event.setYaw(effective.yaw());
        event.setPitch(effective.pitch());
    }

    @Subscribe(priority = EventPriority.LOWEST)
    public void onPlayerInput(PlayerInputEvent event) {
        if (client.player == null) {
            return;
        }
        if (!shouldCorrectMovement() || !rotations().isSilentRotationActive()) {
            rotations().clearMovementSync();
            return;
        }
        boolean sync = isServerSyncEnabled();
        if (!sync && rotations().snapshot().state() == AimState.OVERRIDE) {
            rotations().clearMovementSync();
            return;
        }
        Rotation moveRotation = sync ? rotations().beginMovementSync() : rotations().effectiveRotation();
        if (moveRotation == null) {
            return;
        }
        if (!sync) {
            rotations().clearMovementSync();
        }
        int forward = (int) PlayerInputEvent.movementMultiplier(event.forward, event.backward);
        int strafe = (int) PlayerInputEvent.movementMultiplier(event.left, event.right);
        if (forward == 0 && strafe == 0) {
            if (sync) {
                event.sprint = false;
                rotations().setMovementSyncForwardAllowed(false);
            }
            return;
        }
        int[] corrected = sync
                ? bestSyncedDirection(client.player.getYaw(), moveRotation.yaw(), forward, strafe)
                : steppedDirection(client.player.getYaw(), moveRotation.yaw(), forward, strafe);
        if (corrected == null) {
            return;
        }
        event.forward = corrected[0] > 0;
        event.backward = corrected[0] < 0;
        event.left = corrected[1] > 0;
        event.right = corrected[1] < 0;
        if (corrected[0] <= 0) {
            event.sprint = false;
        }
        if (sync) {
            rotations().setMovementSyncForwardAllowed(corrected[0] > 0);
        }
    }

    static int[] steppedDirection(float playerYaw, float targetYaw, int forward, int strafe) {
        return bestSyncedDirection(playerYaw, targetYaw, forward, strafe);
    }

    static int[] bestSyncedDirection(float playerYaw, float syncYaw, int forward, int strafe) {
        double rawScale = (forward != 0 && strafe != 0) ? INV_SQRT_2 : 1.0;
        double rawForward = forward * rawScale;
        double rawStrafe = strafe * rawScale;
        double playerRad = Math.toRadians(playerYaw);
        double playerSin = Math.sin(playerRad);
        double playerCos = Math.cos(playerRad);
        double desiredX = rawStrafe * playerCos - rawForward * playerSin;
        double desiredZ = rawForward * playerCos + rawStrafe * playerSin;

        double syncRad = Math.toRadians(syncYaw);
        double syncSin = Math.sin(syncRad);
        double syncCos = Math.cos(syncRad);

        // When the player is NOT pressing backward (S) and silent yaw is within normal forward combat arc (<= 67.5 deg),
        // prevent pure A/D strafing from snapping into S+A / S+D (which causes continuous backward drift during A-D strafes).
        boolean disallowBackwardSnap = forward >= 0 && Math.abs(MathHelper.wrapDegrees(syncYaw - playerYaw)) <= 67.5F;

        int[] best = null;
        double bestDot = -Double.MAX_VALUE;
        for (int[] candidate : DIRECTIONS) {
            if (disallowBackwardSnap && candidate[0] < 0) {
                continue;
            }
            double scale = (candidate[0] != 0 && candidate[1] != 0) ? INV_SQRT_2 : 1.0;
            double cf = candidate[0] * scale;
            double cs = candidate[1] * scale;
            double candX = cs * syncCos - cf * syncSin;
            double candZ = cf * syncCos + cs * syncSin;
            double dot = desiredX * candX + desiredZ * candZ;
            if (dot > bestDot) {
                bestDot = dot;
                best = candidate;
            }
        }
        return best;
    }

    
    public static CharSequence bootstrapSilentMode() {
        return MODE_SILENT;
    }

    public static CharSequence bootstrapNoneMode() {
        return MODE_NONE;
    }

    public static CharSequence bootstrapStrictMode() {
        return MODE_STRICT;
    }

    
    public static void assignBootstrapSettings(SilentAim Module, BooleanSetting preparedRenderRotations, ChoiceSetting preparedMode, BooleanSetting preparedServerSync) {
        renderRotationsSetting = preparedRenderRotations;
        mode = preparedMode;
        serverSync = preparedServerSync;
    }

    
    public static void appendBootstrapSettings(SilentAim Module) {
        CatalogSettings.append(Module, mode);
        CatalogSettings.append(Module, serverSync);
        CatalogSettings.append(Module, renderRotationsSetting);
    }
}
