package com.blamejared.clumps.client.runtime;

import com.blamejared.clumps.client.modules.Module;
import com.blamejared.clumps.client.modules.ModuleInfo;
import org.mozilla.javascript.Function;

public class ScriptFeature extends Module {
    private final ScriptDefinition definition;
    private final ScriptEngine engine;
    private final ScriptCallbacks callbacks;

    
    public ScriptFeature(ScriptDefinition definition, ScriptEngine engine) {
        super(new ModuleInfo(definition.name(), definition.description(), definition.moduleCategory(), definition.key()));
        this.definition = definition;
        this.engine = engine;
        callbacks = new ScriptCallbacks(engine, definition);
    }

    
    @Override
    public void initSettings() {
        settings.clear();
        settings.addAll(definition.settings());
    }

    
    @Override
    public void onEnable() {
        super.onEnable();
        callbacks.register();
        invokeHook("enable", definition.onEnable());
    }

    
    @Override
    public void onDisable() {
        invokeHook("disable", definition.onDisable());
        callbacks.unregister();
        super.onDisable();
    }

    
    public String getScriptId() { return definition.id(); }

    private void invokeHook(String name, Function hook) {
        if (hook == null) return;
        try {
            engine.call(hook, definition.scope(), definition.scriptObject(), new Object[0]);
        } catch (Exception exception) {
            ScriptErrorReporter.report(ScriptEngine.LOGGER, definition.id(), "'" + name + "' hook", exception);
        }
    }
}
