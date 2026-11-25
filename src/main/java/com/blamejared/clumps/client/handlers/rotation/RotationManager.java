package com.blamejared.clumps.client.handlers.rotation;

import com.blamejared.clumps.client.ClumpsClient;
import com.blamejared.clumps.client.event.Event;

import com.blamejared.clumps.client.handlers.MouseDelta;
import com.blamejared.clumps.client.diagnostics.DevDiagnostics;
import com.blamejared.clumps.client.event.EventPriority;
import com.blamejared.clumps.client.event.Subscribe;
import com.blamejared.clumps.client.event.input.HandleInputEvent;
import com.blamejared.clumps.client.event.input.MouseMovementEvent;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.lifecycle.WorldTickContextEvent;
import com.blamejared.clumps.client.event.movement.MovementPacketEvent;
import com.blamejared.clumps.client.event.movement.RotationEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.event.network.PacketSendEvent;
import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.internals.SilentAim;
import com.blamejared.clumps.client.utils.Rotation;
import com.blamejared.clumps.client.utils.targeting.AimPointSettings;
import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.blamejared.clumps.client.handlers.ClientHandler;
import com.blamejared.clumps.client.handlers.MouseSensitivity;

public final class RotationManager extends ClientHandler {
    private static final float VISUAL_SMOOTHING = 0.55F;
    private static final float RETURN_TOLERANCE = 1.0F;
    private static final int OVERRIDE_VISUAL_TICKS = 3;
    private final MouseSensitivity sensitivity = new MouseSensitivity();
    private Rotation currentRotation = new Rotation(0.0F, 0.0F);
    private Rotation previousRotation = currentRotation;
    private RotationRequest activeRequest;
    private RotationScope activeScope;
    private Rotation savedRotation;
    private boolean savedSilentActive;
    private boolean scopeUsed;
    private boolean silentActive;
    private int updateTick;
    private int clientTickCounter;
    private int lastSteppedClientTick = -1;
    private int pendingMouseX;
    private int pendingMouseY;
    private float mouseRemainderX;
    private float mouseRemainderY;
    private float serverYaw;
    private float serverPitch;
    private float serverYawDelta;
    private float consumedYawDelta;
    private boolean serverRotationChanged;
    private boolean visualActive;
    private float visualYaw;
    private float previousVisualYaw;
    private float visualPitch;
    private float previousVisualPitch;
    private Rotation heldVisualRotation;
    private int visualHoldTicks;
    private boolean snapVisuals;
    private boolean hasServerRotation;
    private Rotation movementSyncRotation;
    private boolean movementSyncActive;
    private boolean movementSyncForwardAllowed = true;
    private Object trackedPlayer;
    private Object trackedWorld;

    
    public Optional<RotationRequest> request(Module owner, RotationPriority priority) {
        if (!hasPlayerAndWorld()) return Optional.empty();
        if (activeScope != null) {
            if (DevDiagnostics.ENABLED && owner != null) {
                DevDiagnostics.recordDeny(owner.getName().toString(), "ROT", "activeScope_override");
            }
            return Optional.empty();
        }
        synchronizeContext();
        if (activeRequest != null && activeRequest.isActive()) {
            if (activeRequest.owner() == owner) return Optional.of(activeRequest);
            if (!priority.higherThan(activeRequest.priority())) {
                if (DevDiagnostics.ENABLED && owner != null) {
                    String holder = activeRequest.owner() != null ? activeRequest.owner().getName().toString() : "?";
                    DevDiagnostics.recordDeny(owner.getName().toString(), "ROT",
                            "prio=" + priority + "<=" + activeRequest.priority() + " holder=" + holder);
                }
                return Optional.empty();
            }
            if (DevDiagnostics.ENABLED && owner != null) {
                String prevHolder = activeRequest.owner() != null ? activeRequest.owner().getName().toString() : "?";
                DevDiagnostics.logRateLimited("rot-preempt:" + owner.getName() + ":" + prevHolder, 250L,
                        owner.getName().toString(), "ROT_PREEMPT", "over=" + prevHolder + " prio=" + priority);
            }
            activeRequest.invalidate();
            resetMouseInjection();
        }
        activeRequest = new RotationRequest(this, owner, priority);
        return Optional.of(activeRequest);
    }

    
    public RotationState snapshot() {
        Rotation player = playerRotation();
        RotationRequest request = activeRequest != null && activeRequest.isActive() ? activeRequest : null;
        AimState state = activeScope != null ? AimState.OVERRIDE
                : request == null ? AimState.IDLE : request.returning() ? AimState.RETURNING : AimState.AIMING;
        return new RotationState(player, silentActive ? currentRotation : player, silentActive ? previousRotation : player, silentActive, request == null ? null : request.owner(), request == null ? null : request.target(), state);
    }

    
    public boolean isRotationReady() {
        return !serverRotationChanged || serverYawDelta <= 2.0F
                || Math.abs(serverYawDelta - consumedYawDelta) >= 1.0E-4F;
    }

    
    public void acknowledgeRotation() {
        if (serverRotationChanged) consumedYawDelta = serverYawDelta;
        serverRotationChanged = false;
    }

    
    public Rotation serverRotation() {
        return hasServerRotation ? new Rotation(serverYaw, serverPitch) : null;
    }

    
    public boolean aim(Module owner, RotationPriority priority, Vec3d point, RotationSettings settings) {
        Optional<RotationRequest> acquired = request(owner, priority);
        return acquired.isPresent() && acquired.get().aim(point, settings);
    }

    
    public boolean aim(Module owner, RotationPriority priority, LivingEntity target, AimPointSettings targeting, RotationSettings settings) {
        Optional<RotationRequest> acquired = request(owner, priority);
        return acquired.isPresent() && acquired.get().aim(target, targeting, settings);
    }

    public boolean isSilentRotationActive() { return silentActive; }
    public Rotation effectiveRotation() { return silentActive ? currentRotation : playerRotation(); }

    public Rotation beginMovementSync() {
        if (!hasPlayerAndWorld() || !silentActive) {
            clearMovementSync();
            return null;
        }
        movementSyncRotation = currentRotation;
        movementSyncActive = true;
        movementSyncForwardAllowed = true;
        return movementSyncRotation;
    }

    public void clearMovementSync() {
        movementSyncActive = false;
        movementSyncRotation = null;
        movementSyncForwardAllowed = true;
    }

    public boolean isMovementSyncActive() {
        return movementSyncActive && movementSyncRotation != null;
    }

    public Rotation movementRotation() {
        return isMovementSyncActive() ? movementSyncRotation : effectiveRotation();
    }

    public void setMovementSyncForwardAllowed(boolean allowed) {
        movementSyncForwardAllowed = allowed;
    }

    public boolean isMovementSprintBlocked() {
        return isMovementSyncActive() && !movementSyncForwardAllowed;
    }

    
    public boolean beginReturn(Module owner) {
        if (!isOwnedBy(owner)) return false;
        release(owner);
        return isOwnedBy(owner);
    }

    public boolean isOwnedBy(Module owner) {
        return activeRequest != null && activeRequest.isActive() && activeRequest.owner() == owner;
    }

    public boolean canRequest(Module owner, RotationPriority priority) {
        if (activeScope != null) return false;
        return activeRequest == null || !activeRequest.isActive() || activeRequest.owner() == owner
                || priority.higherThan(activeRequest.priority());
    }

    public boolean isAiming() {
        return activeRequest != null && activeRequest.isActive() && !activeRequest.returning();
    }

    public void release(Module owner) {
        if (isOwnedBy(owner)) release(activeRequest);
    }

    public void cancel(Module owner) {
        if (isOwnedBy(owner)) clearRequest();
    }

    
    public Optional<RotationScope> override(Module owner, RotationPriority priority, Rotation rotation) {
        if (!hasPlayerAndWorld()) return Optional.empty();
        synchronizeContext();
        if (activeScope != null || (activeRequest != null && activeRequest.isActive()
                && !priority.higherThan(activeRequest.priority()))) return Optional.empty();
        savedRotation = currentRotation;
        savedSilentActive = silentActive;
        activeScope = new RotationScope(this);
        prepareSilentRotation();
        applyQuantized(rotation.relativeTo(currentRotation));
        return Optional.of(activeScope);
    }

    
    public void reset() {
        if (activeRequest != null) activeRequest.invalidate();
        activeRequest = null;
        if (activeScope != null) activeScope.invalidate();
        activeScope = null;
        silentActive = false;
        savedRotation = null;
        savedSilentActive = false;
        scopeUsed = false;
        sensitivity.reset();
        visualActive = false;
        previousVisualYaw = 0.0F;
        visualHoldTicks = 0;
        clientTickCounter = 0;
        lastSteppedClientTick = -1;
        clearMovementSync();
        resetMouseInjection();
        resetServerTracking(playerRotation());
    }

    boolean isCurrent(RotationRequest request) { return activeRequest == request && request != null; }

    boolean aimAt(RotationRequest request, Vec3d point, RotationSettings settings) {
        if (!canUpdate(request)) return false;
        updateTarget(request, point, null, settings);
        return true;
    }

    boolean aimAt(RotationRequest request, LivingEntity entity, AimPointSettings targeting, RotationSettings settings) {
        if (!canUpdate(request)) return false;
        Vec3d point = request.targetPoint(entity, targeting, targetTickDelta(settings)).orElse(null);
        if (point == null) return false;
        updateTarget(request, point, entity, settings);
        return true;
    }

    private boolean canUpdate(RotationRequest request) {
        if (!hasPlayerAndWorld()) return false;
        synchronizeContext();
        return request == activeRequest && request.isActive();
    }

    private void updateTarget(RotationRequest request, Vec3d point, LivingEntity entity, RotationSettings settings) {
        if (settings.visibility() == RotationVisibility.VISIBLE && silentActive) stopSilentRotation();
        Rotation current = settings.visibility() == RotationVisibility.SILENT && silentActive ? currentRotation : playerRotation();
        if (request.settings() != null && request.settings().visibility() != settings.visibility()) resetMouseInjection();
        request.settings(settings);
        request.target(Rotation.lookingAt(CLIENT.player.getCameraPosVec(targetTickDelta(settings)), point).relativeTo(current), entity);
        request.refreshedTick(updateTick);
        request.refreshedClientTick(clientTickCounter);
        request.returning(false);
    }

    void refresh(RotationRequest request) {
        if (request == activeRequest && request.isActive()) {
            request.refreshedTick(updateTick);
            request.refreshedClientTick(clientTickCounter);
        }
    }

    void release(RotationRequest request) {
        if (request != activeRequest || !request.isActive()) return;
        if (request.settings() == null || request.settings().visibility() == RotationVisibility.VISIBLE) {
            clearRequest();
            return;
        }
        request.returning(true);
        request.refreshedTick(updateTick);
        request.refreshedClientTick(clientTickCounter);
    }

    void markScopeUsed(RotationScope scope) {
        if (scope == activeScope) scopeUsed = true;
    }

    void closeScope(RotationScope scope) {
        if (scope == activeScope) finishScope(false);
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void onMouseMovement(MouseMovementEvent.Pre event) {
        if (activeRequest == null || !activeRequest.isActive() || activeRequest.returning()
                || activeRequest.settings() == null || activeRequest.settings().visibility() != RotationVisibility.VISIBLE) return;
        if (activeRequest.settings().transition() == AimTransition.SNAP) {
            event.deltaX = pendingMouseX;
            event.deltaY = pendingMouseY;
        } else {
            event.deltaX += pendingMouseX;
            event.deltaY += pendingMouseY;
        }
        pendingMouseX = 0;
        pendingMouseY = 0;
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void afterMouseMovement(MouseMovementEvent.Post event) {
        if (!hasPlayerAndWorld()) { reset(); return; }
        synchronizeContext();
        updateTick++;
        if (activeScope != null || activeRequest == null || !activeRequest.isActive()) return;
        releaseIfStale();
        updateRotation();
        lastSteppedClientTick = clientTickCounter;
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void beforeInputEvents(HandleInputEvent.Pre event) {
        if (!hasPlayerAndWorld()) return;
        synchronizeContext();
        if (activeScope == null && activeRequest != null && activeRequest.isActive()) {
            releaseIfStale();
            if (lastSteppedClientTick != clientTickCounter) {
                updateRotation();
                lastSteppedClientTick = clientTickCounter;
            }
        }
        refreshCrosshairTarget();
    }

    private void releaseIfStale() {
        if (!activeRequest.returning()
                && activeRequest.refreshedTick() < updateTick - 1
                && activeRequest.refreshedClientTick() < clientTickCounter - 1) {
            release(activeRequest);
        }
    }

    private void refreshCrosshairTarget() {
        if (silentActive && CLIENT.gameRenderer != null
                && CLIENT.getCameraEntity() != null && CLIENT.interactionManager != null) {
            CLIENT.gameRenderer.updateCrosshairTarget(1.0F);
        }
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void beforeMovementPacket(MovementPacketEvent.Pre event) {
        if (movementSyncActive && movementSyncRotation != null) {
            event.setYaw(movementSyncRotation.yaw());
            event.setPitch(movementSyncRotation.pitch());
        } else if (silentActive) {
            event.setYaw(currentRotation.yaw());
            event.setPitch(currentRotation.pitch());
        }
        recordServerRotation(new Rotation((float) event.getYaw(), (float) event.getPitch()));
    }

    @Subscribe
    private void afterMovementPacket(MovementPacketEvent.Post event) {
        clearMovementSync();
        if (scopeUsed && activeScope != null) finishScope(true);
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void onPacketSent(PacketSendEvent event) {
        if (event.cancelled) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof TeleportConfirmC2SPacket) {
            clearMovementSync();
            if (CLIENT.player != null) {
                Rotation synced = playerRotation();
                resetServerTracking(synced);
                recordServerRotation(synced);
                if (silentActive) {
                    updateCurrent(synced);
                    sensitivity.reset();
                }
            }
            return;
        }
        if (packet instanceof PlayerMoveC2SPacket move && move.changesLook()) {
            recordServerRotation(new Rotation(move.getYaw(serverYaw), move.getPitch(serverPitch)));
        }
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        handleServerPacket(event.getPacket());
    }

    private void handleServerPacket(Packet<?> packet) {
        if (packet instanceof BundleS2CPacket bundle) {
            for (Packet<?> inner : bundle.getPackets()) {
                handleServerPacket(inner);
            }
            return;
        }
        if (packet instanceof GameJoinS2CPacket || packet instanceof PlayerRespawnS2CPacket) {
            CLIENT.execute(this::reset);
        } else if (packet instanceof PlayerPositionLookS2CPacket posPacket) {
            var relatives = posPacket.relatives();
            boolean relYaw = relatives.contains(PositionFlag.Y_ROT);
            boolean relPitch = relatives.contains(PositionFlag.X_ROT);
            float rawYaw = posPacket.change().yaw();
            float rawPitch = posPacket.change().pitch();
            CLIENT.execute(() -> {
                float newYaw = relYaw ? serverYaw + rawYaw : rawYaw;
                float newPitch = relPitch ? serverPitch + rawPitch : rawPitch;
                if (Float.isFinite(newYaw)) {
                    serverYaw = newYaw;
                }
                if (Float.isFinite(newPitch)) {
                    serverPitch = PitchLimits.clampDegrees(newPitch);
                }
                hasServerRotation = true;
                serverRotationChanged = false;
                consumedYawDelta = 0.0F;
                clearMovementSync();
                if (silentActive) {
                    updateCurrent(new Rotation(serverYaw, serverPitch));
                    sensitivity.reset();
                }
            });
        }
    }

    @Subscribe
    private void onWorldChange(WorldTickContextEvent event) {
        reset();
    }

    @Subscribe
    private void onRotation(RotationEvent event) {
        switch (event.getContext()) {
            case MOVEMENT -> { return; }
            case HEAD, BODY -> {
                if (visualActive && SilentAim.renderRotations()) {
                    float resolvedYaw = event.getYaw() + MathHelper.wrapDegrees(visualYaw - event.getYaw());
                    event.setYaw(resolvedYaw);
                    event.setPitch(visualPitch);
                }
            }
            case RENDER_PITCH -> {
                if (visualActive && SilentAim.renderRotations()) {
                    event.setPitch(MathHelper.clamp(MathHelper.lerp(event.getTickDelta(), previousVisualPitch, visualPitch), -90.0F, 90.0F));
                }
            }
            default -> {
                if (silentActive) {
                    event.setYaw(currentRotation.yaw());
                    event.setPitch(currentRotation.pitch());
                }
            }
        }
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void onTick(ClientTickEvent event) {
        clientTickCounter++;
        clearMovementSync();
        if (!hasPlayerAndWorld()) { visualActive = false; return; }
        boolean holding = visualHoldTicks > 0;
        Rotation target = holding ? heldVisualRotation : effectiveRotation();
        if (holding) visualHoldTicks--;
        if (activeRequest != null && activeRequest.settings() != null) snapVisuals = activeRequest.settings().transition() == AimTransition.SNAP;
        if (!visualActive) {
            if (!silentActive && !holding) return;
            visualYaw = playerRotation().yaw();
            previousVisualYaw = visualYaw;
            visualPitch = playerRotation().pitch();
            previousVisualPitch = visualPitch;
            visualActive = true;
        }
        float yawDelta = MathHelper.wrapDegrees(target.yaw() - visualYaw);
        float pitchDelta = target.pitch() - visualPitch;
        previousVisualYaw = visualYaw;
        previousVisualPitch = visualPitch;
        float blend = snapVisuals ? 1.0F : VISUAL_SMOOTHING;
        visualYaw += yawDelta * blend;
        visualPitch += pitchDelta * blend;
        if (!silentActive && !holding && Math.abs(yawDelta) < RETURN_TOLERANCE && Math.abs(pitchDelta) < RETURN_TOLERANCE) visualActive = false;
    }

    private void updateRotation() {
        RotationSettings settings = activeRequest.settings();
        Rotation current = settings != null && settings.visibility() == RotationVisibility.SILENT && silentActive ? currentRotation : playerRotation();
        Rotation desired = activeRequest.returning() ? playerRotation().relativeTo(current) : activeRequest.desiredRotation();
        if (settings == null || desired == null) return;
        float elapsed = activeRequest.elapsedSeconds(System.nanoTime());
        RotationSettings effectiveSettings = activeRequest.returning()
                ? new RotationSettings(
                        RotationVisibility.SILENT,
                        settings.transition() == AimTransition.SNAP ? AimTransition.SNAP : AimTransition.EASED,
                        Math.max(settings.maxYawStep(), 220.0F),
                        Math.max(settings.maxPitchStep(), 180.0F))
                : settings;
        Rotation stepped = activeRequest.transition(current, desired, effectiveSettings, elapsed);
        if (settings.visibility() == RotationVisibility.SILENT || activeRequest.returning()) {
            applyQuantized(stepped);
        } else {
            boolean fineScale = CLIENT.options.getPerspective().isFirstPerson() && CLIENT.player.isUsingSpyglass();
            float step = MouseSensitivity.angularStep(fineScale);
            if (step > 1.0E-5F) {
                boolean invertX = CLIENT.options.getInvertMouseX().getValue();
                boolean invertY = CLIENT.options.getInvertMouseY().getValue();
                float pendingYaw = pendingMouseX * (invertX ? -step : step);
                float pendingPitch = pendingMouseY * (invertY ? -step : step);
                float mouseX = (MathHelper.wrapDegrees(stepped.yaw() - current.yaw()) - pendingYaw) / step + mouseRemainderX;
                float mouseY = (stepped.pitch() - current.pitch() - pendingPitch) / step + mouseRemainderY;
                if (Math.abs(MathHelper.wrapDegrees(desired.yaw() - current.yaw())) < step * 0.5F) mouseX = 0.0F;
                if (Math.abs(desired.pitch() - current.pitch()) < step * 0.5F) mouseY = 0.0F;
                int wholeX = Math.round(mouseX);
                int wholeY = Math.round(mouseY);
                mouseRemainderX = mouseX - wholeX;
                mouseRemainderY = mouseY - wholeY;
                pendingMouseX += invertX ? -wholeX : wholeX;
                pendingMouseY += invertY ? -wholeY : wholeY;
            }
        }
        if (activeRequest.returning() && Math.abs(MathHelper.wrapDegrees(desired.yaw() - effectiveRotation().yaw())) < RETURN_TOLERANCE
                && Math.abs(desired.pitch() - effectiveRotation().pitch()) < RETURN_TOLERANCE) clearRequest();
    }

    private void applyQuantized(Rotation requested) {
        prepareSilentRotation();
        silentActive = true;
        float step = MouseSensitivity.angularStep(false);
        MouseDelta counts = sensitivity.quantize(MathHelper.wrapDegrees(requested.yaw() - currentRotation.yaw()), requested.pitch() - currentRotation.pitch(), false, false, false);
        if (counts.horizontalCounts() == 0 && counts.verticalCounts() == 0) return;
        updateCurrent(new Rotation(currentRotation.yaw() + counts.horizontalCounts() * step, currentRotation.pitch() + counts.verticalCounts() * step));
        refreshCrosshairTarget();
    }

    private void clearRequest() {
        if (activeRequest != null) activeRequest.invalidate();
        activeRequest = null;
        resetMouseInjection();
        if (silentActive) stopSilentRotation();
    }

    private void finishScope(boolean retainVisuals) {
        if (activeScope == null) return;
        activeScope.invalidate();
        activeScope = null;
        scopeUsed = false;
        if (retainVisuals) {
            heldVisualRotation = currentRotation;
            visualHoldTicks = OVERRIDE_VISUAL_TICKS;
        }
        if (savedSilentActive && savedRotation != null) {
            updateCurrent(savedRotation);
            silentActive = true;
        } else {
            stopSilentRotation();
        }
        if (activeRequest != null) activeRequest.resetClock();
        savedRotation = null;
        savedSilentActive = false;
    }

    private void stopSilentRotation() {
        if (!silentActive || CLIENT.player == null) { silentActive = false; return; }
        float playerYaw = CLIENT.player.getYaw();
        float nearestYaw = currentRotation.yaw() + MathHelper.wrapDegrees(playerYaw - currentRotation.yaw());
        float offset = nearestYaw - playerYaw;
        if (offset != 0.0F) {
            CLIENT.player.lastYaw += offset;
            CLIENT.player.renderYaw += offset;
            CLIENT.player.lastRenderYaw += offset;
            CLIENT.player.bodyYaw += offset;
            CLIENT.player.lastBodyYaw += offset;
            CLIENT.player.headYaw += offset;
            CLIENT.player.lastHeadYaw += offset;
            CLIENT.player.setYaw(nearestYaw);
            if (visualActive) {
                visualYaw += offset;
                previousVisualYaw += offset;
            }
        }
        updateCurrent(new Rotation(nearestYaw, CLIENT.player.getPitch()));
        silentActive = false;
    }

    private void prepareSilentRotation() {
        if (silentActive || CLIENT.player == null) return;
        Rotation start = hasServerRotation
                ? new Rotation(serverYaw, PitchLimits.clampDegrees(serverPitch))
                : playerRotation();
        updateCurrent(start);
        sensitivity.reset();
    }

    private void resetMouseInjection() {
        pendingMouseX = 0;
        pendingMouseY = 0;
        mouseRemainderX = 0.0F;
        mouseRemainderY = 0.0F;
    }

    private void resetServerTracking(Rotation rotation) {
        serverYaw = rotation.yaw();
        serverPitch = rotation.pitch();
        consumedYawDelta = 0.0F;
        serverRotationChanged = false;
        hasServerRotation = false;
    }

    private void recordServerRotation(Rotation rotation) {
        if (rotation.yaw() != serverYaw || rotation.pitch() != serverPitch) {
            serverYawDelta = Math.abs(MathHelper.wrapDegrees(rotation.yaw() - serverYaw));
            serverRotationChanged = true;
        }
        serverYaw = rotation.yaw();
        serverPitch = rotation.pitch();
        hasServerRotation = true;
    }

    private void updateCurrent(Rotation rotation) {
        previousRotation = currentRotation;
        currentRotation = new Rotation(rotation.yaw(), PitchLimits.clampDegrees(rotation.pitch()));
    }

    private Rotation playerRotation() {
        return CLIENT.player == null ? new Rotation(0.0F, 0.0F) : new Rotation(CLIENT.player.getYaw(), CLIENT.player.getPitch());
    }

    private boolean hasPlayerAndWorld() { return CLIENT.player != null && CLIENT.world != null; }

    private float targetTickDelta(RotationSettings settings) {
        return settings.visibility() == RotationVisibility.SILENT ? 1.0F : CLIENT.getRenderTickCounter().getTickProgress(false);
    }

    private void synchronizeContext() {
        if (trackedPlayer != CLIENT.player || trackedWorld != CLIENT.world) {
            trackedPlayer = CLIENT.player;
            trackedWorld = CLIENT.world;
            reset();
        }
        trackedPlayer = CLIENT.player;
        trackedWorld = CLIENT.world;
    }
}
