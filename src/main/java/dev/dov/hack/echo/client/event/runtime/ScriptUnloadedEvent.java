package dev.dov.hack.echo.client.event.runtime;

import dev.dov.hack.echo.client.event.Event;
import dev.dov.hack.echo.client.runtime.ScriptFeature;

public class ScriptUnloadedEvent extends Event {
    private final ScriptFeature Module;
    private final String scriptName;

    public ScriptUnloadedEvent(ScriptFeature Module, String scriptName) {
        this.Module = Module;
        this.scriptName = scriptName;
    }

    public ScriptFeature getFeature() { return Module; }

    
    public String getScriptName() { return scriptName; }
}
