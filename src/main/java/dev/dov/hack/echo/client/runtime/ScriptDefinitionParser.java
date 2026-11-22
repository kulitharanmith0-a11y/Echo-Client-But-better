package dev.dov.hack.echo.client.runtime;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.runtime.api.SettingApi;
import dev.dov.hack.echo.client.settings.Setting;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

public final class ScriptDefinitionParser {
    private ScriptDefinitionParser() {
    }

    public static ScriptDefinition parse(Scriptable scope, String id) {
        NativeObject script = readScript(scope, id);
        if (script == null) {
            return null;
        }
        ModuleCategory moduleCategory = parseCategory(id, ScriptProperties.string(script, "ModuleCategory", "UTILITY"));
        if (moduleCategory == null) {
            return null;
        }
        Scriptable events = ScriptProperties.object(script, "events");
        return new ScriptDefinition(
            id, ScriptProperties.string(script, "name", id), ScriptProperties.string(script, "description", ""), moduleCategory, InputKey.parse(ScriptProperties.string(script, "key", "")),
            readSettings(ScriptProperties.object(script, "settings")), readEvents(events, id), ScriptProperties.function(events, "enable"), ScriptProperties.function(events, "disable"), scope,
            script
        );
    }

    private static NativeObject readScript(Scriptable scope, String id) {
        Object value = scope.get("script", scope);
        if (value instanceof NativeObject script) {
            return script;
        }
        ScriptErrorReporter.report(ScriptEngine.LOGGER, id, "must define a 'script' object");
        return null;
    }

    private static List<Setting> readSettings(Scriptable object) {
        List<Setting> settings = new ArrayList<>();
        if (object == null) {
            return settings;
        }
        for (Object id : object.getIds()) {
            Object value = ScriptProperties.property(object, id);
            if (value instanceof SettingApi wrapper) {
                settings.add(wrapper.getSetting());
            }
        }
        return settings;
    }

    private static Map<String, Function> readEvents(Scriptable events, String id) {
        Map<String, Function> callbacks = new LinkedHashMap<>();
        if (events == null) {
            return callbacks;
        }
        for (Object eventId : events.getIds()) {
            String name = String.valueOf(eventId);
            Object value = ScriptProperties.property(events, eventId);
            if (value instanceof Function callback && ScriptCallbacks.eventNames().contains(name)) {
                callbacks.put(name, callback);
            }
        }
        return callbacks;
    }

    private static ModuleCategory parseCategory(String id, String name) {
        return switch (name.toUpperCase()) {
            case "COMBAT" -> ModuleCategory.COMBAT;
            case "MOVEMENT" -> ModuleCategory.MOVEMENT;
            case "RENDER" -> ModuleCategory.RENDER;
            case "WORLD" -> ModuleCategory.WORLD;
            case "UTILITY" -> ModuleCategory.UTILITY;
            default -> {
                ScriptErrorReporter.report(ScriptEngine.LOGGER, id, "unknown ModuleCategory '" + name + "'");
                yield null;
            }
        };
    }
}
