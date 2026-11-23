package com.blamejared.clumps.client.runtime;

import com.blamejared.clumps.client.event.Event;
import com.blamejared.clumps.client.event.EventBus;
import com.blamejared.clumps.client.event.input.KeyboardEvent;
import com.blamejared.clumps.client.event.interaction.AttackAttemptEvent;
import com.blamejared.clumps.client.event.interaction.HandSwingEvent;
import com.blamejared.clumps.client.event.lifecycle.ClientTickEvent;
import com.blamejared.clumps.client.event.lifecycle.PlayerTickEvent;
import com.blamejared.clumps.client.event.movement.MovementPacketEvent;
import com.blamejared.clumps.client.event.network.PacketReceiveEvent;
import com.blamejared.clumps.client.event.network.PacketSendEvent;
import com.blamejared.clumps.client.event.render.HudRenderEvent;
import com.blamejared.clumps.client.event.render.WorldRenderEvent;
import com.blamejared.clumps.client.hud.HudSurfacePreview;
import com.blamejared.clumps.client.runtime.api.AttackEventView;
import com.blamejared.clumps.client.runtime.api.HandSwingEventView;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public record ScriptCallbacks(ScriptEngine engine, ScriptDefinition definition) {
    private static final Map<String, Class<? extends Event>> EVENT_TYPES = Map.ofEntries(
        Map.entry("tick", ClientTickEvent.class), Map.entry("playerTick", PlayerTickEvent.class), Map.entry("render2d", HudRenderEvent.class), Map.entry("render3d", WorldRenderEvent.class), Map.entry("packetSend", PacketSendEvent.class), Map.entry("packetReceive", PacketReceiveEvent.class), Map.entry("move", MovementPacketEvent.class), Map.entry("key", KeyboardEvent.class), Map.entry("attack", AttackAttemptEvent.class), Map.entry("swingHand", HandSwingEvent.class)
    );
    private static final Map<Class<? extends Event>, Function<Event, Object>> EVENT_ADAPTERS = Map.of(
        AttackAttemptEvent.class, event -> new AttackEventView((AttackAttemptEvent) event), HandSwingEvent.class, event -> new HandSwingEventView((HandSwingEvent) event), HudRenderEvent.class, event -> new HudSurfacePreview((HudRenderEvent) event)
    );

    
    public static Set<String> eventNames() { return EVENT_TYPES.keySet(); }

    
    public void register() {
        definition.events().forEach((name, callback) -> {
            Class<? extends Event> type = EVENT_TYPES.get(name);
            Function<Event, Object> adapter = EVENT_ADAPTERS.get(type);
            EventBus.registerCallback(type, this, event -> invoke(callback, adapter, event));
        });
    }

    
    public void unregister() { EventBus.unregisterCallbacks(this); }

    private void invoke(org.mozilla.javascript.Function callback, Function<Event, Object> adapter, Event event) {
        try {
            Object argument = adapter == null ? event : adapter.apply(event);
            engine.call(callback, definition.scope(), definition.scriptObject(), new Object[]{argument});
        } catch (Exception exception) {
            ScriptErrorReporter.report(ScriptEngine.LOGGER, definition.id(), "event handler", exception);
        }
    }
}
