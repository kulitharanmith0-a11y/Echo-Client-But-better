package com.blamejared.clumps.client.runtime;

import com.blamejared.clumps.client.modules.ModuleCategory;
import com.blamejared.clumps.client.keyboard.InputKey;
import com.blamejared.clumps.client.settings.Setting;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public record ScriptDefinition(
    String id, String name, String description, ModuleCategory moduleCategory, InputKey key, List<Setting> settings, Map<String, Function> events, Function onEnable, Function onDisable, Scriptable scope,
    Scriptable scriptObject
) {
}
