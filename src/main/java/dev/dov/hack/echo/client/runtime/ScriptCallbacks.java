package dev.dov.hack.echo.client.runtime;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.event.EventBus;
import dev.dov.hack.echo.client.event.input.KeyboardEvent;
import dev.dov.hack.echo.client.event.interaction.AttackAttemptEvent;
import dev.dov.hack.echo.client.event.interaction.HandSwingEvent;
import dev.dov.hack.echo.client.event.lifecycle.ClientTickEvent;
import dev.dov.hack.echo.client.event.lifecycle.PlayerTickEvent;
import dev.dov.hack.echo.client.event.movement.MovementPacketEvent;
import dev.dov.hack.echo.client.event.network.PacketReceiveEvent;
import dev.dov.hack.echo.client.event.network.PacketSendEvent;
import dev.dov.hack.echo.client.event.render.HudRenderEvent;
import dev.dov.hack.echo.client.event.render.WorldRenderEvent;
import dev.dov.hack.echo.client.hud.HudSurfacePreview;
import dev.dov.hack.echo.client.runtime.api.AttackEventView;
import dev.dov.hack.echo.client.runtime.api.HandSwingEventView;
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
