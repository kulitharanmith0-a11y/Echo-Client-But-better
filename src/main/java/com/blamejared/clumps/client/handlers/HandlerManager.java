package com.blamejared.clumps.client.handlers;

import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.handlers.action.ActionCoordinator;
import com.blamejared.clumps.client.handlers.config.AutoConfigManager;
import com.blamejared.clumps.client.handlers.input.InputSimulator;
import com.blamejared.clumps.client.handlers.rotation.RotationManager;
import java.util.ArrayList;

public final class HandlerManager {
    private final ArrayList<ClientHandler> handlers = new ArrayList<>();
    private final RotationManager rotationManager = new RotationManager();
    private final ActionCoordinator actionCoordinator = new ActionCoordinator();
    private final InputSimulator inputSimulator = new InputSimulator();
    private final AutoConfigManager autoConfigManager = new AutoConfigManager();

    public RotationManager getRotationManager() {
        return rotationManager;
    }

    public ActionCoordinator getActionCoordinator() {
        return actionCoordinator;
    }

    public InputSimulator getInputSimulator() {
        return inputSimulator;
    }

    public AutoConfigManager getAutoConfigManager() {
        return autoConfigManager;
    }

    public void initialize() {
        if (!handlers.isEmpty()) return;
        handlers.add(rotationManager);
        handlers.add(actionCoordinator);
        handlers.add(inputSimulator);
        handlers.add(autoConfigManager);
        handlers.add(new FeatureBindHandler());
        handlers.add(new AttackCooldownTracker());
        handlers.add(new SprintOverrideHandler());
        handlers.add(new DeferredActionQueue());
        handlers.add(new BlockingTicksTracker());
        handlers.add(new ServerFallDistanceTracker());
        handlers.add(new EntityPositionTracker());
        handlers.add(new LatencyTracker());
        for (ClientHandler handler : handlers) EventBus.register(handler);
    }

    public void shutdown() {
        for (ClientHandler handler : handlers) EventBus.unregister(handler);
        DeferredActionQueue.clear();
        autoConfigManager.reset();
        inputSimulator.reset();
        actionCoordinator.reset();
        rotationManager.reset();
        handlers.clear();
    }
}
