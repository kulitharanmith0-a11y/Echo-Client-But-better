package dev.dov.hack.echo.client.runtime;

import dev.dov.hack.echo.client.modules.ModuleCategory;
import dev.dov.hack.echo.client.keyboard.InputKey;
import dev.dov.hack.echo.client.settings.Setting;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public record ScriptDefinition(
    String id, String name, String description, ModuleCategory moduleCategory, InputKey key, List<Setting> settings, Map<String, Function> events, Function onEnable, Function onDisable, Scriptable scope,
    Scriptable scriptObject
) {
}
